package ru.yandex.direct.core.entity.client.service;

import java.util.Collection;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ru.yandex.direct.core.ErrorCodes;
import ru.yandex.direct.core.entity.agency.service.AgencyService;
import ru.yandex.direct.core.entity.application.model.AgencyOptions;
import ru.yandex.direct.core.entity.client.exception.BalanceErrorException;
import ru.yandex.direct.core.entity.client.exception.CantRegisterClientException;
import ru.yandex.direct.core.entity.client.exception.PassportErrorException;
import ru.yandex.direct.core.entity.client.model.AddAgencyClientRequest;
import ru.yandex.direct.core.entity.client.model.AddAgencyClientResponse;
import ru.yandex.direct.core.entity.client.model.ClientWithOptions;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.security.MethodNotAllowedException;
import ru.yandex.direct.core.service.RequestInfoProvider;
import ru.yandex.direct.core.service.integration.balance.BalanceService;
import ru.yandex.direct.core.service.integration.balance.RegisterClientRequest;
import ru.yandex.direct.core.service.integration.balance.RegisterClientResult;
import ru.yandex.direct.core.service.integration.balance.RegisterClientStatus;
import ru.yandex.direct.core.service.integration.passport.PassportService;
import ru.yandex.direct.core.service.integration.passport.RegisterUserRequest;
import ru.yandex.direct.core.service.integration.passport.RegisterUserResult;
import ru.yandex.direct.core.service.integration.passport.RegisterUserStatus;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.i18n.Language;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

public class AddAgencyClientServiceTest {
    private static final UidAndClientId OPERATOR = UidAndClientId.of(1L, ClientId.fromLong(2L));
    private static final UidAndClientId AGENCY = UidAndClientId.of(10L, ClientId.fromLong(20L));
    private static final UidAndClientId AGENCY_MAIN_CHIEF = UidAndClientId.of(100L, ClientId.fromLong(200L));
    private static final String LOGIN = "test";
    private static final String PASSWORD = "password";
    private static final String FIRST_NAME = "testFirstName";
    private static final String LAST_NAME = "testLastName";
    private static final String NAME = FIRST_NAME + " " + LAST_NAME;
    private static final CurrencyCode CURRENCY = CurrencyCode.RUB;
    private static final String NOTIFICATION_EMAIL = "notify@yandex.ru";
    private static final Language NOTIFICATION_LANG = Language.RU;
    private static final String EMAIL = "test@yandex.ru";
    private static final Long UID = 30L;
    private static final ClientId CLIENT_ID = ClientId.fromLong(40L);
    private static final String REQUEST_ID = "1";
    private static final String REMOTE_ID_ADDRESS = "127.0.0.1";

    @Rule
    public ExpectedException expected = ExpectedException.none();

    private RequestInfoProvider requestInfoProvider;
    private UserService userService;
    private AddAgencyClientValidator validator;
    private PassportService passportService;
    private BalanceService balanceService;
    private ClientService clientService;
    private AddAgencyClientService addAgencyClientService;
    private AddAgencyClientRequest request;
    private AgencyService agencyService;
    private AgencyLimitedRepsService agencyLimitedRepsService;
    private RbacService rbacService;
    private FeatureService featureService;


    private static AddAgencyClientRequest createRequest() {
        return new AddAgencyClientRequest()
                .withLogin(LOGIN)
                .withFirstName(FIRST_NAME)
                .withLastName(LAST_NAME)
                .withCurrency(CURRENCY)
                .withNotificationEmail(NOTIFICATION_EMAIL)
                .withNotificationLang(NOTIFICATION_LANG)
                .withSendNews(true)
                .withSendAccNews(true)
                .withSendWarn(true)
                .withHideMarketRating(true)
                .withNoTextAutocorrection(true)
                .withAllowEditCampaigns(true)
                .withAllowImportXls(true)
                .withAllowTransferMoney(true)
                .withSharedAccountEnabled(Boolean.TRUE);
    }

