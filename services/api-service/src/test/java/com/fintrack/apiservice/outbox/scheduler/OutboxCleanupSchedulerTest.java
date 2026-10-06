package com.fintrack.apiservice.outbox.scheduler;

import com.fintrack.apiservice.outbox.metrics.OutboxCleanupMetrics;
import com.fintrack.apiservice.outbox.service.OutboxCleanupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxCleanupSchedulerTest {

    private static final Duration RETENTION = Duration.ofDays(30);
    private static final int BATCH_SIZE = 1000;

    @Mock
    private OutboxCleanupService outboxCleanupService;

    @Mock
    private OutboxCleanupMetrics outboxCleanupMetrics;

    private OutboxCleanupScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new OutboxCleanupScheduler(
                outboxCleanupService,
                outboxCleanupMetrics,
                RETENTION,
                BATCH_SIZE
        );
    }

    @Test
    void runCleanupCycleCallsServiceAndLogsWhenEventsAreDeleted() {
        when(outboxCleanupService.cleanupPublishedEvents(RETENTION, BATCH_SIZE)).thenReturn(500);

        scheduler.runCleanupCycle();

        verify(outboxCleanupService).cleanupPublishedEvents(RETENTION, BATCH_SIZE);
        verify(outboxCleanupMetrics, never()).recordCleanupFailure();
    }

    @Test
    void runCleanupCycleCallsServiceAndDoesNotLogWhenNoEventsDeleted() {
        when(outboxCleanupService.cleanupPublishedEvents(RETENTION, BATCH_SIZE)).thenReturn(0);

        scheduler.runCleanupCycle();

        verify(outboxCleanupService).cleanupPublishedEvents(RETENTION, BATCH_SIZE);
        verify(outboxCleanupMetrics, never()).recordCleanupFailure();
    }

    @Test
    void runCleanupCycleRecordsFailureWhenServiceThrows() {
        when(outboxCleanupService.cleanupPublishedEvents(RETENTION, BATCH_SIZE))
                .thenThrow(new IllegalStateException("Database unavailable"));

        scheduler.runCleanupCycle();

        verify(outboxCleanupMetrics).recordCleanupFailure();
    }
}
