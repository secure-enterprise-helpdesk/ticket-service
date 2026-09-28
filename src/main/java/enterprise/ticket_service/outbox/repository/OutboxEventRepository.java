package enterprise.ticket_service.outbox.repository;

import enterprise.ticket_service.enums.OutboxStatus;
import enterprise.ticket_service.outbox.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxStatus status);
}