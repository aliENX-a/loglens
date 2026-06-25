package com.jai.loglens.dto;

/**
 * Ingest payload. Everything except the raw text is optional - the parser fills in
 * defaults rather than rejecting. Dropping a log line during an outage is worse than
 * storing a slightly messy one.
 */
public record LogIngestRequest(
        String service,
        String level,
        String message,
        String timestamp,
        String logger,
        String thread,
        String traceId,
        String host
) {
}
