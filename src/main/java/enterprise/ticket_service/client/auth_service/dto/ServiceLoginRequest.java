package enterprise.ticket_service.client.auth_service.dto;

import lombok.Data;

    @Data
    public class ServiceLoginRequest {
        private String username;
        private String password;
    }

