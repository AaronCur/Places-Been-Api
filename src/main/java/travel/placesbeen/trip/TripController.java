package travel.placesbeen.trip;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/trips")
@Tag(name = "Trip API", description = "API for managing trips")
public class TripController {
    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @GetMapping(version = "1")
    public ResponseEntity<List<TripResponse>> getAllTrips() {
        throw new UnsupportedOperationException("TODO: 200 with all trips, newest first");
    }

    @PostMapping(version = "1")
    public ResponseEntity<TripResponse> addTrip(@Valid @RequestBody TripRequest tripRequest) {
        throw new UnsupportedOperationException("TODO: 201 + Location header");
    }

    @PutMapping(value = "/{id}", version = "1")
    public ResponseEntity<TripResponse> updateTripById(@PathVariable Long id, @Valid @RequestBody TripUpdateRequest tripUpdateRequest) {
        throw new UnsupportedOperationException("TODO: 200 with the updated trip");
    }

    @DeleteMapping(value = "/{id}", version = "1")
    public ResponseEntity<Void> deleteTripById(@PathVariable Long id) {
        throw new UnsupportedOperationException("TODO: 204");
    }
}
