package ru.yandex.direct.core.service.integration.passport;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.inside.passport.internal.api.PassportClient;
import ru.yandex.inside.passport.internal.api.YaHeaders;
import ru.yandex.inside.passport.internal.api.exceptions.RecoverablePassportClientException;
import ru.yandex.inside.passport.internal.api.models.registration.RegistrationRequest;
import ru.yandex.inside.passport.internal.api.models.registration.RegistrationResponse;
import ru.yandex.inside.passport.internal.api.services.AccountService;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

public class PassportServiceRegisterUserTest {
    private static final String CONSUMER = "cons";
    private static final int MAX_RETRY_COUNT = 2;
    private static final String TRANSACTION_ID = "1";
    private static final String LOGIN = "test";
    private static final String FIRST_NAME = "testFirstName";
    private static final String LAST_NAME = "testLastName";
    private static final RegisterUserRequest REQUEST = new RegisterUserRequest()
            .withLogin(LOGIN)
            .withFirstName(FIRST_NAME)
            .withLastName(LAST_NAME);
    private static final Long UID = 1L;
    private static final String REMOTE_CLIENT_IP = "127.0.0.1";

    private AccountService accountService;
    private PassportService passportService;

    private static RegistrationResponse createSuccessResponse(String uid) {
        RegistrationResponse result = new RegistrationResponse();
        result.setStatus(RegistrationResponse.OK_STATUS);
        result.setUid(uid);
        result.setErrors(emptyList());
        return result;
    }


    private static RegistrationResponse createFailResponse(List<String> errors) {
        RegistrationResponse result = new RegistrationResponse();
        result.setStatus("error");
        result.setErrors(errors);
        return result;
    }

    @Before
    public void setUp() {
        accountService = mock(AccountService.class);

        PassportClient passportClient = mock(PassportClient.class);
        when(passportClient.accounts()).thenReturn(accountService);

        passportService = new PassportService(
                passportClient,
                CONSUMER,
                MAX_RETRY_COUNT,
                0);
    }

    @Test
    public void testSuccess() {
        when(
                accountService.registerByMiddleman(
                        anyString(),
                        anyString(),
                        any(RegistrationRequest.class),
                        any(YaHeaders.class)))
                .thenReturn(createSuccessResponse(UID.toString()));

        RegisterUserResult result = passportService.registerUser(TRANSACTION_ID, REQUEST, REMOTE_CLIENT_IP);

        verify(accountService).registerByMiddleman(
                eq(TRANSACTION_ID),
                eq(CONSUMER),
                any(RegistrationRequest.class),
                /*
                TODO: Добавить в iceberg/inside-passport-internal-api getter-ы
                argThat(
                        allOf(
                                hasProperty("login", equalTo(LOGIN)),
                                hasProperty("firstname", equalTo(FIRST_NAME)),
                                hasProperty("lastname", equalTo(LAST_NAME)))),
                */
                argThat(hasProperty("yaConsumerClientIp", equalTo(REMOTE_CLIENT_IP))));

        assertThat(
                result,
                allOf(
                        hasProperty("status", equalTo(RegisterUserStatus.OK)),
                        hasProperty("uid", equalTo(UID)),
                        hasProperty("email", equalTo(LOGIN + "@" + PassportService.DEFAULT_EMAIL_DOMAIN))));
    }

    private void testFailedWithStatus(String returnedError, RegisterUserStatus expectedStatus) {
        when(
                accountService.registerByMiddleman(
                        anyString(),
                        anyString(),
                        any(RegistrationRequest.class),
                        any(YaHeaders.class)))
                .thenReturn(createFailResponse(singletonList(returnedError)));

        RegisterUserResult result = passportService.registerUser(TRANSACTION_ID, REQUEST, REMOTE_CLIENT_IP);

        assertThat(
                result, hasProperty("status", equalTo(expectedStatus)));
    }

    @Test
    public void testFail_LoginOccupied() {
        testFailedWithStatus(PassportService.ACCOUNT_ALREADY_REGISTERED_ERROR, RegisterUserStatus.LOGIN_OCCUPIED);
    }

    @Test
    public void testFail_RequestFailed() {
        testFailedWithStatus("unknown", RegisterUserStatus.REQUEST_FAILED);
    }

    private void testFailedWithInternalError(Throwable cause, int expectedRetryCount) {
        when(
                accountService.registerByMiddleman(
                        anyString(),
                        anyString(),
                        any(RegistrationRequest.class),
                        any(YaHeaders.class)))
                .thenThrow(cause);

        RegisterUserResult result = passportService.registerUser(TRANSACTION_ID, REQUEST, REMOTE_CLIENT_IP);

        // Проверяем кол-во retry-ев
        verify(accountService, times(expectedRetryCount)).registerByMiddleman(
                anyString(),
                anyString(),
                any(RegistrationRequest.class),
                any(YaHeaders.class));

        assertThat(
                result, hasProperty("status", equalTo(RegisterUserStatus.INTERNAL_ERROR)));
    }

    @Test
    public void testFail_InternalError_Without_Retry() {
        testFailedWithInternalError(new RuntimeException(), 1);
    }

    @Test
    public void testFail_InternalError_With_Retry() {
        testFailedWithInternalError(new RecoverablePassportClientException(TRANSACTION_ID, null), MAX_RETRY_COUNT);
    }
}
