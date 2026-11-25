package com.bingebox.videoservice.service;

import com.bingebox.videoservice.event.VideoUploadedEvent;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class VideoService {

    private final MinioClient minioClient;

    private final KafkaTemplate<String, VideoUploadedEvent> kafkaTemplate;

    @Value("{minio.bucket-name}")
    private String bucketName;

    private static final String VIDEO_UPLOADED_TOPIC = "video.uploaded";


    /**
     * Upload video to aws s3/minio and publish VideoUploadedEvent to kafka
     *
     * FLOW:
     * 1. Recieve multipart video file
     * 2. Generate unique s3/minio key
     * 3. Upload to s3/minio
     * 4. Publis VideoUploadedEvent to kafka
     * 5. Encoding service consumes the kafka event
     */

    public String uploadVideo(String movieId, MultipartFile file) throws Exception {
        //Generate unique s3/minio key for raw video
        //Format: {bucketname}/raw/movieId/uuid_filename.ext

        String videoKey = "raw/" +movieId+"/"+ UUID.randomUUID()
                + "_" +file.getOriginalFilename();

        PutObjectArgs putObjectArgs = PutObjectArgs
                .builder()
                .bucket(bucketName) // Which bucket to store in
                .object(videoKey)   // The unique key/name for this file
                .stream(file.getInputStream(), file.getSize(), (long) -1) // The actual file data, File size (-1 = unknown part size)
                .contentType(file.getContentType()) // e.g. image/jpeg
                .build();

        ObjectWriteResponse response = minioClient.putObject(putObjectArgs);

        log.info("Video uploaded to s3/minio: {}", response);

        // publish event to kafka
        // Encoding service will consume this and start FFmpeg service

        VideoUploadedEvent videoUploadedEvent = VideoUploadedEvent.builder()
                .movieId(movieId)
                .videoKey(videoKey)
                .bucketName(bucketName)
                .originalFileName(file.getOriginalFilename())
                .fileSizeBytes(file.getSize())
                .build();

        kafkaTemplate.send(VIDEO_UPLOADED_TOPIC, videoUploadedEvent);

        log.info("Video uploaded event sent via kafka: {}", response);

        return videoKey;
    }
}
