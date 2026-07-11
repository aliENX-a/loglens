package com.jai.loglens.alerting;

import com.jai.loglens.domain.AlertRule;
import com.jai.loglens.dto.AlertRuleRequest;
import com.jai.loglens.dto.AlertRuleView;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/rules")
public class AlertRuleController {

    private final AlertRuleService ruleService;
    private final AlertEvaluationService evaluationService;

    public AlertRuleController(AlertRuleService ruleService, AlertEvaluationService evaluationService) {
        this.ruleService = ruleService;
        this.evaluationService = evaluationService;
    }

    @GetMapping
    public List<AlertRuleView> list() {
        return ruleService.list().stream().map(AlertRuleView::from).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlertRuleView> get(@PathVariable Long id) {
        return ruleService.get(id)
                .map(r -> ResponseEntity.ok(AlertRuleView.from(r)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<AlertRuleView> create(@Valid @RequestBody AlertRuleRequest request) {
        AlertRule created = ruleService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(AlertRuleView.from(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AlertRuleView> update(@PathVariable Long id,
                                                @Valid @RequestBody AlertRuleRequest request) {
        return ruleService.update(id, request)
                .map(r -> ResponseEntity.ok(AlertRuleView.from(r)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        boolean removed = ruleService.delete(id);
        return removed
                ? ResponseEntity.ok(Map.of("deleted", id))
                : ResponseEntity.notFound().build();
    }

    @PostMapping("/evaluate")
    public Map<String, Object> evaluateNow() {
        evaluationService.scan();
        return Map.of("evaluated", true);
    }
}
