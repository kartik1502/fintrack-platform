package com.fintrack.apiservice.outbox.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxCleanupMetricsTest {

    private MeterRegistry meterRegistry;
    private OutboxCleanupMetrics outboxCleanupMetrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        outboxCleanupMetrics = new OutboxCleanupMetrics(meterRegistry);
    }

    @Test
    void recordDeletedIncrementsCounter() {
        outboxCleanupMetrics.recordDeleted(5);

        Counter counter = meterRegistry.find("fintrack.outbox.cleanup.deleted").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(5.0);
    }

    @Test
    void recordDeletedIgnoresZeroOrNegative() {
        outboxCleanupMetrics.recordDeleted(0);
        outboxCleanupMetrics.recordDeleted(-1);

        Counter counter = meterRegistry.find("fintrack.outbox.cleanup.deleted").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(0.0);
    }

    @Test
    void recordCleanupFailureIncrementsCounter() {
        outboxCleanupMetrics.recordCleanupFailure();

        Counter counter = meterRegistry.find("fintrack.outbox.cleanup.failures").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }
}
