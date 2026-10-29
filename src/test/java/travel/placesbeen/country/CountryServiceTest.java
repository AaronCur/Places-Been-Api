package travel.placesbeen.country;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import travel.placesbeen.city.City;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CountryServiceTest {

    @Mock
    private CountryRepository countryRepository;

    @InjectMocks
    private CountryService countryService;

    private static Country germanyWithCities() {
        Country germany = new Country("Germany");
        germany.setId(1L);
        germany.setFlag("🇩🇪");
        City berlin = new City("Berlin", 52.52, 13.405, germany);
        berlin.setId(10L);
        City munich = new City("Munich", 48.137, 11.575, germany);
        munich.setId(11L);
        germany.getCities().addAll(List.of(berlin, munich));
        return germany;
    }

    @Test
    void getAllCountries_withoutCities_omitsCityList() {
        when(countryRepository.findAll()).thenReturn(List.of(germanyWithCities()));

        List<CountryResponse> result = countryService.getAllCountries(false);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).isEqualTo("Germany");
        assertThat(result.getFirst().flag()).isEqualTo("🇩🇪");
        assertThat(result.getFirst().cities()).isNull();
    }

    @Test
    void getAllCountries_withCities_includesNestedCitiesWithoutCountryFields() {
        when(countryRepository.findAll()).thenReturn(List.of(germanyWithCities()));

        List<CountryResponse> result = countryService.getAllCountries(true);

        assertThat(result.getFirst().cities())
                .extracting("name")
                .containsExactly("Berlin", "Munich");
        // country/countryFlag are redundant inside a country, so they are left null
        assertThat(result.getFirst().cities()).allSatisfy(city -> {
            assertThat(city.country()).isNull();
            assertThat(city.countryFlag()).isNull();
        });
    }

    @Test
    void getAllCountries_returnsEmptyListWhenNoCountries() {
        when(countryRepository.findAll()).thenReturn(List.of());

        assertThat(countryService.getAllCountries(true)).isEmpty();
    }

    @Test
    void getCountryById_returnsCountryWithoutCities() {
        when(countryRepository.findById(1L)).thenReturn(Optional.of(germanyWithCities()));

        CountryResponse response = countryService.getCountryById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Germany");
        assertThat(response.cities()).isNull();
    }

    @Test
    void getCountryById_throwsWhenNotFound() {
        when(countryRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> countryService.getCountryById(42L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("42");
    }
}
