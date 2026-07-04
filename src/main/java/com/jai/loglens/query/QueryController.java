package com.jai.loglens.query;

import com.jai.loglens.dto.LogQueryResult;
import com.jai.loglens.util.TimeParser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/query")
public class QueryController {

    private final QueryService queryService;

    public QueryController(QueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    public LogQueryResult search(
            @RequestParam(required = false) String service,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String traceId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "60") int minutes,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Instant now = Instant.now();
        Instant fromInstant = TimeParser.parseOrNull(from);
        Instant toInstant = TimeParser.parseOrNull(to);

        if (fromInstant == null) {
            fromInstant = now.minus(Duration.ofMinutes(Math.max(minutes, 1)));
        }
        if (toInstant == null) {
            toInstant = now;
        }

        return queryService.search(service, level, keyword, traceId, fromInstant, toInstant, page, size);
    }
}
