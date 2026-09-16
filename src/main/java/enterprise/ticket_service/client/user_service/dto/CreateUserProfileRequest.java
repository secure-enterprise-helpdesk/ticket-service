package enterprise.ticket_service.client.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreateUserProfileRequest {
    private Long id;
    private String userId;
    private String firstName;
    private String lastName;
    private Integer departmentId;
}
