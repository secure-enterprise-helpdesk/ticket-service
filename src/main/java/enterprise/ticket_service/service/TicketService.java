package enterprise.ticket_service.service;

import enterprise.ticket_service.dto.GlobalReq;
import enterprise.ticket_service.dto.Request;
import enterprise.ticket_service.entity.Ticket;
import enterprise.ticket_service.entity.TicketComment;
import enterprise.ticket_service.entity.TicketHistory;
import enterprise.ticket_service.enums.Priority;
import enterprise.ticket_service.enums.TicketStatus;
import enterprise.ticket_service.mapper.TicketMapper;
import enterprise.ticket_service.outbox.service.OutboxService;
import enterprise.ticket_service.repository.TicketCommentRepository;
import enterprise.ticket_service.repository.TicketHistoryRepository;
import enterprise.ticket_service.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketHistoryRepository ticketHistoryRepository;
    private final TicketCommentRepository ticketCommentRepository;
    private final OutboxService outboxService;
    private final TicketMapper ticketMapper;

    @Transactional
    public ResponseEntity<String> createTicket(Request request, Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String userId = jwt.getSubject();

        Ticket ticket = ticketMapper.toTicket(request);
        ticket.setCreatedBy(userId);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setPriority(Priority.MEDIUM);
        ticket.setTicketNumber("TCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        ticketRepository.save(ticket);
        ticketHistoryRepository.save(createHistory(ticket.getTicketNumber(), "CREATE", null, TicketStatus.OPEN.name(), userId));
        outboxService.saveTicketCreatedEvent(ticket, userId);

        return ResponseEntity.ok("Ticket created successfully, Ticket Number: " + ticket.getTicketNumber());
    }

    public ResponseEntity<String> viewTicketById(String ticketNumber, Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String userId = jwt.getSubject();
        String role = jwt.getClaimAsString("role");

        Ticket ticket = ticketRepository.findByTicketNumber(ticketNumber).orElse(null);

        if (ticket == null)
            return ResponseEntity.status(404).body("Ticket not found");

        if ("Employee".equals(role) && !ticket.getCreatedBy().equals(userId))
            return ResponseEntity.status(403).body("You are not authorized to view this ticket");

        if ("Agent".equals(role) && (ticket.getAssignedTo() == null || !ticket.getAssignedTo().equals(userId)))
            return ResponseEntity.status(403).body("You are not authorized to view this ticket");

        return ResponseEntity.ok("Ticket: " + ticket);
    }

    public ResponseEntity<Page<Ticket>> viewTicket(Authentication authentication, Pageable pageable) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String userId = jwt.getSubject();
        String role = jwt.getClaimAsString("role");

        Page<Ticket> tickets = Page.empty();

        if ("Employee".equals(role))
            tickets = ticketRepository.findByCreatedBy(userId, pageable);
        else if ("Agent".equals(role))
            tickets = ticketRepository.findByAssignedTo(userId, pageable);
        else if ("Manager".equals(role))
            tickets = ticketRepository.findAll(pageable);

        return ResponseEntity.ok(tickets);
    }

    @Transactional
    public ResponseEntity<String> updateTicket(GlobalReq globalReq, Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String userId = jwt.getSubject();
        String role = jwt.getClaimAsString("role");

        String ticketNumber = globalReq.getString("ticketNumber");
        String assignedTo = globalReq.getString("assignedTo");
        String status = globalReq.getString("status");

        Ticket ticket = ticketRepository.findByTicketNumber(ticketNumber).orElse(null);

        if (ticket == null)
            return ResponseEntity.status(404).body("Ticket not found");

        if ("Manager".equals(role)) {

            if (assignedTo != null && !assignedTo.equals(ticket.getAssignedTo())) {
                String oldAssignedTo = ticket.getAssignedTo();
                ticket.setAssignedTo(assignedTo);

                if (ticket.getStatus() == TicketStatus.OPEN) {
                    ticket.setStatus(TicketStatus.ASSIGNED);
                    ticketHistoryRepository.save(createHistory(ticket.getTicketNumber(), "STATUS_CHANGE",
                            TicketStatus.OPEN.name(), TicketStatus.ASSIGNED.name(), userId));
                }

                ticketHistoryRepository.save(createHistory(ticket.getTicketNumber(), "ASSIGNED",
                        oldAssignedTo, assignedTo, userId));
            }

            if (status != null) {
                TicketStatus newStatus = parseStatus(status);

                if (newStatus == null)
                    return ResponseEntity.badRequest().body("Invalid ticket status");

                if (ticket.getStatus() != newStatus) {
                    TicketStatus currentStatus = ticket.getStatus();

                    if (!isValidTransition(currentStatus, newStatus))
                        return ResponseEntity.badRequest().body("Invalid status transition: " + currentStatus + " -> " + newStatus);

                    ticket.setStatus(newStatus);
                    ticketHistoryRepository.save(createHistory(ticket.getTicketNumber(), "STATUS_CHANGE",
                            currentStatus.name(), newStatus.name(), userId));
                }
            }

        } else if ("Agent".equals(role)) {

            if (ticket.getAssignedTo() == null || !ticket.getAssignedTo().equals(userId))
                return ResponseEntity.status(403).body("You are not authorized to update this ticket");

            if (status == null)
                return ResponseEntity.badRequest().body("Status is required");

            TicketStatus newStatus = parseStatus(status);

            if (newStatus == null)
                return ResponseEntity.badRequest().body("Invalid ticket status");

            if (ticket.getStatus() != newStatus) {
                TicketStatus currentStatus = ticket.getStatus();

                if (!isValidTransition(currentStatus, newStatus))
                    return ResponseEntity.badRequest().body("Invalid status transition: " + currentStatus + " -> " + newStatus);

                ticket.setStatus(newStatus);
                ticketHistoryRepository.save(createHistory(ticket.getTicketNumber(), "STATUS_CHANGE",
                        currentStatus.name(), newStatus.name(), userId));
            }

        } else {
            return ResponseEntity.status(403).body("You are not authorized to update this ticket");
        }

       Ticket tk = ticketRepository.save(ticket);
        if ("Manager".equals(role)){
            outboxService.saveTicketAssignedEvent(tk,userId);
        }
        if ("Agent".equals(role)){
            outboxService.saveTicketStatusChangedEvent(tk, userId);
        }
        return ResponseEntity.ok("Ticket updated successfully");
    }

    @Transactional
    public ResponseEntity<String> commentOnTicket(GlobalReq globalReq, Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String userId = jwt.getSubject();
        String role = jwt.getClaimAsString("role");

        String ticketNumber = globalReq.getString("ticketNumber");
        String comment = globalReq.getString("comment");

        Ticket ticket = ticketRepository.findByTicketNumber(ticketNumber).orElse(null);

        if (ticket == null)
            return ResponseEntity.status(404).body("Ticket not found");

        if ("Employee".equals(role) && !ticket.getCreatedBy().equals(userId))
            return ResponseEntity.status(403).body("You are not authorized to comment on this ticket");

        if ("Agent".equals(role) && (ticket.getAssignedTo() == null || !ticket.getAssignedTo().equals(userId)))
            return ResponseEntity.status(403).body("You are not authorized to comment on this ticket");

        TicketComment ticketComment = new TicketComment();
        ticketComment.setTicketNumber(ticketNumber);
        ticketComment.setCommentedBy(userId);
        ticketComment.setComment(comment);

        TicketComment ticketComments=ticketCommentRepository.save(ticketComment);
        outboxService.saveTicketCommentAddedEvent(ticket, ticketComments, userId);

        return ResponseEntity.ok("Comment added successfully");
    }

    public ResponseEntity<Page<TicketComment>> viewTicketComments(
            String ticketNumber, Authentication authentication, Pageable pageable) {

        Jwt jwt = (Jwt) authentication.getPrincipal();
        String userId = jwt.getSubject();
        String role = jwt.getClaimAsString("role");

        Ticket ticket = ticketRepository.findByTicketNumber(ticketNumber).orElse(null);

        if (ticket == null)
            return ResponseEntity.status(404).body(null);

        if ("Employee".equals(role) && !ticket.getCreatedBy().equals(userId))
            return ResponseEntity.status(403).body(null);

        if ("Agent".equals(role) && (ticket.getAssignedTo() == null || !ticket.getAssignedTo().equals(userId)))
            return ResponseEntity.status(403).body(null);

        Page<TicketComment> comments = ticketCommentRepository.findByTicketNumber(ticketNumber, pageable);
        return ResponseEntity.ok(comments);
    }

    public ResponseEntity<Page<TicketHistory>> viewTicketHistory(
            String ticketNumber, Authentication authentication, Pageable pageable) {

        Jwt jwt = (Jwt) authentication.getPrincipal();
        String userId = jwt.getSubject();
        String role = jwt.getClaimAsString("role");

        Ticket ticket = ticketRepository.findByTicketNumber(ticketNumber).orElse(null);

        if (ticket == null)
            return ResponseEntity.status(404).body(null);

        if ("Employee".equals(role) && !ticket.getCreatedBy().equals(userId))
            return ResponseEntity.status(403).body(null);

        if ("Agent".equals(role) && (ticket.getAssignedTo() == null || !ticket.getAssignedTo().equals(userId)))
            return ResponseEntity.status(403).body(null);

        Page<TicketHistory> history = ticketHistoryRepository.findByTicketNumber(ticketNumber, pageable);
        return ResponseEntity.ok(history);
    }

    private boolean isValidTransition(TicketStatus current, TicketStatus next) {
        if (current == null || next == null)
            return false;

        return switch (current) {
            case OPEN -> next == TicketStatus.ASSIGNED;
            case ASSIGNED -> next == TicketStatus.IN_PROGRESS;
            case IN_PROGRESS -> next == TicketStatus.RESOLVED;
            case RESOLVED -> next == TicketStatus.CLOSED || next == TicketStatus.REOPENED;
            case REOPENED -> next == TicketStatus.IN_PROGRESS;
            case CLOSED -> false;
        };
    }

    private TicketStatus parseStatus(String statusValue) {
        if (statusValue == null || statusValue.trim().isEmpty())
            return null;

        try {
            return TicketStatus.valueOf(statusValue.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private TicketHistory createHistory(
            String ticketNumber, String action, String oldValue, String newValue, String changedBy) {

        TicketHistory history = new TicketHistory();
        history.setTicketNumber(ticketNumber);
        history.setAction(action);
        history.setOldValue(oldValue);
        history.setNewValue(newValue);
        history.setChangedBy(changedBy);
        return history;
    }
}