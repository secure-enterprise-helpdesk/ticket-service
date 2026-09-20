package enterprise.ticket_service.scheduler;

import enterprise.ticket_service.service.TicketAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TicketAssignmentScheduler {

    private final TicketAssignmentService ticketAssignmentService;

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Kolkata")
    public void assignPendingTickets() {
        ticketAssignmentService.assignPendingTickets();
    }
}