package enterprise.ticket_service.service;

import enterprise.ticket_service.client.user_service.UserService;
import enterprise.ticket_service.client.user_service.dto.CreateUserProfileRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResilientUserService {

    private final UserService userService;

    @Retry(name = "userService")
    @CircuitBreaker(name = "userService", fallbackMethod = "getAgentsFallback")
    public List<CreateUserProfileRequest> getAgents(String authorization) {
        ResponseEntity<List<CreateUserProfileRequest>> response = userService.getAgents(authorization);
        return response.getBody();
    }

    private List<CreateUserProfileRequest> getAgentsFallback(String authorization, Throwable exception) {
        log.warn("Unable to retrieve agents from user-service; skipping ticket assignment for this run");
        return List.of();
    }
}