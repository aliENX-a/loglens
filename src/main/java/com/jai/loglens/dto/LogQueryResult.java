package com.jai.loglens.dto;

import java.util.List;

public record LogQueryResult(
        long total,
        int page,
        int size,
        List<LogEventView> items
) {
}
