package com.jai.loglens.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

public final class TimeParser {

    private TimeParser() {
    }

    /**
     * Accepts ISO-8601 instants, naive local/offset date-times (assumed UTC) and
     * epoch seconds/millis. Returns null when nothing matches so callers can decide
     * whether a missing timestamp is fatal or not.
     */
    public static Instant parseOrNull(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim();
        if (v.isEmpty()) {
            return null;
        }
        if (v.matches("\\d+")) {
            try {
                long n = Long.parseLong(v);
                if (v.length() >= 12) {
                    return Instant.ofEpochMilli(n);
                }
                if (v.length() >= 9) {
                    return Instant.ofEpochSecond(n);
                }
            } catch (NumberFormatException e) {
                return null;
            }
            return null;
        }
        try {
            return Instant.parse(v);
        } catch (DateTimeParseException ignored) {
            // try the looser forms
        }
        String normalized = v.replace(' ', 'T').replace(',', '.');
        try {
            return OffsetDateTime.parse(normalized).toInstant();
        } catch (DateTimeParseException ignored) {
            // no offset present
        }
        try {
            return LocalDateTime.parse(normalized).atZone(ZoneOffset.UTC).toInstant();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }
}
