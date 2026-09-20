package enterprise.ticket_service.client.auth_service;

import enterprise.ticket_service.client.auth_service.dto.ServiceLoginRequest;
import enterprise.ticket_service.client.user_service.dto.CreateUserProfileRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "auth-service", url = "${services.auth-service.url}")
public interface AuthService {

    @PostMapping("/internal/auth/login")
    ResponseEntity<Map<String, Object>> login(@RequestBody ServiceLoginRequest request);

}