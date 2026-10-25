package enterprise.ticket_service.repository;

import enterprise.ticket_service.entity.Ticket;
import enterprise.ticket_service.enums.Priority;
import enterprise.ticket_service.enums.TicketStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TicketRepositoryTest.CacheTestConfiguration.class)
class TicketRepositoryTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private TicketRepository ticketRepository;

    @TestConfiguration
    static class CacheTestConfiguration {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("tickets");
        }
    }

    @Test
    void shouldSaveAndFindTicketByTicketNumber() {
        Ticket saved = ticketRepository.saveAndFlush(validTicket("TCK-1001"));

        Optional<Ticket> found = ticketRepository.findByTicketNumber("TCK-1001");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getTitle()).isEqualTo("Printer is offline");
    }

    @Test
    void shouldReturnEmptyForUnknownTicketNumber() {
        assertThat(ticketRepository.findByTicketNumber("TCK-UNKNOWN")).isEmpty();
    }

    @Test
    void shouldRejectDuplicateTicketNumbers() {
        ticketRepository.saveAndFlush(validTicket("TCK-DUPLICATE"));

        assertThrows(DataIntegrityViolationException.class,
                () -> ticketRepository.saveAndFlush(validTicket("TCK-DUPLICATE")));
    }

    @Test
    void shouldIncrementVersionWhenTicketIsUpdated() {
        Ticket ticket = ticketRepository.saveAndFlush(validTicket("TCK-VERSION"));
        Long initialVersion = ticket.getVersion();

        ticket.setTitle("Printer is back online");
        Ticket updated = ticketRepository.saveAndFlush(ticket);

        assertThat(initialVersion).isNotNull();
        assertThat(updated.getVersion()).isEqualTo(initialVersion + 1);
    }

    private Ticket validTicket(String ticketNumber) {
        Ticket ticket = new Ticket();
        ticket.setTicketNumber(ticketNumber);
        ticket.setTitle("Printer is offline");
        ticket.setDescription("The office printer is not responding.");
        ticket.setCategory("IT");
        ticket.setPriority(Priority.MEDIUM);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setCreatedBy("employee-1");
        ticket.setDepartmentId(1);
        return ticket;
    }
}
