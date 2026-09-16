package enterprise.ticket_service.service;

import enterprise.ticket_service.dto.GlobalReq;
import enterprise.ticket_service.dto.Request;
import enterprise.ticket_service.entity.Ticket;
import enterprise.ticket_service.entity.TicketComment;
import enterprise.ticket_service.entity.TicketHistory;
import enterprise.ticket_service.enums.Priority;
import enterprise.ticket_service.enums.TicketStatus;
import enterprise.ticket_service.mapper.TicketMapper;
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

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    @Autowired
    private TicketHistoryRepository ticketHistoryRepository;
    @Autowired
    private TicketCommentRepository ticketCommentRepository;
    private final TicketMapper ticketMapper;

    public ResponseEntity<String> createTicket(
            Request request,
            Authentication authentication) {

        Jwt jwt = (Jwt) authentication.getPrincipal();
        String userId = jwt.getSubject();

        Ticket ticket = ticketMapper.toTicket(request);

        ticket.setCreatedBy(userId);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setPriority(Priority.MEDIUM);
        ticket.setTicketNumber("TCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        ticketRepository.save(ticket);
        ticketHistoryRepository.save(
                createHistory(ticket.getTicketNumber(), "CREATE", null, TicketStatus.OPEN.toString(), userId)
        );
        return ResponseEntity.ok("Ticket created successfully, Ticket Number: " + ticket.getTicketNumber());
    }

    public ResponseEntity<String> viewTicketById(String ticketNumber, Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();

        String userId = jwt.getSubject();
        String role = jwt.getClaimAsString("role");

        Ticket ticket = ticketRepository.findByTicketNumber(ticketNumber).orElse(null);

        if ("Employee".equals(role) && (ticket == null || !ticket.getCreatedBy().equals(userId))) {
            ticket = null;
        } else if ("Agent".equals(role) && (ticket == null || !ticket.getAssignedTo().equals(userId))) {
            ticket = null;
        }

        return ResponseEntity.ok("Tickets: " + ticket);
    }

    public ResponseEntity<Page<Ticket>> viewTicket(Authentication authentication, Pageable pageable) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String userId = jwt.getSubject();
        String role = jwt.getClaimAsString("role");
        Page<Ticket> tickets = Page.empty();
        if ("Employee".equals(role)) {
            tickets = ticketRepository.findByCreatedBy(userId, pageable);
        } else if ("Agent".equals(role)) {
            tickets = ticketRepository.findByAssignedTo(userId, pageable);
        }else if ("Manager".equals(role)) {
            tickets = ticketRepository.findAll(pageable);
        }
        return ResponseEntity.ok(tickets);
    }

    public ResponseEntity<String> updateTicket(GlobalReq globalReq, Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String userId = jwt.getSubject();
        String role = jwt.getClaimAsString("role");
        String ticketNumber = globalReq.getString("ticketNumber");
        String assignedTo = globalReq.getString("assignedTo");
        String status = globalReq.getString("status");
        Ticket ticket = ticketRepository.findByTicketNumber(ticketNumber).orElse(null);
        if (ticket == null) {
            return ResponseEntity.badRequest().body("Ticket not found");
        }
        if ("Manager".equals(role)) {
            if (assignedTo != null && !assignedTo.equals(ticket.getAssignedTo())) {
                String oldAssignedTo = ticket.getAssignedTo();
                ticket.setAssignedTo(assignedTo);
                if (ticket.getStatus() == null || ticket.getStatus() == TicketStatus.OPEN) {
                    ticket.setStatus(TicketStatus.ASSIGNED);
                }
                ticketHistoryRepository.save(createHistory(ticket.getTicketNumber(), "ASSIGNED", oldAssignedTo, assignedTo, userId));
            }

            if (status != null) {
                TicketStatus newStatus = parseStatus(status);
                if (newStatus != null && ticket.getStatus() != newStatus) {
                    String oldStatus = ticket.getStatus() != null ? ticket.getStatus().name() : null;
                    ticket.setStatus(newStatus);
                    ticketHistoryRepository.save(
                            createHistory(ticket.getTicketNumber(), newStatus.name(), oldStatus, newStatus.name(), userId)
                    );
                }
            }
        } else if ("Agent".equals(role)) {
            if (status == null) {
                return ResponseEntity.badRequest().body("Status is required");
            }

            TicketStatus newStatus = parseStatus(status);
            if (newStatus == null) {
                return ResponseEntity.badRequest().body("Invalid ticket status");
            }

            if (ticket.getStatus() != newStatus) {
                String oldStatus = ticket.getStatus() != null ? ticket.getStatus().name() : null;
                ticket.setStatus(newStatus);
                ticketHistoryRepository.save(
                        createHistory(ticket.getTicketNumber(), newStatus.name(), oldStatus, newStatus.name(), userId)
                );
            }
        } else {
            return ResponseEntity.status(403).body("You are not authorized to update this ticket");
        }
        ticketRepository.save(ticket);
        return ResponseEntity.ok("Ticket updated successfully");
    }
    private TicketHistory createHistory(String ticketNumber, String action, String oldValue, String newValue, String changedBy) {
        TicketHistory ticketHistory = new TicketHistory();
        ticketHistory.setTicketNumber(ticketNumber);
        ticketHistory.setAction(action);
        ticketHistory.setOldValue(oldValue);
        ticketHistory.setNewValue(newValue);
        ticketHistory.setChangedBy(changedBy);
        return ticketHistory;
    }

    private TicketStatus parseStatus(String statusValue) {
        if (statusValue == null || statusValue.trim().isEmpty()) {
            return null;
        }
        return TicketStatus.valueOf(statusValue.trim().toUpperCase());
    }

    public ResponseEntity<String> commentOnTicket(GlobalReq globalReq, Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String userId = jwt.getSubject();
        String role = jwt.getClaimAsString("role");

        String ticketNumber = globalReq.getString("ticketNumber");
        String comment = globalReq.getString("comment");

        Ticket ticket = ticketRepository.findByTicketNumber(ticketNumber).orElse(null);

        if (ticket == null) {
            return ResponseEntity.badRequest().body("Ticket not found");
        }
        if ((role.equals("Employee") && !ticket.getCreatedBy().equals(userId)) ||
                (role.equals("Agent") && !ticket.getAssignedTo().equals(userId))) {
            return ResponseEntity.status(403).body("You are not authorized to comment on this ticket");
        }

        TicketComment ticketComment = new TicketComment();
        ticketComment.setTicketNumber(ticketNumber);
        ticketComment.setCommentedBy(userId);
        ticketComment.setComment(comment);

        ticketCommentRepository.save(ticketComment);

        return ResponseEntity.ok("Comment added successfully");
    }

    public ResponseEntity<Page<TicketComment>> viewTicketComments(String ticketNumber, Authentication authentication, Pageable pageable) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String userId = jwt.getSubject();
        String role = jwt.getClaimAsString("role");
        Ticket ticket = ticketRepository.findByTicketNumber(ticketNumber).orElse(null);

        if (ticket == null) {
            return ResponseEntity.badRequest().body(null);
        }
        if ((role.equals("Employee") && !ticket.getCreatedBy().equals(userId)) ||
                (role.equals("Agent") && !ticket.getAssignedTo().equals(userId))) {
            return ResponseEntity.status(403).body(null);
        }
        Page<TicketComment> ticketComments = ticketCommentRepository.findByTicketNumber(ticketNumber, pageable);
        return ResponseEntity.ok(ticketComments);
    }
}