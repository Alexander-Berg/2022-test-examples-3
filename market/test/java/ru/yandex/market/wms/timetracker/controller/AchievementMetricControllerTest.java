package ru.yandex.market.wms.timetracker.controller;

import java.time.Instant;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.wms.achievement.model.metric.MetricEventDto;
import ru.yandex.market.wms.achievement.model.metric.PackingParcelMetric;
import ru.yandex.market.wms.timetracker.config.NoAuthTest;
import ru.yandex.market.wms.timetracker.service.AchievementMetricService;

@WebMvcTest(AchievementMetricController.class)
@ActiveProfiles("test")
@Import(NoAuthTest.class)
class AchievementMetricControllerTest {

    @Autowired
    private AchievementMetricService achievementMetricService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @TestConfiguration
    public static class TestConfig {
        @Bean
        AchievementMetricService achievementMetricService() {
            return Mockito.mock(AchievementMetricService.class);
        }
    }

    @Test
    void sendEventToAchievement() throws Exception {

        Mockito.doNothing().when(achievementMetricService)
                .sendMetricToAchievement(ArgumentMatchers.any(MetricEventDto.class));

        final MetricEventDto contentExpected =
                new MetricEventDto(
                        new PackingParcelMetric(1),
                        "test",
                        "SOF",
                        null,
                        Instant.parse("2022-06-10T12:00:00Z"));

        mockMvc
                .perform(
                        MockMvcRequestBuilders.post("/achievement-metric")
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(contentExpected)))
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
    }

    @Test
    void sendEventToAchievementValidationError() throws Exception {

        Mockito.doNothing().when(achievementMetricService)
                .sendMetricToAchievement(ArgumentMatchers.any(MetricEventDto.class));

        final String contentExpected = "randon text";

        mockMvc
                .perform(
                        MockMvcRequestBuilders.post("/achievement-metric")
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(contentExpected)))
                .andExpect(MockMvcResultMatchers.status().is4xxClientError());
    }
}
