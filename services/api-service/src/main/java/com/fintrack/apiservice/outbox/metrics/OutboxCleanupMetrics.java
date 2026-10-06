package com.fintrack.apiservice.outbox.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class OutboxCleanupMetrics {

    private static final String DELETED_METRIC = "fintrack.outbox.cleanup.deleted";
    private static final String CLEANUP_FAILURE_METRIC = "fintrack.outbox.cleanup.failures";

    private final Counter deletedCounter;
    private final Counter cleanupFailureCounter;

    public OutboxCleanupMetrics(MeterRegistry meterRegistry) {
        this.deletedCounter = Counter.builder(DELETED_METRIC)
                .description("Number of published outbox events deleted during cleanup")
                .register(meterRegistry);

        this.cleanupFailureCounter = Counter.builder(CLEANUP_FAILURE_METRIC)
                .description("Number of failed outbox cleanup cycles")
                .register(meterRegistry);
    }

    public void recordDeleted(int count) {
        if (count > 0) {
            deletedCounter.increment(count);
        }
    }

    public void recordCleanupFailure() {
        cleanupFailureCounter.increment();
    }
}
