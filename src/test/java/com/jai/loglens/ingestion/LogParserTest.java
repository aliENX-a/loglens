package com.jai.loglens.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jai.loglens.domain.LogEvent;
import com.jai.loglens.dto.LogIngestRequest;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class LogParserTest {

    private final LogParser parser = new LogParser(new ObjectMapper());

    private LogEvent parse(String message) {
        return parser.parse(new LogIngestRequest(null, null, message, null, null, null, null, null));
    }

    @Test
    void readsFieldsFromJsonPayload() {
        LogEvent e = parse("{\"service\":\"payment-service\",\"level\":\"ERROR\","
                + "\"message\":\"gateway timeout\",\"traceId\":\"abc123\"}");

        assertThat(e.getServiceName()).isEqualTo("payment-service");
        assertThat(e.getLevel()).isEqualTo("ERROR");
        assertThat(e.getMessage()).isEqualTo("gateway timeout");
        assertThat(e.getTraceId()).isEqualTo("abc123");
    }

    @Test
    void explicitRequestFieldsWinOverJson() {
        LogEvent e = parser.parse(new LogIngestRequest("billing", "WARN",
                "{\"service\":\"payment-service\",\"message\":\"x\"}", null, null, null, null, null));

        assertThat(e.getServiceName()).isEqualTo("billing");
        assertThat(e.getLevel()).isEqualTo("WARN");
    }

    @Test
    void pullsLevelAndTimestampOutOfPlainText() {
        LogEvent e = parse("2026-06-20T10:15:30.123 ERROR payment gateway unreachable");

        assertThat(e.getLevel()).isEqualTo("ERROR");
        assertThat(e.getEventTime()).isEqualTo(Instant.parse("2026-06-20T10:15:30.123Z"));
    }

    @Test
    void normalisesLevelAliases() {
        assertThat(parse("{\"level\":\"warning\",\"message\":\"m\"}").getLevel()).isEqualTo("WARN");
        assertThat(parse("{\"level\":\"err\",\"message\":\"m\"}").getLevel()).isEqualTo("ERROR");
        assertThat(parse("{\"level\":\"critical\",\"message\":\"m\"}").getLevel()).isEqualTo("FATAL");
    }

    @Test
    void unknownLevelFallsBackToInfo() {
        assertThat(parse("{\"level\":\"NOT_A_LEVEL\",\"message\":\"m\"}").getLevel()).isEqualTo("INFO");
    }

    @Test
    void unnamedServiceBecomesUnknown() {
        assertThat(parse("just a bare line").getServiceName()).isEqualTo("unknown");
        assertThat(parse("just a bare line").getLevel()).isEqualTo("INFO");
    }

    @Test
    void parsesEpochTimestamps() {
        long millis = 1750414530123L;
        LogEvent e = parser.parse(new LogIngestRequest("s", null, "m",
                String.valueOf(millis), null, null, null, null));
        assertThat(e.getEventTime()).isEqualTo(Instant.ofEpochMilli(millis));
    }

    @Test
    void malformedJsonIsTreatedAsPlainText() {
        LogEvent e = parse("{\"service\": \"broken\", ");
        assertThat(e.getServiceName()).isEqualTo("unknown");
        assertThat(e.getMessage()).startsWith("{\"service\"");
    }
}
