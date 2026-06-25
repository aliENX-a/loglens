package com.jai.loglens.dto;

import com.jai.loglens.domain.LogEvent;

import java.time.Instant;

public record LogEventView(
        Long id,
        Instant eventTime,
        String service,
        String level,
        String message,
        String logger,
        String thread,
        String traceId,
        String host
) {

    public static LogEventView from(LogEvent e) {
        return new LogEventView(
                e.getId(),
                e.getEventTime(),
                e.getServiceName(),
                e.getLevel(),
                e.getMessage(),
                e.getLoggerName(),
                e.getThreadName(),
                e.getTraceId(),
                e.getHost()
        );
    }
}
