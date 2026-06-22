package com.jai.loglens.domain;

public enum LogLevel {

    TRACE(10),
    DEBUG(20),
    INFO(30),
    WARN(40),
    ERROR(50),
    FATAL(60);

    private final int severity;

    LogLevel(int severity) {
        this.severity = severity;
    }

    public int getSeverity() {
        return severity;
    }

    public static LogLevel parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return INFO;
        }
        String value = raw.trim().toUpperCase();
        switch (value) {
            case "WARNING":
                return WARN;
            case "ERR":
                return ERROR;
            case "CRITICAL":
            case "SEVERE":
                return FATAL;
            default:
                break;
        }
        try {
            return LogLevel.valueOf(value);
        } catch (IllegalArgumentException e) {
            return INFO;
        }
    }

    public static boolean isKnown(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String value = raw.trim().toUpperCase();
        if (value.equals("WARNING") || value.equals("ERR") || value.equals("CRITICAL") || value.equals("SEVERE")) {
            return true;
        }
        try {
            LogLevel.valueOf(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
