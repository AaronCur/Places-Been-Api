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

    private Country germany;
    private Country france;

    @BeforeEach
    void setUp() {
        germany = entityManager.persist(new Country("Germany"));
        france = entityManager.persist(new Country("France"));
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
    void existsByNameAndCountry_isCaseInsensitive() {
        assertThat(cityRepository.existsByNameIgnoreCaseAndCountry_NameIgnoreCase("BERLIN", "germany")).isTrue();
    }

    @Test
    void existsByNameAndCountry_isFalseForCityInDifferentCountry() {
        assertThat(cityRepository.existsByNameIgnoreCaseAndCountry_NameIgnoreCase("Berlin", "France")).isFalse();
    }

    @Test
    void countByCountryId_countsOnlyThatCountrysCities() {
        assertThat(cityRepository.countByCountryId(germany.getId())).isEqualTo(2);
        assertThat(cityRepository.countByCountryId(france.getId())).isEqualTo(1);
    }

    @Test
    void findCountryByName_isCaseInsensitive() {
        assertThat(countryRepository.findByNameIgnoreCase("gErMaNy"))
                .hasValueSatisfying(c -> assertThat(c.getId()).isEqualTo(germany.getId()));
        assertThat(countryRepository.findByNameIgnoreCase("Spain")).isEmpty();
    }
}
