package com.jai.loglens.repository;

import com.jai.loglens.domain.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {

    Optional<AlertRule> findByName(String name);

    List<AlertRule> findByEnabledTrue();
}
