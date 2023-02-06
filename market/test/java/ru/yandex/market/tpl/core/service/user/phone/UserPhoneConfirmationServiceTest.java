package ru.yandex.market.tpl.core.service.user.phone;

import java.time.Clock;
import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.user.phone.RequestClientInfo;
import ru.yandex.market.tpl.common.passport.client.PassportApiClient;
import ru.yandex.market.tpl.common.passport.client.exception.ErrorCode;
import ru.yandex.market.tpl.common.passport.client.exception.ErrorEntry;
import ru.yandex.market.tpl.common.passport.client.exception.PassportClientErrorException;
import ru.yandex.market.tpl.common.passport.client.model.CreateTrackResponse;
import ru.yandex.market.tpl.common.passport.client.model.FormattedPhoneNumber;
import ru.yandex.market.tpl.common.passport.client.model.PhoneConfirmCommitResponse;
import ru.yandex.market.tpl.common.passport.client.model.PhoneConfirmSubmitResponse;
import ru.yandex.market.tpl.common.passport.client.model.Status;
import ru.yandex.market.tpl.common.passport.client.model.ValidatePhoneNumberResponse;
import ru.yandex.market.tpl.common.util.exception.TplErrorCode;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserProperties;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.user.UserUtil;
import ru.yandex.market.tpl.core.exception.TplPassportServiceException;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.api.model.user.phone.ConfirmMethod.BY_FLASH_CALL;
import static ru.yandex.market.tpl.api.model.user.phone.ConfirmMethod.BY_SMS;

@RequiredArgsConstructor
public class UserPhoneConfirmationServiceTest extends TplAbstractTest {

    private static final RequestClientInfo REQUEST_CLIENT_INFO =
            RequestClientInfo.builder().clientIp("ip").clientUserAgent("useragent").build();

    private static final String TRACK_ID = "track-id";
    private static final String PHONE_NUMBER = "+79161234567";
    private static final String CODE = "123456";
    private static final FormattedPhoneNumber FORMATTED_PHONE_NUMBER = new FormattedPhoneNumber()
            .international("+7 916 123-45-67")
            .e164("+79161234567")
            .original("+79161234567")
            .maskedInternational("+7 916 123-**-**")
            .maskedE164("+7916123****")
            .maskedOriginal("+7916123****");

    private final UserPhoneConfirmationService service;
    private final PassportApiClient passportApiClient;
    private final TestUserHelper userHelper;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final UserPropertyService userPropertyService;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    private User user;

    @AfterEach
    void resetMocks() {
        Mockito.reset(passportApiClient);
    }

