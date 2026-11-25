package com.bingebox.contentservice.entity;

public enum VideoStatus {
    PENDING, // Movie ready but not uploaded
    UPLOADED, // Raw video uploaded to S3
    ENCODING, // FFMPEG is encoding the video
    ENCODED, // FFMPEG completed the video encoding
    READY, // HLS Playlist ready
    FAILED // Encoding Failed
}
