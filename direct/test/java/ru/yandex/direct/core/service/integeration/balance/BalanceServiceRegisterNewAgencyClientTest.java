package ru.yandex.direct.core.service.integeration.balance;

import java.util.Arrays;
import java.util.Collections;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.direct.balance.client.BalanceClient;
import ru.yandex.direct.balance.client.exception.BalanceClientException;
import ru.yandex.direct.balance.client.model.request.CreateClientRequest;
import ru.yandex.direct.balance.client.model.response.ClientPassportInfo;
import ru.yandex.direct.core.service.integration.balance.BalanceService;
import ru.yandex.direct.core.service.integration.balance.RegisterClientRequest;
import ru.yandex.direct.core.service.integration.balance.RegisterClientResult;
import ru.yandex.direct.core.service.integration.balance.RegisterClientStatus;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.model.UidAndClientId;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static ru.yandex.direct.core.service.integration.balance.BalanceService.BEGIN_OF_TIME_FOR_MULTICURRENCY_CLIENT;

/**
 * Тест на регистрацию нового клиента агенства в балансе
 */
public class BalanceServiceRegisterNewAgencyClientTest {
    private static final Long OPERATOR_UID = 1L;
    private static final UidAndClientId AGENCY = UidAndClientId.of(2L, ClientId.fromLong(3L));
    private static final Long CLIENT_UID = 4L;
    private static final String CLIENT_NAME = "client";
    private static final String CLIENT_EMAIL = "client@yandex.ru";
    private static final CurrencyCode CLIENT_CURRENCY = CurrencyCode.RUB;
    private static final Long CLIENT_ID = 5L;
    private static final Integer DIRECT_SERVICE_ID = 6;
    private static final String DIRECT_SERVICE_FAKE_TOKEN = "DIRECT_SERVICE_FAKE_TOKEN";

    private RegisterClientRequest request = new RegisterClientRequest()
            .withClientUid(CLIENT_UID)
            .withName(CLIENT_NAME)
            .withEmail(CLIENT_EMAIL)
            .withCurrency(CLIENT_CURRENCY);
    private BalanceClient balanceClient;
    private BalanceService balanceService;

    @Before
    public void setUp() {
        balanceClient = mock(BalanceClient.class);
        when(balanceClient.createClient(
                anyLong(), any(CreateClientRequest.class))).thenReturn(CLIENT_ID);
        balanceService = new BalanceService(balanceClient, DIRECT_SERVICE_ID, DIRECT_SERVICE_FAKE_TOKEN);
        when(balanceClient.getClientRepresentativePassports(
                anyLong(), anyLong()))
                .thenReturn(Collections.singletonList(new ClientPassportInfo()));
        balanceService = new BalanceService(balanceClient, DIRECT_SERVICE_ID, DIRECT_SERVICE_FAKE_TOKEN);
    }

    private void checkTestSuccessResult(
            RegisterClientResult result, Matcher<CreateClientRequest> currencyMatcher) {
        assertThat(
                result,
                allOf(
                        hasProperty("status", equalTo(RegisterClientStatus.OK)),
                        hasProperty("clientId", equalTo(ClientId.fromLong(CLIENT_ID)))));

        Mockito.verify(balanceClient).createClient(
                eq(OPERATOR_UID),
                argThat(
                        allOf(
                                hasProperty("clientTypeId", equalTo(0)),
                                hasProperty("name", equalTo(CLIENT_NAME)),
                                hasProperty("email", equalTo(CLIENT_EMAIL)),
                                hasProperty("agencyId", equalTo((int) AGENCY.getClientId().asLong())),
                                hasProperty("isAgency", equalTo(false)),
                                hasProperty("serviceId", equalTo(DIRECT_SERVICE_ID)),
                                currencyMatcher)));
        Mockito.verify(balanceClient).createUserClientAssociation(
                eq(OPERATOR_UID), eq(CLIENT_ID), eq(CLIENT_UID));
        Mockito.verify(balanceClient).getClientRepresentativePassports(
                eq(OPERATOR_UID), eq(CLIENT_ID));
        Mockito.verify(balanceClient).editPassport(
                eq(OPERATOR_UID),
                eq(CLIENT_UID),
                argThat(hasProperty("isMain", equalTo(1))));
    }

