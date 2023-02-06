package ru.yandex.market.logistics.management.controller;

import java.time.Duration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.util.CleanDatabase;
import ru.yandex.market.logistics.management.util.TestUtil;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@CleanDatabase
public class WarehouseHandlingDurationControllerTest extends AbstractContextualTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DatabaseSetup("/data/service/partner/warehouse_duration_get.xml")
    void testGetDuration() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/externalApi/warehouse-handling-duration/1"))
            .andExpect(status().isOk())
            .andReturn();

        String content = result.getResponse().getContentAsString();
        String durationString = objectMapper.readValue(content, String.class);
        Duration duration = Duration.parse(durationString);

        softly.assertThat(duration)
            .isEqualTo(Duration.ofSeconds(300));
    }

    @Test
    @DatabaseSetup("/data/service/partner/warehouse_duration_update_successful.xml")
    @ExpectedDatabase(
        value = "/data/service/partner/after/warehouse_duration_update_successful.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testUpdateDuration() throws Exception {
        mockMvc.perform(
            MockMvcRequestBuilders.patch("/externalApi/warehouse-handling-duration/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.pathToJson("data/controller/warehouse_handling_duration/update_request.json"))
        )
            .andExpect(status().isOk());
    }

    @Test
    void testUpdateDurationInvalidRequest() throws Exception {
        MvcResult result = mockMvc.perform(
            MockMvcRequestBuilders.patch("/externalApi/warehouse-handling-duration/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtil.pathToJson("data/controller/warehouse_handling_duration/invalid_request.json"))
        )
            .andExpect(status().isBadRequest())
            .andReturn();

        softly.assertThat(result.getResolvedException())
            .isInstanceOf(HttpMessageNotReadableException.class)
            .hasMessageContaining("Text cannot be parsed to a Duration");
    }

}
