package travel.placesbeen.country;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import travel.placesbeen.city.CityResponse;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CountryController.class)
class CountryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CountryService countryService;

    @Test
    void shouldReturnCountriesWithoutCitiesByDefault() throws Exception {
        when(countryService.getAllCountries(false))
                .thenReturn(List.of(new CountryResponse(1L, "Germany", "🇩🇪", null)));

        mockMvc.perform(get("/api/countries").header("Api-Version", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Germany"))
                .andExpect(jsonPath("$[0].flag").value("🇩🇪"))
                .andExpect(jsonPath("$[0].cities").doesNotExist());

        verify(countryService).getAllCountries(false);
    }

    @Test
    void shouldReturnCountriesWithCitiesWhenRequested() throws Exception {
        CityResponse berlin = new CityResponse(10L, "Berlin", 52.52, 13.405, null, null);
        when(countryService.getAllCountries(true))
                .thenReturn(List.of(new CountryResponse(1L, "Germany", "🇩🇪", List.of(berlin))));

        mockMvc.perform(get("/api/countries").param("includeCities", "true").header("Api-Version", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cities[0].name").value("Berlin"))
                .andExpect(jsonPath("$[0].cities[0].country").doesNotExist());
    }

    @Test
    void shouldReturn400WhenIncludeCitiesIsNotBoolean() throws Exception {
        mockMvc.perform(get("/api/countries").param("includeCities", "maybe").header("Api-Version", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value 'maybe' for parameter 'includeCities'"));
    }

    @Test
    void shouldReturn400WhenIdIsNotANumber() throws Exception {
        mockMvc.perform(get("/api/countries/abc").header("Api-Version", "1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnCountryById() throws Exception {
        when(countryService.getCountryById(1L)).thenReturn(new CountryResponse(1L, "Germany", null, null));

        mockMvc.perform(get("/api/countries/1").header("Api-Version", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Germany"))
                .andExpect(jsonPath("$.flag").doesNotExist());
    }

    @Test
    void shouldReturn404WhenCountryNotFound() throws Exception {
        when(countryService.getCountryById(42L)).thenThrow(new EntityNotFoundException("Country not found with id: 42"));

        mockMvc.perform(get("/api/countries/42").header("Api-Version", "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Country not found with id: 42"));
    }
}
