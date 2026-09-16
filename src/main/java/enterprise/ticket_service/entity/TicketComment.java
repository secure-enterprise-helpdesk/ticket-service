package enterprise.ticket_service.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
@Data
@Entity
public class TicketComment {
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;
    @Column(name = "ticket_number", nullable = false, length = 30)
    private String ticketNumber;
    @Column(name = "commented_by", nullable = false, length = 100)
    private String commentedBy;
    @Column(name = "comment", nullable = false, length = 500)
    private String comment;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
