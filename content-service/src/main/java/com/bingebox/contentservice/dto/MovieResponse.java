package com.bingebox.contentservice.dto;

import com.bingebox.contentservice.entity.Genre;
import com.bingebox.contentservice.entity.VideoStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MovieResponse {
    private String id;

    private String title;

    private String description;

    private Genre genre;

    private String director;

    private String cast;

    private String thumbnailUrl;

    private Integer releaseYear;

    private Double rating;

    private Integer durationMinutes;

    private String videoKey;

    private String hlsUrl;

    private VideoStatus videoStatus;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
