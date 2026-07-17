package com.jai.loglens.demo;

import com.jai.loglens.domain.LogEvent;
import com.jai.loglens.domain.LogLevel;
import com.jai.loglens.repository.AlertRepository;
import com.jai.loglens.repository.LogEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

@Service
public class DemoDataService {

    private static final Logger log = LoggerFactory.getLogger(DemoDataService.class);

    public static final String[] SERVICES = {
            "payment-service", "checkout-service", "inventory-service", "auth-service", "api-gateway"
    };

    private static final String[] HOSTS = {"node-a", "node-b", "node-c"};
    private static final String[] THREADS = {"http-nio-8080-exec-1", "http-nio-8080-exec-2", "scheduler-1", "worker-3"};
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private final LogEventRepository logRepository;
    private final AlertRepository alertRepository;

    public DemoDataService(LogEventRepository logRepository, AlertRepository alertRepository) {
        this.logRepository = logRepository;
        this.alertRepository = alertRepository;
    }

    public boolean isStale() {
        Instant newest = logRepository.findNewestEventTime();
        return newest == null || newest.isBefore(Instant.now().minus(Duration.ofMinutes(20)));
    }

    @Transactional
    public int seed(int events) {
        alertRepository.deleteAll();
        logRepository.deleteAll();

        List<LogEvent> batch = generate(Math.max(events, 100), Instant.now());
        logRepository.saveAll(batch);
        log.info("seeded {} demo log events", batch.size());
        return batch.size();
    }

    @Transactional
    public int injectIncident(String service, int count) {
        Random rnd = new Random();
        Instant now = Instant.now();
        List<LogEvent> batch = new ArrayList<>();
        for (int i = 0; i < Math.max(count, 1); i++) {
            Instant ts = now.minus(Duration.ofSeconds(rnd.nextInt(240)));
            LogLevel level = rnd.nextInt(100) < 80 ? LogLevel.ERROR : LogLevel.FATAL;
            batch.add(event(service, level, ts, rnd));
        }
        logRepository.saveAll(batch);
        return batch.size();
    }

    @Transactional
    public int silenceService(String service, int minutes) {
        return logRepository.deleteRecentForService(service,
                Instant.now().minus(Duration.ofMinutes(Math.max(minutes, 1))));
    }

    private List<LogEvent> generate(int events, Instant now) {
        Random rnd = new Random(2026);
        Instant start = now.minus(Duration.ofHours(6));
        long spanMillis = Duration.between(start, now).toMillis();
        List<LogEvent> out = new ArrayList<>(events + 400);

        // Normal, steady traffic for every service (including inventory) so that
        // at baseline none of the alert rules breach. The demo drills
        // (/demo/incident, /demo/silence) are what push the system into an alert
        // state, which makes a clean live demonstration possible.
        for (int i = 0; i < events; i++) {
            String service = SERVICES[rnd.nextInt(SERVICES.length)];
            double fraction = Math.pow(rnd.nextDouble(), 0.85);
            Instant ts = start.plusMillis((long) (spanMillis * fraction));
            out.add(event(service, normalLevel(rnd), ts, rnd));
        }

        out.sort(Comparator.comparing(LogEvent::getEventTime));
        return out;
    }

    private LogEvent event(String service, LogLevel level, Instant eventTime, Random rnd) {
        String message = text(service, level, rnd);
        return new LogEvent(
                eventTime,
                service,
                level.name(),
                message,
                "com.company." + service.replace("-service", "") + ".Service",
                THREADS[rnd.nextInt(THREADS.length)],
                traceId(rnd),
                HOSTS[rnd.nextInt(HOSTS.length)],
                message,
                Instant.now()
        );
    }

    private static LogLevel normalLevel(Random rnd) {
        int r = rnd.nextInt(100);
        if (r < 62) {
            return LogLevel.INFO;
        }
        if (r < 78) {
            return LogLevel.DEBUG;
        }
        if (r < 88) {
            return LogLevel.WARN;
        }
        if (r < 97) {
            return LogLevel.ERROR;
        }
        return LogLevel.FATAL;
    }

    private String text(String service, LogLevel level, Random rnd) {
        int id = 1000 + rnd.nextInt(9000);
        int ms = 50 + rnd.nextInt(4950);

        return switch (service) {
            case "payment-service" -> switch (level) {
                case ERROR -> rnd.nextBoolean()
                        ? "payment gateway timeout after " + ms + " ms"
                        : "card authorization declined for order " + id;
                case FATAL -> "payment processor connection pool exhausted";
                case WARN -> "retrying payment gateway call, attempt " + (1 + rnd.nextInt(3));
                case DEBUG -> "acquiring lock for settlement batch " + (100 + rnd.nextInt(900));
                default -> "processed payment for order " + id + " amount " + (10 + rnd.nextInt(990)) + ".00";
            };
            case "checkout-service" -> switch (level) {
                case ERROR -> "checkout failed, upstream returned 503 for cart " + id;
                case FATAL -> "checkout pipeline deadlock detected";
                case WARN -> "slow checkout step took " + ms + " ms";
                case DEBUG -> "inventory pre-check returned " + rnd.nextInt(40) + " items";
                default -> "cart validated for session " + id;
            };
            case "inventory-service" -> switch (level) {
                case ERROR -> "stock reservation conflict for sku SKU-" + id;
                case FATAL -> "inventory store unreachable";
                case WARN -> "low stock for sku SKU-" + id;
                case DEBUG -> "cache warm for " + rnd.nextInt(500) + " skus";
                default -> "stock reserved for sku SKU-" + id;
            };
            case "auth-service" -> switch (level) {
                case ERROR -> "JWT signature validation failed for user " + id;
                case FATAL -> "identity provider handshake failed";
                case WARN -> "failed login attempt for user " + id;
                case DEBUG -> "session refreshed in " + rnd.nextInt(30) + " ms";
                default -> "token issued for user " + id;
            };
            case "api-gateway" -> switch (level) {
                case ERROR -> "upstream timeout calling payment-service after " + ms + " ms";
                case FATAL -> "no healthy upstreams available";
                case WARN -> "rate limit approaching for client " + id;
                case DEBUG -> "upstream health check ok";
                default -> "routed request " + id + " in " + (rnd.nextInt(300)) + " ms";
            };
            default -> "sample log line " + id;
        };
    }

    private static String traceId(Random rnd) {
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            sb.append(HEX[rnd.nextInt(HEX.length)]);
        }
        return sb.toString();
    }
}
