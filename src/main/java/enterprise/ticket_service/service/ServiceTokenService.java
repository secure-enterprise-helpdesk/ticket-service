package enterprise.ticket_service.service;

import enterprise.ticket_service.client.auth_service.AuthService;
import enterprise.ticket_service.client.auth_service.dto.ServiceLoginRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ServiceTokenService {

    private final AuthService authService;
    private final String username;
    private final String password;

    public ServiceTokenService(
            AuthService authService,
            @Value("${services.auth-service.username}") String username,
            @Value("${services.auth-service.password}") String password) {
        this.authService = authService;
        this.username = username;
        this.password = password;
    }

    public String getToken() {
        ServiceLoginRequest request = new ServiceLoginRequest();
        request.setUsername(username);
        request.setPassword(password);

        ResponseEntity<Map<String, Object>> response = authService.login(request);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("Auth service rejected the ticket-service login");
        }

        return extractAccessToken(response.getBody());
    }

    private String extractAccessToken(Map<String, Object> body) {
        Object value = body.get("accessToken");
        if (value instanceof String accessToken && !accessToken.isBlank()) {
            return accessToken;
        }
        throw new IllegalStateException("Auth service response did not contain an accessToken");
    }
}
