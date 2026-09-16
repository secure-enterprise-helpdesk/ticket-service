package enterprise.ticket_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GlobalReq {
    private Map<String, Object> data;

    // Helper method to extract string values safely without null pointer exceptions
    public String getString(String key) {
        return (data != null && data.containsKey(key)) ? String.valueOf(data.get(key)) : null;
    }
}