    @Test
    public void testSuccess_With_Any_Currency_Except_YndxFixed() {
        RegisterClientResult result = balanceService.registerNewClient(
                OPERATOR_UID,
                AGENCY,
                request);

        checkTestSuccessResult(
                result,
                Matchers.<CreateClientRequest>both(hasProperty("currency", equalTo(CLIENT_CURRENCY.name())))
                        .and(hasProperty("migrateToCurrency", equalTo(BEGIN_OF_TIME_FOR_MULTICURRENCY_CLIENT))));
    }

    @Test
    public void testSuccess_With_YndxFixed_Currency() {
        RegisterClientResult result = balanceService.registerNewClient(
                OPERATOR_UID,
                AGENCY,
                request.withCurrency(CurrencyCode.YND_FIXED));

        checkTestSuccessResult(
                result,
                Matchers.<CreateClientRequest>both(hasProperty("currency", nullValue()))
                        .and(hasProperty("migrateToCurrency", nullValue())));
    }

    @Test
    public void testFail_If_BalanceCreateClient_Fail() {
        when(balanceClient.createClient(anyLong(), any(CreateClientRequest.class)))
                .thenThrow(new BalanceClientException("CreateClient"));

        assertThat(
                balanceService.registerNewClient(OPERATOR_UID, AGENCY, request),
                hasProperty("status", equalTo(RegisterClientStatus.CANT_CREATE_CLIENT)));
    }

    @Test
    public void testFail_If_BalanceCreateClient_Fail_With_UnknownError() {
        when(balanceClient.createClient(anyLong(), any(CreateClientRequest.class)))
                .thenThrow(new RuntimeException());

        assertThat(
                balanceService.registerNewClient(OPERATOR_UID, AGENCY, request),
                hasProperty("status", equalTo(RegisterClientStatus.UNKNOWN_ERROR)));
    }

    @Test
    public void testFail_If_BalanceCreateUserClientAssociation_Fail() {
        doThrow(new BalanceClientException("CreateUserClientAssociation"))
                .when(balanceClient)
                .createUserClientAssociation(anyLong(), anyLong(), anyLong());

        assertThat(
                balanceService.registerNewClient(OPERATOR_UID, AGENCY, request),
                hasProperty("status", equalTo(RegisterClientStatus.CANT_CREATE_CLIENT_ASSOCIATION)));
    }

    @Test
    public void testFail_If_BalanceCreateUserClientAssociation_Fail_With_UnknownError() {
        doThrow(new RuntimeException())
                .when(balanceClient)
                .createUserClientAssociation(anyLong(), anyLong(), anyLong());

        assertThat(
                balanceService.registerNewClient(OPERATOR_UID, AGENCY, request),
                hasProperty("status", equalTo(RegisterClientStatus.UNKNOWN_ERROR)));
    }

    @Test
    public void testFail_If_BalanceGetClientRepresentativePassports_Fail() {
        doThrow(new BalanceClientException("GetClientRepresentativePassports"))
                .when(balanceClient)
                .getClientRepresentativePassports(anyLong(), anyLong());

        assertThat(
                balanceService.registerNewClient(OPERATOR_UID, AGENCY, request),
                hasProperty("status", equalTo(RegisterClientStatus.CANT_CREATE_CLIENT_ASSOCIATION)));
    }

    @Test
    public void test_BalanceEditPassport_Not_Called_If_More_Than_One_Representative() {
        when(balanceClient.getClientRepresentativePassports(
                anyLong(), anyLong()))
                .thenReturn(Arrays.asList(new ClientPassportInfo(), new ClientPassportInfo()));

        balanceService.registerNewClient(OPERATOR_UID, AGENCY, request);

        verify(balanceClient, never())
                .editPassport(anyLong(), anyLong(), any(ClientPassportInfo.class));
    }

    @Test
    public void testFail_If_BalanceEditPassport_Fail() {
        doThrow(new BalanceClientException("EditPassport"))
                .when(balanceClient)
                .editPassport(anyLong(), anyLong(), any(ClientPassportInfo.class));

        assertThat(
                balanceService.registerNewClient(OPERATOR_UID, AGENCY, request),
                hasProperty("status", equalTo(RegisterClientStatus.CANT_CREATE_CLIENT_ASSOCIATION)));
    }

    @Test
    public void testFail_If_BalanceEditPassport_Fail_With_UnknownError() {
        doThrow(new RuntimeException())
                .when(balanceClient)
                .editPassport(anyLong(), anyLong(), any(ClientPassportInfo.class));

        assertThat(
                balanceService.registerNewClient(OPERATOR_UID, AGENCY, request),
                hasProperty("status", equalTo(RegisterClientStatus.UNKNOWN_ERROR)));
    }
}
