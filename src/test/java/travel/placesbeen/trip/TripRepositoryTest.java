package travel.placesbeen.trip;

import org.hibernate.Hibernate;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import travel.placesbeen.city.City;
import travel.placesbeen.country.CountryRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class TripRepositoryTest {

    // createdAt is set by the service in the real app; any fixed value will do here
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private CountryRepository countryRepository;

    private City lisbon;
    private City porto;

    @BeforeEach
    void setUp() {
        // Countries are seeded by Flyway (V3), so look them up rather than creating them
        var portugal = countryRepository.findByIsoCode("PT").orElseThrow();
        lisbon = entityManager.persist(new City("Lisbon", "Lisbon", 38.722, -9.139, portugal));
        porto = entityManager.persist(new City("Porto", "Porto", 41.150, -8.611, portugal));
    }

    @Test
    void findAllByOrderByStartDateDescIdDesc_returnsNewestTripFirst() {
        persistTrip(lisbon, LocalDate.of(2024, 5, 1), null);
        persistTrip(porto, LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 14));
        persistTrip(lisbon, LocalDate.of(2025, 8, 20), LocalDate.of(2025, 8, 27));
        entityManager.flush();

        assertThat(tripRepository.findAllByOrderByStartDateDescIdDesc())
                .extracting(Trip::getStartDate)
                .containsExactly(
                        LocalDate.of(2026, 3, 10),
                        LocalDate.of(2025, 8, 20),
                        LocalDate.of(2024, 5, 1));
    }

    @Test
    void findAllByOrderByStartDateDescIdDesc_breaksSameDayTiesByNewestId() {
        LocalDate sameDay = LocalDate.of(2025, 6, 1);
        Trip first = persistTrip(lisbon, sameDay, null);
        Trip second = persistTrip(porto, sameDay, null);
        entityManager.flush();

        assertThat(tripRepository.findAllByOrderByStartDateDescIdDesc())
                .extracting(Trip::getId)
                .containsExactly(second.getId(), first.getId());
    }

    @Test
    void findAllByOrderByStartDateDescIdDesc_fetchesCityAndCountryInTheSameQuery() {
        persistTrip(lisbon, LocalDate.of(2025, 6, 1), null);
        entityManager.flush();
        // Empty the persistence context, otherwise the trip comes back with the city we just persisted
        // already loaded and this test would pass even without the @EntityGraph
        entityManager.clear();

        List<Trip> trips = tripRepository.findAllByOrderByStartDateDescIdDesc();

        assertThat(trips).hasSize(1);
        Trip trip = trips.getFirst();
        assertThat(Hibernate.isInitialized(trip.getCity())).isTrue();
        assertThat(Hibernate.isInitialized(trip.getCity().getCountry())).isTrue();
    }

    @Test
    void findWithCityById_fetchesCityAndCountryInTheSameQuery() {
        Trip saved = persistTrip(lisbon, LocalDate.of(2025, 6, 1), null);
        entityManager.flush();
        entityManager.clear(); // as above: without this the city is already loaded and the test proves nothing

        assertThat(tripRepository.findWithCityById(saved.getId()))
                .hasValueSatisfying(trip -> {
                    assertThat(Hibernate.isInitialized(trip.getCity())).isTrue();
                    assertThat(Hibernate.isInitialized(trip.getCity().getCountry())).isTrue();
                });
    }

    @Test
    void findWithCityById_isEmptyForUnknownId() {
        assertThat(tripRepository.findWithCityById(999L)).isEmpty();
    }

    @Test
    void existsByCity_Id_isTrueForCityWithTrips() {
        persistTrip(lisbon, LocalDate.of(2024, 5, 1), null);
        persistTrip(lisbon, LocalDate.of(2025, 8, 20), LocalDate.of(2025, 8, 27));
        entityManager.flush();

        assertThat(tripRepository.existsByCity_Id(lisbon.getId())).isTrue();
    }

    @Test
    void existsByCity_Id_isFalseForCityWithNoTrips() {
        // Lisbon has a trip, so this also checks the query is scoped to the one city
        persistTrip(lisbon, LocalDate.of(2024, 5, 1), null);
        entityManager.flush();

        assertThat(tripRepository.existsByCity_Id(porto.getId())).isFalse();
    }

    @Test
    void dayTripWithNoEndDateIsSaved() {
        Trip dayTrip = persistTrip(porto, LocalDate.of(2025, 6, 1), null);
        entityManager.flush();
        entityManager.clear();

        assertThat(tripRepository.findById(dayTrip.getId()))
                .hasValueSatisfying(trip -> {
                    assertThat(trip.getStartDate()).isEqualTo(LocalDate.of(2025, 6, 1));
                    assertThat(trip.getEndDate()).isNull();
                });
    }

    @Test
    void endDateBeforeStartDateIsRejected() {
        // ck_trip_dates (V5) is the backstop behind the request validation
        assertThatThrownBy(() -> {
            persistTrip(lisbon, LocalDate.of(2025, 6, 10), LocalDate.of(2025, 6, 9));
            entityManager.flush();
        }).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void endDateOnStartDateIsAllowed() {
        persistTrip(lisbon, LocalDate.of(2025, 6, 10), LocalDate.of(2025, 6, 10));
        entityManager.flush(); // CHECK is end_date >= start_date, so the same day passes
    }

    private Trip persistTrip(City city, LocalDate startDate, LocalDate endDate) {
        return entityManager.persist(new Trip(city, startDate, endDate, null, CREATED_AT));
    }
}
