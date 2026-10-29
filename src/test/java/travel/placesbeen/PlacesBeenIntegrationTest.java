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
        cityRepository.deleteAll();
        countryRepository.deleteAll();
    }

    private String createCity(String name, String country, double lat, double lon) throws Exception {
        String body = """
                {"name": "%s", "country": "%s", "latitude": %s, "longitude": %s}
                """.formatted(name, country, lat, lon);

        MvcResult result = mockMvc.perform(post("/api/cities").header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return result.getResponse().getHeader("Location");
    }

    @Test
    void createdCityIsRetrievableAtLocationHeader() throws Exception {
        String location = createCity("Berlin", "Germany", 52.52, 13.405);

        mockMvc.perform(get(location).header("Api-Version", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Berlin"))
                .andExpect(jsonPath("$.country").value("Germany"))
                .andExpect(jsonPath("$.latitude").value(52.52));
    }

    @Test
    void addingCitiesCreatesCountryOnceAndUpdatesStats() throws Exception {
        createCity("Berlin", "Germany", 52.52, 13.405);
        createCity("Munich", "germany", 48.137, 11.575); // different case, same country
        createCity("Lyon", "France", 45.764, 4.835);

        mockMvc.perform(get("/api/stats/summary").header("Api-Version", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCountries").value(2))
                .andExpect(jsonPath("$.totalCities").value(3));
    }

    @Test
    void countriesEndpointNestsCitiesWhenRequested() throws Exception {
        createCity("Berlin", "Germany", 52.52, 13.405);

        mockMvc.perform(get("/api/countries").param("includeCities", "true").header("Api-Version", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Germany"))
                .andExpect(jsonPath("$[0].cities[0].name").value("Berlin"));
    }

    @Test
    void duplicateCityIsRejectedWithConflict() throws Exception {
        createCity("Berlin", "Germany", 52.52, 13.405);

        mockMvc.perform(post("/api/cities").header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "berlin", "country": "GERMANY", "latitude": 1.0, "longitude": 1.0}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void updatingCityChangesItsFields() throws Exception {
        String location = createCity("Berlin", "Germany", 52.52, 13.405);

        mockMvc.perform(put(location).header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Berlin Mitte", "country": "Germany", "latitude": 52.5, "longitude": 13.4}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Berlin Mitte"));

        mockMvc.perform(get(location).header("Api-Version", "1"))
                .andExpect(jsonPath("$.name").value("Berlin Mitte"))
                .andExpect(jsonPath("$.longitude").value(13.4));
    }

    @Test
    void deletingLastCityInCountryAlsoDeletesCountry() throws Exception {
        String berlin = createCity("Berlin", "Germany", 52.52, 13.405);
        String munich = createCity("Munich", "Germany", 48.137, 11.575);

        mockMvc.perform(delete(berlin).header("Api-Version", "1")).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/countries").header("Api-Version", "1"))
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(delete(munich).header("Api-Version", "1")).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/countries").header("Api-Version", "1"))
                .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get(munich).header("Api-Version", "1"))
                .andExpect(status().isNotFound());
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
