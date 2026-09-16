package enterprise.ticket_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "ticket_history",
        indexes = {
                @Index(name = "idx_history_ticket", columnList = "ticket_number"),
                @Index(name = "idx_history_changed_at", columnList = "changed_at")
        }
)
@Getter
@Setter
public class TicketHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_number", nullable = false, length = 30)
    private String ticketNumber;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(name = "old_value", length = 255)
    private String oldValue;

    @Column(name = "new_value", length = 255)
    private String newValue;

    @Column(name = "changed_by", nullable = false, length = 50)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @PrePersist
    protected void onCreate() {
        changedAt = Instant.now();
    }
}