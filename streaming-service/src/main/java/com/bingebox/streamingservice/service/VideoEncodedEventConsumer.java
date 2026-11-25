package com.bingebox.streamingservice.service;

import com.bingebox.streamingservice.event.VideoEncodedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class VideoEncodedEventConsumer {
    private final StreamingService streamingService;
    @KafkaListener(
            topics = "video.encoded",
            groupId = "steaming-service-group"
    )
    public void consume(VideoEncodedEvent event) {
        log.info("Consumed VideoUploadedEvent for movie upload");

        try {
            streamingService.streamVideo(event);
        } catch (Exception e) {
            log.error("Streaming for movie failed: {} - {}", event.getMovieId() ,e.getMessage());
        }

        // save file
    }

}
