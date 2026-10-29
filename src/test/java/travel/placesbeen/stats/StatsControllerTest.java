package travel.placesbeen.stats;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatsController.class)
class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StatsService statsService;

    @Test
    void shouldReturnStatsSummary() throws Exception {
        when(statsService.getStatsSummary()).thenReturn(new StatsSummaryResponse(3, 12));

        mockMvc.perform(get("/api/stats/summary").header("Api-Version", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCountries").value(3))
                .andExpect(jsonPath("$.totalCities").value(12));
    }
}
