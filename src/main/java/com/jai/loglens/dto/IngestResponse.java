package com.jai.loglens.dto;

public record IngestResponse(
        int accepted,
        int buffered,
        long ingestedTotal
) {
}
