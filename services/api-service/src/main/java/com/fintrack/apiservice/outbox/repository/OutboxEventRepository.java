package com.fintrack.apiservice.outbox.repository;

import com.fintrack.apiservice.outbox.entity.OutboxEvent;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Query(value = """
            SELECT *
            FROM outbox_event
            WHERE status = 'PENDING'
              AND available_at <= CURRENT_TIMESTAMP
            ORDER BY available_at ASC, id ASC
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> findAvailablePendingForUpdate(@Param("batchSize") int batchSize);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT event FROM OutboxEvent event WHERE event.id = :eventId")
    Optional<OutboxEvent> findByIdForUpdate(@Param("eventId") Long eventId);

    @Query(value = """
            SELECT *
            FROM outbox_event
            WHERE status = 'PROCESSING'
              AND locked_at <= :staleBefore
            ORDER BY locked_at ASC, id ASC
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> findStaleProcessingForUpdate(@Param("staleBefore") Instant staleBefore,
                                                   @Param("batchSize") int batchSize);

    @Query(value = """
            WITH candidates AS (
                SELECT id
                FROM outbox_event
                WHERE status = 'PUBLISHED'
                  AND published_at <= :retentionCutoff
                ORDER BY published_at ASC, id ASC
                LIMIT :batchSize
                FOR UPDATE SKIP LOCKED
            )
            DELETE FROM outbox_event
            WHERE id IN (SELECT id FROM candidates)
            """, nativeQuery = true)
    @org.springframework.data.jpa.repository.Modifying
    int deletePublishedEventsOlderThan(@Param("retentionCutoff") Instant retentionCutoff,
                                       @Param("batchSize") int batchSize);
}