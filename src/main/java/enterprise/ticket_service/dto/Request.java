package enterprise.ticket_service.dto;

import enterprise.ticket_service.enums.Priority;
import enterprise.ticket_service.enums.TicketStatus;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Version;
import lombok.Data;

import java.time.Instant;

@Data
public class Request {
    private String title;
    private String description;
    private String category;
    private Integer departmentId;
}
