package com.bingebox.videoservice.event;

import lombok.*;

/**
 * Event published to kafka when a video is uploaded to S3.
 * Encoding service consume this to start FFmpeg processing.
 *
 * TOPIC: video.uploaded
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VideoUploadedEvent {
    private String movieId;
    private String videoKey;
    private String bucketName;
    private String originalFileName;
    private Long fileSizeBytes;
}
