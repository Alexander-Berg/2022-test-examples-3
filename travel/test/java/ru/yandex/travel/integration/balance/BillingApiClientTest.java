package ru.yandex.travel.integration.balance;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.Response;
import org.dom4j.Document;
import org.dom4j.Node;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.travel.commons.http.apiclient.HttpApiParsingException;
import ru.yandex.travel.commons.logging.AsyncHttpClientWrapper;
import ru.yandex.travel.integration.balance.model.BillingClient;
import ru.yandex.travel.integration.balance.model.BillingContract;
import ru.yandex.travel.integration.balance.model.BillingContractType;
import ru.yandex.travel.integration.balance.model.BillingPaymentType;
import ru.yandex.travel.integration.balance.model.BillingPerson;
import ru.yandex.travel.integration.balance.model.BillingUrPerson;
import ru.yandex.travel.integration.balance.responses.BillingCreateContractResponse;
import ru.yandex.travel.integration.balance.xmlrpc.XmlRpcRuntimeException;
import ru.yandex.travel.integration.balance.xmlrpc.XmlUtils;
import ru.yandex.travel.testing.misc.TestResources;
import ru.yandex.travel.tvm.TvmWrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BillingApiClientTest {
    public static final long DEFAULT_OPERATOR_ID = 7265137513L;

    private BillingApiProperties properties;
    private AsyncHttpClientWrapper ahcWrapper;
    private BillingApiClient apiClient;

    @Before
    public void init() {
        properties = BillingApiProperties.builder()
                .baseUrl("https://uri")
                .httpRequestTimeout(Duration.ZERO)
                .httpReadTimeout(Duration.ZERO)
                .build();
        ahcWrapper = Mockito.mock(AsyncHttpClientWrapper.class);
        apiClient = new BillingApiClient(ahcWrapper, properties, null);
    }

    @Test
    public void updatePayment_success() {
        AtomicReference<Document> req = mockResponse("Balance.UpdatePayment",
                "finances/billing_api/update_payment_ok.xml");
        apiClient.updatePayment(-641, 123L, Instant.parse("2019-12-20T18:27:52Z"));
        verify(ahcWrapper, times(1)).executeRequest(any(), any());
        assertThat(req.get().valueOf("/methodCall/params/param[1]/value/struct/member[name='ServiceID']/value/i4"))
                .isEqualTo("-641");
        assertThat(req.get().valueOf("/methodCall/params/param[1]/value/struct/member[name='TransactionID']/value/i4"))
                .isEqualTo("123");
        assertThat(req.get().valueOf("/methodCall/params/param[2]/value/struct/member/value"))
                .isEqualTo("2019-12-20T21:27:52");
    }

    @Test
    public void updatePayment_success_warn() {
        // same as above, the only difference is a warning message in logs, shouldn't fail processing
        mockResponse("Balance.UpdatePayment", "finances/billing_api/update_payment_ok_repeat.xml");
        apiClient.updatePayment(641, 1, Instant.now());
        verify(ahcWrapper, times(1)).executeRequest(any(), any());
    }

    @Test
    public void updatePayment_failure() {
        mockResponse("Balance.UpdatePayment", "finances/billing_api/update_payment_not_found.xml");
        assertThatThrownBy(() -> apiClient.updatePayment(641, 1, Instant.now()))
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessageContaining("updatePayment call failed: code=-1, message=Payment was not found");
        verify(ahcWrapper, times(1)).executeRequest(any(), any());
    }

    @Test
    public void testTvmSupportDisabled() {
        TvmWrapper tvm = Mockito.mock(TvmWrapper.class);
        apiClient = new BillingApiClient(ahcWrapper, properties, tvm);
        mockResponse("Balance.UpdatePayment", "finances/billing_api/update_payment_ok.xml");
        apiClient.updatePayment(641, 1, Instant.now());
        verify(ahcWrapper, times(1)).executeRequest(any(), any());
        verify(tvm, times(0)).getServiceTicket(any());
    }

    @Test
    public void testTvmSupportEnabled() {
        TvmWrapper tvm = Mockito.mock(TvmWrapper.class);
        properties.setTvmEnabled(true);
        apiClient = new BillingApiClient(ahcWrapper, properties, tvm);
        mockResponse("Balance.UpdatePayment", "finances/billing_api/update_payment_ok.xml");
        apiClient.updatePayment(641, 1, Instant.now());
        verify(ahcWrapper, times(1)).executeRequest(any(), any());
        verify(tvm, times(1)).getServiceTicket(any());
    }

    @Test
    public void testTvmSupportEnabledButNotProvided() {
        properties.setTvmEnabled(true);
        assertThatThrownBy(() -> new BillingApiClient(ahcWrapper, properties, null))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tvm support requires a not null TvmWrapper");
    }

    @Test
    public void getClientContracts_ok() {
        AtomicReference<Document> req = mockResponse("Balance.GetClientContracts",
                "finances/billing_api/get_client_contracts_single_active.xml");
        BillingClientContract expectedContract = BillingClientContract.builder()
                .id(787627L)
                .externalId("252649/19")
                .contractType(0)
                .personId(7639849L)
                .managerCode(31699L)
                .services(List.of(641, 35))
                .currency("USD")
                .paymentType(3)
                .dt(LocalDateTime.parse("2019-05-01T00:00:00"))
                .active(true)
                .signed(true)
                .faxed(true)
                .cancelled(false)
                .deactivated(false)
                .suspended(false)
                .build();
        assertThat(apiClient.getClientContracts(726537515L)).hasSize(1)
                .first().isEqualTo(expectedContract);
        assertThat(req.get().valueOf("/methodCall/params/param/value/struct/member/value/i4"))
                .isEqualTo("726537515");
    }

    @Test
    public void getClientContracts_inactive() {
        mockResponse("Balance.GetClientContracts",
                "finances/billing_api/get_client_contracts_single_inactive.xml");
        BillingClientContract expectedContract = BillingClientContract.builder()
                .active(false)
                .build();
        assertThat(apiClient.getClientContracts(1)).hasSize(1)
                .first().isEqualTo(expectedContract);
    }

    @Test
    public void getClientContracts_noAgreements() {
        mockResponse("Balance.GetClientContracts",
                "finances/billing_api/get_client_contracts_single_no_agreements.xml");
        assertThat(apiClient.getClientContracts(1)).isEqualTo(List.of());
    }

    @Test
    public void getClientContracts_int32Overflow() {
        AtomicReference<Document> req = mockResponse("Balance.GetClientContracts",
                "finances/billing_api/get_client_contracts_single_i32_overflow.xml");
        BillingClientContract expectedContract = BillingClientContract.builder()
                .id(87236497210432434L)
                .build();
        assertThat(apiClient.getClientContracts(9726537515213332L)).isEqualTo(List.of(expectedContract));
        assertThat(req.get().valueOf("/methodCall/params/param/value/struct/member/value"))
                .isEqualTo("9726537515213332");
    }

    @Test
    public void getClientPersons() {
        AtomicReference<Document> req = mockResponse("Balance.GetClientPersons",
                "finances/billing_api/get_client_persons_sample.xml");
        BillingPerson expectedPerson = BillingPerson.builder()
                .id(11996179L)
                .city("г Санкт-Петербург")
                .account("40702810500000059042")
                .bik("044525700")
                .phone("7(495)641 10 00 123123123")
                .inn("381101250840")
                .postCode("190031")
                .clientId(1337966112L)
                .name("АО \"ЦДТ на Ленинском\"")
                .longName("АО \"ЦДТ на Ленинском\"")
                .fiasGuid("c2deb16a-0330-4f05-821f-1d09c93331e6")
                .legalAddress("Россия, Москва, Ленинский проспект, 146")
                .dt("2020-07-20 13:30:15")
                .postSuffix("Россия, Санкт-Петербург, набережная реки Фонтанки, 97")
                .type("ur")
                .email("new-email@test.test")
                .apiVersion("0")
                .build();
        assertThat(apiClient.getClientPersons(1337966112L)).hasSize(1)
                .first().isEqualTo(expectedPerson);
        assertThat(req.get().valueOf("/methodCall/params/param/value"))
                .isEqualTo("1337966112");
    }

    @Test
    public void getPerson() {
        AtomicReference<Document> req = mockResponse("Balance.GetPerson",
                "finances/billing_api/get_person_sample.xml");
        BillingPerson expectedPerson = BillingPerson.builder()
                .id(11996179L)
                .city("г Санкт-Петербург")
                .account("40702810500000059042")
                .bik("044525700")
                .phone("7(495)641 10 00 123123123")
                .inn("381101250840")
                .postCode("190031")
                .clientId(1337966112L)
                .name("АО \"ЦДТ на Ленинском\"")
                .longName("АО \"ЦДТ на Ленинском\"")
                .fiasGuid("c2deb16a-0330-4f05-821f-1d09c93331e6")
                .legalAddress("Россия, Москва, Ленинский проспект, 146")
                .dt("2020-07-20 13:30:15")
                .postSuffix("Россия, Санкт-Петербург, набережная реки Фонтанки, 97")
                .type("ur")
                .email("new-email@test.test")
                .apiVersion("0")
                .build();
        assertThat(apiClient.getPerson(11996179L)).isEqualTo(expectedPerson);
        assertThat(req.get().valueOf("/methodCall/params/param/value/struct/member/value"))
                .isEqualTo("11996179");
    }

    @Test
    public void getClient() {
        AtomicReference<Document> req = mockResponse("Balance.GetClientByIdBatch",
                "finances/billing_api/get_client_sample.xml");
        BillingClient expectedClient = BillingClient.builder()
                .clientTypeId(0)
                .isAgency(false)
                .agencyId(0)
                .clientId(1337971706L)
                .name("АО \"ЦДТ на Ленинском\"")
                .city("")
                .fax("")
                .url("")
                .phone("")
                .email("")
                .build();
        assertThat(apiClient.getClient(1337971706L)).isEqualTo(expectedClient);
        assertThat(req.get().valueOf("/methodCall/params/param/value/array/data/value/i4"))
                .isEqualTo("1337971706");
    }

    @Test
    public void createClient_success() {
        AtomicReference<Document> req = mockResponse("Balance2.CreateClient",
                "finances/billing_api/create_client_success.xml");
        BillingClient client = BillingClient.builder().name("Test Client").build();
        assertThat(apiClient.createClient(DEFAULT_OPERATOR_ID, client)).isEqualTo(132587132L);
        Node params = req.get().selectSingleNode("/methodCall/params");
        // only non-null parameters are sent
        assertThat(params.selectNodes("param")).hasSize(2);
        assertThat(params.valueOf("param[1]/value")).isEqualTo("7265137513");
        assertThat(params.valueOf("param[2]/value/struct/member/name")).isEqualTo("NAME");
        assertThat(params.valueOf("param[2]/value/struct/member/value")).isEqualTo("Test Client");
    }

    @Test
    public void createClient_fault() {
        mockResponse("Balance2.CreateClient", "finances/billing_api/create_client_fault.xml");
        BillingClient client = BillingClient.builder().name("Test Client 2").build();
        assertThatThrownBy(() -> apiClient.createClient(DEFAULT_OPERATOR_ID, client))
                .isExactlyInstanceOf(HttpApiParsingException.class)
                .hasCauseInstanceOf(XmlRpcRuntimeException.class);
    }

    @Test
    public void createClient_updateAttempt() {
        BillingClient client = BillingClient.builder().clientId(87362846324L).build();
        assertThatThrownBy(() -> apiClient.createClient(DEFAULT_OPERATOR_ID, client))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null clientId is expected");
    }

    @Test
    public void updateClient_createNewAttempt() {
        BillingClient client = BillingClient.builder().build();
        assertThatThrownBy(() -> apiClient.updateClient(DEFAULT_OPERATOR_ID, client))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("the clientId field should be set");
    }

    @Test
    public void createPerson_success() {
        AtomicReference<Document> req = mockResponse("Balance2.CreatePerson",
                "finances/billing_api/create_person_success.xml");
        BillingUrPerson person = BillingUrPerson.builder()
                .name("Test Payer")
                .email("hotel@example.com")
                .build();
        assertThat(apiClient.createPerson(726513242323L, person)).isEqualTo(32047352L);
        Node params = req.get().selectSingleNode("/methodCall/params");
        // only non-null parameters are sent
        assertThat(params.selectNodes("param")).hasSize(2);
        assertThat(params.selectNodes("param[2]/value/struct/member")).hasSize(3);
        assertThat(params.valueOf("param[1]/value")).isEqualTo("726513242323");
        assertThat(params.valueOf("param[2]/value/struct/member[name='name']/value")).isEqualTo("Test Payer");
        assertThat(params.valueOf("param[2]/value/struct/member[name='type']/value")).isEqualTo("ur");
        assertThat(params.valueOf("param[2]/value/struct/member[name='email']/value")).isEqualTo("hotel@example.com");
    }

    @Test
    public void createPerson_updateAttempt() {
        BillingUrPerson person = BillingUrPerson.builder().personId(916278947124L).build();
        assertThatThrownBy(() -> apiClient.createPerson(DEFAULT_OPERATOR_ID, person))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null personId is expected");
    }

    @Test
    public void updatePerson_createNewAttempt() {
        BillingUrPerson person = BillingUrPerson.builder().build();
        assertThatThrownBy(() -> apiClient.updatePerson(DEFAULT_OPERATOR_ID, person))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("the personId field should be set");
    }

    @Test
    public void createOffer_success() {
        AtomicReference<Document> req = mockResponse("Balance2.CreateOffer",
                "finances/billing_api/create_offer_success.xml");
        BillingContract contract = BillingContract.builder()
                .contractType(BillingContractType.GENERAL)
                .paymentType(BillingPaymentType.POSTPAID)
                .startDate(LocalDate.parse("2020-07-01"))
                .build();
        assertThat(apiClient.createContract(72324322323L, contract))
                .isEqualTo(new BillingCreateContractResponse(1505698L, "588155/20"));
        Node params = req.get().selectSingleNode("/methodCall/params");
        // only non-null parameters are sent
        assertThat(params.selectNodes("param")).hasSize(2);
        assertThat(params.selectNodes("param[2]/value/struct/member")).hasSize(3);
        assertThat(params.valueOf("param[1]/value")).isEqualTo("72324322323");
        assertThat(params.valueOf("param[2]/value/struct/member[name='ctype']/value")).isEqualTo("GENERAL");
        assertThat(params.valueOf("param[2]/value/struct/member[name='payment_type']/value")).isEqualTo("3");
        assertThat(params.valueOf("param[2]/value/struct/member[name='start_dt']/value")).isEqualTo("2020-07-01");
    }

    private AtomicReference<Document> mockResponse(String apiMethod, String responseResource) {
        AtomicReference<Document> reqRef = new AtomicReference<>();
        String okRspXml = TestResources.readResource(responseResource);
        Response testRsp = Mockito.mock(Response.class);
        when(testRsp.getStatusCode()).thenReturn(200);
        when(testRsp.getResponseBody()).thenReturn(okRspXml);
        when(ahcWrapper.executeRequest(any(), any())).thenAnswer(invocation -> {
            RequestBuilder rb = invocation.getArgument(0);
            Document req = XmlUtils.parseDocument(rb.build().getStringData());
            if (!req.valueOf("/methodCall/methodName").equals(apiMethod)) {
                return invocation.callRealMethod();
            }
            reqRef.set(req);
            return CompletableFuture.completedFuture(testRsp);
        });
        return reqRef;
    }
}