    @Before
    public void setUp() {
        requestInfoProvider = mock(RequestInfoProvider.class);
        when(requestInfoProvider.getRequestId())
                .thenReturn(REQUEST_ID);
        when(requestInfoProvider.getRemoteIpAddress())
                .thenReturn(REMOTE_ID_ADDRESS);

        userService = mock(UserService.class);
        when(userService.canAgencyCreateSubclient(any(UidAndClientId.class)))
                .thenReturn(true);

        validator = mock(AddAgencyClientValidator.class);
        when(validator.validate(any(UidAndClientId.class), any(AgencyOptions.class),
                any(AddAgencyClientRequest.class)))
                .thenAnswer(i -> new ValidationResult<AddAgencyClientRequest, Defect>(
                        (AddAgencyClientRequest) i.getArguments()[2]));

        passportService = mock(PassportService.class);
        when(
                passportService.registerUser(
                        anyString(),
                        any(RegisterUserRequest.class),
                        anyString()))
                .thenReturn(RegisterUserResult.success(UID, EMAIL, PASSWORD));

        balanceService = mock(BalanceService.class);
        when(
                balanceService.registerNewClient(
                        anyLong(),
                        any(UidAndClientId.class),
                        any(RegisterClientRequest.class)))
                .thenReturn(RegisterClientResult.success(CLIENT_ID));

        clientService = mock(ClientService.class);
        when(clientService.registerSubclient(
                anyLong(),
                anyCollection(),
                any(ClientWithOptions.class),
                anySet()))
                .thenReturn(true);

        agencyService = mock(AgencyService.class);
        when(agencyService.getAgencyOptions(any(ClientId.class)))
                .thenReturn(new AgencyOptions(false, false));

        agencyLimitedRepsService = mock(AgencyLimitedRepsService.class);

        rbacService = mock(RbacService.class);
        when(rbacService.getChiefOfGroupForLimitedAgencyReps(anyCollection()))
                .thenReturn(emptyMap());

        featureService = mock(FeatureService.class);
        when(featureService.getEnabledForClientId(any(ClientId.class)))
                .thenReturn(emptySet());

        addAgencyClientService = new AddAgencyClientService(
                userService, validator, passportService, balanceService, clientService, agencyService,
                agencyLimitedRepsService, rbacService, featureService);

        request = createRequest();
    }

    @Test
    public void testProcessRequest_PermissionDenied() {
        when(userService.canAgencyCreateSubclient(any(UidAndClientId.class)))
                .thenReturn(false);

        expected.expect(MethodNotAllowedException.class);

        addAgencyClientService.processRequest(
                requestInfoProvider, OPERATOR, AGENCY, AGENCY_MAIN_CHIEF, request);
    }


    @Test
    public void testProcessRequest_ValidationFail() {
        when(validator.validate(any(), any(AgencyOptions.class), any()))
                .thenAnswer(
                        a -> new ValidationResult<>((AddAgencyClientRequest) a.getArguments()[2])
                                .addError(CommonDefects.invalidValue()));

        addAgencyClientService.processRequest(
                requestInfoProvider, OPERATOR, AGENCY, AGENCY_MAIN_CHIEF, request);

        // Убеждаемся в том что никуда больше не ходим (возможно стоит в сервисе объединить данные вызовы в один метод)
        verify(passportService, never()).registerUser(anyString(), any(), anyString());

        verify(balanceService, never()).registerNewClient(anyLong(), any(), any());

        verify(clientService, never()).registerSubclient(anyLong(), any(), any(), any());
    }

    @Test
    public void testProcessRequest_Success() {
        Result<AddAgencyClientResponse> result = addAgencyClientService.processRequest(
                requestInfoProvider, OPERATOR, AGENCY, AGENCY_MAIN_CHIEF, request);

        verify(userService).canAgencyCreateSubclient(eq(AGENCY_MAIN_CHIEF));

        verify(validator).validate(eq(AGENCY), any(AgencyOptions.class), eq(request));

        verify(passportService).registerUser(
                eq(REQUEST_ID),
                argThat(
                        beanDiffer(
                                new RegisterUserRequest()
                                        .withLogin(LOGIN)
                                        .withFirstName(FIRST_NAME)
                                        .withLastName(LAST_NAME))),
                eq(REMOTE_ID_ADDRESS));

        verify(balanceService).registerNewClient(
                eq(OPERATOR.getUid()),
                eq(AGENCY),
                argThat(
                        beanDiffer(
                                new RegisterClientRequest()
                                        .withClientUid(UID)
                                        .withName(NAME)
                                        .withEmail(EMAIL)
                                        .withCurrency(CURRENCY)
                                        .withAgency(true))));

        verify(clientService).registerSubclient(
                eq(OPERATOR.getUid()),
                (Collection<UidAndClientId>) argThat(hasSize(1)),
                argThat(
                        beanDiffer(
                                new ClientWithOptions()
                                        .withRole(RbacRole.CLIENT)
                                        .withLogin(LOGIN)
                                        .withUid(UID)
                                        .withClientId(CLIENT_ID)
                                        .withEmail(NOTIFICATION_EMAIL)
                                        .withName(NAME)
                                        .withCurrency(CURRENCY)
                                        .withNotificationEmail(NOTIFICATION_EMAIL)
                                        .withNotificationLang(NOTIFICATION_LANG)
                                        .withSendNews(request.isSendNews())
                                        .withSendAccNews(request.isSendAccNews())
                                        .withSendWarn(request.isSendWarn())
                                        .withHideMarketRating(request.isHideMarketRating())
                                        .withNoTextAutocorrection(request.isNoTextAutocorrection())
                                        .withAllowEditCampaigns(request.getAllowEditCampaigns())
                                        .withAllowImportXls(request.getAllowImportXls())
                                        .withAllowTransferMoney(request.getAllowTransferMoney())
                                        .withSharedAccountDisabled(!request.getSharedAccountEnabled().orElse(true))
                        )),
                any());

        assertThat(
                result,
                allOf(
                        hasProperty("successful", equalTo(true)),
                        hasProperty("result", beanDiffer(
                                new AddAgencyClientResponse()
                                        .withLogin(LOGIN)
                                        .withPassword(PASSWORD)
                                        .withEmail(EMAIL)
                                        .withClientId(CLIENT_ID)))));
    }

