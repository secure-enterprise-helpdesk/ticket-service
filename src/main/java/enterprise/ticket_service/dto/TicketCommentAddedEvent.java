package enterprise.ticket_service.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class TicketCommentAddedEvent {

    private UUID eventId;
    private String eventType;
    private Instant timestamp;
    private String ticketId;
    private String ticketNumber;
    private String createdBy;
    private String assignedTo;
    private String commentedBy;
    private String comment;
}