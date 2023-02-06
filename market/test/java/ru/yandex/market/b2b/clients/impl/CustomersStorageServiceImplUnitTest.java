package ru.yandex.market.b2b.clients.impl;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import net.spy.memcached.MemcachedClientIF;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.b2b.balance.model.PersonStructure;
import ru.yandex.market.b2b.balance.service.BalanceService;
import ru.yandex.market.b2b.clients.EdoService;
import ru.yandex.market.b2b.clients.SanctionsListService;
import ru.yandex.market.b2b.clients.impl.customers.CustomersStorageServiceImpl;
import ru.yandex.market.b2b.customers.internal.UserData;
import ru.yandex.market.common.retrofit.ExecuteCall;
import ru.yandex.mj.generated.client.taxi_authproxy.api.TaxiAuthproxyApiClient;
import ru.yandex.mj.generated.client.taxi_authproxy.model.CorpClientIdRequest;
import ru.yandex.mj.generated.client.taxi_authproxy.model.CorpClientIdResponse;
import ru.yandex.mj.generated.client.taxi_can_order.api.TaxiCanOrderApiClient;
import ru.yandex.mj.generated.client.taxi_can_order.model.V1ClientCanOrderStatus;
import ru.yandex.mj.generated.client.taxi_can_order.model.V1ClientsCanOrderResponse;
import ru.yandex.mj.generated.client.taxi_corp_contracts.api.TaxiCorpContractsApiClient;
import ru.yandex.mj.generated.client.taxi_corp_contracts.model.Contract;
import ru.yandex.mj.generated.client.taxi_corp_contracts.model.ContractsResponse;
import ru.yandex.mj.generated.client.taxi_corp_request_market_offer.api.TaxiCorpRequestMarketOfferApiClient;
import ru.yandex.mj.generated.client.taxi_corp_request_market_offer.model.MarketClientStatusResponse;
import ru.yandex.mj.generated.client.taxi_corp_request_market_offer.model.MarketClientStatusResponse.StatusEnum;
import ru.yandex.mj.generated.server.model.CheckResponseDto;
import ru.yandex.mj.generated.server.model.EdoStatus;
import ru.yandex.mj.generated.server.model.EdoStatusDto;
import ru.yandex.mj.generated.server.model.RegistrationStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CustomersStorageServiceImplUnitTest {

    @Mock
    private MemcachedClientIF memcachedClient;

    @Mock
    private TaxiCanOrderApiClient taxiCanOrderApiClient;

    @Mock
    private TaxiAuthproxyApiClient authProxyClient;

    @Mock
    private TaxiCorpContractsApiClient contractsClient;

    @Mock
    private TaxiCorpRequestMarketOfferApiClient marketOfferApiClient;

    @Mock
    private BalanceService balanceService;

    @Mock
    private EdoService edoService;

    @Mock
    private SanctionsListService sanctionsListService;

    private CustomersStorageServiceImpl storage;

    @BeforeEach
    public void setUp(){
        storage = new CustomersStorageServiceImpl(
                balanceService,
                memcachedClient,
                authProxyClient,
                taxiCanOrderApiClient,
                contractsClient,
                marketOfferApiClient,
                edoService,
                sanctionsListService,
                true
        );
    }

    @Test
    public void checkCustomers_ifCachedThenReturnsCached() {
        String uid = "user_id";
        UserData user = UserData.newBuilder()
                .setCorpId("corp_id")
                .setAvailable(true)
                .setCanOrder(true)
                .setContractId(1L)
                .build();
        Mockito.doReturn(user).when(memcachedClient)
                .get(eq("u_" + uid), Mockito.any());

        CheckResponseDto response = storage.checkCustomers(uid);

        assertTrue(response.getRegistered());
        assertTrue(response.getHasContract());
        verify(memcachedClient).get(eq("u_" + uid), Mockito.any());
    }

    @Test
    public void checkCustomers_ifNotCachedThenSearchesForCorpId() {
        String uid = "user_id";
        ExecuteCall call = Mockito.mock(ExecuteCall.class);
        CorpClientIdResponse response = new CorpClientIdResponse();
        Mockito.doReturn(CompletableFuture.completedFuture(response)).when(call)
                .schedule();
        Mockito.doReturn(call).when(authProxyClient)
                .v1AuthproxyCorpClientIdPost(any());

        storage.checkCustomers(uid);

        ArgumentCaptor<CorpClientIdRequest> captor = ArgumentCaptor.forClass(CorpClientIdRequest.class);
        verify(authProxyClient).v1AuthproxyCorpClientIdPost(captor.capture());
        assertEquals(uid, captor.getValue().getUid());
    }

    @Test
    public void checkCustomers_ifNotCachedAndNoCorpIdThenAddNotFoundToCache() {
        // Given
        String uid = "user_id";

        String corpId = null;
        setCorpClientIdTo(corpId);

        // When
        storage.checkCustomers(uid);

        // Then
        ArgumentCaptor<UserData> captor = ArgumentCaptor.forClass(UserData.class);
        verify(memcachedClient).set(eq("u_" + uid), eq(5), captor.capture(), Mockito.any());
        assertFalse(captor.getValue().getAvailable());
    }

    private void setCorpClientIdTo(String corpId) {
        ExecuteCall call = Mockito.mock(ExecuteCall.class);
        CorpClientIdResponse response = new CorpClientIdResponse();
        response.setCorpClientId(corpId);
        Mockito.doReturn(CompletableFuture.completedFuture(response)).when(call)
                .schedule();
        Mockito.doReturn(call).when(authProxyClient)
                .v1AuthproxyCorpClientIdPost(any());
    }

    @Test
    public void checkCustomers_ifNotCachedAndHasCorpIdAndContractIdThenAddFoundToCache() {
        // Given
        String uid = "user_id";

        String corpId = "corp_id";
        setCorpClientIdTo(corpId);
        setCanOrderToEmptyResponse();
        setContractsTo(contractResponse(contact(1, true, "2", List.of("market"))));
        setRegistrationStatusToSuccessResponse();

        // When
        storage.checkCustomers(uid);

        // Then
        ArgumentCaptor<UserData> captor = ArgumentCaptor.forClass(UserData.class);
        verify(memcachedClient).set(eq("u_" + uid), eq(30 * 60), captor.capture(), Mockito.any());
        UserData cached = captor.getValue();
        assertTrue(cached.getAvailable());
        assertEquals(corpId, cached.getCorpId());
    }

    private void setContractsTo(ContractsResponse response) {
        ExecuteCall call = Mockito.mock(ExecuteCall.class);
        Mockito.doReturn(CompletableFuture.completedFuture(response)).when(call)
                .schedule();
        Mockito.doReturn(call).when(contractsClient)
                .contractsGet(anyString(), any(), anyBoolean());
    }

    private ContractsResponse contractResponse(Contract... contracts) {
        return new ContractsResponse().contracts(List.of(contracts));
    }

    private Contract contact(Integer contactId, Boolean isActive, String billingClientId, List<String> services) {
        return new Contract()
                .contractId(contactId)
                .services(services)
                .isActive(isActive)
                .billingClientId(billingClientId);
    }

    private void setCanOrderToEmptyResponse() {
        ExecuteCall call = Mockito.mock(ExecuteCall.class);
        Mockito.doReturn(CompletableFuture.completedFuture(null)).when(call)
                .schedule();
        Mockito.doReturn(call).when(taxiCanOrderApiClient)
                .v1ClientsCanOrderMarketPost(any());
    }

    private void setContractsToEmptyResponse() {
        ExecuteCall call = Mockito.mock(ExecuteCall.class);
        Mockito.doReturn(CompletableFuture.completedFuture(null)).when(call)
                .schedule();
        Mockito.doReturn(call).when(contractsClient)
                .contractsGet(anyString(), any(), anyBoolean());
    }

    private void setRegistrationStatusToSuccessResponse() {
        MarketClientStatusResponse response = new MarketClientStatusResponse().status(StatusEnum.SUCCESS);
        ExecuteCall call = Mockito.mock(ExecuteCall.class);
        Mockito.doReturn(CompletableFuture.completedFuture(response)).when(call)
                .schedule();
        Mockito.doReturn(call).when(marketOfferApiClient)
                .v1MarketClientStatusPost(anyString());
    }

    @Test
    public void checkCustomers_ifNotCachedAndHasCorpIdThenLoadsStatus() {
        // Given
        String uid = "user_id";

        String corpId = "corp_id";
        setCorpClientIdTo(corpId);
        setCanOrderToEmptyResponse();
        setContractsToEmptyResponse();
        setRegistrationStatusTo(corpId, status(StatusEnum.PENDING, "Message"));

        // When
        storage.checkCustomers(uid);

        // Then
        verify(marketOfferApiClient).v1MarketClientStatusPost(corpId);
    }

    private void setRegistrationStatusTo(String clientId, MarketClientStatusResponse response) {
        ExecuteCall call = Mockito.mock(ExecuteCall.class);
        Mockito.doReturn(CompletableFuture.completedFuture(response)).when(call)
                .schedule();
        Mockito.doReturn(call).when(marketOfferApiClient)
                .v1MarketClientStatusPost(clientId);
    }

    private static MarketClientStatusResponse status(StatusEnum status, String message) {
        return new MarketClientStatusResponse()
                .status(status)
                .message(message);
    }

    @ParameterizedTest(name = "hasCustomers_forStatus {0} caches for {1} seconds")
    @MethodSource("cacheTimeCorrelation")
    public void checkCustomers_ifNotCachedAndHasCorpIdThenCashesFor(MarketClientStatusResponse response, Integer time) {
        // Given
        String uid = "user_id";

        String corpId = "corp_id";
        setCorpClientIdTo(corpId);
        setCanOrderToEmptyResponse();
        setContractsTo(contractResponse(contact(1, true, "2", List.of("market"))));
        setRegistrationStatusTo(corpId, response);

        // When
        storage.checkCustomers(uid);

        // Then
        verify(memcachedClient).set(eq("u_" + uid), eq(time), Mockito.any(), Mockito.any());
    }

    private static Stream<Arguments> cacheTimeCorrelation() {
        return Stream.of(
                Arguments.of(status(StatusEnum.SUCCESS), 30 * 60),
                Arguments.of(status(StatusEnum.PENDING), 3),
                Arguments.of(status(StatusEnum.ERROR), 30 * 60),
                Arguments.of(status(StatusEnum.EMPTY), 30 * 60),
                Arguments.of(null, 3)
        );
    }

    private static MarketClientStatusResponse status(StatusEnum status) {
        return status(status, "Message");
    }

    @ParameterizedTest(name = "hasCustomers_correctlyMaps {0} to {1} for status")
    @MethodSource("statusCorrelation")
    public void checkCustomers_correctlyMapsStatus(StatusEnum source, RegistrationStatus result) {
        // Given
        String uid = "user_id";

        String corpId = "corp_id";
        setCorpClientIdTo(corpId);
        setCanOrderToEmptyResponse();
        setContractsToEmptyResponse();
        setRegistrationStatusTo(corpId, status(source, "Message"));

        // When
        CheckResponseDto responseDto = storage.checkCustomers(uid);

        // Then
        assertEquals(result, responseDto.getRegistrationStatus().getStatus());
        assertEquals("Message", responseDto.getRegistrationStatus().getMessage());
    }

    private static Stream<Arguments> statusCorrelation() {
        return Stream.of(
                Arguments.of(StatusEnum.SUCCESS, RegistrationStatus.SUCCESS),
                Arguments.of(StatusEnum.PENDING, RegistrationStatus.PENDING),
                Arguments.of(StatusEnum.ERROR, RegistrationStatus.ERROR),
                Arguments.of(StatusEnum.EMPTY, RegistrationStatus.EMPTY)
        );
    }

    @Test
    public void checkCustomers_ifNullResponseThenNullRegistrationStatus() {
        // Given
        String uid = "user_id";

        String corpId = "corp_id";
        setCorpClientIdTo(corpId);
        setCanOrderToEmptyResponse();
        setContractsToEmptyResponse();
        setRegistrationStatusTo(corpId, status(null, "Message"));

        // When
        CheckResponseDto responseDto = storage.checkCustomers(uid);

        // Then
        assertNull(responseDto.getRegistrationStatus());
    }

    @ParameterizedTest(name = "hasCustomers_correctlyMaps \"{0}\" to \"{1}\" for status message")
    @MethodSource("statusMessageCorrelation")
    public void checkCustomers_correctlyMapsMessageStatus(String source, String result) {
        // Given
        String uid = "user_id";

        String corpId = "corp_id";
        setCorpClientIdTo(corpId);
        setCanOrderToEmptyResponse();
        setContractsToEmptyResponse();
        setRegistrationStatusTo(corpId, status(StatusEnum.ERROR, source));

        // When
        CheckResponseDto responseDto = storage.checkCustomers(uid);

        // Then
        assertEquals(result, responseDto.getRegistrationStatus().getMessage());
    }

    private static Stream<Arguments> statusMessageCorrelation() {
        return Stream.of(
                Arguments.of("Message", "Message"),
                Arguments.of("", null),
                Arguments.of(null, null)
        );
    }

    @Test
    public void checkCustomers_ifContactIsNotActiveThenHasContractFalse() {
        // Given
        String uid = "user_id";

        String corpId = "corp_id";
        Boolean isActiveContract = false;
        List<String> services = List.of("market");

        setCorpClientIdTo(corpId);
        setCanOrderToEmptyResponse();
        setContractsTo(contractResponse(contact(1, isActiveContract, "2", services)));
        setRegistrationStatusTo(corpId, status(StatusEnum.SUCCESS, "Message"));

        // When
        CheckResponseDto result = storage.checkCustomers(uid);

        // Then
        Assertions.assertFalse(result.getHasContract());
    }

    @Test
    public void checkCustomers_ifContactIsNotMarketThenHasContractFalse() {
        // Given
        String uid = "user_id";

        String corpId = "corp_id";
        Boolean isActiveContract = true;
        List<String> services = List.of("not_a_market");

        setCorpClientIdTo(corpId);
        setCanOrderToEmptyResponse();
        setContractsTo(contractResponse(contact(1, isActiveContract, "2", services)));
        setRegistrationStatusTo(corpId, status(StatusEnum.SUCCESS, "Message"));

        // When
        CheckResponseDto result = storage.checkCustomers(uid);

        // Then
        Assertions.assertFalse(result.getHasContract());
    }

    @Test
    public void checkCustomers_ifContactIsMarketAndActiveThenHasContractTrue() {
        // Given
        String uid = "user_id";

        String corpId = "corp_id";
        Boolean isActiveContract = true;
        List<String> services = List.of("market");

        setCorpClientIdTo(corpId);
        setCanOrderToEmptyResponse();
        setContractsTo(contractResponse(contact(1, isActiveContract, "2", services)));
        setRegistrationStatusTo(corpId, status(StatusEnum.SUCCESS, "Message"));

        // When
        CheckResponseDto result = storage.checkCustomers(uid);

        // Then
        Assertions.assertTrue(result.getHasContract());
    }

    @Test
    public void checkCustomers_ifContactIsNotMarketThenHasCustomersFalse() {
        // Given
        String uid = "user_id";

        String corpId = "corp_id";
        Boolean isActiveContract = true;
        List<String> services = List.of("not_a_market");

        setCorpClientIdTo(corpId);
        setCanOrderToEmptyResponse();
        setContractsTo(contractResponse(contact(1, isActiveContract, null, services)));
        setRegistrationStatusTo(corpId, status(StatusEnum.SUCCESS, "Message"));

        // When
        CheckResponseDto result = storage.checkCustomers(uid);

        // Then
        Assertions.assertFalse(result.getHasCustomers());
    }

    @Test
    public void checkCustomers_ifContactIsMarketAndActiveThenHasCustomersTrue() {
        // Given
        String uid = "user_id";

        String corpId = "corp_id";
        Boolean isActiveContract = true;
        List<String> services = List.of("market");

        setCorpClientIdTo(corpId);
        setCanOrderTo(canOrderResponse(true));
        setContractsTo(contractResponse(contact(1, isActiveContract, "2", services)));
        setRegistrationStatusTo(corpId, status(StatusEnum.SUCCESS, "Message"));

        // When
        CheckResponseDto result = storage.checkCustomers(uid);

        // Then
        Assertions.assertTrue(result.getHasCustomers());
    }

    private void setCanOrderTo(V1ClientsCanOrderResponse response) {
        ExecuteCall call = Mockito.mock(ExecuteCall.class);
        Mockito.doReturn(CompletableFuture.completedFuture(response)).when(call)
                .schedule();
        Mockito.doReturn(call).when(taxiCanOrderApiClient)
                .v1ClientsCanOrderMarketPost(any());
    }

    private V1ClientsCanOrderResponse canOrderResponse(boolean canOrder) {
        return new V1ClientsCanOrderResponse()
                .addStatusesItem(new V1ClientCanOrderStatus().canOrder(canOrder));
    }



    @Test
    public void checkCustomer_returnsCorrectUid() {
        // Given
        String uid = "user_id";
        String id = "2";

        String corpId = "corp_id";
        String billingClientId = "2";
        Boolean isActiveContract = true;
        List<String> services = List.of("market");

        setCorpClientIdTo(corpId);
        setCanOrderTo(canOrderResponse(true));
        setContractsTo(contractResponse(contact(1, isActiveContract, billingClientId, services)));
        setRegistrationStatusTo(corpId, status(StatusEnum.SUCCESS, "Message"));

        // When
        CheckResponseDto result = storage.checkCustomer(uid, id);

        // Then
        Assertions.assertEquals(uid, result.getUid());
    }

    @Test
    public void checkCustomers_ifContactIsMarketAndNotActiveThenHasCustomersFalse() {
        // Given
        String uid = "user_id";

        String corpId = "corp_id";
        Boolean isActiveContract = false;
        List<String> services = List.of("market");

        setCorpClientIdTo(corpId);
        setCanOrderTo(canOrderResponse(true));
        setContractsTo(contractResponse(contact(1, isActiveContract, "2", services)));
        setRegistrationStatusTo(corpId, status(StatusEnum.SUCCESS, "Message"));

        // When
        CheckResponseDto result = storage.checkCustomers(uid);

        // Then
        Assertions.assertFalse(result.getHasCustomers());
    }

    @Test
    public void checkCustomer_ifCorpIdFoundThenRegisteredTrue() {
        // Given
        String uid = "user_id";
        String id = "2";

        String corpId = "corp_id";
        String billingClientId = "2";
        Boolean isActiveContract = true;
        List<String> services = List.of("market");

        setCorpClientIdTo(corpId);
        setCanOrderTo(canOrderResponse(true));
        setContractsTo(contractResponse(contact(1, isActiveContract, billingClientId, services)));
        setRegistrationStatusTo(corpId, status(StatusEnum.SUCCESS, "Message"));

        // When
        CheckResponseDto result = storage.checkCustomer(uid, id);

        // Then
        Assertions.assertTrue(result.getRegistered());
    }

    @Test
    public void checkCustomer_ifCorpIdNotFoundThenRegisteredFalse() {
        // Given
        String uid = "user_id";
        String id = "balance_id";

        String corpId = null;
        setCorpClientIdTo(corpId);

        // When
        CheckResponseDto result = storage.checkCustomer(uid, id);

        // Then
        Assertions.assertFalse(result.getRegistered());
    }

    @Test
    public void checkCustomer_ifContractFoundThenHasContractTrue() {
        // Given
        String uid = "user_id";
        String id = "2";

        String corpId = "corp_id";
        String billingClientId = id;
        Boolean isActiveContract = true;
        List<String> services = List.of("market");

        setCorpClientIdTo(corpId);
        setCanOrderTo(canOrderResponse(true));
        setContractsTo(contractResponse(contact(1, isActiveContract, billingClientId, services)));
        setRegistrationStatusTo(corpId, status(StatusEnum.SUCCESS, "Message"));

        // When
        CheckResponseDto result = storage.checkCustomer(uid, id);

        // Then
        Assertions.assertTrue(result.getHasContract());
    }

    @Test
    public void checkCustomer_ifContractNotFoundByBillingClientIdThenHasContractFalse() {
        // Given
        String uid = "user_id";
        String id = "2";

        String corpId = "corp_id";
        String billingClientId = "3";
        Boolean isActiveContract = true;
        List<String> services = List.of("market");

        setCorpClientIdTo(corpId);
        setCanOrderTo(canOrderResponse(true));
        setContractsTo(contractResponse(contact(1, isActiveContract, billingClientId, services)));
        setRegistrationStatusTo(corpId, status(StatusEnum.SUCCESS, "Message"));

        // When
        CheckResponseDto result = storage.checkCustomer(uid, id);

        // Then
        Assertions.assertFalse(result.getHasContract());
    }

    @Test
    public void checkCustomer_ifContractNotFoundByActivityThenHasContractFalse() {
        // Given
        String uid = "user_id";
        String id = "2";

        String corpId = "corp_id";
        String billingClientId = "2";
        Boolean isActiveContract = false;
        List<String> services = List.of("market");

        setCorpClientIdTo(corpId);
        setCanOrderTo(canOrderResponse(true));
        setContractsTo(contractResponse(contact(1, isActiveContract, billingClientId, services)));
        setRegistrationStatusTo(corpId, status(StatusEnum.SUCCESS, "Message"));

        // When
        CheckResponseDto result = storage.checkCustomer(uid, id);

        // Then
        Assertions.assertFalse(result.getHasContract());
    }

    @Test
    public void checkCustomer_ifContractNotFoundByServicesMatchThenHasContractFalse() {
        // Given
        String uid = "user_id";
        String id = "2";

        String corpId = "corp_id";
        String billingClientId = "2";
        Boolean isActiveContract = true;
        List<String> services = List.of("not_a_market");

        setCorpClientIdTo(corpId);
        setCanOrderTo(canOrderResponse(true));
        setContractsTo(contractResponse(contact(1, isActiveContract, billingClientId, services)));
        setRegistrationStatusTo(corpId, status(StatusEnum.SUCCESS, "Message"));

        // When
        CheckResponseDto result = storage.checkCustomer(uid, id);

        // Then
        Assertions.assertFalse(result.getHasContract());
    }

    @Test
    public void checkCustomer_ifContractFoundThenHasCustomersTrue() {
        // Given
        String uid = "user_id";
        String id = "123";

        String corpId = "corp_id";
        String billingClientId = id;
        Boolean isActiveContract = true;
        List<String> services = List.of("market");

        setCorpClientIdTo(corpId);
        setCanOrderTo(canOrderResponse(true));
        setContractsTo(contractResponse(contact(1, isActiveContract, billingClientId, services)));
        setRegistrationStatusTo(corpId, status(StatusEnum.SUCCESS, "Message"));
        setUpPersonsTo(persons());
        setUpEdoTo(EdoStatus.ENABLED);

        // When
        CheckResponseDto result = storage.checkCustomer(uid, id);

        // Then
        Assertions.assertTrue(result.getHasCustomers());
    }

    private void setUpPersonsTo(PersonStructure person) {
        Mockito.doReturn(List.of(person)).when(balanceService)
                .getClientPersons(anyLong(), anyInt());
    }

    private PersonStructure persons() {
        return new PersonStructure().setDT("dt");
    }

    private void setUpEdoTo(EdoStatus edoStatus) {
        Mockito.doReturn(new EdoStatusDto().status(edoStatus))
                .when(edoService).getStatus(any(), any());
    }

    @Test
    public void checkCustomer_ifContractNotFoundByBillingClientIdThenHasCustomersFalse() {
        // Given
        String uid = "user_id";
        String id = "2";

        String corpId = "corp_id";
        String billingClientId = "3";
        Boolean isActiveContract = true;
        List<String> services = List.of("market");

        setCorpClientIdTo(corpId);
        setCanOrderTo(canOrderResponse(true));
        setContractsTo(contractResponse(contact(1, isActiveContract, billingClientId, services)));
        setRegistrationStatusTo(corpId, status(StatusEnum.SUCCESS, "Message"));

        // When
        CheckResponseDto result = storage.checkCustomer(uid, id);

        // Then
        Assertions.assertFalse(result.getHasCustomers());
    }

    @Test
    public void checkCustomer_ifContractNotFoundByActivityThenHasCustomersFalse() {
        // Given
        String uid = "user_id";
        String id = "2";

        String corpId = "corp_id";
        String billingClientId = "2";
        Boolean isActiveContract = false;
        List<String> services = List.of("market");

        setCorpClientIdTo(corpId);
        setCanOrderTo(canOrderResponse(true));
        setContractsTo(contractResponse(contact(1, isActiveContract, billingClientId, services)));
        setRegistrationStatusTo(corpId, status(StatusEnum.SUCCESS, "Message"));

        // When
        CheckResponseDto result = storage.checkCustomer(uid, id);

        // Then
        Assertions.assertFalse(result.getHasCustomers());
    }

    @Test
    public void checkCustomer_ifContractNotFoundByServicesMatchThenHasCustomersFalse() {
        // Given
        String uid = "user_id";
        String id = "2";

        String corpId = "corp_id";
        String billingClientId = "2";
        Boolean isActiveContract = true;
        List<String> services = List.of("not_a_market");

        setCorpClientIdTo(corpId);
        setCanOrderTo(canOrderResponse(true));
        setContractsTo(contractResponse(contact(1, isActiveContract, billingClientId, services)));
        setRegistrationStatusTo(corpId, status(StatusEnum.SUCCESS, "Message"));

        // When
        CheckResponseDto result = storage.checkCustomer(uid, id);

        // Then
        Assertions.assertFalse(result.getHasCustomers());
    }

    @ParameterizedTest(name = "checkCustomers_correctlyMaps {0} to {1} for status")
    @MethodSource("statusCorrelation")
    public void checkCustomer_ifRegistrationStatusIsPresentThenMapsCorrectly(StatusEnum source,
                                                                             RegistrationStatus expected) {
        // Given
        String uid = "user_id";
        String id = "2";

        String corpId = "corp_id";
        String billingClientId = "2";
        Boolean isActiveContract = true;
        List<String> services = List.of("market");

        setCorpClientIdTo(corpId);
        setCanOrderTo(canOrderResponse(true));
        setContractsTo(contractResponse(contact(1, isActiveContract, billingClientId, services)));
        setRegistrationStatusTo(corpId, status(source, "Message"));

        // When
        CheckResponseDto result = storage.checkCustomer(uid, id);

        // Then
        assertEquals(expected, result.getRegistrationStatus().getStatus());
    }

    @Test
    public void checkCustomer_ifEdoEnabledThenHasEdoTrue() {
        // Given
        String uid = "user_id";
        String id = "123";

        String corpId = "corp_id";
        String billingClientId = "123";
        Boolean isActiveContract = true;
        List<String> services = List.of("market");

        setCorpClientIdTo(corpId);
        setCanOrderTo(canOrderResponse(true));
        setContractsTo(contractResponse(contact(1, isActiveContract, billingClientId, services)));
        setRegistrationStatusTo(corpId, status(StatusEnum.SUCCESS, "Message"));
        setUpPersonsTo(persons());
        setUpEdoTo(EdoStatus.ENABLED);

        // When
        CheckResponseDto result = storage.checkCustomer(uid, id);

        // Then
        assertTrue(result.getHasEDO());
    }

    @Test
    public void checkCustomer_ifRegisteredIsFalseThenCanOrderIsFalse() {
        // Given
        String uid = "user_id";
        String id = "123";

        setCorpClientIdTo(null); // registered is false because a corpId (GO) is not found

        // When
        CheckResponseDto result = storage.checkCustomer(uid, id);

        // Then
        assertFalse(result.getCanOrder());
    }

    @Test
    public void checkCustomer_ifAllConditionsMetThenCanOrderIsTrue() {
        // Given
        String uid = "user_id";
        String id = "123";

        String corpId = "corp_id";
        String billingClientId = "123";
        Boolean isActiveContract = true;
        List<String> services = List.of("market");

        setCorpClientIdTo(corpId);
        setCanOrderTo(canOrderResponse(true));
        setContractsTo(contractResponse(contact(1, isActiveContract, billingClientId, services)));
        setRegistrationStatusTo(corpId, status(StatusEnum.SUCCESS, "Message"));
        setUpPersonsTo(persons().setInn("inn"));
        setUpEdoTo(EdoStatus.ENABLED);

        // When
        CheckResponseDto result = storage.checkCustomer(uid, id);

        // Then
        assertTrue(result.getCanOrder());
    }

    @Test
    public void checkCustomer_ifHasCustomerIsFalseThenCanOrderIsFalse() {
        // Given
        String uid = "user_id";
        String id = "123";

        String corpId = "corp_id";
        String billingClientId = "not_123";  // hasCustomer is false because a Contract is not found
        Boolean isActiveContract = true;
        List<String> services = List.of("market");

        setCorpClientIdTo(corpId);
        setCanOrderTo(canOrderResponse(true));
        setContractsTo(contractResponse(contact(1, isActiveContract, billingClientId, services)));
        setRegistrationStatusTo(corpId, status(StatusEnum.SUCCESS, "Message"));

        // When
        CheckResponseDto result = storage.checkCustomer(uid, id);

        // Then
        assertFalse(result.getCanOrder());
    }

    @Test
    public void checkCustomer_ifHasContractIsFalseThenCanOrderIsFalse() {
        // Given
        String uid = "user_id";
        String id = "123";

        String corpId = "corp_id";
        String billingClientId = "123";
        Boolean isActiveContract = true;
        List<String> services = List.of("market");

        setCorpClientIdTo(corpId);
        setCanOrderTo(canOrderResponse(true));
        // hasContact is false because a Contact#contactId is null / or Contact not found
        setContractsTo(contractResponse(contact(null, isActiveContract, billingClientId, services)));
        setRegistrationStatusTo(corpId, status(StatusEnum.SUCCESS, "Message"));
        setUpPersonsTo(persons().setInn("inn"));
        setUpEdoTo(EdoStatus.ENABLED);

        // When
        CheckResponseDto result = storage.checkCustomer(uid, id);

        // Then
        assertFalse(result.getCanOrder());
    }

    @Test
    public void checkCustomer_ifCanOrderIsFalseThenCanOrderIsFalse() {
        // Given
        String uid = "user_id";
        String id = "123";

        String corpId = "corp_id";
        String billingClientId = "123";
        Boolean isActiveContract = true;
        List<String> services = List.of("market");

        setCorpClientIdTo(corpId);
        setCanOrderTo(canOrderResponse(false));  // canOrder - a direct response form external API
        setContractsTo(contractResponse(contact(1, isActiveContract, billingClientId, services)));
        setRegistrationStatusTo(corpId, status(StatusEnum.SUCCESS, "Message"));

        // When
        CheckResponseDto result = storage.checkCustomer(uid, id);

        // Then
        assertFalse(result.getCanOrder());
    }

    @Test
    public void checkCustomer_ifInnUnderSanctionsThenCanOrderIsFalse() {
        // Given
        String uid = "user_id";
        String id = "123";

        String corpId = "corp_id";
        String billingClientId = "123";
        Boolean isActiveContract = true;
        List<String> services = List.of("market");

        setCorpClientIdTo(corpId);
        setCanOrderTo(canOrderResponse(true));
        setContractsTo(contractResponse(contact(1, isActiveContract, billingClientId, services)));
        setRegistrationStatusTo(corpId, status(StatusEnum.SUCCESS, "Message"));
        setUpPersonsTo(persons().setInn("inn"));
        setUpEdoTo(EdoStatus.ENABLED);
        Mockito.doReturn(true).when(sanctionsListService).contains("inn"); // under sanctions

        // When
        CheckResponseDto result = storage.checkCustomer(uid, id);

        // Then
        assertFalse(result.getCanOrder());
    }

    @Test
    public void checkCustomer_ifInnUnderSanctionsThenInSanctionsTrue() {
        // Given
        String uid = "user_id";
        String id = "123";

        String corpId = "corp_id";
        String billingClientId = "123";
        Boolean isActiveContract = true;
        List<String> services = List.of("market");

        setCorpClientIdTo(corpId);
        setCanOrderTo(canOrderResponse(true));
        setContractsTo(contractResponse(contact(1, isActiveContract, billingClientId, services)));
        setRegistrationStatusTo(corpId, status(StatusEnum.SUCCESS, "Message"));
        setUpPersonsTo(persons().setInn("inn"));
        setUpEdoTo(EdoStatus.ENABLED);
        Mockito.doReturn(true).when(sanctionsListService).contains("inn"); // under sanctions

        // When
        CheckResponseDto result = storage.checkCustomer(uid, id);

        // Then
        assertTrue(result.getInSanctionsList());
    }

    @Test
    public void checkCustomer_ifInnUnderSanctionsThenInSanctionsFalse() {
        // Given
        String uid = "user_id";
        String id = "123";

        String corpId = "corp_id";
        String billingClientId = "123";
        Boolean isActiveContract = true;
        List<String> services = List.of("market");

        setCorpClientIdTo(corpId);
        setCanOrderTo(canOrderResponse(true));
        setContractsTo(contractResponse(contact(1, isActiveContract, billingClientId, services)));
        setRegistrationStatusTo(corpId, status(StatusEnum.SUCCESS, "Message"));
        setUpPersonsTo(persons().setInn("inn"));
        setUpEdoTo(EdoStatus.ENABLED);
        Mockito.doReturn(false).when(sanctionsListService).contains("inn"); // not under sanctions

        // When
        CheckResponseDto result = storage.checkCustomer(uid, id);

        // Then
        assertFalse(result.getInSanctionsList());
    }

    @Test
    public void checkCustomer_ifCachedThenReturnsCached() {
        // Given
        String uid = "user_id";
        String id = "123";

        UserData user = UserData.newBuilder()
                .setCorpId("corp_id")
                .setAvailable(true)
                .setCanOrder(true)
                .setContractId(1L)
                .build();
        Mockito.doReturn(user).when(memcachedClient)
                .get(eq("u_" + uid + "_billingId_" + id), Mockito.any());

        // When
        CheckResponseDto response = storage.checkCustomer(uid, id);

        // Then
        assertTrue(response.getRegistered());
        assertTrue(response.getHasContract());
        verify(memcachedClient).get(eq("u_" + uid + "_billingId_" + id), Mockito.any());
    }

    @Test
    public void checkCustomer_ifEdoEnabledThenCanOrderIsTrue() {
        // Given
        String uid = "user_id";
        String id = "123";

        String corpId = "corp_id";
        String billingClientId = "123";
        Boolean isActiveContract = true;
        List<String> services = List.of("market");

        setCorpClientIdTo(corpId);
        setCanOrderTo(canOrderResponse(true));
        setContractsTo(contractResponse(contact(1, isActiveContract, billingClientId, services)));
        setRegistrationStatusTo(corpId, status(StatusEnum.SUCCESS, "Message"));
        setUpPersonsTo(persons());
        setUpEdoTo(EdoStatus.ENABLED);

        // When
        CheckResponseDto result = storage.checkCustomer(uid, id);

        // Then
        assertTrue(result.getCanOrder());
    }

    @Test
    public void checkCustomer_ifEdoDisabledThenCanOrderIsFalse() {
        // Given
        String uid = "user_id";
        String id = "123";

        String corpId = "corp_id";
        String billingClientId = "123";
        Boolean isActiveContract = true;
        List<String> services = List.of("market");

        setCorpClientIdTo(corpId);
        setCanOrderTo(canOrderResponse(true));
        setContractsTo(contractResponse(contact(1, isActiveContract, billingClientId, services)));
        setRegistrationStatusTo(corpId, status(StatusEnum.SUCCESS, "Message"));
        setUpPersonsTo(persons());
        setUpEdoTo(EdoStatus.DISABLED);

        // When
        CheckResponseDto result = storage.checkCustomer(uid, id);

        // Then
        assertFalse(result.getCanOrder());
    }
}
