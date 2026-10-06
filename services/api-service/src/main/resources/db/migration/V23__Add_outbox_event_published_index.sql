CREATE INDEX idx_outbox_event_published_cleanup
    ON outbox_event (published_at, id)
    WHERE status = 'PUBLISHED';
