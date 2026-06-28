package com.jai.loglens.repository;

import com.jai.loglens.domain.Alert;
import com.jai.loglens.domain.AlertStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByStatusOrderByTriggeredAtDesc(AlertStatus status);

    Optional<Alert> findByDedupKey(String dedupKey);

    long countByStatus(AlertStatus status);

    List<Alert> findByRuleIdAndStatus(Long ruleId, AlertStatus status);

    List<Alert> findByRuleIdAndTriggeredAtAfter(Long ruleId, Instant after);
}
