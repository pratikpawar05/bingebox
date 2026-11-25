package com.bingebox.encodingservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kafka event consume when a video is uploaded to S3.
 *
 * TOPIC: video.encoded
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VideoEncodedEvent {
    private String movieId;
    private String hslUrl;
    private String masterPlaylistKey;
    private String success;
    private String errorMessage;
}