    @BeforeEach
    void init() {
        user = userHelper.findOrCreateUser(100500L);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void validatePhone(boolean isValidForCall) {
        // given
        mockValidateOkResponse(isValidForCall);
        // when
        var result = service.validatePhone(REQUEST_CLIENT_INFO, PHONE_NUMBER, BY_FLASH_CALL, TRACK_ID);
        // then
        assertThat(result).isEqualTo(isValidForCall);
    }

    @Test
    void commitPhoneWhenOk() {
        // given
        when(passportApiClient.phoneConfirmCommit(any(), any(), any()))
                .thenReturn(new PhoneConfirmCommitResponse().status(Status.OK).number(FORMATTED_PHONE_NUMBER));
        // when
        var result = service.commitPhone(REQUEST_CLIENT_INFO, CODE, TRACK_ID, user);
        // then
        assertThat(result.getPhoneNumber()).isEqualTo(FORMATTED_PHONE_NUMBER.getE164());
        transactionTemplate.execute(ts -> {
            var updatedUser = userHelper.findOrCreateUser(user.getUid());
            assertThat(updatedUser.getPhone()).isEqualTo(FORMATTED_PHONE_NUMBER.getE164());
            assertThat(updatedUser.getPhoneConfirmedAt()).isEqualTo(Instant.now(getClock()));
            return true;
        });
    }

    @Test
    void commitPhoneWhenInvalidCode() {
        // given
        when(passportApiClient.phoneConfirmCommit(any(), any(), any()))
                .thenThrow(PassportClientErrorException.fromSingleError(
                        HttpStatus.OK.toString(), ErrorEntry.create(ErrorCode.CODE_INVALID.getCode())));
        // when
        var exception = assertThrows(TplPassportServiceException.class,
                () -> service.commitPhone(REQUEST_CLIENT_INFO, CODE, TRACK_ID, user));
        // then
        assertException(exception, TplErrorCode.INVALID_CONFIRMATION_CODE);
        transactionTemplate.execute(ts -> {
            var updatedUser = userHelper.findOrCreateUser(user.getUid());
            assertThat(updatedUser.getPhone()).isNotEqualTo(FORMATTED_PHONE_NUMBER.getE164());
            assertThat(updatedUser.getPhoneConfirmedAt()).isNull();
            return true;
        });
    }

    @Test
    void submitPhoneByFlashCallWhenOk() {
        // given
        when(passportApiClient.phoneConfirmSubmit(any(), any(), any()))
                .thenReturn(new PhoneConfirmSubmitResponse()
                        .status(Status.OK)
                        .number(FORMATTED_PHONE_NUMBER)
                        .trackId(TRACK_ID)
                        .codeLength(1234)
                );
        mockValidateOkResponse(true);
        mockCreateTrackOkResponse();
        // when
        var result = service.submitPhone(REQUEST_CLIENT_INFO, FORMATTED_PHONE_NUMBER.getE164(),
                BY_FLASH_CALL, null);
        // then
        assertThat(result.getPhoneNumber()).isEqualTo(FORMATTED_PHONE_NUMBER.getE164());
        assertThat(result.getDenyResendUntil()).isNull();
        assertThat(result.getCodeLength()).isEqualTo(1234);
        assertThat(result.getTrackId()).isEqualTo(TRACK_ID);
    }

    @Test
    void submitPhoneByFlashCallWhenNumberInvalid() {
        // given
        when(passportApiClient.validatePhoneNumber(any(), any(), any()))
                .thenReturn(new ValidatePhoneNumberResponse()
                        .status(Status.OK)
                        .phoneNumber(FORMATTED_PHONE_NUMBER)
                        .validForCall(false)
                        .validForFlashCall(false)
                );
        mockCreateTrackOkResponse();
        // when
        var exception = assertThrows(TplPassportServiceException.class,
                () -> service.submitPhone(
                        REQUEST_CLIENT_INFO, FORMATTED_PHONE_NUMBER.getE164(), BY_FLASH_CALL, null)
        );
        // then
        assertException(exception, TplErrorCode.INVALID_PHONE_NUMBER);
    }

    @Test
    void submitPhoneBySmsWhenOk() {
        // given
        when(passportApiClient.phoneConfirmSubmit(any(), any(), any()))
                .thenReturn(new PhoneConfirmSubmitResponse()
                        .status(Status.OK)
                        .number(FORMATTED_PHONE_NUMBER)
                        .trackId(TRACK_ID)
                        .codeLength(1234)
                        .denyResendUntil(123456L)
                );
        mockCreateTrackOkResponse();
        // when
        var result = service.submitPhone(REQUEST_CLIENT_INFO, FORMATTED_PHONE_NUMBER.getE164(),
                BY_SMS, null);
        // then
        assertThat(result.getPhoneNumber()).isEqualTo(FORMATTED_PHONE_NUMBER.getE164());
        assertThat(result.getDenyResendUntil()).isEqualTo(Instant.ofEpochMilli(123456L));
        assertThat(result.getCodeLength()).isEqualTo(1234);
        assertThat(result.getTrackId()).isEqualTo(TRACK_ID);
    }

    @Test
    void submitPhoneBySmsWhenNumberInvalid() {
        // given
        when(passportApiClient.phoneConfirmSubmit(any(), any(), any()))
                .thenThrow(PassportClientErrorException.fromSingleError(
                        HttpStatus.OK.toString(), ErrorEntry.create(ErrorCode.NUMBER_INVALID.getCode())));
        mockCreateTrackOkResponse();
        // when
        var exception = assertThrows(TplPassportServiceException.class,
                () -> service.submitPhone(
                        REQUEST_CLIENT_INFO, FORMATTED_PHONE_NUMBER.getE164(), BY_SMS, null)
        );
        // then
        assertException(exception, TplErrorCode.INVALID_PHONE_NUMBER);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void isPhoneConfirmationRequired(boolean wasPhoneConfirmed) {
        // given
        transactionTemplate.execute(transactionStatus -> {
            userPropertyService.addPropertyToUser(user, UserProperties.PHONE_CONFIRMATION_REQUIREMENT_ENABLED, true);
            if (wasPhoneConfirmed) {
                UserUtil.setPhoneConfirmedAt(user, Instant.now(clock));
            }
            return true;
        });
        // when
        var result = service.isPhoneConfirmationRequired(user);
        // then
        assertThat(result.getIsPhoneConfirmationRequired()).isEqualTo(!wasPhoneConfirmed);
    }

    @Test
    void canDisablePhoneConfirmation() {
        // given
        transactionTemplate.execute(transactionStatus -> {
            userPropertyService.addPropertyToUser(user, UserProperties.PHONE_CONFIRMATION_REQUIREMENT_ENABLED, true);
            configurationServiceAdapter.insertValue(
                    ConfigurationProperties.PHONE_CONFIRMATION_REQUIREMENT_DISABLED, true);
            return true;
        });
        // when
        var result = service.isPhoneConfirmationRequired(user);
        // then
        assertFalse(result.getIsPhoneConfirmationRequired());
    }

    private void mockValidateOkResponse(boolean isValidForCall) {
        when(passportApiClient.validatePhoneNumber(any(), any(), any()))
                .thenReturn(new ValidatePhoneNumberResponse()
                        .status(Status.OK)
                        .phoneNumber(FORMATTED_PHONE_NUMBER)
                        .validForCall(isValidForCall)
                        .validForFlashCall(isValidForCall)
                );
    }

    private void mockCreateTrackOkResponse() {
        when(passportApiClient.createTrack(any(), any(), any()))
                .thenReturn(new CreateTrackResponse().status(Status.OK).id(TRACK_ID));
    }

    private void assertException(TplPassportServiceException exception, TplErrorCode errorCode) {
        assertThat(exception.getCode()).isEqualTo(errorCode.name());
        assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(exception.getMessage()).contains(TRACK_ID);
    }
}
