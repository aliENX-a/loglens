package com.jai.loglens.ingestion;

import com.jai.loglens.dto.IngestResponse;
import com.jai.loglens.dto.LogIngestRequest;
import com.jai.loglens.repository.LogEventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/logs")
public class IngestionController {

    private final IngestionService ingestionService;
    private final IngestBuffer buffer;
    private final LogEventRepository repository;

    public IngestionController(IngestionService ingestionService, IngestBuffer buffer, LogEventRepository repository) {
        this.ingestionService = ingestionService;
        this.buffer = buffer;
        this.repository = repository;
    }

    @PostMapping("/ingest")
    public ResponseEntity<IngestResponse> ingest(@RequestBody LogIngestRequest request) {
        int accepted = ingestionService.accept(List.of(request));
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new IngestResponse(accepted, buffer.size(), repository.count()));
    }

    @PostMapping("/ingest/batch")
    public ResponseEntity<IngestResponse> ingestBatch(@RequestBody List<LogIngestRequest> requests) {
        int accepted = ingestionService.accept(requests);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new IngestResponse(accepted, buffer.size(), repository.count()));
    }

    @PostMapping("/flush")
    public Map<String, Object> flush() {
        ingestionService.flushAll();
        return Map.of(
                "buffered", buffer.size(),
                "written", buffer.getWritten(),
                "dropped", buffer.getDropped()
        );
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return Map.of(
                "capacity", buffer.getCapacity(),
                "buffered", buffer.size(),
                "accepted", buffer.getAccepted(),
                "written", buffer.getWritten(),
                "dropped", buffer.getDropped(),
                "stored", repository.count()
        );
    }
}
