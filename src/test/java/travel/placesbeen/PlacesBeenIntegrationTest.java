package travel.placesbeen;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import travel.placesbeen.city.CityRepository;
import travel.placesbeen.country.CountryRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
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

    @MockitoSpyBean // real repository; two tests make its duplicate check miss, as a race would
    private CityRepository cityRepository;

    @Autowired
    private CountryRepository countryRepository;

    @BeforeEach
    void cleanDatabase() {
        cityRepository.deleteAll(); // countries are seeded reference data, so they stay
    }

    private String createCity(String name, String state, String countryCode, double lat, double lon) throws Exception {
        String body = """
                {"name": "%s", "state": "%s", "countryCode": "%s", "latitude": %s, "longitude": %s}
                """.formatted(name, state, countryCode, lat, lon);

        MvcResult result = mockMvc.perform(post("/api/cities").header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return result.getResponse().getHeader("Location");
    }

    @Test
    void createdCityIsRetrievableAtLocationHeader() throws Exception {
        String location = createCity("Berlin", "Berlin", "DE", 52.52, 13.405);

        mockMvc.perform(get(location).header("Api-Version", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Berlin"))
                .andExpect(jsonPath("$.state").value("Berlin"))
                .andExpect(jsonPath("$.country").value("Germany"))
                .andExpect(jsonPath("$.countryCode").value("DE"))
                .andExpect(jsonPath("$.countryFlag").value("🇩🇪"))
                .andExpect(jsonPath("$.latitude").value(52.52));
    }

    @Test
    void cityWithoutStateIsCreatedAndReturnedWithoutOne() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/cities").header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Gibraltar", "countryCode": "GI", "latitude": 36.14, "longitude": -5.35}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        mockMvc.perform(get(result.getResponse().getHeader("Location")).header("Api-Version", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Gibraltar"))
                .andExpect(jsonPath("$.state").doesNotExist());
    }

    @Test
    void addingCitiesCountsEachVisitedCountryOnce() throws Exception {
        createCity("Berlin", "Berlin", "DE", 52.52, 13.405);
        createCity("Munich", "Bavaria", "DE", 48.137, 11.575); // same country, counted once
        createCity("Lyon", "Métropole de Lyon", "FR", 45.764, 4.835);

        mockMvc.perform(get("/api/stats/summary").header("Api-Version", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCountries").value(2))
                .andExpect(jsonPath("$.totalCities").value(3));
    }

    @Test
    void sameCityNameInTwoStatesOfOneCountryIsAllowed() throws Exception {
        createCity("Portland", "Oregon", "US", 45.523, -122.676);
        createCity("Portland", "Maine", "US", 43.661, -70.255);

        mockMvc.perform(get("/api/stats/summary").header("Api-Version", "1"))
                .andExpect(jsonPath("$.totalCountries").value(1))
                .andExpect(jsonPath("$.totalCities").value(2));
    }

    @Test
    void sameCityNameAndStateIsRejectedWithConflict() throws Exception {
        createCity("Portland", "Oregon", "US", 45.523, -122.676);

        mockMvc.perform(post("/api/cities").header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "portland", "state": "Oregon", "countryCode": "US", "latitude": 45.523, "longitude": -122.676}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void sameCityWithNoStateIsRejectedWithConflict() throws Exception {
        String body = """
                {"name": "Gibraltar", "countryCode": "GI", "latitude": 36.14, "longitude": -5.35}
                """;

        mockMvc.perform(post("/api/cities").header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/cities").header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void stateDifferingOnlyInCaseOrSpacesIsRejectedWithConflict() throws Exception {
        createCity("Portland", "Maine", "US", 43.661, -70.255);

        for (String state : new String[]{"maine", " Maine "}) {
            mockMvc.perform(post("/api/cities").header("Api-Version", "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "Portland", "state": "%s", "countryCode": "US", "latitude": 43.661, "longitude": -70.255}
                                    """.formatted(state)))
                    .andExpect(status().isConflict());
        }
    }

    @Test
    void duplicateThatGetsPastTheCheckIsRejectedByTheDatabaseWithConflict() throws Exception {
        createCity("Portland", "Maine", "US", 43.661, -70.255);
        doReturn(false).when(cityRepository)
                .existsByNameIgnoreCaseAndStateIgnoreCaseAndCountry_IsoCode("Portland", "Maine", "US");

        mockMvc.perform(post("/api/cities").header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Portland", "state": "Maine", "countryCode": "US", "latitude": 43.661, "longitude": -70.255}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Database conflict: The record may already exist or violates a database constraint."));
    }

    @Test
    void renameThatGetsPastTheCheckIsRejectedByTheDatabaseWithConflict() throws Exception {
        createCity("Munich", "Bavaria", "DE", 48.137, 11.575);
        String nuremberg = createCity("Nuremberg", "Bavaria", "DE", 49.452, 11.077);
        doReturn(false).when(cityRepository)
                .existsByNameIgnoreCaseAndStateIgnoreCaseAndCountry_IsoCode("Munich", "Bavaria", "DE");

        // The UPDATE only runs at commit, so this also proves that failure still reaches the exception handler
        mockMvc.perform(put(nuremberg).header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Munich", "latitude": 49.452, "longitude": 11.077}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Database conflict: The record may already exist or violates a database constraint."));
    }

    @Test
    void sameCityNameInTwoCountriesIsAllowed() throws Exception {
        createCity("Valencia", "Valencia", "ES", 39.47, -0.376);
        createCity("Valencia", "Carabobo", "VE", 10.162, -68.008);

        mockMvc.perform(get("/api/stats/summary").header("Api-Version", "1"))
                .andExpect(jsonPath("$.totalCountries").value(2))
                .andExpect(jsonPath("$.totalCities").value(2));
    }

    @Test
    void countriesEndpointReturnsIsoCodeAndDerivedFlag() throws Exception {
        createCity("Berlin", "Berlin", "DE", 52.52, 13.405);

        mockMvc.perform(get("/api/countries").header("Api-Version", "1"))
                .andExpect(jsonPath("$[0].isoCode").value("DE"))
                .andExpect(jsonPath("$[0].flag").value("🇩🇪"));
    }

    @Test
    void countriesEndpointNestsCitiesWhenRequested() throws Exception {
        createCity("Berlin", "Berlin", "DE", 52.52, 13.405);

        mockMvc.perform(get("/api/countries").param("includeCities", "true").header("Api-Version", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Germany"))
                .andExpect(jsonPath("$[0].cities[0].name").value("Berlin"))
                .andExpect(jsonPath("$[0].cities[0].state").value("Berlin"));
    }

    @Test
    void duplicateCityIsRejectedWithConflict() throws Exception {
        createCity("Berlin", "Berlin", "DE", 52.52, 13.405);

        mockMvc.perform(post("/api/cities").header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "berlin", "state": "Berlin", "countryCode": "DE", "latitude": 1.0, "longitude": 1.0}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void updatingCityChangesItsFields() throws Exception {
        String location = createCity("Berlin", "Berlin", "DE", 52.52, 13.405);

        mockMvc.perform(put(location).header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Berlin Mitte", "latitude": 52.5, "longitude": 13.4}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Berlin Mitte"));

        mockMvc.perform(get(location).header("Api-Version", "1"))
                .andExpect(jsonPath("$.name").value("Berlin Mitte"))
                .andExpect(jsonPath("$.state").value("Berlin")) // not part of an update
                .andExpect(jsonPath("$.longitude").value(13.4));
    }

    @Test
    void renamingCityToExistingNameInSameStateIsRejectedWithConflict() throws Exception {
        createCity("Munich", "Bavaria", "DE", 48.137, 11.575);
        String nuremberg = createCity("Nuremberg", "Bavaria", "DE", 49.452, 11.077);

        mockMvc.perform(put(nuremberg).header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "MUNICH", "latitude": 49.452, "longitude": 11.077}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void renamingCityToNameUsedInAnotherStateIsAllowed() throws Exception {
        createCity("Berlin", "Berlin", "DE", 52.52, 13.405);
        String munich = createCity("Munich", "Bavaria", "DE", 48.137, 11.575);

        mockMvc.perform(put(munich).header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Berlin", "latitude": 48.137, "longitude": 11.575}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("Bavaria"));
    }

    @Test
    void updateIgnoresCountryCodeSoCityStaysInItsCountry() throws Exception {
        String location = createCity("Berlin", "Berlin", "DE", 52.52, 13.405);

        mockMvc.perform(put(location).header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Berlin", "countryCode": "FR", "latitude": 52.52, "longitude": 13.405}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countryCode").value("DE")); // not part of CityUpdateRequest, so ignored
    }

    @Test
    void deletingLastCityInCountryStopsItBeingVisited() throws Exception {
        String berlin = createCity("Berlin", "Berlin", "DE", 52.52, 13.405);
        String munich = createCity("Munich", "Bavaria", "DE", 48.137, 11.575);

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
