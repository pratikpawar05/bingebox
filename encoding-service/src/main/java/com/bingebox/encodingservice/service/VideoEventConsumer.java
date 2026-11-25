package com.bingebox.encodingservice.service;

import com.bingebox.encodingservice.event.VideoUploadedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class VideoEventConsumer {

    private final EncodingService encodingService;

    /**
     * Listens to video.uploaded kafka topic
     * Trigged when video service upload raw video to s3/minio.
     *
     * FLOW:
     * Video service -> upload s3 -> kafka (video.uploded) topic
     *                            -> This consumer
     *                            -> EncodingService -> FFmpeg -> S3
     *                            -> kafka (video.encoded)
     */
    @KafkaListener(
            topics = "video.uploaded",
            groupId = "encoding-service-group"
    )
    public void consume(VideoUploadedEvent event) {
        log.info("Consumed VideoUploadedEvent for movie upload");

        try {
            encodingService.encodeVideo(event);
        } catch (Exception e) {
            log.error("Encoding for movie failed: {} - {}", event.getMovieId() ,e.getMessage());
        }

        // save file
    }
}
