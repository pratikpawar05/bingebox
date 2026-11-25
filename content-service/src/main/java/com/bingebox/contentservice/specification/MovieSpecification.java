package com.bingebox.contentservice.specification;

import com.bingebox.contentservice.dto.MovieFilterRequest;
import com.bingebox.contentservice.entity.Genre;
import com.bingebox.contentservice.entity.Movie;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;

@Slf4j
public class MovieSpecification {

    public static Specification<Movie> filter(
            MovieFilterRequest filter
    ) {
        return Specification.allOf(
                search(filter.getSearch()),
                releaseYear(filter.getReleaseYear()),
                genre(filter.getGenre())
        );
    }

    private static Specification<Movie> search(String search) {

        return (root, query, cb) -> {

            if (search == null || search.isBlank()) {
                return null;
            }

            String keyword = "%" + search.toLowerCase() + "%";

            return cb.or(
                    cb.like(cb.lower(root.get("title")), keyword),
                    cb.like(cb.lower(root.get("description")), keyword)
            );
        };
    }

    private static Specification<Movie> releaseYear(Integer releaseYear) {
        return (root, query, cb) ->
                releaseYear == null ? null : cb.equal(root.get("releaseYear"), releaseYear);
    }

    private static Specification<Movie> genre(Genre genre) {
        return (root, query, cb) ->
                genre == null ? null : cb.equal(root.get("genre"), genre);
    }
}
