package com.bingebox.streamingservice.service;

import com.bingebox.streamingservice.dto.StreamingResponse;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.Http;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class StreamingService {
    private final MinioClient minioClient;
    private final RedisTemplate redisTemplate;


    @Value("{minio.bucket-name}")
    private String bucketName;

    @Value("{minio.presigned-url-expiry}")
    private Integer expiry;

    private final static String STREAMING_URL_CACHE_PREFIX = "streaming:url:";

    /**
     * Get streaming URL for a movie
     *
     * FLOW:
     * 1. Check redis cache for presigned url
     * 2. If cached - return that url
     * 3. If not cached = generate presgined url from s3/minio
     * 4. Cache the url in redis
     * 5. Return streaming url
     *
     * Why presigned url?
     * - s3 bucket is private room - videos are not publicly accessible
     * - Presigned URL gives temporary access (X minutes)
     * - Prevent unauthorized video downloads
     *
     * @param movieId
     * @return
     */
    public StreamingResponse getStreamingUrl(String movieId, String playlistKey) throws Exception {
        String cacheKey = STREAMING_URL_CACHE_PREFIX + movieId;

        String cachedUrl = redisTemplate.opsForValue().get(cacheKey);
        if (cachedUrl != null) {
            log.info("Return Streaming url for that movie: {}", movieId);
            return StreamingResponse.builder()
                    .movieId(movieId)
                    .streamingUrl(cachedUrl)
                    .quality("1080, 720, 480, 360")
                    .expiredInMinutes(expiry)
                    .build();
        }

        //Generate new presigned url from s3/minio
        generatePresignedObjectUrl(p);
       redisTemplate.opsForValue().set(cacheKey, url, expiry, TimeUnit.MINUTES);

        System.out.println(url);

        return StreamingResponse.builder()
                .movieId(movieId)
                .streamingUrl(url)
                .quality("1080, 720, 480, 360")
                .expiredInMinutes(expiry)
                .build();

    }

    private String generatePresignedObjectUrl(String key) throws Exception {
        GetPresignedObjectUrlArgs getPresignedObjectUrlArgs = GetPresignedObjectUrlArgs.builder()
                .method(Http.Method.GET)
                .bucket(bucketName)
                .object(key)
                .expiry(expiry, TimeUnit.MINUTES)
//                .extraQueryParams(reqParams)
                .build();
        return minioClient.getPresignedObjectUrl(getPresignedObjectUrlArgs);
    }

    public void invalidateCache(String movieId) {
        String cacheKey = STREAMING_URL_CACHE_PREFIX + movieId;

        redisTemplate.delete(cacheKey);
        log.info("Deleted streaming URL cache for movie: {}", movieId);
    }
}
