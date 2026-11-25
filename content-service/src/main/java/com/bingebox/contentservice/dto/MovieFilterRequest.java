package com.bingebox.contentservice.dto;

import com.bingebox.contentservice.entity.Genre;
import lombok.Data;

@Data
public class MovieFilterRequest {
    private Integer page = 1;
    private Integer limit = 10;
    private String search;
    private Genre genre;
    private Integer releaseYear;
}
