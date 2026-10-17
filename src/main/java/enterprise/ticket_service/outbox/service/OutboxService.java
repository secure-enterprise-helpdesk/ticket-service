package enterprise.ticket_service.outbox.service;

import enterprise.ticket_service.dto.TicketAssignedEvent;
import enterprise.ticket_service.dto.TicketCommentAddedEvent;
import enterprise.ticket_service.dto.TicketCreatedEvent;
import enterprise.ticket_service.dto.TicketStatusChangedEvent;
import enterprise.ticket_service.entity.Ticket;
import enterprise.ticket_service.entity.TicketComment;
import enterprise.ticket_service.enums.OutboxStatus;
import enterprise.ticket_service.enums.TicketEventType;
import enterprise.ticket_service.outbox.entity.OutboxEvent;
import enterprise.ticket_service.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.json.JsonParseException;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public void saveTicketCreatedEvent(Ticket ticket, String userId) {

        try {
            UUID eventId = UUID.randomUUID();

            TicketCreatedEvent event = TicketCreatedEvent.builder()
                    .eventId(eventId)
                    .eventType("TICKET_CREATED")
                    .timestamp(Instant.now())
                    .ticketId(ticket.getId().toString())
                    .ticketNumber(ticket.getTicketNumber())
                    .userId(userId)
                    .departmentId(ticket.getDepartmentId())
                    .priority(ticket.getPriority().name())
                    .build();

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventId(eventId)
                    .aggregateType("TICKET")
                    .aggregateId(ticket.getTicketNumber())
                    .eventType("TICKET_CREATED")
                    .payload(objectMapper.writeValueAsString(event))
                    .createdAt(Instant.now())
                    .status(OutboxStatus.PENDING)
                    .retryCount(0)
                    .build();

            outboxEventRepository.save(outboxEvent);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create outbox event", e);
        }
    }

    public  void saveTicketAssignedEvent(Ticket ticket, String userId) {
        UUID eventId = UUID.randomUUID();
        TicketAssignedEvent event = TicketAssignedEvent.builder()
                .eventId(eventId)
                .eventType(TicketEventType.TICKET_ASSIGNED.toString())
                .timestamp(ticket.getUpdatedAt())
                .changedBy(userId)
                .assignedTo(ticket.getAssignedTo())
                .createdBy(ticket.getCreatedBy())
                .ticketId(ticket.getId().toString())
                .ticketNumber(ticket.getTicketNumber())
                .build();

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .eventId(eventId)
                .aggregateType("TICKET")
                .aggregateId(ticket.getTicketNumber())
                .eventType(TicketEventType.TICKET_ASSIGNED.toString())
                .payload(objectMapper.writeValueAsString(event))
                .createdAt(Instant.now())
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();

        outboxEventRepository.save(outboxEvent);
    }

    public void saveTicketStatusChangedEvent(
            Ticket ticket,
            String userId,
            String oldStatus,
            String newStatus
    ) {
        UUID eventId = UUID.randomUUID();
        TicketStatusChangedEvent event = TicketStatusChangedEvent.builder()
                .eventId(eventId)
                .eventType(TicketEventType.TICKET_STATUS_CHANGED.toString())
                .timestamp(ticket.getUpdatedAt())
                .changedBy(userId)
                .assignedTo(ticket.getAssignedTo())
                .createdBy(ticket.getCreatedBy())
                .ticketId(ticket.getId().toString())
                .ticketNumber(ticket.getTicketNumber())
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .build();

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .eventId(eventId)
                .aggregateType("TICKET")
                .aggregateId(ticket.getTicketNumber())
                .eventType(TicketEventType.TICKET_STATUS_CHANGED.toString())
                .payload(objectMapper.writeValueAsString(event))
                .createdAt(Instant.now())
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();

        outboxEventRepository.save(outboxEvent);
    }

    public  void saveTicketCommentAddedEvent(Ticket ticket, TicketComment ticketComment, String userId) {
        UUID eventId = UUID.randomUUID();
        TicketCommentAddedEvent event = TicketCommentAddedEvent.builder()
                .eventId(eventId)
                .eventType(TicketEventType.TICKET_COMMENT_ADDED.toString())
                .timestamp(ticketComment.getCreatedAt())
                .assignedTo(ticket.getAssignedTo())
                .createdBy(ticket.getCreatedBy())
                .ticketId(ticket.getId().toString())
                .commentedBy(userId)
                .comment(ticketComment.getComment())
                .ticketNumber(ticket.getTicketNumber())
                .build();

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .eventId(eventId)
                .aggregateType("TICKET")
                .aggregateId(ticket.getTicketNumber())
                .eventType(TicketEventType.TICKET_COMMENT_ADDED.toString())
                .payload(objectMapper.writeValueAsString(event))
                .createdAt(Instant.now())
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();

        outboxEventRepository.save(outboxEvent);
    }

}
