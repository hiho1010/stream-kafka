package org.kafka.event;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;

@Getter
public abstract class BaseEvent {
    private final String eventId;
    private final LocalDateTime occurredAt;

    protected BaseEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = LocalDateTime.now();
    }
}