package com.jai.loglens.ingestion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jai.loglens.domain.LogEvent;
import com.jai.loglens.domain.LogLevel;
import com.jai.loglens.dto.LogIngestRequest;
import com.jai.loglens.util.TimeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class LogParser {

    private static final Logger log = LoggerFactory.getLogger(LogParser.class);

    private static final String DEFAULT_SERVICE = "unknown";

    private static final Pattern LEVEL_TOKEN =
            Pattern.compile("\\b(TRACE|DEBUG|INFO|WARN|WARNING|ERROR|ERR|FATAL|SEVERE|CRITICAL)\\b",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern LEADING_TIMESTAMP =
            Pattern.compile("^\\s*(\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}(?:[,.]\\d{1,9})?(?:Z|[+-]\\d{2}:?\\d{2})?)");

    private final ObjectMapper mapper;

    public LogParser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public LogEvent parse(LogIngestRequest req) {
        Instant now = Instant.now();
        String raw = req.message() == null ? "" : req.message();
        String text = raw;

        String service = clean(req.service());
        String level = clean(req.level());
        String loggerName = clean(req.logger());
        String thread = clean(req.thread());
        String traceId = clean(req.traceId());
        String host = clean(req.host());
        Instant eventTime = parseTimestamp(req.timestamp());

        JsonNode node = tryJson(text);
        if (node != null) {
            if (service == null) {
                service = pick(node, "service", "serviceName", "app", "application");
            }
            if (level == null) {
                level = pick(node, "level", "severity", "logLevel", "loglevel");
            }
            if (loggerName == null) {
                loggerName = pick(node, "logger", "loggerName", "source", "class");
            }
            if (thread == null) {
                thread = pick(node, "thread", "threadName");
            }
            if (traceId == null) {
                traceId = pick(node, "traceId", "trace_id", "trace", "correlationId");
            }
            if (host == null) {
                host = pick(node, "host", "hostname", "hostName", "node");
            }
            if (eventTime == null) {
                eventTime = parseTimestamp(pick(node, "timestamp", "time", "@timestamp", "eventTime"));
            }
            String embedded = pick(node, "message", "msg", "log", "event");
            if (embedded != null) {
                text = embedded;
            }
        } else {
            if (level == null) {
                Matcher m = LEVEL_TOKEN.matcher(text);
                if (m.find()) {
                    level = m.group(1);
                }
            }
            if (eventTime == null) {
                Matcher m = LEADING_TIMESTAMP.matcher(text);
                if (m.find()) {
                    eventTime = parseTimestamp(m.group(1));
                }
            }
        }

        return new LogEvent(
                eventTime == null ? now : eventTime,
                service == null ? DEFAULT_SERVICE : service,
                LogLevel.parse(level).name(),
                clip(text, 2048),
                clip(loggerName, 128),
                clip(thread, 64),
                clip(traceId, 64),
                clip(host, 64),
                clip(raw, 1024),
                now
        );
    }

    public Instant parseTimestamp(String value) {
        return TimeParser.parseOrNull(value);
    }

    private JsonNode tryJson(String text) {
        if (text == null) {
            return null;
        }
        String t = text.trim();
        if (!t.startsWith("{") || !t.endsWith("}")) {
            return null;
        }
        try {
            return mapper.readTree(t);
        } catch (Exception e) {
            return null;
        }
    }

    private String pick(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode n = node.get(key);
            if (n != null && !n.isNull() && !n.isMissingNode()) {
                String s = clean(n.asText());
                if (s != null) {
                    return s;
                }
            }
        }
        return null;
    }

    private static String clean(String s) {
        if (s == null) {
            return null;
        }
        String v = s.trim();
        return v.isEmpty() ? null : v;
    }

    private static String clip(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
