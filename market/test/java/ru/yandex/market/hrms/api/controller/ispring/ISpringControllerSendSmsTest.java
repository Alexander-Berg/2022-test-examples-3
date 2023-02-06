package ru.yandex.market.hrms.api.controller.ispring;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import one.util.streamex.StreamEx;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;
import ru.yandex.market.hrms.core.domain.ispring.ISpringSmsStatus;
import ru.yandex.market.hrms.core.service.sms.YaSmsResponse;
import ru.yandex.market.hrms.core.service.sms.YaSmsSendStatusEnum;
import ru.yandex.market.hrms.core.service.sms.YaSmsService;
import ru.yandex.market.ispring.ISpringClient;

import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DbUnitDataSet(before = "ISpringSmsControllerSendSmsTest.before.csv")
public class ISpringControllerSendSmsTest extends AbstractApiTest {
    private final static DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @MockBean
    private ISpringClient iSpringClient;

    @MockBean
    private YaSmsService yaSmsService;

    @BeforeEach
    public void mockServices() throws Exception {
        Mockito.doNothing().when(iSpringClient).changePassword(any(), any());
        Mockito.when(yaSmsService.sendSms(any(), any()))
                    .thenReturn(new YaSmsResponse(1L, YaSmsSendStatusEnum.SUCCESS, ""));
    }

    @Test
    void shouldSentSmsWhenEmployeeIsActiveFullCheck() throws Exception {
        List<String> ispringIds = List.of("ispring-id-employee", "ispring-id-outstaff", "ispring-id-candidate");
        String requestJson = generateJsonRequest(ispringIds);

        mockMvc.perform(post("/lms/ispring/sms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("send_sms_response_correct.json"), true));
    }

    @Test
    void shouldReturnErrorWhenIspringAccountNotExists() throws Exception {
        List<String> ispringIds = List.of("ispring-id-not-exist");
        String requestJson = generateJsonRequest(ispringIds);

        mockMvc.perform(post("/lms/ispring/sms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.results[0].smsStatus").value(ISpringSmsStatus.FAILED.name()))
                .andExpect(jsonPath("$.results[0].errorMessage", containsStringIgnoringCase(
                        "Не найден работник с ispringId = ispring-id-not-exist")));
    }

    @Test
    void shouldReturnErrorWhenEmployeeIsNotActive() throws Exception {
        List<String> ispringIds = List.of("ispring-id-deactivated");
        String requestJson = generateJsonRequest(ispringIds);

        mockMvc.perform(post("/lms/ispring/sms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.results[0].smsStatus").value(ISpringSmsStatus.FAILED.name()))
                .andExpect(jsonPath("$.results[0].errorMessage", containsStringIgnoringCase(
                        "Не найден работник с ispringId = ispring-id-deactivated")));
    }

    @Test
    void shouldReturnErrorWhenSmsCouldNotSend() throws Exception {
        doThrow(new RuntimeException("Sms send error")).
                when(iSpringClient).changePassword(any(), any());

        List<String> ispringIds = List.of("ispring-id-employee");
        String requestJson = generateJsonRequest(ispringIds);

        mockMvc.perform(post("/lms/ispring/sms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.results[0].smsStatus").value(ISpringSmsStatus.FAILED.name()))
                .andExpect(jsonPath("$.results[0].errorMessage",
                        containsStringIgnoringCase("Произошла ошибка при отправке SMS")));
    }

    @Test
    void shouldReturnErrorWhenTooManyRequests() throws Exception {
        LocalDateTime now = LocalDateTime.parse("2022-03-04 12:01:00", DATE_TIME_FORMATTER);
        mockClock(now);

        List<String> ispringIds = List.of("ispring-id-employee");
        String requestJson = generateJsonRequest(ispringIds);
        mockMvc.perform(post("/lms/ispring/sms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.results[0].smsStatus").value(ISpringSmsStatus.SENT.name()));

        mockClock(now.plusSeconds(30));
        mockMvc.perform(post("/lms/ispring/sms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.results[0].smsStatus").value(ISpringSmsStatus.FAILED.name()));
    }

    @Test
    void shouldSentSmsWhenExistEmployeeAndCandidateWithSamePhone() throws Exception {
        LocalDateTime now = LocalDateTime.parse("2022-03-04 12:01:00", DATE_TIME_FORMATTER);
        mockClock(now);

        List<String> ispringIds = List.of("ispring-id-duplicate");
        String requestJson = generateJsonRequest(ispringIds);
        mockMvc.perform(post("/lms/ispring/sms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.results[0].smsStatus").value(ISpringSmsStatus.SENT.name()));
    }

    private String generateJsonRequest(List<String> ispringIds) {
        String listIds = StreamEx.of(ispringIds)
                .map("\"%s\""::formatted)
                .collect(Collectors.joining(", "));

        return "{ \"ispringIds\": [%s]}".formatted(listIds);
    }
}
