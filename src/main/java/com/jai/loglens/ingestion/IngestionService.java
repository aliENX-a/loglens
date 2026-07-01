package com.jai.loglens.ingestion;

import com.jai.loglens.dto.LogIngestRequest;
import com.jai.loglens.repository.LogEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final LogParser parser;
    private final IngestBuffer buffer;
    private final LogEventRepository repository;
    private final int batchSize;

    public IngestionService(LogParser parser,
                            IngestBuffer buffer,
                            LogEventRepository repository,
                            @Value("${loglens.ingest.batch-size:500}") int batchSize) {
        this.parser = parser;
        this.buffer = buffer;
        this.repository = repository;
        this.batchSize = batchSize;
    }

    public int accept(List<LogIngestRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return 0;
        }
        int accepted = 0;
        for (LogIngestRequest request : requests) {
            if (request == null) {
                continue;
            }
            try {
                if (buffer.offer(parser.parse(request))) {
                    accepted++;
                }
            } catch (RuntimeException e) {
                log.warn("dropped malformed payload: {}", e.getMessage());
            }
        }
        return accepted;
    }

    @Scheduled(fixedDelayString = "${loglens.ingest.flush-interval-ms:200}")
    public void flush() {
        if (buffer.size() == 0) {
            return;
        }
        List<com.jai.loglens.domain.LogEvent> batch = buffer.drain(batchSize);
        if (batch.isEmpty()) {
            return;
        }
        repository.saveAll(batch);
        buffer.addWritten(batch.size());
    }

    public void flushAll() {
        while (buffer.size() > 0) {
            flush();
        }
    }
}
