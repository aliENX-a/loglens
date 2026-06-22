package com.jai.loglens.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "log_events", indexes = {
        @Index(name = "idx_log_time", columnList = "eventTime"),
        @Index(name = "idx_log_service", columnList = "serviceName"),
        @Index(name = "idx_log_level", columnList = "level"),
        @Index(name = "idx_log_trace", columnList = "traceId")
})
public class LogEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant eventTime;

    @Column(nullable = false, length = 64)
    private String serviceName;

    @Column(nullable = false, length = 16)
    private String level;

    @Column(length = 2048)
    private String message;

    @Column(length = 128)
    private String loggerName;

    @Column(length = 64)
    private String threadName;

    @Column(length = 64)
    private String traceId;

    @Column(length = 64)
    private String host;

    @Column(length = 4096)
    private String rawPayload;

    @Column(nullable = false)
    private Instant ingestedAt;

    protected LogEvent() {
    }

    public LogEvent(Instant eventTime, String serviceName, String level, String message,
                    String loggerName, String threadName, String traceId, String host,
                    String rawPayload, Instant ingestedAt) {
        this.eventTime = eventTime;
        this.serviceName = serviceName;
        this.level = level;
        this.message = message;
        this.loggerName = loggerName;
        this.threadName = threadName;
        this.traceId = traceId;
        this.host = host;
        this.rawPayload = rawPayload;
        this.ingestedAt = ingestedAt;
    }

    public Long getId() {
        return id;
    }

    public Instant getEventTime() {
        return eventTime;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public String getThreadName() {
        return threadName;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getHost() {
        return host;
    }

    public String getRawPayload() {
        return rawPayload;
    }

    public Instant getIngestedAt() {
        return ingestedAt;
    }
}
