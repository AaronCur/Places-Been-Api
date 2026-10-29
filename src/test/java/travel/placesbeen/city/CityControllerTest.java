package travel.placesbeen.city;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CityController.class)
public class CityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CityService cityService;

    private static final String VALID_BODY = """
            {
                "name": "Berlin",
                "country": "Germany",
                "latitude": 52.52,
                "longitude": 13.405
            }
            """;

    @Test
    void shouldReturnAllCities() throws Exception {
        CityResponse city = new CityResponse(1L, "Berlin", 52.52, 13.405, "Germany", "🇩🇪");
        when(cityService.getAllCities()).thenReturn(List.of(city));

        mockMvc.perform(get("/api/cities").header("Api-Version", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Berlin"));
    }

    @Test
    void shouldReturnCityById() throws Exception {
        CityResponse city = new CityResponse(1L, "Berlin", 52.52, 13.405, "Germany", "🇩🇪");
        when(cityService.getCityById(1L)).thenReturn(city);

        mockMvc.perform(get("/api/cities/1").header("Api-Version", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Berlin"));
    }

    @Test
    void shouldAddCity() throws Exception {
        CityResponse response = new CityResponse(1L, "Berlin", 52.52, 13.405, "Germany", "🇩🇪");
        when(cityService.addCity(any(CityRequest.class))).thenReturn(response);

        // Use a Java Text Block string literal instead of ObjectMapper
        String jsonRequestBody = """
                {
                    "name": "Berlin",
                    "country": "Germany",
                    "latitude": 52.52,
                    "longitude": 13.405
                }
                """;

        mockMvc.perform(post("/api/cities").header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequestBody)) // 👈 Injected directly
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("Berlin"));
    }

    @Test
    void shouldUpdateCity() throws Exception {
        CityResponse response = new CityResponse(1L, "Berlin Updated", 52.52, 13.405, "Germany", "🇩🇪");
        when(cityService.updateCityById(any(CityRequest.class), eq(1L))).thenReturn(response);

        // Use a Java Text Block string literal instead of ObjectMapper
        String jsonRequestBody = """
                {
                    "name": "Berlin Updated",
                    "country": "Germany",
                    "latitude": 52.52,
                    "longitude": 13.405
                }
                """;

        mockMvc.perform(put("/api/cities/1").header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequestBody)) // 👈 Injected directly
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Berlin Updated"));
    }

    @Test
    void shouldDeleteCity() throws Exception {
        mockMvc.perform(delete("/api/cities/1").header("Api-Version", "1"))
                .andExpect(status().isNoContent());

        verify(cityService).deleteCityById(1L);
    }

    @Test
    void shouldReturnEmptyListWhenNoCities() throws Exception {
        when(cityService.getAllCities()).thenReturn(List.of());

        mockMvc.perform(get("/api/cities").header("Api-Version", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void shouldReturn404WhenCityNotFound() throws Exception {
        when(cityService.getCityById(99L)).thenThrow(new EntityNotFoundException("City not found with id: 99"));

        mockMvc.perform(get("/api/cities/99").header("Api-Version", "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("City not found with id: 99"))
                .andExpect(jsonPath("$.path").value("/api/cities/99"));
    }

    @Test
    void shouldReturn404WhenDeletingMissingCity() throws Exception {
        doThrow(new EntityNotFoundException("City not found with id: 99")).when(cityService).deleteCityById(99L);

        mockMvc.perform(delete("/api/cities/99").header("Api-Version", "1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn409WhenCityAlreadyExists() throws Exception {
        when(cityService.addCity(any(CityRequest.class)))
                .thenThrow(new IllegalStateException("City already exists with name: Berlin in country: Germany"));

        mockMvc.perform(post("/api/cities").header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("City already exists with name: Berlin in country: Germany"));
    }

    @Test
    void shouldReturn400WhenRequiredFieldsMissing() throws Exception {
        String invalidBody = """
                {
                    "name": "",
                    "latitude": 52.52
                }
                """;

        mockMvc.perform(post("/api/cities").header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("name")))
                .andExpect(jsonPath("$.message", containsString("country")))
                .andExpect(jsonPath("$.message", containsString("longitude")));

        verifyNoInteractions(cityService);
    }

    @Test
    void shouldReturn400WhenUpdateBodyInvalid() throws Exception {
        mockMvc.perform(put("/api/cities/1").header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cityService);
    }

    @Test
    void shouldReturn400WhenJsonMalformed() throws Exception {
        mockMvc.perform(post("/api/cities").header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"Berlin\", "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed JSON request body or invalid data types."));
    }

    @Test
    void shouldReturn400WhenCoordinateIsNotANumber() throws Exception {
        String body = """
                {
                    "name": "Berlin",
                    "country": "Germany",
                    "latitude": "north",
                    "longitude": 13.405
                }
                """;

        mockMvc.perform(post("/api/cities").header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn405ForUnsupportedMethod() throws Exception {
        mockMvc.perform(patch("/api/cities/1").header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void shouldReturn500WithoutLeakingDetailsOnUnexpectedError() throws Exception {
        when(cityService.getAllCities()).thenThrow(new RuntimeException("db password is hunter2"));

        mockMvc.perform(get("/api/cities").header("Api-Version", "1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred on the server."));
    }
}