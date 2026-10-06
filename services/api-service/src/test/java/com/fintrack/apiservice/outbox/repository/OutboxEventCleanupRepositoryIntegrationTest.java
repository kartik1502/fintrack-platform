package com.fintrack.apiservice.outbox.repository;

import com.fintrack.apiservice.outbox.entity.OutboxEvent;
import com.fintrack.apiservice.outbox.entity.OutboxEventStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.flyway.enabled=true"
})
class OutboxEventCleanupRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15-alpine");

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAllInBatch();
    }

    @Test
    void deletePublishedEventsOlderThanDeletesOnlyMatchingEvents() {
        Instant now = Instant.now();
        Instant cutoff = now.minus(30, ChronoUnit.DAYS);

        OutboxEvent oldPublished = createEvent();
        oldPublished.claim("owner", now);
        oldPublished.markPublished("owner", cutoff.minusSeconds(1));

        OutboxEvent exactCutoffPublished = createEvent();
        exactCutoffPublished.claim("owner", now);
        exactCutoffPublished.markPublished("owner", cutoff);

        OutboxEvent newPublished = createEvent();
        newPublished.claim("owner", now);
        newPublished.markPublished("owner", cutoff.plusSeconds(1));

        OutboxEvent oldPending = createEvent();

        OutboxEvent oldFailed = createEvent();
        oldFailed.claim("owner", now);
        oldFailed.markFailed("owner", "error");

        outboxEventRepository.saveAll(List.of(
                oldPublished, exactCutoffPublished, newPublished, oldPending, oldFailed
        ));

        int deletedCount = outboxEventRepository.deletePublishedEventsOlderThan(cutoff, 10);

        assertThat(deletedCount).isEqualTo(2);

        List<OutboxEvent> remaining = outboxEventRepository.findAll();
        assertThat(remaining).hasSize(3);
        assertThat(remaining).extracting(OutboxEvent::getId)
                .containsExactlyInAnyOrder(newPublished.getId(), oldPending.getId(), oldFailed.getId());
    }

    @Test
    void deletePublishedEventsOlderThanHonorsBatchSize() {
        Instant now = Instant.now();
        Instant cutoff = now.minus(30, ChronoUnit.DAYS);

        for (int i = 0; i < 5; i++) {
            OutboxEvent event = createEvent();
            event.claim("owner", now);
            event.markPublished("owner", cutoff.minusSeconds(10));
            outboxEventRepository.save(event);
        }

        int deletedCount = outboxEventRepository.deletePublishedEventsOlderThan(cutoff, 3);

        assertThat(deletedCount).isEqualTo(3);
        assertThat(outboxEventRepository.count()).isEqualTo(2);
    }

    private OutboxEvent createEvent() {
        return OutboxEvent.create(
                UUID.randomUUID(),
                "AGGREGATE",
                1L,
                "EVENT_TYPE",
                1,
                Map.of("key", "value")
        );
    }
}
