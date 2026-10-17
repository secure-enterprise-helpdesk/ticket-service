package enterprise.ticket_service.outbox.repository;

import enterprise.ticket_service.enums.OutboxStatus;
import enterprise.ticket_service.outbox.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Query("""
            select event from OutboxEvent event
            where event.status in :statuses
              and event.retryCount < :maxRetries
              and (event.nextAttemptAt is null or event.nextAttemptAt <= :now)
            order by event.createdAt asc
            """)
    List<OutboxEvent> findReadyToPublish(
            @Param("statuses") List<OutboxStatus> statuses,
            @Param("maxRetries") int maxRetries,
            @Param("now") Instant now
    );
}