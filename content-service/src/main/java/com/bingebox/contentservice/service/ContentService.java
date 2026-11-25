package com.bingebox.contentservice.service;

import com.bingebox.contentservice.dto.MovieFilterRequest;
import com.bingebox.contentservice.dto.MovieRequest;
import com.bingebox.contentservice.dto.MovieResponse;
import com.bingebox.contentservice.entity.Movie;
import com.bingebox.contentservice.entity.VideoStatus;
import com.bingebox.contentservice.repository.MovieRepository;
import com.bingebox.contentservice.specification.MovieSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContentService {
    private final MovieRepository movieRepository;

    public MovieResponse addMovie(MovieRequest request) {
        log.info("Adding new movie: {}", request);

        Movie movie = new Movie();
        movie.setTitle(request.getTitle());
        movie.setDescription(request.getDescription());
        movie.setGenre(request.getGenre());
        movie.setDirector(request.getDirector());
        movie.setCast(request.getCast());
        movie.setThumbnailUrl(request.getThumbnailUrl());
        movie.setReleaseYear(request.getReleaseYear());
        movie.setRating(request.getRating());
        movie.setDurationMinutes(request.getDurationMinutes());
        movie.setVideoStatus(VideoStatus.PENDING);

        Movie savedMovie = movieRepository.save(movie);
        log.info("Saved new movie: {}", savedMovie.getId());

        return mapToResponse(savedMovie);
    }

    public List<MovieResponse> getAllMovies(MovieFilterRequest request) {

        Pageable pageable = PageRequest.of(
                request.getPage() - 1,
                request.getLimit()
        );

        Specification<Movie> specification = MovieSpecification.filter(request);

        return movieRepository.findAll(specification, pageable)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public MovieResponse show(String id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Not a valid movieId"));
        return this.mapToResponse(movie);
    }

    public void updateVideoKey(String id, String videoKey) {
        log.info("Updating video key for movie: {}", id);

        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Movie not found"));

        movie.setVideoKey(videoKey);
        movie.setVideoStatus(VideoStatus.UPLOADED);
        movieRepository.save(movie);
    }

    public void updateHlsUrl(String id, String hslUrl) {
        log.info("Updating hls url for movie: {}", id);

        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Movie not found"));

        movie.setHlsUrl(hslUrl);
        movie.setVideoStatus(VideoStatus.READY);
        movieRepository.save(movie);

        log.info("Movie is now ready for streaming: {}", movie.getTitle());
    }

    private MovieResponse mapToResponse(Movie movie) {
        return MovieResponse.builder()
                .title(movie.getTitle())
                .description(movie.getDescription())
                .genre(movie.getGenre())
                .director(movie.getDirector())
                .cast(movie.getCast())
                .thumbnailUrl(movie.getThumbnailUrl())
                .releaseYear(movie.getReleaseYear())
                .rating(movie.getRating())
                .durationMinutes(movie.getDurationMinutes())
                .videoStatus(movie.getVideoStatus())
                .videoKey(movie.getVideoKey())
                .hlsUrl(movie.getHlsUrl())
                .createdAt(movie.getCreatedAt())
                .updatedAt(movie.getUpdatedAt())
                .build();
    }
}
