package com.fintrack.apiservice.outbox.service;

import com.fintrack.apiservice.outbox.metrics.OutboxCleanupMetrics;
import com.fintrack.apiservice.outbox.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxCleanupServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private OutboxCleanupMetrics outboxCleanupMetrics;

    @InjectMocks
    private OutboxCleanupService outboxCleanupService;

    @Test
    void cleanupPublishedEventsDelegatesToRepositoryAndRecordsMetrics() {
        when(outboxEventRepository.deletePublishedEventsOlderThan(any(Instant.class), eq(500))).thenReturn(42);

        int result = outboxCleanupService.cleanupPublishedEvents(Duration.ofDays(30), 500);

        assertThat(result).isEqualTo(42);

        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(outboxEventRepository).deletePublishedEventsOlderThan(cutoffCaptor.capture(), eq(500));

        Instant cutoff = cutoffCaptor.getValue();
        assertThat(cutoff).isBefore(Instant.now());

        verify(outboxCleanupMetrics).recordDeleted(42);
    }

    @Test
    void cleanupPublishedEventsRejectsNullRetention() {
        assertThatThrownBy(() -> outboxCleanupService.cleanupPublishedEvents(null, 500))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Retention period must be non-negative");

        verifyNoInteractions(outboxEventRepository);
        verifyNoInteractions(outboxCleanupMetrics);
    }

    @Test
    void cleanupPublishedEventsRejectsNegativeRetention() {
        assertThatThrownBy(() -> outboxCleanupService.cleanupPublishedEvents(Duration.ofDays(-1), 500))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Retention period must be non-negative");

        verifyNoInteractions(outboxEventRepository);
        verifyNoInteractions(outboxCleanupMetrics);
    }

    @Test
    void cleanupPublishedEventsRejectsInvalidBatchSize() {
        assertThatThrownBy(() -> outboxCleanupService.cleanupPublishedEvents(Duration.ofDays(30), 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Batch size must be positive");

        verifyNoInteractions(outboxEventRepository);
        verifyNoInteractions(outboxCleanupMetrics);
    }
}
