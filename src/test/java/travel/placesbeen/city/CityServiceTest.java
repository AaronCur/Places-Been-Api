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

    private static Country country(Long id, String name, String flag) {
        Country country = new Country(name);
        country.setId(id);
        country.setFlag(flag);
        return country;
    }

    private static City city(Long id, String name, Country country) {
        City city = new City(name, 52.52, 13.405, country);
        city.setId(id);
        return city;
    }

    @Test
    void addCity_createsNewCountryWhenNoneExists() {
        CityRequest request = new CityRequest("Berlin", "Germany", 52.52, 13.405);
        when(cityRepository.existsByNameIgnoreCaseAndCountry_NameIgnoreCase("Berlin", "Germany")).thenReturn(false);
        when(countryRepository.findByNameIgnoreCase("Germany")).thenReturn(Optional.empty());
        when(cityRepository.save(any(City.class))).thenAnswer(inv -> {
            City saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        CityResponse response = cityService.addCity(request);

        ArgumentCaptor<City> captor = ArgumentCaptor.forClass(City.class);
        verify(cityRepository).save(captor.capture());
        assertThat(captor.getValue().getCountry().getId()).isNull(); // new, unsaved country
        assertThat(captor.getValue().getCountry().getName()).isEqualTo("Germany");

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Berlin");
        assertThat(response.country()).isEqualTo("Germany");
        assertThat(response.latitude()).isEqualTo(52.52);
        assertThat(response.longitude()).isEqualTo(13.405);
    }

    @Test
    void addCity_reusesExistingCountry() {
        Country germany = country(7L, "Germany", "🇩🇪");
        CityRequest request = new CityRequest("Munich", "germany", 48.137, 11.575);
        when(cityRepository.existsByNameIgnoreCaseAndCountry_NameIgnoreCase("Munich", "germany")).thenReturn(false);
        when(countryRepository.findByNameIgnoreCase("germany")).thenReturn(Optional.of(germany));
        when(cityRepository.save(any(City.class))).thenAnswer(inv -> inv.getArgument(0));

        CityResponse response = cityService.addCity(request);

        ArgumentCaptor<City> captor = ArgumentCaptor.forClass(City.class);
        verify(cityRepository).save(captor.capture());
        assertThat(captor.getValue().getCountry()).isSameAs(germany);
        assertThat(response.country()).isEqualTo("Germany");
        assertThat(response.countryFlag()).isEqualTo("🇩🇪");
    }

    @Test
    void addCity_throwsWhenCityAlreadyExistsInCountry() {
        CityRequest request = new CityRequest("Berlin", "Germany", 52.52, 13.405);
        when(cityRepository.existsByNameIgnoreCaseAndCountry_NameIgnoreCase("Berlin", "Germany")).thenReturn(true);

        assertThatThrownBy(() -> cityService.addCity(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Berlin")
                .hasMessageContaining("Germany");

        verify(cityRepository, never()).save(any());
        verifyNoInteractions(countryRepository);
    }

    @Test
    void getAllCities_mapsEveryCity() {
        Country germany = country(1L, "Germany", "🇩🇪");
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
        when(cityRepository.findById(1L)).thenReturn(Optional.of(city(1L, "Berlin", country(1L, "Germany", null))));

        CityResponse response = cityService.getCityById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Berlin");
        assertThat(response.countryFlag()).isNull();
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
        City existing = city(1L, "Berlin", country(1L, "Germany", null));
        when(cityRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(cityRepository.save(existing)).thenReturn(existing);

        CityResponse response = cityService.updateCityById(new CityRequest("Berlin Mitte", "Germany", 1.0, 2.0), 1L);

        assertThat(response.name()).isEqualTo("Berlin Mitte");
        assertThat(response.latitude()).isEqualTo(1.0);
        assertThat(response.longitude()).isEqualTo(2.0);
    }

    @Test
    void updateCityById_throwsWhenNotFound() {
        when(cityRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cityService.updateCityById(new CityRequest("X", "Y", 0.0, 0.0), 99L))
                .isInstanceOf(EntityNotFoundException.class);

        verify(cityRepository, never()).save(any());
    }

    @Test
    void deleteCityById_deletesCountryWhenLastCityRemoved() {
        Country germany = country(5L, "Germany", null);
        when(cityRepository.findById(1L)).thenReturn(Optional.of(city(1L, "Berlin", germany)));
        when(cityRepository.countByCountryId(5L)).thenReturn(0L);

        cityService.deleteCityById(1L);

        verify(cityRepository).deleteById(1L);
        verify(countryRepository).delete(germany);
    }

    @Test
    void deleteCityById_keepsCountryWhenOtherCitiesRemain() {
        Country germany = country(5L, "Germany", null);
        when(cityRepository.findById(1L)).thenReturn(Optional.of(city(1L, "Berlin", germany)));
        when(cityRepository.countByCountryId(5L)).thenReturn(2L);

        cityService.deleteCityById(1L);

        verify(cityRepository).deleteById(1L);
        verify(countryRepository, never()).delete(any());
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
