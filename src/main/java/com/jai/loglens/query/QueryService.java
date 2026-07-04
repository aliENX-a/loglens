package com.jai.loglens.query;

import com.jai.loglens.domain.LogEvent;
import com.jai.loglens.dto.LogEventView;
import com.jai.loglens.dto.LogQueryResult;
import com.jai.loglens.repository.LogEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class QueryService {

    private final LogEventRepository repository;

    public QueryService(LogEventRepository repository) {
        this.repository = repository;
    }

    public LogQueryResult search(String service,
                                 String level,
                                 String keyword,
                                 String traceId,
                                 Instant from,
                                 Instant to,
                                 int page,
                                 int size) {
        Specification<LogEvent> spec = LogSpecification.matching(service, level, keyword, traceId, from, to);

        int safeSize = Math.min(Math.max(size, 1), 500);
        PageRequest pageable = PageRequest.of(Math.max(page, 0), safeSize,
                Sort.by(Sort.Direction.DESC, "eventTime"));

        Page<LogEvent> result = repository.findAll(spec, pageable);
        List<LogEventView> items = result.getContent().stream().map(LogEventView::from).toList();

        return new LogQueryResult(result.getTotalElements(), pageable.getPageNumber(), safeSize, items);
    }

    public long countMatching(String service, String level, String keyword, Instant from, Instant to) {
        return repository.count(LogSpecification.matching(service, level, keyword, null, from, to));
    }

    public long countErrors(String service, Instant from, Instant to) {
        return repository.count(LogSpecification.errorsOnly(service, from, to));
    }
}
