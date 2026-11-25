package com.bingebox.videoservice.controller;

import com.bingebox.videoservice.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("api/v1/videos")
@Slf4j
@RequiredArgsConstructor
public class VideoController {
    private final VideoService videoService;

    /**
     * Upload video file for a movie to S3/minio
     * Accepts multipart file upload
     *
     * POST /api/v1/videos/upload/{movieId}
     */
    @PostMapping("upload/{movieId}")
    public ResponseEntity<String> uploadVideo(
            @PathVariable String movieId,
            @RequestParam MultipartFile videoFile
            )  throws Exception {
        log.info("Video upload request for moveId: {} & file name: {} ", movieId, videoFile.getOriginalFilename());

        if (videoFile.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        String videoKey = videoService.uploadVideo(movieId, videoFile);

        return ResponseEntity.ok(
                "Video uploaded succesfully! Video key: " + videoKey +
                        "Encoding started automatically by kafka!"
        );
    }

}
