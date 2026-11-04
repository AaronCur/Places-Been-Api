package travel.placesbeen.city;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import travel.placesbeen.country.Country;
import travel.placesbeen.country.CountryRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class CityRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private CountryRepository countryRepository;

    @BeforeEach
    void setUp() {
        // Countries are seeded by Flyway (V3), so look them up rather than creating them
        Country germany = countryRepository.findByIsoCode("DE").orElseThrow();
        Country france = countryRepository.findByIsoCode("FR").orElseThrow();
        entityManager.persist(new City("Munich", "Bavaria", 48.137, 11.575, germany));
        entityManager.persist(new City("Berlin", "Berlin", 52.52, 13.405, germany));
        entityManager.persist(new City("Lyon", "Métropole de Lyon", 45.764, 4.835, france));
        entityManager.flush();
    }

    @Test
    void findAllByOrderByIdAsc_returnsCitiesInInsertionOrder() {
        assertThat(cityRepository.findAllByOrderByIdAsc())
                .extracting(City::getName)
                .containsExactly("Munich", "Berlin", "Lyon");
    }

    @Test
    void existsByNameStateAndCountry_isCaseInsensitiveOnCityName() {
        assertThat(cityRepository.existsByNameIgnoreCaseAndStateIgnoreCaseAndCountry_IsoCode("BERLIN", "Berlin", "DE")).isTrue();
    }

    @Test
    void existsByNameStateAndCountry_isFalseForCityInDifferentCountry() {
        assertThat(cityRepository.existsByNameIgnoreCaseAndStateIgnoreCaseAndCountry_IsoCode("Berlin", "Berlin", "FR")).isFalse();
    }

    @Test
    void existsByNameStateAndCountry_isFalseForCityInDifferentState() {
        assertThat(cityRepository.existsByNameIgnoreCaseAndStateIgnoreCaseAndCountry_IsoCode("Berlin", "Bavaria", "DE")).isFalse();
    }

    @Test
    void existsByNameStateAndCountry_isCaseInsensitiveOnState() {
        assertThat(cityRepository.existsByNameIgnoreCaseAndStateIgnoreCaseAndCountry_IsoCode("Munich", "BAVARIA", "DE")).isTrue();
    }

    @Test
    void sameCityNameCanExistInTwoCountries() {
        entityManager.persist(new City("Valencia", "Valencia", 39.47, -0.376, countryRepository.findByIsoCode("ES").orElseThrow()));
        entityManager.persist(new City("Valencia", "Carabobo", 10.162, -68.008, countryRepository.findByIsoCode("VE").orElseThrow()));
        entityManager.flush(); // would fail here if city.name were still unique on its own

        assertThat(cityRepository.existsByNameIgnoreCaseAndStateIgnoreCaseAndCountry_IsoCode("Valencia", "Valencia", "ES")).isTrue();
        assertThat(cityRepository.existsByNameIgnoreCaseAndStateIgnoreCaseAndCountry_IsoCode("Valencia", "Carabobo", "VE")).isTrue();
    }

    @Test
    void findByNameStateAndCountry_matchesOnStateIncludingNoState() {
        Country usa = countryRepository.findByIsoCode("US").orElseThrow();
        Country gibraltar = countryRepository.findByIsoCode("GI").orElseThrow();
        entityManager.persist(new City("Portland", "Oregon", 45.523, -122.676, usa));
        entityManager.persist(new City("Portland", "Maine", 43.661, -70.255, usa));
        entityManager.persist(new City("Gibraltar", null, 36.14, -5.35, gibraltar));
        entityManager.flush();

        assertThat(cityRepository.findByNameIgnoreCaseAndStateIgnoreCaseAndCountry_IsoCode("Portland", "Maine", "US"))
                .hasValueSatisfying(city -> assertThat(city.getState()).isEqualTo("Maine"));
        assertThat(cityRepository.findByNameIgnoreCaseAndStateIgnoreCaseAndCountry_IsoCode("Portland", "Texas", "US")).isEmpty();
        assertThat(cityRepository.existsByNameIgnoreCaseAndStateIgnoreCaseAndCountry_IsoCode("Portland", "Maine", "US")).isTrue();

        // After the calls above on purpose: a null state needs "state is null", not the cached "state = ?"
        assertThat(cityRepository.findByNameIgnoreCaseAndStateIgnoreCaseAndCountry_IsoCode("Gibraltar", null, "GI")).isPresent();
        assertThat(cityRepository.findByNameIgnoreCaseAndStateIgnoreCaseAndCountry_IsoCode("Portland", null, "US")).isEmpty();
        assertThat(cityRepository.existsByNameIgnoreCaseAndStateIgnoreCaseAndCountry_IsoCode("Gibraltar", null, "GI")).isTrue();
    }

    @Test
    void sameCityNameCanExistInTwoStatesOfOneCountry() {
        Country usa = countryRepository.findByIsoCode("US").orElseThrow();
        entityManager.persist(new City("Portland", "Oregon", 45.523, -122.676, usa));
        entityManager.persist(new City("Portland", "Maine", 43.661, -70.255, usa));
        entityManager.flush(); // would fail here if the name were still unique per country
    }

    @Test
    void cityWithNoStateCanExistAlongsideSameNameWithState() {
        Country usa = countryRepository.findByIsoCode("US").orElseThrow();
        entityManager.persist(new City("Portland", "Oregon", 45.523, -122.676, usa));
        entityManager.persist(new City("Portland", null, 45.523, -122.676, usa));
        entityManager.flush(); // NULLS NOT DISTINCT only makes nulls equal to each other, not to 'Oregon'

        assertThat(cityRepository.existsByNameIgnoreCaseAndStateIgnoreCaseAndCountry_IsoCode("Portland", null, "US")).isTrue();
        assertThat(cityRepository.existsByNameIgnoreCaseAndStateIgnoreCaseAndCountry_IsoCode("Portland", "Oregon", "US")).isTrue();
    }

    @Test
    void sameCityNameAndStateInOneCountryIsRejected() {
        Country usa = countryRepository.findByIsoCode("US").orElseThrow();
        entityManager.persist(new City("Portland", "Oregon", 45.523, -122.676, usa));

        assertThatThrownBy(() -> {
            entityManager.persist(new City("Portland", "Oregon", 45.523, -122.676, usa));
            entityManager.flush();
        }).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void sameCityNameWithNoStateIsRejected() {
        Country gibraltar = countryRepository.findByIsoCode("GI").orElseThrow();
        entityManager.persist(new City("Gibraltar", null, 36.14, -5.35, gibraltar));

        // NULLS NOT DISTINCT: a plain unique constraint would treat the two nulls as different and allow this
        assertThatThrownBy(() -> {
            entityManager.persist(new City("Gibraltar", null, 36.14, -5.35, gibraltar));
            entityManager.flush();
        }).isInstanceOf(ConstraintViolationException.class);
    }
}
