package com.jai.loglens.demo;

import com.jai.loglens.alerting.AlertEvaluationService;
import com.jai.loglens.alerting.AlertRuleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DemoDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);

    private final DemoDataService demoDataService;
    private final AlertRuleService ruleService;
    private final AlertEvaluationService evaluationService;

    private final boolean seedOnStartup;
    private final boolean reseedStale;
    private final int events;

    public DemoDataLoader(DemoDataService demoDataService,
                          AlertRuleService ruleService,
                          AlertEvaluationService evaluationService,
                          @Value("${loglens.demo.seed-on-startup:true}") boolean seedOnStartup,
                          @Value("${loglens.demo.reseed-stale:true}") boolean reseedStale,
                          @Value("${loglens.demo.events:2500}") int events) {
        this.demoDataService = demoDataService;
        this.ruleService = ruleService;
        this.evaluationService = evaluationService;
        this.seedOnStartup = seedOnStartup;
        this.reseedStale = reseedStale;
        this.events = events;
    }

    @Override
    public void run(ApplicationArguments args) {
        ruleService.seedDefaultsIfEmpty();

        if (!seedOnStartup) {
            return;
        }
        if (demoDataService.isStale() && reseedStale) {
            demoDataService.seed(events);
        } else {
            log.info("existing log data is fresh, keeping it");
        }

        evaluationService.scan();
    }
}
