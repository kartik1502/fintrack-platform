package com.fintrack.apiservice.outbox.service;

import com.fintrack.apiservice.outbox.metrics.OutboxCleanupMetrics;
import com.fintrack.apiservice.outbox.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
public class OutboxCleanupService {

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxCleanupMetrics outboxCleanupMetrics;

    public OutboxCleanupService(OutboxEventRepository outboxEventRepository,
                                OutboxCleanupMetrics outboxCleanupMetrics) {
        this.outboxEventRepository = outboxEventRepository;
        this.outboxCleanupMetrics = outboxCleanupMetrics;
    }

    @Transactional
    public int cleanupPublishedEvents(Duration retentionPeriod, int batchSize) {
        if (retentionPeriod == null || retentionPeriod.isNegative()) {
            throw new IllegalArgumentException("Retention period must be non-negative");
        }
        if (batchSize < 1) {
            throw new IllegalArgumentException("Batch size must be positive");
        }

        Instant retentionCutoff = Instant.now().minus(retentionPeriod);
        int deletedCount = outboxEventRepository.deletePublishedEventsOlderThan(retentionCutoff, batchSize);

        outboxCleanupMetrics.recordDeleted(deletedCount);

        return deletedCount;
    }
}
