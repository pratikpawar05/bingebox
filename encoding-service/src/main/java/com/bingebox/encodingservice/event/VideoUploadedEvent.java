package com.bingebox.encodingservice.event;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kafka event consumed from topic: video.uploaded when a video is uploaded to S3.
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
