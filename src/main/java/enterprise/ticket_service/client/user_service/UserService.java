package enterprise.ticket_service.client.user_service;

import enterprise.ticket_service.client.user_service.dto.CreateUserProfileRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "user-service", url = "${services.user-service.url}")
public interface UserService {

    String ADMIN_BASE = "/users/admin";

    @PreAuthorize("hasRole('Manager')")
    @PutMapping(ADMIN_BASE + "/update")
    ResponseEntity<String> update(@RequestParam String userId, @RequestParam String role);

    @GetMapping("/users/internal/agents")
    ResponseEntity<List<CreateUserProfileRequest>> getAgents(
            @RequestHeader("Authorization") String authorization
    );

    @PreAuthorize("hasRole('Agent', 'Manager')")
    @GetMapping(ADMIN_BASE + "/employees")
    ResponseEntity<List<CreateUserProfileRequest>> getEmployees();
}