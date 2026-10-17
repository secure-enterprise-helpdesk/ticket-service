package enterprise.ticket_service.service;

import enterprise.ticket_service.entity.Ticket;
import enterprise.ticket_service.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CachedTicketService {

    private final TicketRepository ticketRepository;

    @Cacheable(value = "tickets", key = "#ticketNumber", unless = "#result == null")
    public Ticket getCachedTicket(String ticketNumber) {
        System.out.println("CACHE MISS - Fetching ticket from PostgreSQL: " + ticketNumber);
        return ticketRepository.findByTicketNumber(ticketNumber).orElse(null);
    }
}
