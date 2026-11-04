package travel.placesbeen.trip;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import travel.placesbeen.city.CityService;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private CityService cityService;

    @InjectMocks
    private TripService tripService;

    @Test
    @Disabled("TODO")
    void addTrip_reusesExistingCity() {
        // given a city with the same name, state and country already exists
        // when a trip is added there
        // then the trip is saved against that city and no new city is created
    }

    @Test
    @Disabled("TODO")
    void addTrip_createsCityWhenNew() {
        // given no matching city
        // when a trip is added
        // then a city is created in the seeded country and the trip saved against it
    }

    @Test
    @Disabled("TODO")
    void addTrip_throwsWhenCountryCodeUnknown() {
        // given an unknown country code
        // when a trip is added
        // then UnknownCountryCodeException and nothing is saved
    }

    @Test
    @Disabled("TODO")
    void addTrip_setsCreatedAtFromClock() {
        // given a fixed Clock
        // when a trip is added
        // then createdAt is that instant
    }

    @Test
    @Disabled("TODO")
    void getAllTrips_mapsTripsWithCityAndCountry() {
        // given two trips from the repository
        // when getAllTrips
        // then both are mapped in the same order, with nested city, country and flag
    }

    @Test
    @Disabled("TODO")
    void updateTripById_changesDatesAndNotesOnly() {
        // given an existing trip
        // when it is updated
        // then dates and notes change, the city doesn't
    }

    @Test
    @Disabled("TODO")
    void updateTripById_throwsWhenNotFound() {
        // given no trip with id 99
        // when it is updated
        // then EntityNotFoundException and nothing is saved
    }

    @Test
    @Disabled("TODO")
    void deleteTripById_removesCityWhenItWasItsLastTrip() {
        // given the city has no other trips
        // when the trip is deleted
        // then the trip and its city are deleted
    }

    @Test
    @Disabled("TODO")
    void deleteTripById_keepsCityWhenOtherTripsRemain() {
        // given the city has another trip
        // when the trip is deleted
        // then only the trip is deleted
    }

    @Test
    @Disabled("TODO")
    void deleteTripById_throwsWhenNotFound() {
        // given no trip with id 99
        // when it is deleted
        // then EntityNotFoundException and nothing is deleted
    }
}
