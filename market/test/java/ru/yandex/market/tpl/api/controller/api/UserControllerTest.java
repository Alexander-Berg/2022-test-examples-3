package ru.yandex.market.tpl.api.controller.api;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.api.BaseShallowTest;
import ru.yandex.market.tpl.api.WebLayerTest;
import ru.yandex.market.tpl.api.facade.UserFacade;
import ru.yandex.market.tpl.api.model.user.UserDto;
import ru.yandex.market.tpl.api.model.user.UserRole;
import ru.yandex.market.tpl.api.model.user.phone.CommitPhoneDto;
import ru.yandex.market.tpl.api.model.user.phone.RequestClientInfo;
import ru.yandex.market.tpl.api.model.user.phone.SubmitPhoneDto;
import ru.yandex.market.tpl.common.util.exception.TplErrorCode;
import ru.yandex.market.tpl.core.exception.TplPassportServiceException;

import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.tpl.api.ResourcesUtil.getFileContent;

@WebLayerTest(UserController.class)
public class UserControllerTest extends BaseShallowTest {

    private static final String PHONE_NUMBER = "+70001234567";
    private static final String TRACK_ID = "track-id";

    @MockBean
    private UserFacade userFacade;

    @BeforeEach
    void init() {
        doReturn(RequestClientInfo.builder().clientIp("1.1.1.1").clientUserAgent("Yandex Browser").build())
                .when(requestInfoService).extractRequestClientInfo(notNull());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldReturnUserDtoWithPhoneConfirmRequirement(boolean isPhoneConfirmationRequired) throws Exception {
        var userDto = createUserDto();
        userDto.setIsPhoneConfirmationRequired(isPhoneConfirmationRequired);
        doReturn(userDto).when(userFacade).get(notNull());

        String expectedPath = isPhoneConfirmationRequired ?
                "user/phone/user_dto_response_phone_required.json" :
                "user/phone/user_dto_response_phone_not_required.json";

        mockMvc.perform(
                        get("/api/user")
                                .header(AUTHORIZATION, AUTH_HEADER_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(expectedPath)));
    }

    @Test
    void shouldReturnCommittedPhone() throws Exception {
        doReturn(createCommitPhoneDto())
                .when(userFacade).commitPhone(notNull(), notNull(), notNull());

        mockMvc.perform(
                        post("/api/user/phone/commit")
                                .header(AUTHORIZATION, AUTH_HEADER_VALUE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(getFileContent("user/phone/commit_request.json")))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("user/phone/commit_response.json")));
    }

    @Test
    void shouldReturnSubmitPhone() throws Exception {
        doReturn(createSubmitPhoneDto())
                .when(userFacade).submitPhone(notNull(), notNull());

        mockMvc.perform(
                        post("/api/user/phone/submit")
                                .header(AUTHORIZATION, AUTH_HEADER_VALUE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(getFileContent("user/phone/submit_request.json")))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("user/phone/submit_response.json")));
    }

    @Test
    void shouldReturnErrorWhenInvalidPhone() throws Exception {
        doThrow(new TplPassportServiceException(TplErrorCode.INVALID_PHONE_NUMBER, "message"))
                .when(userFacade).submitPhone(notNull(), notNull());

        mockMvc.perform(
                        post("/api/user/phone/submit")
                                .header(AUTHORIZATION, AUTH_HEADER_VALUE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(getFileContent("user/phone/submit_request.json")))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void shouldReturnErrorWhenInvalidCode() throws Exception {
        doThrow(new TplPassportServiceException(TplErrorCode.INVALID_CONFIRMATION_CODE, "message"))
                .when(userFacade).commitPhone(notNull(), notNull(), notNull());

        mockMvc.perform(
                        post("/api/user/phone/commit")
                                .header(AUTHORIZATION, AUTH_HEADER_VALUE)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(getFileContent("user/phone/commit_request.json")))
                .andExpect(status().is4xxClientError());
    }

    private SubmitPhoneDto createSubmitPhoneDto() {
        return new SubmitPhoneDto(TRACK_ID, PHONE_NUMBER, Instant.parse("2022-03-17T21:00:00Z"), 6);
    }

    private CommitPhoneDto createCommitPhoneDto() {
        return new CommitPhoneDto(PHONE_NUMBER);
    }

    private UserDto createUserDto() {
        return new UserDto(
                123L,
                123456L,
                "email@example.com",
                "Full Name",
                UserRole.COURIER,
                null,
                Instant.parse("2022-03-17T21:00:00Z"),
                null,
                PHONE_NUMBER,
                null
        );
    }
}
