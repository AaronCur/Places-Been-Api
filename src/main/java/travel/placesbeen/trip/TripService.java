package travel.placesbeen.trip;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import travel.placesbeen.city.CityService;

import java.util.List;

@Service
public class TripService {

    private final TripRepository tripRepository;
    private final CityService cityService;
    // TODO: inject a java.time.Clock for createdAt (needs a Clock bean, e.g. in config/)

    public TripService(TripRepository tripRepository, CityService cityService) {
        this.tripRepository = tripRepository;
        this.cityService = cityService;
    }

    @Transactional
    public TripResponse addTrip(TripRequest tripRequest) {
        throw new UnsupportedOperationException("TODO: find or create the city by (name, state, country), then save the trip");
    }

    public List<TripResponse> getAllTrips() {
        throw new UnsupportedOperationException("TODO: all trips newest first, mapped to responses");
    }

    @Transactional
    public TripResponse updateTripById(TripUpdateRequest tripUpdateRequest, Long id) {
        throw new UnsupportedOperationException("TODO: change dates and notes only; 404 if missing");
    }

    @Transactional
    public void deleteTripById(Long id) {
        throw new UnsupportedOperationException("TODO: delete the trip, then its city if that was its last trip; 404 if missing");
    }

    private TripResponse mapToResponse(Trip trip) {
        throw new UnsupportedOperationException("TODO: map trip, its city and its country (flag via Flags.fromIsoCode)");
    }

}
