package com.jai.loglens.demo;

import com.jai.loglens.alerting.AlertEvaluationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/demo")
public class DemoController {

    private final DemoDataService demoDataService;
    private final AlertEvaluationService evaluationService;
    private final int defaultEvents;

    public DemoController(DemoDataService demoDataService,
                          AlertEvaluationService evaluationService,
                          @Value("${loglens.demo.events:2500}") int defaultEvents) {
        this.demoDataService = demoDataService;
        this.evaluationService = evaluationService;
        this.defaultEvents = defaultEvents;
    }

    @PostMapping("/seed")
    public Map<String, Object> seed(@RequestParam(required = false) Integer events) {
        int seeded = demoDataService.seed(events == null ? defaultEvents : events);
        evaluationService.scan();
        return Map.of("seeded", seeded, "services", String.join(",", DemoDataService.SERVICES));
    }

    @PostMapping("/incident")
    public Map<String, Object> incident(@RequestParam(defaultValue = "payment-service") String service,
                                        @RequestParam(defaultValue = "80") int count) {
        int injected = demoDataService.injectIncident(service, count);
        evaluationService.scan();
        return Map.of("service", service, "injectedErrors", injected, "evaluated", true);
    }

    @PostMapping("/silence")
    public Map<String, Object> silence(@RequestParam(defaultValue = "inventory-service") String service,
                                       @RequestParam(defaultValue = "30") int minutes) {
        int removed = demoDataService.silenceService(service, minutes);
        evaluationService.scan();
        return Map.of("service", service, "removedRecentLogs", removed, "evaluated", true);
    }
}
