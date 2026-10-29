package travel.placesbeen.country;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import travel.placesbeen.city.City;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CountryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CountryRepository countryRepository;

    private void visit(String isoCode, String cityName) {
        entityManager.persist(new City(cityName, 0.0, 0.0, countryRepository.findByIsoCode(isoCode).orElseThrow()));
        entityManager.flush();
        entityManager.clear(); // so countries are reloaded with their cities
    }

    @Test
    void everyCountryIsSeeded() {
        assertThat(countryRepository.count()).isEqualTo(250);
    }

    @Test
    void findByIsoCode_returnsSeededCountry() {
        assertThat(countryRepository.findByIsoCode("DE"))
                .hasValueSatisfying(c -> assertThat(c.getName()).isEqualTo("Germany"));
        assertThat(countryRepository.findByIsoCode("QQ")).isEmpty();
    }

    @Test
    void visitedQueries_onlyIncludeCountriesWithCities() {
        visit("FR", "Lyon");
        visit("DE", "Berlin");
        visit("DE", "Munich");

        assertThat(countryRepository.findByCitiesIsNotEmptyOrderByNameAsc())
                .extracting(Country::getIsoCode)
                .containsExactly("FR", "DE"); // France before Germany, by name
        assertThat(countryRepository.countByCitiesIsNotEmpty()).isEqualTo(2); // Germany counted once
    }

    @Test
    void visitedQueries_areEmptyWhenNothingVisited() {
        assertThat(countryRepository.findByCitiesIsNotEmptyOrderByNameAsc()).isEmpty();
        assertThat(countryRepository.countByCitiesIsNotEmpty()).isZero();
    }
}
