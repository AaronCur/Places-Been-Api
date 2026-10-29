package travel.placesbeen.city;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import travel.placesbeen.country.Country;
import travel.placesbeen.country.CountryRepository;

import static org.assertj.core.api.Assertions.assertThat;

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
        entityManager.persist(new City("Munich", 48.137, 11.575, germany));
        entityManager.persist(new City("Berlin", 52.52, 13.405, germany));
        entityManager.persist(new City("Lyon", 45.764, 4.835, france));
        entityManager.flush();
    }

    @Test
    void findAllByOrderByIdAsc_returnsCitiesInInsertionOrder() {
        assertThat(cityRepository.findAllByOrderByIdAsc())
                .extracting(City::getName)
                .containsExactly("Munich", "Berlin", "Lyon");
    }

    @Test
    void existsByNameAndCountry_isCaseInsensitiveOnCityName() {
        assertThat(cityRepository.existsByNameIgnoreCaseAndCountry_IsoCode("BERLIN", "DE")).isTrue();
    }

    @Test
    void existsByNameAndCountry_isFalseForCityInDifferentCountry() {
        assertThat(cityRepository.existsByNameIgnoreCaseAndCountry_IsoCode("Berlin", "FR")).isFalse();
    }

    @Test
    void sameCityNameCanExistInTwoCountries() {
        entityManager.persist(new City("Valencia", 39.47, -0.376, countryRepository.findByIsoCode("ES").orElseThrow()));
        entityManager.persist(new City("Valencia", 10.162, -68.008, countryRepository.findByIsoCode("VE").orElseThrow()));
        entityManager.flush(); // would fail here if city.name were still unique on its own

        assertThat(cityRepository.existsByNameIgnoreCaseAndCountry_IsoCode("Valencia", "ES")).isTrue();
        assertThat(cityRepository.existsByNameIgnoreCaseAndCountry_IsoCode("Valencia", "VE")).isTrue();
    }
}
