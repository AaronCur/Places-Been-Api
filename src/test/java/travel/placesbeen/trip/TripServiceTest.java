package travel.placesbeen.trip;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import travel.placesbeen.city.City;
import travel.placesbeen.city.CityService;
import travel.placesbeen.country.Country;
import travel.placesbeen.country.UnknownCountryCodeException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-05T10:00:00Z");

    @Mock
    private TripRepository tripRepository;

    @Mock
    private CityService cityService;

    // Built by hand rather than @InjectMocks: Mockito would inject a mock Clock, and we want a fixed real one
    private TripService tripService;

    private Country portugal;
    private City lisbon;

    @BeforeEach
    void setUp() {
        tripService = new TripService(tripRepository, cityService, Clock.fixed(NOW, ZoneOffset.UTC));

        portugal = new Country("PT", "Portugal");
        portugal.setId(3L);
        lisbon = new City("Lisbon", "Lisbon", 38.722, -9.139, portugal);
        lisbon.setId(5L);
    }

    private static Trip trip(Long id, City city, LocalDate start, LocalDate end, String notes) {
        Trip trip = new Trip(city, start, end, notes, NOW);
        trip.setId(id);
        return trip;
    }

    private static TripRequest lisbonRequest() {
        return new TripRequest("Lisbon", "Lisbon", "PT", 38.722, -9.139,
                LocalDate.of(2025, 8, 20), LocalDate.of(2025, 8, 27), "Pastéis de nata");
    }

    // Echo back what was saved, with an id, the way JPA would
    private void saveAssignsId(Long id) {
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> {
            Trip saved = inv.getArgument(0);
            saved.setId(id);
            return saved;
        });
    }

    @Test
    void addTrip_savesTripAgainstCityFromFindOrCreate() {
        // Whether Lisbon was found or created is CityService's business (see CityServiceTest);
        // TripService just passes the place through and links the trip to whatever comes back
        when(cityService.findOrCreate("Lisbon", "Lisbon", "PT", 38.722, -9.139)).thenReturn(lisbon);
        saveAssignsId(12L);

        tripService.addTrip(lisbonRequest());

        ArgumentCaptor<Trip> captor = ArgumentCaptor.forClass(Trip.class);
        verify(tripRepository).save(captor.capture());
        Trip saved = captor.getValue();
        assertThat(saved.getCity()).isSameAs(lisbon);
        assertThat(saved.getStartDate()).isEqualTo(LocalDate.of(2025, 8, 20));
        assertThat(saved.getEndDate()).isEqualTo(LocalDate.of(2025, 8, 27));
        assertThat(saved.getNotes()).isEqualTo("Pastéis de nata");
    }

    @Test
    void addTrip_returnsSavedTripWithCityAndCountry() {
        when(cityService.findOrCreate(any(), any(), any(), any(), any())).thenReturn(lisbon);
        saveAssignsId(12L);

        TripResponse response = tripService.addTrip(lisbonRequest());

        assertThat(response.id()).isEqualTo(12L);
        assertThat(response.city()).isEqualTo(new TripResponse.CitySummary(5L, "Lisbon", "Lisbon", 38.722, -9.139));
        assertThat(response.country()).isEqualTo(new TripResponse.CountrySummary("PT", "Portugal", "🇵🇹"));
    }

    @Test
    void addTrip_throwsWhenCountryCodeUnknown() {
        when(cityService.findOrCreate(any(), any(), any(), any(), any()))
                .thenThrow(new UnknownCountryCodeException("XX"));

        assertThatThrownBy(() -> tripService.addTrip(lisbonRequest()))
                .isInstanceOf(UnknownCountryCodeException.class);

        verifyNoInteractions(tripRepository);
    }

    @Test
    void addTrip_setsCreatedAtFromClock() {
        when(cityService.findOrCreate(any(), any(), any(), any(), any())).thenReturn(lisbon);
        saveAssignsId(12L);

        tripService.addTrip(lisbonRequest());

        ArgumentCaptor<Trip> captor = ArgumentCaptor.forClass(Trip.class);
        verify(tripRepository).save(captor.capture());
        assertThat(captor.getValue().getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void getAllTrips_mapsTripsWithCityAndCountry() {
        Country spain = new Country("ES", "Spain");
        City valencia = new City("Valencia", "Valencia", 39.47, -0.376, spain);
        valencia.setId(7L);
        when(tripRepository.findAllByOrderByStartDateDescIdDesc()).thenReturn(List.of(
                trip(2L, valencia, LocalDate.of(2026, 3, 10), null, null),
                trip(1L, lisbon, LocalDate.of(2025, 8, 20), LocalDate.of(2025, 8, 27), "Pastéis de nata")));

        List<TripResponse> trips = tripService.getAllTrips();

        // Keeps the repository's order (newest first) rather than re-sorting
        assertThat(trips).extracting(TripResponse::id).containsExactly(2L, 1L);

        TripResponse dayTrip = trips.getFirst();
        assertThat(dayTrip.startDate()).isEqualTo(LocalDate.of(2026, 3, 10));
        assertThat(dayTrip.endDate()).isNull();
        assertThat(dayTrip.city().name()).isEqualTo("Valencia");
        assertThat(dayTrip.country()).isEqualTo(new TripResponse.CountrySummary("ES", "Spain", "🇪🇸"));

        TripResponse lisbonTrip = trips.get(1);
        assertThat(lisbonTrip.endDate()).isEqualTo(LocalDate.of(2025, 8, 27));
        assertThat(lisbonTrip.notes()).isEqualTo("Pastéis de nata");
        assertThat(lisbonTrip.city()).isEqualTo(new TripResponse.CitySummary(5L, "Lisbon", "Lisbon", 38.722, -9.139));
        assertThat(lisbonTrip.country().flag()).isEqualTo("🇵🇹");
    }

    @Test
    void updateTripById_changesDatesAndNotesOnly() {
        Trip existing = trip(1L, lisbon, LocalDate.of(2025, 8, 20), LocalDate.of(2025, 8, 27), "Old notes");
        when(tripRepository.findWithCityById(1L)).thenReturn(Optional.of(existing));
        when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> inv.getArgument(0));
        TripUpdateRequest update = new TripUpdateRequest(LocalDate.of(2025, 8, 21), null, "New notes");

        TripResponse response = tripService.updateTripById(update, 1L);

        assertThat(existing.getStartDate()).isEqualTo(LocalDate.of(2025, 8, 21));
        assertThat(existing.getEndDate()).isNull(); // clearing the end date turns it into a day trip
        assertThat(existing.getNotes()).isEqualTo("New notes");
        assertThat(existing.getCity()).isSameAs(lisbon);
        assertThat(existing.getCreatedAt()).isEqualTo(NOW);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.startDate()).isEqualTo(LocalDate.of(2025, 8, 21));
        assertThat(response.city().name()).isEqualTo("Lisbon");
        verifyNoInteractions(cityService);
    }

    @Test
    void getTripById_mapsTripWithCityAndCountry() {
        Trip trip = trip(1L, lisbon, LocalDate.of(2025, 8, 20), LocalDate.of(2025, 8, 27), "Pastéis de nata");
        when(tripRepository.findWithCityById(1L)).thenReturn(Optional.of(trip));

        TripResponse response = tripService.getTripById(1L);

        assertThat(response).isEqualTo(new TripResponse(1L,
                LocalDate.of(2025, 8, 20), LocalDate.of(2025, 8, 27), "Pastéis de nata",
                new TripResponse.CitySummary(5L, "Lisbon", "Lisbon", 38.722, -9.139),
                new TripResponse.CountrySummary("PT", "Portugal", "🇵🇹")));
    }

    @Test
    void getTripById_throwsWhenNotFound() {
        when(tripRepository.findWithCityById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tripService.getTripById(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Trip not found with id: 99");
    }

    @Test
    void updateTripById_throwsWhenNotFound() {
        when(tripRepository.findWithCityById(99L)).thenReturn(Optional.empty());
        TripUpdateRequest update = new TripUpdateRequest(LocalDate.of(2025, 8, 21), null, null);

        assertThatThrownBy(() -> tripService.updateTripById(update, 99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Trip not found with id: 99");

        verify(tripRepository, never()).save(any());
    }

    @Test
    void deleteTripById_removesCityWhenItWasItsLastTrip() {
        Trip trip = trip(1L, lisbon, LocalDate.of(2025, 8, 20), null, null);
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(tripRepository.existsByCity_Id(5L)).thenReturn(false);

        tripService.deleteTripById(1L);

        // Order matters: the city can't go while a trip still references it (fk_trip_city)
        InOrder inOrder = inOrder(tripRepository, cityService);
        inOrder.verify(tripRepository).delete(trip);
        inOrder.verify(tripRepository).existsByCity_Id(5L);
        inOrder.verify(cityService).deleteCityById(5L);
    }

    @Test
    void deleteTripById_keepsCityWhenOtherTripsRemain() {
        Trip trip = trip(1L, lisbon, LocalDate.of(2025, 8, 20), null, null);
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(tripRepository.existsByCity_Id(5L)).thenReturn(true);

        tripService.deleteTripById(1L);

        verify(tripRepository).delete(trip);
        verify(cityService, never()).deleteCityById(any());
    }

    @Test
    void deleteTripById_throwsWhenNotFound() {
        when(tripRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tripService.deleteTripById(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Trip not found with id: 99");

        verify(tripRepository, never()).delete(any());
        verifyNoInteractions(cityService);
    }
}
