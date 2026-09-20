package enterprise.ticket_service.controller;

import enterprise.ticket_service.dto.GlobalReq;
import enterprise.ticket_service.dto.Request;
import enterprise.ticket_service.entity.Ticket;
import enterprise.ticket_service.entity.TicketComment;
import enterprise.ticket_service.entity.TicketHistory;
import enterprise.ticket_service.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {

    private TicketService ticketService;

    @PostMapping("/create")
    public ResponseEntity<String> createTicket(@RequestBody Request request, Authentication authentication) {
        return ticketService.createTicket(request, authentication);
    }

    @GetMapping("/view")
    public ResponseEntity<Page<Ticket>> viewTickets(Authentication authentication, Pageable pageable) {
        return ticketService.viewTicket(authentication, pageable);
    }

    @GetMapping("/view/id")
    public ResponseEntity<String> viewTicketById(@RequestParam String ticketNumber, Authentication authentication) {
        return ticketService.viewTicketById(ticketNumber, authentication);
    }

    @PostMapping("/update")
    public ResponseEntity<String> updateTicket(@RequestBody GlobalReq globalReq, Authentication authentication) {
        return ticketService.updateTicket(globalReq, authentication);
    }

    @PostMapping("/comment")
    public ResponseEntity<String> commentOnTicket(@RequestBody GlobalReq globalReq, Authentication authentication) {
        return ticketService.commentOnTicket(globalReq, authentication);
    }

    @GetMapping("/comments")
    public ResponseEntity<Page<TicketComment>> viewTicketComments(@RequestParam String ticketNumber, Authentication authentication, Pageable pageable) {
        return ticketService.viewTicketComments(ticketNumber, authentication, pageable);
    }

    @GetMapping("/history")
    public ResponseEntity<Page<TicketHistory>> viewTicketHistory(@RequestParam String ticketNumber, Authentication authentication, Pageable pageable) {
        return ticketService.viewTicketHistory(ticketNumber, authentication, pageable);
    }
}