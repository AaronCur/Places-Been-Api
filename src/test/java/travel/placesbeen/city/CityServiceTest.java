package travel.placesbeen.city;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import travel.placesbeen.country.Country;
import travel.placesbeen.country.CountryRepository;
import travel.placesbeen.country.UnknownCountryCodeException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CityServiceTest {

    @Mock
    private CityRepository cityRepository;

    @Mock
    private CountryRepository countryRepository;

    @InjectMocks
    private CityService cityService;

    private static Country country(Long id, String name, String isoCode) {
        Country country = new Country(isoCode, name);
        country.setId(id);
        return country;
    }

    private static City city(Long id, String name, Country country) {
        City city = new City(name, 52.52, 13.405, country);
        city.setId(id);
        return city;
    }

    @Test
    void addCity_attachesCityToSeededCountry() {
        Country germany = country(7L, "Germany", "DE");
        CityRequest request = new CityRequest("Berlin", "DE", 52.52, 13.405);
        when(countryRepository.findByIsoCode("DE")).thenReturn(Optional.of(germany));
        when(cityRepository.existsByNameIgnoreCaseAndCountry_IsoCode("Berlin", "DE")).thenReturn(false);
        when(cityRepository.save(any(City.class))).thenAnswer(inv -> {
            City saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        CityResponse response = cityService.addCity(request);

        ArgumentCaptor<City> captor = ArgumentCaptor.forClass(City.class);
        verify(cityRepository).save(captor.capture());
        assertThat(captor.getValue().getCountry()).isSameAs(germany);
        verify(countryRepository, never()).save(any()); // countries are seeded, never created here

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Berlin");
        assertThat(response.country()).isEqualTo("Germany");
        assertThat(response.countryFlag()).isEqualTo("🇩🇪");
        assertThat(response.latitude()).isEqualTo(52.52);
        assertThat(response.longitude()).isEqualTo(13.405);
    }

    @Test
    void addCity_throwsWhenCountryCodeUnknown() {
        when(countryRepository.findByIsoCode("QQ")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cityService.addCity(new CityRequest("Nowhere", "QQ", 0.0, 0.0)))
                .isInstanceOf(UnknownCountryCodeException.class)
                .hasMessageContaining("QQ");

        verifyNoInteractions(cityRepository);
    }

    @Test
    void addCity_throwsWhenCityAlreadyExistsInCountry() {
        CityRequest request = new CityRequest("Berlin", "DE", 52.52, 13.405);
        when(countryRepository.findByIsoCode("DE")).thenReturn(Optional.of(country(7L, "Germany", "DE")));
        when(cityRepository.existsByNameIgnoreCaseAndCountry_IsoCode("Berlin", "DE")).thenReturn(true);

        assertThatThrownBy(() -> cityService.addCity(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Berlin")
                .hasMessageContaining("Germany");

        verify(cityRepository, never()).save(any());
    }

    @Test
    void getAllCities_mapsEveryCity() {
        Country germany = country(1L, "Germany", "DE");
        when(cityRepository.findAllByOrderByIdAsc()).thenReturn(List.of(
                city(1L, "Berlin", germany),
                city(2L, "Hamburg", germany)));

        List<CityResponse> result = cityService.getAllCities();

        assertThat(result).extracting(CityResponse::name).containsExactly("Berlin", "Hamburg");
        assertThat(result).allSatisfy(c -> assertThat(c.country()).isEqualTo("Germany"));
    }

    @Test
    void getAllCities_returnsEmptyListWhenNoCities() {
        when(cityRepository.findAllByOrderByIdAsc()).thenReturn(List.of());

        assertThat(cityService.getAllCities()).isEmpty();
    }

    @Test
    void getCityById_returnsCity() {
        when(cityRepository.findById(1L)).thenReturn(Optional.of(city(1L, "Berlin", country(1L, "Germany", "DE"))));

        CityResponse response = cityService.getCityById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Berlin");
        assertThat(response.countryFlag()).isEqualTo("🇩🇪");
    }

    @Test
    void getCityById_throwsWhenNotFound() {
        when(cityRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cityService.getCityById(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updateCityById_updatesNameAndCoordinates() {
        City existing = city(1L, "Berlin", country(1L, "Germany", "DE"));
        when(cityRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(cityRepository.save(existing)).thenReturn(existing);

        CityResponse response = cityService.updateCityById(new CityRequest("Berlin Mitte", "DE", 1.0, 2.0), 1L);

        assertThat(response.name()).isEqualTo("Berlin Mitte");
        assertThat(response.latitude()).isEqualTo(1.0);
        assertThat(response.longitude()).isEqualTo(2.0);
    }

    @Test
    void updateCityById_throwsWhenNotFound() {
        when(cityRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cityService.updateCityById(new CityRequest("X", "YY", 0.0, 0.0), 99L))
                .isInstanceOf(EntityNotFoundException.class);

        verify(cityRepository, never()).save(any());
    }

    @Test
    void deleteCityById_deletesCityButNeverTheSeededCountry() {
        when(cityRepository.findById(1L)).thenReturn(Optional.of(city(1L, "Berlin", country(5L, "Germany", "DE"))));

        cityService.deleteCityById(1L);

        verify(cityRepository).deleteById(1L);
        verifyNoInteractions(countryRepository);
    }

    @Test
    void deleteCityById_throwsWhenNotFound() {
        when(cityRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cityService.deleteCityById(99L))
                .isInstanceOf(EntityNotFoundException.class);

        verify(cityRepository, never()).deleteById(any());
        verifyNoInteractions(countryRepository);
    }
}
