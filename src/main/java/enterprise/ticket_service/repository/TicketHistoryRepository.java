package enterprise.ticket_service.repository;

import enterprise.ticket_service.entity.TicketHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketHistoryRepository extends JpaRepository<TicketHistory,Long> {
    TicketHistory findTopByTicketNumberOrderByIdDesc(String ticketNumber);
    Page<TicketHistory> findByTicketNumber(String ticketNumber, Pageable pageable);
}
