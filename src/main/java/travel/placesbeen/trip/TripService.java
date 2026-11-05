package travel.placesbeen.trip;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import travel.placesbeen.city.City;
import travel.placesbeen.city.CityService;
import travel.placesbeen.country.Country;
import travel.placesbeen.country.Flags;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class TripService {

    private final TripRepository tripRepository;
    private final CityService cityService;
    private final Clock clock;

    public TripService(TripRepository tripRepository, CityService cityService, Clock clock) {
        this.tripRepository = tripRepository;
        this.cityService = cityService;
        this.clock = clock;
    }

    public TripResponse getTripById(Long id) {
        return mapToResponse(findTripWithCityById(id));
    }

    @Transactional
    public TripResponse addTrip(TripRequest tripRequest) {
        City city = cityService.findOrCreate(tripRequest.cityName(), tripRequest.state(), tripRequest.countryCode(), tripRequest.latitude(), tripRequest.longitude());

        Trip trip = new Trip(city,tripRequest.startDate(),tripRequest.endDate(), tripRequest.notes(), Instant.now(clock));
        Trip savedTrip = tripRepository.save(trip);
        return mapToResponse(savedTrip);
    }

    public List<TripResponse> getAllTrips() {
        return tripRepository.findAllByOrderByStartDateDescIdDesc().stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public TripResponse updateTripById(TripUpdateRequest tripUpdateRequest, Long id) {

        Trip trip = findTripWithCityById(id);
        trip.setStartDate(tripUpdateRequest.startDate());
        trip.setEndDate(tripUpdateRequest.endDate());
        trip.setNotes(tripUpdateRequest.notes());
        Trip savedTrip = tripRepository.save(trip);
        return mapToResponse(savedTrip);
    }

    @Transactional
    public void deleteTripById(Long id) {
        Trip trip = findTripById(id);
        Long cityId =  trip.getCity().getId();
        tripRepository.delete(trip);

        if (!tripRepository.existsByCity_Id(cityId)) {
            cityService.deleteCityById(cityId);
        }

    }

    // The trip only; its city stays a lazy proxy (enough for getCity().getId())
    private Trip findTripById(Long id) {
       return tripRepository.findById(id).orElseThrow(() -> tripNotFound(id));
    }

    // The trip with its city and country loaded, for methods that map it to a response
    private Trip findTripWithCityById(Long id) {
        return tripRepository.findWithCityById(id).orElseThrow(() -> tripNotFound(id));
    }

    private static EntityNotFoundException tripNotFound(Long id) {
        return new EntityNotFoundException("Trip not found with id: " + id);
    }

    private TripResponse mapToResponse(Trip trip) {
        City city = trip.getCity();
        Country country = trip.getCity().getCountry();
        TripResponse.CitySummary citySummary = new TripResponse.CitySummary(city.getId(), city.getName(), city.getState(), city.getLatitude(), city.getLongitude());
        TripResponse.CountrySummary countrySummary = new TripResponse.CountrySummary(country.getIsoCode(), country.getName(), Flags.fromIsoCode(country.getIsoCode()));
        return new TripResponse(
                 trip.getId(),
                 trip.getStartDate(),
                 trip.getEndDate(),
                 trip.getNotes(),
                 citySummary,
                 countrySummary
        );
    }
}
