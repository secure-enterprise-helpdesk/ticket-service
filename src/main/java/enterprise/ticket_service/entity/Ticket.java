package enterprise.ticket_service.entity;

import enterprise.ticket_service.enums.Priority;
import enterprise.ticket_service.enums.TicketStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "tickets",
        indexes = {
                @Index(name = "idx_ticket_created_by", columnList = "created_by"),
                @Index(name = "idx_ticket_assigned_to", columnList = "assigned_to"),
                @Index(name = "idx_ticket_department", columnList = "department_id"),
                @Index(name = "idx_ticket_status", columnList = "status")
        }
)
@Getter
@Setter
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_number", nullable = false, unique = true, length = 30)
    private String ticketNumber;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 50)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketStatus status;

    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;

    @Column(name = "assigned_to", length = 50)
    private String assignedTo;

    @Column(name = "department_id", nullable = false)
    private Integer departmentId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}