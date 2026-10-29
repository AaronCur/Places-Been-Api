package travel.placesbeen;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import travel.placesbeen.city.CityRepository;
import travel.placesbeen.country.CountryRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests through the full Spring context against the in-memory H2 database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PlacesBeenIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private CountryRepository countryRepository;

    @BeforeEach
    void cleanDatabase() {
        cityRepository.deleteAll(); // countries are seeded reference data, so they stay
    }

    private String createCity(String name, String countryCode, double lat, double lon) throws Exception {
        String body = """
                {"name": "%s", "countryCode": "%s", "latitude": %s, "longitude": %s}
                """.formatted(name, countryCode, lat, lon);

        MvcResult result = mockMvc.perform(post("/api/cities").header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return result.getResponse().getHeader("Location");
    }

    @Test
    void createdCityIsRetrievableAtLocationHeader() throws Exception {
        String location = createCity("Berlin", "DE", 52.52, 13.405);

        mockMvc.perform(get(location).header("Api-Version", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Berlin"))
                .andExpect(jsonPath("$.country").value("Germany"))
                .andExpect(jsonPath("$.latitude").value(52.52));
    }

    @Test
    void addingCitiesCountsEachVisitedCountryOnce() throws Exception {
        createCity("Berlin", "DE", 52.52, 13.405);
        createCity("Munich", "DE", 48.137, 11.575); // same country, counted once
        createCity("Lyon", "FR", 45.764, 4.835);

        mockMvc.perform(get("/api/stats/summary").header("Api-Version", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCountries").value(2))
                .andExpect(jsonPath("$.totalCities").value(3));
    }

    @Test
    void sameCityNameInTwoCountriesIsAllowed() throws Exception {
        createCity("Valencia", "ES", 39.47, -0.376);
        createCity("Valencia", "VE", 10.162, -68.008);

        mockMvc.perform(get("/api/stats/summary").header("Api-Version", "1"))
                .andExpect(jsonPath("$.totalCountries").value(2))
                .andExpect(jsonPath("$.totalCities").value(2));
    }

    @Test
    void countriesEndpointReturnsIsoCodeAndDerivedFlag() throws Exception {
        createCity("Berlin", "DE", 52.52, 13.405);

        mockMvc.perform(get("/api/countries").header("Api-Version", "1"))
                .andExpect(jsonPath("$[0].isoCode").value("DE"))
                .andExpect(jsonPath("$[0].flag").value("🇩🇪"));
    }

    @Test
    void countriesEndpointNestsCitiesWhenRequested() throws Exception {
        createCity("Berlin", "DE", 52.52, 13.405);

        mockMvc.perform(get("/api/countries").param("includeCities", "true").header("Api-Version", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Germany"))
                .andExpect(jsonPath("$[0].cities[0].name").value("Berlin"));
    }

    @Test
    void duplicateCityIsRejectedWithConflict() throws Exception {
        createCity("Berlin", "DE", 52.52, 13.405);

        mockMvc.perform(post("/api/cities").header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "berlin", "countryCode": "DE", "latitude": 1.0, "longitude": 1.0}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void updatingCityChangesItsFields() throws Exception {
        String location = createCity("Berlin", "DE", 52.52, 13.405);

        mockMvc.perform(put(location).header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Berlin Mitte", "countryCode": "DE", "latitude": 52.5, "longitude": 13.4}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Berlin Mitte"));

        mockMvc.perform(get(location).header("Api-Version", "1"))
                .andExpect(jsonPath("$.name").value("Berlin Mitte"))
                .andExpect(jsonPath("$.longitude").value(13.4));
    }

    @Test
    void deletingLastCityInCountryStopsItBeingVisited() throws Exception {
        String berlin = createCity("Berlin", "DE", 52.52, 13.405);
        String munich = createCity("Munich", "DE", 48.137, 11.575);

        mockMvc.perform(delete(berlin).header("Api-Version", "1")).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/countries").header("Api-Version", "1"))
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(delete(munich).header("Api-Version", "1")).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/countries").header("Api-Version", "1"))
                .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get(munich).header("Api-Version", "1"))
                .andExpect(status().isNotFound());
        assertThat(countryRepository.findByIsoCode("DE")).isPresent(); // seeded country is kept
    }

    @Test
    void unknownCountryCodeIsRejectedWith400() throws Exception {
        mockMvc.perform(post("/api/cities").header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Nowhere", "countryCode": "QQ", "latitude": 0.0, "longitude": 0.0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unknown country code: QQ"));
    }

    @Test
    void unknownIdsReturn404() throws Exception {
        mockMvc.perform(get("/api/cities/999999").header("Api-Version", "1"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/countries/999999").header("Api-Version", "1"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/cities/999999").header("Api-Version", "1"))
                .andExpect(status().isNotFound());
    }
}
