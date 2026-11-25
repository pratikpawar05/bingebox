package com.bingebox.encodingservice.service;

import com.bingebox.encodingservice.event.VideoEncodedEvent;
import com.bingebox.encodingservice.event.VideoUploadedEvent;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class EncodingService {

    private final MinioClient minioClient;

    private final KafkaTemplate<String, VideoEncodedEvent> kafkaTemplate;

    @Value("${ffmpeg.path}")
    private String ffmpegPath;

    @Value("{minio.bucket-name}")
    private String bucketName;

    @Value("{encoding.base-path}")
    private String basePath;

    private static final String VIDEO_ENCODED_TOPIC = "video.encoded";


    // Video qualities to encode
    // Format: Resolution, bitrate, height
    private static final List<int[]> VIDEO_QUALITIES = Arrays.asList(
        new int[]{1920, 5000, 1080}, // 1080p - 5000k bitrate
        new int[]{1280, 2800, 720},
        new int[]{854, 1200, 480},
        new int[]{640, 800, 360}
    );

    /**
     * Main encoding pipeline
     *
     * Steps:
     * 1. Download the raw video from file storage
     * 2. Encode to multiple qualities using FFMPEG
     * 3. Generate HLS Video (.m3u8) for each quality
     * 4. Create Master Playlist
     * 5. Upload all encoded files back to S3/Minio
     * 6. Publish VideoEncodedEvent to kafka
     * @Param: VideoUploadedEvent
     */
    public void encodeVideo(VideoUploadedEvent event) {

        // Create a unique path to store the raw file
        String jobPath = basePath + "/" + event.getMovieId(); //eg: "basepath:{tmp/encoding}/movie_uuid/"

        String encodedPath = jobPath + "/encoded"; //eg: "basepath:{tmp/encoding}/movie_uuid/encoded/"

        try {
            Files.createDirectories(Paths.get(jobPath));
            Files.createDirectories(Paths.get(encodedPath));
            // 1. Download the raw video file from s3/minio storage
            String localPath = jobPath + "/raw_video.mp4"; //eg: "basepath:{tmp/encoding}/movie_uuid/raw_video.mp4"
            downloadVideo(event.getVideoKey(), localPath);
            // 2. Encode to multiple qualities using FFMPEG
            // &&
            // 3. Generate HLS Video (.m3u8) for each quality
            for (int[] qualities : VIDEO_QUALITIES) {
                int width = qualities[0];
                int bitrate = qualities[1];
                int height = qualities[2];

                String qualityDir = encodedPath + height + "p"; //eg: "basepath:{tmp/encoding}/movie_uuid/encoded/1080p/"
                Files.createDirectories(Paths.get(qualityDir));

                encodeToHLS(localPath, qualityDir, width, bitrate, height);
            }
            // 4. Create Master Playlist
            String masterPlaylistPath = encodedPath + "/master.m3u8";  //eg: "basepath:{tmp/encoding}/movie_uuid/encoded/master.m3u8"
            generateMasterPlaylist(masterPlaylistPath);
            log.info("Master playlist generated");
            // 5. Upload all resource files to S3/Minio
            String encodedPrefix = "encoded/" + event.getMovieId() + "/";
            uploadEncodedFiles(encodedPath, encodedPrefix);
            log.info("All encoded files uploaded to s3/minio");
            // 6. Publish VideoEncodedEvent to kafka
            VideoEncodedEvent videoEncodedEvent = VideoEncodedEvent.builder()
                    .movieId(event.getMovieId())
//                    .hlsUrl()
                    .build();
            kafkaTemplate.send(VIDEO_ENCODED_TOPIC, videoEncodedEvent);



        } catch (Exception e) {

            VideoEncodedEvent failureEvent = VideoEncodedEvent.builder()
                    .movieId(event.getMovieId())
                    .errorMessage("Failed to encode the video")
                    .build();
            kafkaTemplate.send(VIDEO_ENCODED_TOPIC, failureEvent);

            throw new RuntimeException(e);
        } finally {
            //cleanup temp files
            cleanupTempFiles(jobPath);
        }
    }

    private void downloadVideo(String videoKey, String localVideoPath) throws Exception {
        DownloadObjectArgs downloadObjectArgs = DownloadObjectArgs.builder()
                .bucket(bucketName)
                .object(videoKey)
                .filename(localVideoPath)
                .overwrite(true)
                .build();

        minioClient.downloadObject(downloadObjectArgs);
    }

    /**
     * Encode raw video to HLS using FFmpeg
     *
     * FFmpeg command created
     * - It creates multiple .ts segment files (10 seconds each)
     * - A .m3u8 playlist file for quality shared
     *
     */
    private void encodeToHLS(String inpPath, String outDir, int width, int height, int bitrate) throws IOException, InterruptedException {
         String playlistPath = outDir + "/playlist.m3u8";
         String segmentPattern = outDir + "/segment_%3d.ts";
         // FFmpeg to convert video to .m3u8

        List<String> command = Arrays.asList(
                ffmpegPath,
                "-i", inpPath,                              // Input file
                "vf", "scale="+width+":"+height,            // Scale to resolution i.e. 1920 * 1080
                "-c:v", "libx264",                          // Video codec
                "-b:v", bitrate +"k",                       // video bitrate
                "-c:a", "aac",                              // Audio codec
                "-b:a", "128k",                             // Audio bitrate
                "-hls_time", "10",                          // 10 seconds chunks/segments
                "-hls_list_size", "0",                      // Keep all chunks/segments
                "-hls_segment_filename", segmentPattern,    // Segment naming
                "-f", "hls",                                // Output format: HLS
                playlistPath                                // Output path
           );

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw  new RuntimeException("FFMPEG encoding failed with exit code: " + exitCode);
        }
    }

    /**
     * Generate Master HLS playlist that references all quality playlists
     * This first file that will be received by video player
     * @param masterPlaylistPath
     * @throws IOException
     */
    public void generateMasterPlaylist(String masterPlaylistPath) throws IOException {
        StringBuilder master = new StringBuilder();
        master.append("#EXTM3U\n");
        master.append("EXT-X-VERSION:3\n\n");
        master.append("EXT-X-VERSION:3\n\n");

        for (int[] quality : VIDEO_QUALITIES) {
            int width = quality[0];
            int bitrate = quality[1];
            int height = quality[2];

            master.append("#EXT-X-STREAM-INF:BANDWIDTH=")
                    .append(bitrate * 1000)
                    .append(", RESOLUTION=").append(width).append("x").append("height")
                    .append(",CODECS=\"avc1.42e01e,mp4a.40.2\"\n")
                    .append(height).append("p/playlist.m3u8\n\n");
        }

        Files.writeString(Paths.get(masterPlaylistPath), master.toString());
    }

    public void uploadEncodedFiles(String encodedPath, String encodedPrefix) throws Exception {
        File directory = new File(encodedPath);
        uploadDirectory(directory, encodedPath, encodedPrefix);
    }

    private void uploadDirectory(File directory, String encodedPath, String encodedPrefix) throws Exception {
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                uploadDirectory(file, encodedPath, encodedPrefix);
            } else {
                String relativePath = file.getAbsolutePath()
                        .substring(encodedPath.length() + 1)
                        .replace("\\", "/");
                // Part 1: absolutePath = /tmp/encoding/movie_uuid/encoded/1080p/playlist.m3u8
                // Part 2: substring(encodedPath.length() + 1) = length of (/tmp/encoding/movie_uuid/encoded) + 1
                // O/P of Part 2 after getting remaining substring: 1080p/playlist.m3u8
                // Part 3: replace if any \\ with / O/P: 1080p/playlist.m3u8

                String fileKey = encodedPrefix + relativePath; // eg: bucketname/encoded/movie_uuid/1080p/playlist.m3u8

                String contentType = file.getName().endsWith(".m3u8")
                        ? "application/x-mpegURL" : "video/MP2T";
                UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder()
                        .bucket(bucketName) // Which bucket to store in
                        .object(fileKey)   // The unique key/name for this file
                        .filename(file.getAbsolutePath())   // The actual file path
                        .contentType(contentType) // e.g. application/x-mpegURL
                        .build();

                ObjectWriteResponse response = minioClient.uploadObject(uploadObjectArgs);
                log.info("Video uploaded to s3/minio: {}", response);
            }

        }
    }

    public void cleanupTempFiles(String jobPath) {
        try {
            Path dirPath = Paths.get(jobPath);
            if (Files.exists(dirPath)) {
                Files.walk(dirPath).sorted(java.util.Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
                log.info("Deleted temp files for job: {}", jobPath);
            }
        } catch (IOException e) {
            log.warn("Failed to cleanup temp files: {}", jobPath);
            throw new RuntimeException(e);
        }
    }
}
