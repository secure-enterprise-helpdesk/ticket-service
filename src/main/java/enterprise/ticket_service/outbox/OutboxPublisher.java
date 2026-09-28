package enterprise.ticket_service.outbox;

import enterprise.ticket_service.enums.OutboxStatus;
import enterprise.ticket_service.outbox.entity.OutboxEvent;
import enterprise.ticket_service.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private static final String TOPIC = "ticket-events";

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 5000)
    public void publishEvents() {

        List<OutboxEvent> events =
                outboxEventRepository.findByStatusOrderByCreatedAtAsc(
                        OutboxStatus.PENDING);

        for (OutboxEvent event : events) {
            publish(event);
        }
    }

    private void publish(OutboxEvent event) {

        try {
            kafkaTemplate.send(
                    TOPIC,
                    event.getAggregateId(),
                    event.getPayload()
            ).get();

            event.setStatus(OutboxStatus.PUBLISHED);
            event.setPublishedAt(Instant.now());

            outboxEventRepository.save(event);

            System.out.println(
                    "Event published successfully: " + event.getEventId());

        } catch (Exception e) {

            event.setRetryCount(event.getRetryCount() + 1);
            event.setStatus(OutboxStatus.FAILED);

            outboxEventRepository.save(event);

            System.err.println(
                    "Failed to publish event: " + event.getEventId());

            e.printStackTrace();
        }
    }
}