package enterprise.ticket_service.repository;

import enterprise.ticket_service.entity.Ticket;
import enterprise.ticket_service.enums.TicketStatus;
import feign.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Page<Ticket> findByCreatedBy(String userId, Pageable pageable);

    Page<Ticket> findByAssignedTo(String userId, Pageable pageable);

    Optional<Ticket> findByTicketNumber(String ticketNumber);

    @Query("""
        SELECT t.assignedTo, COUNT(t)
        FROM Ticket t
        WHERE t.status NOT IN :statuses
        AND t.assignedTo IS NOT NULL
        GROUP BY t.assignedTo
    """)
    List<Object[]> findAgentWorkloads(@Param("statuses") List<TicketStatus> statuses);

    @Query("""
        SELECT t.ticketNumber
        FROM Ticket t
        WHERE t.status = :status
        AND t.assignedTo IS NULL
    """)
    List<String> findPendingTicketNumbers(@Param("status") TicketStatus status);
}
