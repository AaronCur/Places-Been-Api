package travel.placesbeen.city;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CityController.class)
public class CityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CityService cityService;

    @Test
    void shouldReturnAllCities() throws Exception {
        CityResponse city = new CityResponse(1L, "Berlin", 52.52, 13.405, "Germany", "🇩🇪");
        when(cityService.getAllCities()).thenReturn(List.of(city));

        mockMvc.perform(get("/api/cities").header("X-API-VERSION", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Berlin"));
    }

    @Test
    void shouldReturnCityById() throws Exception {
        CityResponse city = new CityResponse(1L, "Berlin", 52.52, 13.405, "Germany", "🇩🇪");
        when(cityService.getCityById(1L)).thenReturn(city);

        mockMvc.perform(get("/api/cities/1").header("X-API-VERSION", "1"))
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

        mockMvc.perform(post("/api/cities").header("X-API-VERSION", "1")
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

        mockMvc.perform(put("/api/cities/1").header("X-API-VERSION", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequestBody)) // 👈 Injected directly
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Berlin Updated"));
    }

    @Test
    void shouldDeleteCity() throws Exception {
        mockMvc.perform(delete("/api/cities/1").header("X-API-VERSION", "1"))
                .andExpect(status().isNoContent());
    }
}