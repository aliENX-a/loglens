package com.jai.loglens.query;

import com.jai.loglens.domain.LogEvent;
import com.jai.loglens.domain.LogLevel;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class LogSpecification {

    private LogSpecification() {
    }

    /**
     * Single definition of "which logs match" so the search API and the alert engine can
     * never drift apart. Null filter values mean "don't care".
     */
    public static Specification<LogEvent> matching(String service,
                                                   String level,
                                                   String keyword,
                                                   String traceId,
                                                   Instant from,
                                                   Instant to) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (isSet(service)) {
                predicates.add(cb.equal(root.get("serviceName"), service));
            }
            if (isSet(level)) {
                predicates.add(cb.equal(root.get("level"), LogLevel.parse(level).name()));
            }
            if (isSet(traceId)) {
                predicates.add(cb.equal(root.get("traceId"), traceId));
            }
            if (isSet(keyword)) {
                String pattern = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.<String>get("message")), pattern));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<Instant>get("eventTime"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<Instant>get("eventTime"), to));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    public static Specification<LogEvent> errorsOnly(String service, Instant from, Instant to) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (isSet(service)) {
                predicates.add(cb.equal(root.get("serviceName"), service));
            }
            predicates.add(cb.greaterThanOrEqualTo(root.<Instant>get("eventTime"), from));
            predicates.add(cb.lessThanOrEqualTo(root.<Instant>get("eventTime"), to));
            predicates.add(cb.or(
                    cb.equal(root.get("level"), LogLevel.ERROR.name()),
                    cb.equal(root.get("level"), LogLevel.FATAL.name())
            ));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private static boolean isSet(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
