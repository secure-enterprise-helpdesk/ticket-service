package enterprise.ticket_service.repository;

import enterprise.ticket_service.entity.TicketHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketHistoryRepository extends JpaRepository<TicketHistory,Long> {
    TicketHistory findTopByTicketNumberOrderByIdDesc(String ticketNumber);
}
