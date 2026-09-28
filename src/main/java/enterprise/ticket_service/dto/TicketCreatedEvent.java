package enterprise.ticket_service.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class TicketCreatedEvent {

    private UUID eventId;
    private String eventType;
    private Instant timestamp;
    private String ticketId;
    private String ticketNumber;
    private String userId;
    private Integer departmentId;
    private String priority;
}