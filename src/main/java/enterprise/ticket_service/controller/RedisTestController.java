package enterprise.ticket_service.controller;

import enterprise.ticket_service.service.RedisTestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/redis")
@RequiredArgsConstructor
public class RedisTestController {

    private final RedisTestService redisTestService;

    @PostMapping
    public ResponseEntity<String> save(
            @RequestParam String key,
            @RequestParam String value) {

        redisTestService.save(key, value);
        return ResponseEntity.ok("Saved");
    }

    @GetMapping
    public ResponseEntity<String> get(
            @RequestParam String key) {

        String value = redisTestService.get(key);

        if (value == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(value);
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(
            @RequestParam String key) {

        redisTestService.delete(key);
        return ResponseEntity.noContent().build();
    }
}