package ru.yandex.market.wms.timetracker.controller;

import java.time.LocalDateTime;
import java.util.List;

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

import ru.yandex.market.wms.timetracker.config.NoAuthTest;
import ru.yandex.market.wms.timetracker.model.enums.EmployeeStatus;
import ru.yandex.market.wms.timetracker.model.enums.EnumerationOrder;
import ru.yandex.market.wms.timetracker.response.SystemActivityAssignRequest;
import ru.yandex.market.wms.timetracker.response.SystemActivityResponse;
import ru.yandex.market.wms.timetracker.service.SystemActivityService;
import ru.yandex.market.wms.timetracker.specification.rsql.ApiField;

@WebMvcTest(SystemActivityController.class)
@ActiveProfiles("test")
@Import(NoAuthTest.class)
class SystemActivityControllerTest {

    @Autowired
    private SystemActivityService systemActivityService;

    @Autowired
    private MockMvc mockMvc;

    @TestConfiguration
    public static class TestConfig {
        @Bean("SystemActivityServiceUseCaseService")
        SystemActivityService systemActivityService() {
            return Mockito.mock(SystemActivityService.class);
        }
    }

    @Test
    void activities() throws Exception {

        Mockito.when(systemActivityService.activities()).thenReturn(List.of(
                "CONSOLIDATION",
                "PRECONSOLIDATION")
        );

        String contentExpected = "[\"CONSOLIDATION\", \"PRECONSOLIDATION\"]";

        mockMvc
                .perform(MockMvcRequestBuilders.get("/system-activity/sof/list"))
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andExpect(MockMvcResultMatchers.content().json(contentExpected));
    }

    @Test
    void assigned() throws Exception {

        String contentExpected = "{" +
                "\"limit\": 20,  " +
                "\"offset\": 0, " +
                "\"total\": 300, " +
                "\"content\": [" +
                "               {" +
                "                   \"userName\" : \"test\", " +
                "                   \"process\": \"process\", " +
                "                   \"assigner\": \"assigner\", " +
                "                   \"create_time\": \"2021-12-15T12:30:50\", " +
                "                   \"expected_end_time\": \"2021-12-15T12:30:50\", " +
                "                   \"user_started_activity\": true" +
                "               }" +
                "           ]" +
                "}";

        Mockito.when(systemActivityService.assignedProcessTotal(
                ArgumentMatchers.eq("sof"), ArgumentMatchers.eq("")))
                .thenReturn(300L);

        Mockito.when(systemActivityService.currentAssignedProcess(
                        ArgumentMatchers.eq("sof"),
                        ArgumentMatchers.any(Integer.class),
                        ArgumentMatchers.any(Integer.class),
                        ArgumentMatchers.any(String.class),
                        ArgumentMatchers.any(ApiField.class),
                        ArgumentMatchers.any(EnumerationOrder.class)
                ))
                .thenReturn(
                        List.of(
                                SystemActivityResponse.builder()
                                        .userName("test")
                                        .process("process")
                                        .assigner("assigner")
                                        .createTime(LocalDateTime.of(2021, 12, 15, 12, 30, 50))
                                        .expectedEndTime(LocalDateTime.of(2021, 12, 15, 12, 30, 50))
                                        .userStartedActivity(true)
                                        .build()
                        )
                );

        mockMvc
                .perform(MockMvcRequestBuilders.get("/system-activity/sof/assigned"))
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andExpect(MockMvcResultMatchers.content().json(contentExpected));
    }

    @Test
    void assign() throws Exception {

        String content = "{" +
                "\"assigner\" : \"assigner\", " +
                "\"expected_end_time\" : null, " +
                "\"users\" : [\"test\",\"secondTest\"]}";

        Mockito.doNothing().when(systemActivityService).assign(
                ArgumentMatchers.eq("sof"),
                ArgumentMatchers.any(EmployeeStatus.class),
                ArgumentMatchers.any(SystemActivityAssignRequest.class)
        );

        mockMvc
                .perform(MockMvcRequestBuilders.post("/system-activity/sof/dropping/assign")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
    }

    @Test
    void assignBadRequestWhenUserNameEmpty() throws Exception {

        String content = "{\"users\" : [\"test\",\"\"]}";

        mockMvc
                .perform(MockMvcRequestBuilders.post("/system-activity/sof/dropping/assign")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(MockMvcResultMatchers.status().is4xxClientError());
    }

    @Test
    void assignBadRequestWhenUserNameNull() throws Exception {

        String content = "{\"users\" : [\"test\",null]}";

        mockMvc
                .perform(MockMvcRequestBuilders.post("/system-activity/sof/dropping/assign")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(MockMvcResultMatchers.status().is4xxClientError());
    }

    @Test
    void finish() throws Exception {
        String content = "{\"users\" : [\"test\",\"secondTest\"]}";

        Mockito.doNothing().when(systemActivityService).finish(
                ArgumentMatchers.eq("sof"),
                ArgumentMatchers.anyCollection()
        );

        mockMvc
                .perform(MockMvcRequestBuilders.post("/system-activity/sof/finish")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful());
    }

    @Test
    void finishBadRequestWhenUserNameEmpty() throws Exception {

        String content = "{\"users\" : [\"test\",\"\"]}";

        mockMvc
                .perform(MockMvcRequestBuilders.post("/system-activity/sof/finish")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(MockMvcResultMatchers.status().is4xxClientError());
    }

    @Test
    void finishBadRequestWhenUserNameNull() throws Exception {

        String content = "{\"users\" : [\"test\",null]}";

        mockMvc
                .perform(MockMvcRequestBuilders.post("/system-activity/sof/finish")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(MockMvcResultMatchers.status().is4xxClientError());
    }
}
