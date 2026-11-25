package com.bingebox.contentservice.controller;

import com.bingebox.contentservice.dto.MovieFilterRequest;
import com.bingebox.contentservice.dto.MovieRequest;
import com.bingebox.contentservice.dto.MovieResponse;
import com.bingebox.contentservice.service.ContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/movies")
@Slf4j
@RequiredArgsConstructor
public class ContentController {
    private final ContentService contentService;

    // Add new movie to catalog
    @PostMapping
    public ResponseEntity<MovieResponse> addMovie(
            @Valid @RequestBody MovieRequest movieRequest
            ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(contentService.addMovie(movieRequest));
    }

    // Get all movies
    @GetMapping
    public ResponseEntity<List<MovieResponse>> index(
            MovieFilterRequest filterRequest
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(contentService.getAllMovies(filterRequest));
    }

    // Get movie by id
    @GetMapping("/{movie}")
    public ResponseEntity<MovieResponse> index(
            String movie
    ) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(contentService.show(movie));
    }
}
