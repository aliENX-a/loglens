package com.jai.loglens.ingestion;

import com.jai.loglens.domain.LogEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Bounded hand-off between the HTTP threads and the writer. If the writer falls behind,
 * the queue fills and we start dropping instead of letting the heap grow - logs are
 * valuable, but a dead aggregator is worse than a few missing lines.
 */
@Component
public class IngestBuffer {

    private final BlockingQueue<LogEvent> queue;
    private final int capacity;

    private final AtomicLong accepted = new AtomicLong();
    private final AtomicLong dropped = new AtomicLong();
    private final AtomicLong written = new AtomicLong();

    public IngestBuffer(@Value("${loglens.ingest.queue-capacity:50000}") int capacity) {
        this.capacity = capacity;
        this.queue = new ArrayBlockingQueue<>(capacity);
    }

    public boolean offer(LogEvent event) {
        boolean ok = queue.offer(event);
        if (ok) {
            accepted.incrementAndGet();
        } else {
            dropped.incrementAndGet();
        }
        return ok;
    }

    public List<LogEvent> drain(int max) {
        List<LogEvent> batch = new ArrayList<>(Math.min(max, 1024));
        queue.drainTo(batch, max);
        return batch;
    }

    public int size() {
        return queue.size();
    }

    public int getCapacity() {
        return capacity;
    }

    public long getAccepted() {
        return accepted.get();
    }

    public long getDropped() {
        return dropped.get();
    }

    public long getWritten() {
        return written.get();
    }

    public void addWritten(long n) {
        written.addAndGet(n);
    }
}