    private void testProcessRequestPassportFailed(
            RegisterUserStatus actualErrorStatus, int expectedErrorCode) {
        when(
                passportService.registerUser(
                        anyString(),
                        any(),
                        anyString()))
                .thenReturn(RegisterUserResult.error(actualErrorStatus));

        expected.expect(PassportErrorException.class);
        expected.expect(hasProperty("code", equalTo(expectedErrorCode)));

        addAgencyClientService.processRequest(
                requestInfoProvider, OPERATOR, AGENCY, AGENCY_MAIN_CHIEF, request);
    }

    @Test
    public void testProcessRequest_Passport_Failed_With_LoginOccupied() {
        testProcessRequestPassportFailed(
                RegisterUserStatus.LOGIN_OCCUPIED,
                ErrorCodes.LOGIN_OCCUPED);
    }

    @Test
    public void testProcessRequest_Passport_Failed_With_RequestFailed() {
        testProcessRequestPassportFailed(
                RegisterUserStatus.REQUEST_FAILED,
                ErrorCodes.CANT_CREATE_LOGIN);
    }

    @Test
    public void testProcessRequest_Passport_Failed_With_InternalError() {
        testProcessRequestPassportFailed(
                RegisterUserStatus.INTERNAL_ERROR,
                ErrorCodes.CANT_CREATE_LOGIN);
    }

    private void testProcessRequestBalanceFailed(
            RegisterClientStatus actualErrorStatus, int expectedErrorCode) {
        when(
                balanceService.registerNewClient(anyLong(), any(), any()))
                .thenReturn(
                        RegisterClientResult.error(actualErrorStatus));

        expected.expect(BalanceErrorException.class);
        expected.expect(hasProperty("code", equalTo(expectedErrorCode)));

        addAgencyClientService.processRequest(
                requestInfoProvider, OPERATOR, AGENCY, AGENCY_MAIN_CHIEF, request);
    }

    @Test
    public void testProcessRequest_Balance_Failed_With_InvalidParams() {
        testProcessRequestBalanceFailed(
                RegisterClientStatus.INVALID_PARAMS,
                ErrorCodes.CANT_CREATE_CLIENT);
    }

    @Test
    public void testProcessRequest_Balance_Failed_With_CantCreateClient() {
        testProcessRequestBalanceFailed(
                RegisterClientStatus.CANT_CREATE_CLIENT,
                ErrorCodes.CANT_CREATE_CLIENT);
    }

    @Test
    public void testProcessRequest_Balance_Failed_With_CantCreateClientAssociation() {
        testProcessRequestBalanceFailed(
                RegisterClientStatus.CANT_CREATE_CLIENT_ASSOCIATION,
                ErrorCodes.CANT_CREATE_CLIENT);
    }

    @Test
    public void testProcessRequest_Balance_Failed_With_UnknownError() {
        testProcessRequestBalanceFailed(
                RegisterClientStatus.UNKNOWN_ERROR,
                ErrorCodes.CANT_CREATE_CLIENT);
    }

    @Test
    public void testProcessRequest_With_RegisterSubclient_Failed() {
        when(clientService.registerSubclient(
                anyLong(),
                anyCollection(),
                any(ClientWithOptions.class),
                anySet()))
                .thenReturn(false);

        expected.expect(CantRegisterClientException.class);
        expected.expect(hasProperty("code", equalTo(ErrorCodes.CANT_CREATE_CLIENT)));

        addAgencyClientService.processRequest(
                requestInfoProvider, OPERATOR, AGENCY, AGENCY_MAIN_CHIEF, request);
    }
}
