package com.bingebox.contentservice.dto;

import com.bingebox.contentservice.entity.Genre;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MovieRequest {
    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotBlank(message = "Genre is required")
    private Genre genre;

    private String director;

    private String cast;

    private String thumbnailUrl;

    private Integer releaseYear;

    private Double rating;

    private Integer durationMinutes;
}
