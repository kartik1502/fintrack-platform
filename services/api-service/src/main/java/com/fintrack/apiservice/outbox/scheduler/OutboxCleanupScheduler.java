package com.fintrack.apiservice.outbox.scheduler;

import com.fintrack.apiservice.outbox.metrics.OutboxCleanupMetrics;
import com.fintrack.apiservice.outbox.service.OutboxCleanupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConditionalOnProperty(name = "fintrack.outbox.cleanup.enabled", havingValue = "true")
public class OutboxCleanupScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxCleanupScheduler.class);

    private final OutboxCleanupService outboxCleanupService;
    private final OutboxCleanupMetrics outboxCleanupMetrics;
    private final Duration retentionDuration;
    private final int batchSize;

    public OutboxCleanupScheduler(OutboxCleanupService outboxCleanupService,
                                  OutboxCleanupMetrics outboxCleanupMetrics,
                                  @Value("${fintrack.outbox.cleanup.retention-duration:30d}") Duration retentionDuration,
                                  @Value("${fintrack.outbox.cleanup.batch-size:1000}") int batchSize) {
        this.outboxCleanupService = outboxCleanupService;
        this.outboxCleanupMetrics = outboxCleanupMetrics;
        this.retentionDuration = retentionDuration;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${fintrack.outbox.cleanup.fixed-delay-ms:3600000}")
    public void runCleanupCycle() {
        try {
            int deletedCount = outboxCleanupService.cleanupPublishedEvents(retentionDuration, batchSize);

            if (deletedCount > 0) {
                LOGGER.info("Completed outbox cleanup cycle: deletedCount={}", deletedCount);
            }
        } catch (Exception exception) {
            outboxCleanupMetrics.recordCleanupFailure();
            LOGGER.error("Outbox cleanup cycle failed", exception);
        }
    }
}
