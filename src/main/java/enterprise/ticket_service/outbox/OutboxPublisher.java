package enterprise.ticket_service.outbox;

import enterprise.ticket_service.enums.OutboxStatus;
import enterprise.ticket_service.outbox.entity.OutboxEvent;
import enterprise.ticket_service.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private static final String TOPIC = "ticket-events";
    private static final int MAX_ATTEMPTS = 10;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 5000)
    public void publishEvents() {

        List<OutboxEvent> events = outboxEventRepository.findReadyToPublish(
                List.of(OutboxStatus.PENDING, OutboxStatus.FAILED),
                MAX_ATTEMPTS,
                Instant.now()
        );

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

            Instant publishedAt = Instant.now();
            event.setStatus(OutboxStatus.PUBLISHED);
            event.setPublishedAt(publishedAt);
            event.setNextAttemptAt(null);

            outboxEventRepository.save(event);

            log.info("Published outbox event {}", event.getEventId());

        } catch (Exception e) {
            int retryCount = event.getRetryCount() + 1;
            event.setRetryCount(retryCount);
            if (retryCount >= MAX_ATTEMPTS) {
                event.setStatus(OutboxStatus.FAILED);
                event.setNextAttemptAt(null);
                log.error("Outbox event {} exhausted {} publish attempts",
                        event.getEventId(), MAX_ATTEMPTS, e);
            } else {
                long delaySeconds = Math.min(300L, 5L * (1L << Math.min(retryCount, 6)));
                event.setStatus(OutboxStatus.PENDING);
                event.setNextAttemptAt(Instant.now().plusSeconds(delaySeconds));
                log.warn("Failed to publish outbox event {}; retry {} scheduled in {} seconds",
                        event.getEventId(), retryCount, delaySeconds, e);
            }

            outboxEventRepository.save(event);
        }
    }
}