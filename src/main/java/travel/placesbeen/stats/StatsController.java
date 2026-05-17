package travel.placesbeen.stats;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@Tag(name = "Stats API", description = "API for managing stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping(value = "/summary", version = "1")
    public ResponseEntity<StatsSummaryResponse> getStatsSummary() {
        return ResponseEntity.ok(statsService.getStatsSummary());
    }
}
