package travel.placesbeen.trip;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
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
        return ResponseEntity.ok(tripService.getAllTrips());
    }

    @GetMapping(value = "/{id}", version = "1")
    public ResponseEntity<TripResponse> getTripById(@PathVariable Long id) {
        return ResponseEntity.ok(tripService.getTripById(id));
    }

    @PostMapping(version = "1")
    public ResponseEntity<TripResponse> addTrip(@Valid @RequestBody TripRequest tripRequest) {
        TripResponse newTrip = tripService.addTrip(tripRequest);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()    // Takes "http://localhost:8080/api/v1/trips"
                .path("/{id}")           // Appends "/{id}"
                .buildAndExpand(newTrip.id())
                .toUri();
        return ResponseEntity.created(location).body(newTrip);
    }

    @PutMapping(value = "/{id}", version = "1")
    public ResponseEntity<TripResponse> updateTripById(@PathVariable Long id, @Valid @RequestBody TripUpdateRequest tripUpdateRequest) {
        return ResponseEntity.ok(tripService.updateTripById(tripUpdateRequest, id));
    }

    @DeleteMapping(value = "/{id}", version = "1")
    public ResponseEntity<Void> deleteTripById(@PathVariable Long id) {
        tripService.deleteTripById(id);
        return ResponseEntity.noContent().build();
    }
}
