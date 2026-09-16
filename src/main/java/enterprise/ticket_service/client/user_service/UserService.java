package enterprise.ticket_service.client.user_service;

import enterprise.ticket_service.client.user_service.dto.CreateUserProfileRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "user-service", url = "${services.user-service.url}")
@RequestMapping("/users/admin")
public interface UserService {
    @PreAuthorize("hasRole('Manager')")
    @PutMapping("/update")
    public ResponseEntity<String> update(@RequestParam String  userId, @RequestParam String role);

    @PreAuthorize("hasRole('Manager')")
    @GetMapping("/agents")
    public ResponseEntity<List<CreateUserProfileRequest>> getAgents();

    @PreAuthorize("hasRole('Agent', 'Manager')")
    @GetMapping("/employees")
    public ResponseEntity<List<CreateUserProfileRequest>> getEmployees();
}