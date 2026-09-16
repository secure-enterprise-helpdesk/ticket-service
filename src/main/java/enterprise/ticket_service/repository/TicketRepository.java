package enterprise.ticket_service.repository;

import enterprise.ticket_service.entity.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Page<Ticket> findByCreatedBy(String userId, Pageable pageable);

    Page<Ticket> findByAssignedTo(String userId, Pageable pageable);

    Optional<Ticket> findByTicketNumber(String ticketNumber);
}
