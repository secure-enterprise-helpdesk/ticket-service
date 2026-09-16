package enterprise.ticket_service.repository;

import enterprise.ticket_service.entity.TicketComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketCommentRepository extends JpaRepository<TicketComment, Integer> {
    Page<TicketComment> findByTicketNumber(String ticketNumber, Pageable pageable);
}
