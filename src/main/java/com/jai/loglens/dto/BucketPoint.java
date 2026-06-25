package com.jai.loglens.dto;

import java.time.Instant;

public record BucketPoint(
        Instant bucketStart,
        long total,
        long errors
) {
}
