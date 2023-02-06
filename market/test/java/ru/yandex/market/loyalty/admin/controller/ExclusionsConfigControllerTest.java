package ru.yandex.market.loyalty.admin.controller;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.core.model.exclusions.ExclusionsConfigEntry;
import ru.yandex.market.loyalty.test.TestFor;

import static org.junit.Assert.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author <a href="mailto:khamitov-rail@yandex-team.ru">Rail Khamitov</a>
 * @date 08.11.2021
 */
@TestFor(ExclusionsConfigController.class)
public class ExclusionsConfigControllerTest extends MarketLoyaltyAdminMockedDbTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private Clock clock;

    @Test
    public void shouldSaveNewVersion() throws Exception {
        String config = "{\"key\": \"value\"}";
        ExclusionsConfigEntry exclusionsConfigEntry = new ExclusionsConfigEntry(10000L,
                "MARKETDISCOUNT-0000",
                Timestamp.from(Instant.now(clock)),
                "admin_user", config);
        mockMvc
                .perform(post("/api/exclusions_config/save")
                        .content(objectMapper.writeValueAsString(exclusionsConfigEntry))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        ExclusionsConfigEntry latestConfig = getLatestConfig();
        assertEquals(config, latestConfig.getConfig());
    }

    @Test
    public void shouldFailWithWrongTicketFormat() throws Exception {
        ExclusionsConfigEntry exclusionsConfigEntry = new ExclusionsConfigEntry(1000L,
                "123",
                Timestamp.from(Instant.now(clock)),
                "admin_user", "{\"key\":\"value\"}");
        mockMvc
                .perform(post("/api/exclusions_config/save")
                        .content(objectMapper.writeValueAsString(exclusionsConfigEntry))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(log())
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void shouldFailWithWrongJsonConfigFormat() throws Exception {
        String wrongJson = "{\"key\":\"value}";
        ExclusionsConfigEntry exclusionsConfigEntry = new ExclusionsConfigEntry(1000L,
                "123",
                Timestamp.from(Instant.now(clock)),
                "admin_user", wrongJson);
        mockMvc
                .perform(post("/api/exclusions_config/save")
                        .content(objectMapper.writeValueAsString(exclusionsConfigEntry))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()))
                .andDo(log())
                .andExpect(status().isUnprocessableEntity());
    }

    private ExclusionsConfigEntry getLatestConfig() throws Exception {
        String contentAsString = mockMvc
                .perform(get("/api/exclusions_config/getActive").with(csrf()))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(contentAsString, ExclusionsConfigEntry.class);
    }

}
