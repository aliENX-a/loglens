package com.jai.loglens.retention;

import com.jai.loglens.repository.LogEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
public class RetentionService {

    private static final Logger log = LoggerFactory.getLogger(RetentionService.class);

    private final LogEventRepository repository;
    private final int retentionDays;

    public RetentionService(LogEventRepository repository,
                            @Value("${loglens.retention.days:7}") int retentionDays) {
        this.repository = repository;
        this.retentionDays = retentionDays;
    }

    @Scheduled(fixedDelayString = "${loglens.retention.scan-interval-ms:3600000}")
    @Transactional
    public int purge() {
        Instant cutoff = Instant.now().minus(Duration.ofDays(Math.max(retentionDays, 1)));
        int removed = repository.deleteOlderThan(cutoff);
        if (removed > 0) {
            log.info("retention removed {} events older than {}", removed, cutoff);
        }
        return removed;
    }
}
