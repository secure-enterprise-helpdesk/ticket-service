package enterprise.ticket_service.service;

import enterprise.ticket_service.config.RedisCacheConfig;
import enterprise.ticket_service.entity.Ticket;
import enterprise.ticket_service.enums.Priority;
import enterprise.ticket_service.enums.TicketStatus;
import enterprise.ticket_service.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableCaching
@Import({
        CachedTicketService.class,
        RedisCacheConfig.class,
        CachedTicketServiceRedisIntegrationTest.RedisCacheTestConfiguration.class
})
class CachedTicketServiceRedisIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16");

    @Container
    @SuppressWarnings("deprecation")
    static final FixedHostPortGenericContainer<?> redis =
            new FixedHostPortGenericContainer<>("redis:7-alpine")
                    .withFixedExposedPort(6380, 6379)
                    .waitingFor(Wait.forLogMessage(".*Ready to accept connections.*\\n", 1));

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private CachedTicketService cachedTicketService;

    @Autowired
    private CacheManager cacheManager;

    @Test
    void shouldCacheOnMissReturnCachedValueOnHitAndRefreshAfterEviction() {
        assertThat(cacheManager).isInstanceOf(RedisCacheManager.class);
        Ticket ticket = ticketRepository.saveAndFlush(validTicket());
        var ticketsCache = cacheManager.getCache("tickets");
        assertThat(ticketsCache).isNotNull();
        assertThat(ticketsCache.get(ticket.getTicketNumber())).isNull();

        Ticket cacheMissResult = cachedTicketService.getCachedTicket(ticket.getTicketNumber());
        assertThat(cacheMissResult.getTitle()).isEqualTo("Printer is offline");
        assertThat(ticketsCache.get(ticket.getTicketNumber())).isNotNull();

        Ticket persistedTicket = ticketRepository.findByTicketNumber(ticket.getTicketNumber())
                .orElseThrow();
        persistedTicket.setTitle("Printer is back online");
        ticketRepository.saveAndFlush(persistedTicket);

        Ticket cacheHitResult = cachedTicketService.getCachedTicket(ticket.getTicketNumber());
        assertThat(cacheHitResult.getTitle()).isEqualTo("Printer is offline");

        ticketsCache.evict(ticket.getTicketNumber());
        assertThat(ticketsCache.get(ticket.getTicketNumber())).isNull();

        Ticket afterEviction = cachedTicketService.getCachedTicket(ticket.getTicketNumber());
        assertThat(afterEviction.getTitle()).isEqualTo("Printer is back online");
    }

    private Ticket validTicket() {
        Ticket ticket = new Ticket();
        ticket.setTicketNumber("TCK-CACHE-1001");
        ticket.setTitle("Printer is offline");
        ticket.setDescription("The office printer is not responding.");
        ticket.setCategory("IT");
        ticket.setPriority(Priority.MEDIUM);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setCreatedBy("employee-1");
        ticket.setDepartmentId(1);
        return ticket;
    }

    @TestConfiguration
    static class RedisCacheTestConfiguration {

        @Bean
        RedisConnectionFactory redisConnectionFactory() {
            return new LettuceConnectionFactory("localhost", 6380);
        }

        @Bean
        CacheManager cacheManager(
                RedisConnectionFactory redisConnectionFactory,
                RedisCacheConfiguration redisCacheConfiguration) {
            return RedisCacheManager.builder(redisConnectionFactory)
                    .cacheDefaults(redisCacheConfiguration)
                    .build();
        }
    }
}
