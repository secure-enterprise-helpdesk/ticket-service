package enterprise.ticket_service.outbox.entity;


import enterprise.ticket_service.enums.OutboxStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;


@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID eventId;

    @Column(nullable = false, length = 50)
    private String aggregateType;

    @Column(nullable = false, length = 100)
    private String aggregateId;

    @Column(nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant publishedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    @Column(nullable = false)
    private int retryCount;
}
