package com.jai.loglens.alerting;

import com.jai.loglens.domain.Alert;
import com.jai.loglens.domain.AlertStatus;
import com.jai.loglens.dto.AlertView;
import com.jai.loglens.repository.AlertRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

    private final AlertRepository repository;

    public AlertController(AlertRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<AlertView> list(@RequestParam(defaultValue = "FIRING") String status,
                                @RequestParam(defaultValue = "50") int limit) {
        int cap = Math.min(Math.max(limit, 1), 500);

        if ("ALL".equalsIgnoreCase(status)) {
            return repository.findAll().stream()
                    .sorted((a, b) -> b.getTriggeredAt().compareTo(a.getTriggeredAt()))
                    .limit(cap)
                    .map(AlertView::from)
                    .toList();
        }

        AlertStatus parsed;
        try {
            parsed = AlertStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            parsed = AlertStatus.FIRING;
        }
        return repository.findByStatusOrderByTriggeredAtDesc(parsed).stream()
                .limit(cap)
                .map(AlertView::from)
                .toList();
    }

    @GetMapping("/count")
    public Map<String, Object> count() {
        return Map.of(
                "firing", repository.countByStatus(AlertStatus.FIRING),
                "resolved", repository.countByStatus(AlertStatus.RESOLVED)
        );
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<AlertView> resolve(@PathVariable Long id) {
        Optional<Alert> alert = repository.findById(id);
        if (alert.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Alert a = alert.get();
        a.resolve(Instant.now());
        return ResponseEntity.ok(AlertView.from(repository.save(a)));
    }
}
