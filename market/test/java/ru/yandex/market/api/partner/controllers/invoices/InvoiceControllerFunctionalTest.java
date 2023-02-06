package ru.yandex.market.api.partner.controllers.invoices;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException.BadRequest;
import org.springframework.web.client.HttpClientErrorException.Forbidden;
import org.springframework.web.client.HttpServerErrorException.InternalServerError;

import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.context.FunctionalTestHelper;
import ru.yandex.market.api.partner.controllers.invoices.dto.MakeInvoiceRequestDTO;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.http.BalanceInvoiceService;
import ru.yandex.market.core.balance.http.BalanceInvoiceService.CopyInterface;
import ru.yandex.market.core.balance.model.PaymentRequest;
import ru.yandex.market.core.billing.BillingService;
import ru.yandex.market.core.billing.CampaignSpendingService;
import ru.yandex.market.core.billing.model.CampaignSpendingInfo;
import ru.yandex.market.core.billing.model.RequestPaymentsInfo;
import ru.yandex.market.common.balance.xmlrpc.model.CreateInvoiceStructure;
import ru.yandex.market.common.balance.xmlrpc.model.GetInvoiceStructure;
import ru.yandex.market.common.balance.xmlrpc.model.InvoiceDetailsStructure;
import ru.yandex.market.common.balance.xmlrpc.model.InvoiceParamStructure;
import ru.yandex.market.common.balance.xmlrpc.model.InvoiceStructure;
import ru.yandex.market.common.balance.xmlrpc.model.PcpContractStructure;
import ru.yandex.market.common.balance.xmlrpc.model.PcpListStructure;
import ru.yandex.market.common.balance.xmlrpc.model.PcpPaysysStructure;
import ru.yandex.market.common.balance.xmlrpc.model.PcpPersonStructure;
import ru.yandex.market.common.balance.xmlrpc.model.PersonParentStructure;
import ru.yandex.market.common.balance.xmlrpc.model.ResponseChoicesStructure;
import ru.yandex.market.mbi.util.MbiAsserts;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.matchers.HttpClientErrorMatcher.bodyMatches;
import static ru.yandex.market.core.matchers.HttpClientErrorMatcher.hasErrorCode;
import static ru.yandex.market.mbi.util.MbiMatchers.jsonEquals;

class InvoiceControllerFunctionalTest extends FunctionalTest {

    private static final String ACCESS_DENIED = getJsonString("access-denied.json");
    private static final String BAD_REQUEST = getJsonString("bad-request.json");
    private static final String INTERNAL_ERROR = getJsonString("internal-error.json");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final long UID = 67282295;
    private static final long CLIENT = 57485;

    private static final long CAMPAIGN_ID = 10774;
    private static final long REQUEST_ID = (long) Integer.MAX_VALUE + 15;
    private static final long INVOICE_ID = REQUEST_ID + 16;
    private static final long PERSON_ID = 3;
    private static final long PAYSYS_ID = 10;
    private static final long CONTRACT_ID = 5;

    private static final long DATE = 1583416565000L;

    @Autowired
    private BalanceService balanceService;

    @Autowired
    private CampaignSpendingService campaignSpendingService;

    @Autowired
    private BillingService billingService;

    @Autowired
    private BalanceInvoiceService backendClient;

    private static MakeInvoiceRequestDTO makeInvoiceRequestDTO() {
        final var req = new MakeInvoiceRequestDTO();
        req.setRequestId(REQUEST_ID);
        req.setPersonId(PERSON_ID);
        req.setPaysysId(PAYSYS_ID);
        return req;
    }

    private static RequestPaymentsInfo requestPaymentsInfo() {
        return new RequestPaymentsInfo("//userPath", "//adminPath", REQUEST_ID);
    }

    private static CreateInvoiceStructure createInvoiceStructure(double sum) {
        return new CreateInvoiceStructure(Map.of(
                CreateInvoiceStructure.FIELD_INVOICES, List.of(
                        Map.of(InvoiceStructure.FIELD_INVOICE, Map.of(
                                InvoiceDetailsStructure.FIELD_ID, 1001,
                                InvoiceDetailsStructure.FIELD_EXTERNAL_ID, "И-1001",
                                InvoiceDetailsStructure.FIELD_TOTAL_SUM, new BigDecimal(sum),
                                InvoiceDetailsStructure.FIELD_DT, new Date(DATE),
                                InvoiceDetailsStructure.FIELD_CURRENCY, "RUR"
                        ))
                ).toArray()
        ));
    }

    private static GetInvoiceStructure getInvoiceStructure() {
        return getInvoiceStructure(24266.4);
    }

    private static GetInvoiceStructure getInvoiceStructure(double totalSum) {
        return new GetInvoiceStructure(Map.of(
                GetInvoiceStructure.FIELD_DT, new Date(DATE),
                GetInvoiceStructure.FIELD_CURRENCY_RATE, new BigDecimal("1.0"),
                GetInvoiceStructure.FIELD_CURRENCY, "RUR",
                GetInvoiceStructure.FIELD_EXTERNAL_ID, "Б-1",
                GetInvoiceStructure.FIELD_TOTAL_SUM, new BigDecimal(totalSum),
                GetInvoiceStructure.FIELD_CLIENT_ID, 57485,
                GetInvoiceStructure.FIELD_PERSON_ID, 2));
    }

    private static ResponseChoicesStructure responseChoicesStructure() {
        return new ResponseChoicesStructure(Map.of(
                ResponseChoicesStructure.FIELD_PERSONS_PARENT, Map.of(
                        PersonParentStructure.FIELD_ID, PERSON_ID,
                        PersonParentStructure.FIELD_NAME, "Тестовое имя",
                        PersonParentStructure.FIELD_IS_AGENCY, 1,
                        PersonParentStructure.FIELD_AGENCY_ID, 4),
                ResponseChoicesStructure.FIELD_PCP_LIST, List.of(
                        Map.of(
                                PcpListStructure.FIELD_CONTRACT, Map.of(
                                        PcpContractStructure.FIELD_ID, CONTRACT_ID,
                                        PcpContractStructure.FIELD_CLIENT_ID, 6,
                                        PcpContractStructure.FIELD_PERSON_ID, 7,
                                        PcpContractStructure.FIELD_EXTERNAL_ID, "ext-1"),
                                PcpListStructure.FIELD_PERSON, Map.of(
                                        PcpPersonStructure.FIELD_REGION_ID, 8,
                                        PcpPersonStructure.FIELD_NAME, "name",
                                        PcpPersonStructure.FIELD_ID, 9,
                                        PcpPersonStructure.FIELD_LEGAL_ENTITY, 1,
                                        PcpPersonStructure.FIELD_RESIDENT, 1,
                                        PcpPersonStructure.FIELD_TYPE, "type"),
                                PcpListStructure.FIELD_PAYSYSES, List.of(
                                        Map.of(
                                                PcpPaysysStructure.FIELD_ID, PAYSYS_ID,
                                                PcpPaysysStructure.FIELD_NAME, "payment_name",
                                                PcpPaysysStructure.FIELD_PAYMENT_METHOD_CODE, "payment_method_code",
                                                PcpPaysysStructure.FIELD_PAYMENT_LIMIT, 336.4,
                                                PcpPaysysStructure.FIELD_REGION_ID, 11,
                                                PcpPaysysStructure.FIELD_LEGAL_ENTITY, 1,
                                                PcpPaysysStructure.FIELD_RESIDENT, 1,
                                                PcpPaysysStructure.FIELD_CC, "cc1"
                                        )).toArray())
                        , Map.of(
                                PcpListStructure.FIELD_CONTRACT, Map.of(
                                        PcpContractStructure.FIELD_ID, 50,
                                        PcpContractStructure.FIELD_CLIENT_ID, 60,
                                        PcpContractStructure.FIELD_PERSON_ID, 70,
                                        PcpContractStructure.FIELD_EXTERNAL_ID, "ext-10"),
                                PcpListStructure.FIELD_PERSON, Map.of(
                                        PcpPersonStructure.FIELD_REGION_ID, 80,
                                        PcpPersonStructure.FIELD_NAME, "name0",
                                        PcpPersonStructure.FIELD_ID, 90,
                                        PcpPersonStructure.FIELD_LEGAL_ENTITY, 0,
                                        PcpPersonStructure.FIELD_RESIDENT, 0,
                                        PcpPersonStructure.FIELD_TYPE, "type0"),
                                PcpListStructure.FIELD_PAYSYSES, List.of(
                                        Map.of(
                                                PcpPaysysStructure.FIELD_ID, 100,
                                                PcpPaysysStructure.FIELD_NAME, "payment_name0",
                                                PcpPaysysStructure.FIELD_PAYMENT_METHOD_CODE, "payment_method_code0",
                                                PcpPaysysStructure.FIELD_PAYMENT_LIMIT, 3360.4,
                                                PcpPaysysStructure.FIELD_REGION_ID, 110,
                                                PcpPaysysStructure.FIELD_LEGAL_ENTITY, 1,
                                                PcpPaysysStructure.FIELD_RESIDENT, 1,
                                                PcpPaysysStructure.FIELD_CC, "cc10"),
                                        Map.of(
                                                PcpPaysysStructure.FIELD_ID, 101,
                                                PcpPaysysStructure.FIELD_NAME, "payment_name01",
                                                PcpPaysysStructure.FIELD_PAYMENT_METHOD_CODE, "payment_method_code01",
                                                PcpPaysysStructure.FIELD_PAYMENT_LIMIT, 33601.4,
                                                PcpPaysysStructure.FIELD_REGION_ID, 1101,
                                                PcpPaysysStructure.FIELD_LEGAL_ENTITY, 1,
                                                PcpPaysysStructure.FIELD_RESIDENT, 1,
                                                PcpPaysysStructure.FIELD_CC, "cc101"
                                        )).toArray()
                        )).toArray()
        ));
    }

    private static String getJsonString(String resource) {
        return StringTestUtil.getString(InvoiceControllerFunctionalTest.class, resource);
    }

    @BeforeEach
    void initTest() {
        final long min10ye = 1000;
        final Map<Long, CampaignSpendingInfo> result = Map.of(CAMPAIGN_ID, new CampaignSpendingInfo(CAMPAIGN_ID, 0, 0, min10ye, 0, 0, 0, 0));
        doReturn(result).when(campaignSpendingService).getCampaignsSpendings(eq(List.of(CAMPAIGN_ID)));
    }

    @DisplayName("Запрос счета неизвестной кампании")
    @Test
    void requestInvoiceUnknown() {
        assertThat(Assertions.assertThrows(Forbidden.class, () -> request(123, 100.0)),
                allOf(
                        hasErrorCode(HttpStatus.FORBIDDEN),
                        bodyMatches(jsonEquals(ACCESS_DENIED))
                ));
    }

    @DisplayName("Запрос счета - меньше 10уе")
    @Test
    void requestInvoiceTooLow() {
        assertThat(Assertions.assertThrows(BadRequest.class, () -> request(9.99)),
                allOf(
                        hasErrorCode(HttpStatus.BAD_REQUEST),
                        bodyMatches(jsonEquals(String.format(BAD_REQUEST,
                                "Invalid payment sum 9.99, must be from 10.00 to 2147483647")))
                ));
    }

    @DisplayName("Запрос счета - больше 2^31")
    @Test
    void requestInvoiceTooMuch() {
        assertThat(Assertions.assertThrows(BadRequest.class, () -> request((long) Integer.MAX_VALUE + 1)),
                allOf(
                        hasErrorCode(HttpStatus.BAD_REQUEST),
                        bodyMatches(jsonEquals(String.format(BAD_REQUEST,
                                "Invalid payment sum 2147483648, must be from 10.00 to 2147483647")))
                ));
    }

    @DisplayName("Запрос счета - не удалось получить варианты ответа")
    @Test
    void requestInvoiceNoChoices() {
        final var amount = new BigDecimal("933.15");
        final var req = List.of(new PaymentRequest(CAMPAIGN_ID, amount));
        when(billingService.requestPayments(eq(req), eq(UID))).thenReturn(requestPaymentsInfo());

        // Не ответили на balanceService.getRequestChoices
        assertThat(Assertions.assertThrows(InternalServerError.class, () -> request(amount.doubleValue())),
                allOf(
                        hasErrorCode(HttpStatus.INTERNAL_SERVER_ERROR),
                        bodyMatches(jsonEquals(INTERNAL_ERROR))
                ));
    }

    @DisplayName("Запрос счета с пустыми вариантами ответа")
    @Test
    void requestInvoiceEmptyChoices() {
        final var amount = new BigDecimal("933.15");
        final var req = List.of(new PaymentRequest(CAMPAIGN_ID, amount));
        when(billingService.requestPayments(eq(req), eq(UID))).thenReturn(requestPaymentsInfo());
        when(balanceService.getRequestChoices(eq(UID), eq(REQUEST_ID), eq(null)))
                .thenReturn(new ResponseChoicesStructure(Map.of()));

        final ResponseEntity<String> response = request(amount.doubleValue());
        MbiAsserts.assertJsonEquals(getJsonString("request-empty.json"), response.getBody());
    }

    @DisplayName("Запрос счета с вариантами ответа")
    @Test
    void requestInvoice() {
        final var amount = new BigDecimal("933.15");
        final var req = List.of(new PaymentRequest(CAMPAIGN_ID, amount));
        when(billingService.requestPayments(eq(req), eq(UID))).thenReturn(requestPaymentsInfo());
        when(balanceService.getRequestChoices(eq(UID), eq(REQUEST_ID), eq(null))).thenReturn(responseChoicesStructure());

        final ResponseEntity<String> response = request(amount.doubleValue());
        MbiAsserts.assertJsonEquals(getJsonString("request-complete.json"), response.getBody());
    }

    @DisplayName("Выставление счета с некорректными настройками (requestId)")
    @Test
    void makeInvoiceInvalidRequest() {
        final var req = makeInvoiceRequestDTO();
        req.setRequestId(0);
        assertThat(Assertions.assertThrows(BadRequest.class, () -> make(req)),
                allOf(
                        hasErrorCode(HttpStatus.BAD_REQUEST),
                        bodyMatches(jsonEquals(String.format(BAD_REQUEST, "requestId is mandatory")))
                ));
    }

    @DisplayName("Выставление счета с некорректными настройками (paysysId)")
    @Test
    void makeInvoiceInvalidPaysys() {
        final var req = makeInvoiceRequestDTO();
        req.setPaysysId(0);
        assertThat(Assertions.assertThrows(BadRequest.class, () -> make(req)),
                allOf(
                        hasErrorCode(HttpStatus.BAD_REQUEST),
                        bodyMatches(jsonEquals(String.format(BAD_REQUEST, "paysysId is mandatory")))
                ));
    }

    @DisplayName("Выставление счета с некорректными настройками (personId)")
    @Test
    void makeInvoiceInvalidPerson() {
        final var req = makeInvoiceRequestDTO();
        req.setPersonId(0);
        assertThat(Assertions.assertThrows(BadRequest.class, () -> make(req)),
                allOf(
                        hasErrorCode(HttpStatus.BAD_REQUEST),
                        bodyMatches(jsonEquals(String.format(BAD_REQUEST, "personId is mandatory")))
                ));
    }

    @DisplayName("Выставление счета с некорректными настройками (credit without contractId)")
    @Test
    void makeInvoiceInvalidCreditContract() {
        final var req = makeInvoiceRequestDTO();
        req.setCredit(true);
        assertThat(Assertions.assertThrows(BadRequest.class, () -> make(req)),
                allOf(
                        hasErrorCode(HttpStatus.BAD_REQUEST),
                        bodyMatches(jsonEquals(String.format(BAD_REQUEST,
                                "contractId is mandatory when credit option is enabled")))
                ));
    }

    @DisplayName("Выставление счета с некорректными настройками (credit with overdraft)")
    @Test
    void makeInvoiceInvalidCreditWithOverdraft() {
        final var req = makeInvoiceRequestDTO();
        req.setCredit(true);
        req.setContractId(CONTRACT_ID);
        req.setOverdraft(true);
        assertThat(Assertions.assertThrows(BadRequest.class, () -> make(req)),
                allOf(
                        hasErrorCode(HttpStatus.BAD_REQUEST),
                        bodyMatches(jsonEquals(String.format(BAD_REQUEST,
                                "credit and overdraft options cannot be enabled at the same time")))
                ));
    }

    @DisplayName("Выставление счета без ответа баланса")
    @Test
    void makeInvoiceNoBalanceResponse() {
        // Не ответили на balanceService.createInvoice2
        assertThat(Assertions.assertThrows(InternalServerError.class, () -> make(makeInvoiceRequestDTO())),
                allOf(
                        hasErrorCode(HttpStatus.INTERNAL_SERVER_ERROR),
                        bodyMatches(jsonEquals(INTERNAL_ERROR))
                ));
    }

    @DisplayName("Выставление овердрфтного счета с договором")
    @Test
    void makeInvoiceOverdraftWithContract() {
        final var req = makeInvoiceRequestDTO();
        req.setOverdraft(true);
        req.setContractId(CONTRACT_ID);
        assertThat(Assertions.assertThrows(BadRequest.class, () -> make(req)),
                allOf(
                        hasErrorCode(HttpStatus.BAD_REQUEST),
                        bodyMatches(jsonEquals(String.format(BAD_REQUEST,
                                "contractId is not allowed when overdraft option is enabled")))
                ));
    }

    @DisplayName("Выставление обычного счета")
    @Test
    void makeInvoice() {
        final var request = makeInvoiceRequestDTO();

        final var expect = new InvoiceParamStructure();
        expect.setRequestId(REQUEST_ID);
        expect.setPaysysId(PAYSYS_ID);
        expect.setPersonId(PERSON_ID);

        when(balanceService.createInvoice2(eq(expect), eq(UID))).thenReturn(createInvoiceStructure(10.2));

        final ResponseEntity<String> response = make(request);
        MbiAsserts.assertJsonEquals(String.format(getJsonString("make-complete.json"), "10.2"), response.getBody());
    }

    @DisplayName("Выставление обычного счета с договором")
    @Test
    void makeInvoiceWithContract() {
        final var request = makeInvoiceRequestDTO();
        request.setContractId(CONTRACT_ID);

        final var expect = new InvoiceParamStructure();
        expect.setRequestId(REQUEST_ID);
        expect.setPaysysId(PAYSYS_ID);
        expect.setPersonId(PERSON_ID);
        expect.setContractId(CONTRACT_ID);

        when(balanceService.createInvoice2(eq(expect), eq(UID))).thenReturn(createInvoiceStructure(10.2));

        final ResponseEntity<String> response = make(request);
        MbiAsserts.assertJsonEquals(String.format(getJsonString("make-complete.json"), "10.2"), response.getBody());
    }

    @DisplayName("Выставление овердрфтного счета")
    @Test
    void makeInvoiceOverdraft() {
        final var request = makeInvoiceRequestDTO();
        request.setOverdraft(true);

        final var expect = new InvoiceParamStructure();
        expect.setRequestId(REQUEST_ID);
        expect.setPaysysId(PAYSYS_ID);
        expect.setPersonId(PERSON_ID);
        expect.setOverdraft(true);

        when(balanceService.createInvoice2(eq(expect), eq(UID))).thenReturn(createInvoiceStructure(10.3));

        final ResponseEntity<String> response = make(request);
        MbiAsserts.assertJsonEquals(String.format(getJsonString("make-complete.json"), "10.3"), response.getBody());
    }

    @DisplayName("Выставление кредитного счета")
    @Test
    void makeInvoiceCredit() {
        final var request = makeInvoiceRequestDTO();
        request.setCredit(true);
        request.setContractId(CONTRACT_ID);

        final var expect = new InvoiceParamStructure();
        expect.setRequestId(REQUEST_ID);
        expect.setPaysysId(PAYSYS_ID);
        expect.setPersonId(PERSON_ID);
        expect.setCredit(true);
        expect.setContractId(CONTRACT_ID);

        when(balanceService.createInvoice2(eq(expect), eq(UID))).thenReturn(createInvoiceStructure(0.0));

        final ResponseEntity<String> response = make(request);
        MbiAsserts.assertJsonEquals(String.format(getJsonString("make-complete.json"), "0.0"), response.getBody());
    }

    @DisplayName("Печать счета неизвестной кампанией")
    @Test
    void printInvoiceUnknown() {
        assertThat(Assertions.assertThrows(Forbidden.class, () -> print(123)),
                allOf(
                        hasErrorCode(HttpStatus.FORBIDDEN),
                        bodyMatches(jsonEquals(ACCESS_DENIED))
                ));
    }

    @DisplayName("Печать счета пользователя, у которого нет доступа к кампании")
    @Test
    void printInvoiceUserNoAccess() {
        when(balanceService.getInvoice(eq(UID), eq(INVOICE_ID))).thenReturn(getInvoiceStructure());
        // Не ответили на billingService.canUserPayCampaigns
        assertThat(Assertions.assertThrows(BadRequest.class, this::print),
                allOf(
                        hasErrorCode(HttpStatus.BAD_REQUEST),
                        bodyMatches(jsonEquals(String.format(BAD_REQUEST,
                                "User 67282295 has no access to client 57485")))
                ));
    }

    @DisplayName("Печать счета если баланс не ответил")
    @Test
    void printInvoiceNoBalanceResponse() {
        assertThat(Assertions.assertThrows(InternalServerError.class, this::print),
                allOf(
                        hasErrorCode(HttpStatus.INTERNAL_SERVER_ERROR),
                        bodyMatches(jsonEquals(INTERNAL_ERROR))
                ));
    }

    @DisplayName("Печать нулевого счета (для контракта)")
    @Test
    void printInvoiceEmpty() {
        when(balanceService.getInvoice(eq(UID), eq(INVOICE_ID))).thenReturn(getInvoiceStructure(0)); // 0 - лицевой счет
        when(billingService.canUserPayCampaigns(eq(UID), eq(CLIENT), eq(List.of(CAMPAIGN_ID)))).thenReturn(true);

        assertThat(Assertions.assertThrows(BadRequest.class, this::print),
                allOf(
                        hasErrorCode(HttpStatus.BAD_REQUEST),
                        bodyMatches(jsonEquals(String.format(BAD_REQUEST,
                                "Unable to print credit invoice (with zero sum)")))
                ));
    }

    @DisplayName("Печать счета")
    @Test
    void printInvoice() {
        when(balanceService.getInvoice(eq(UID), eq(INVOICE_ID))).thenReturn(getInvoiceStructure());
        when(billingService.canUserPayCampaigns(eq(UID), eq(CLIENT), eq(List.of(CAMPAIGN_ID)))).thenReturn(true);

        // Любое содержимое будет перезаписано в outputStream сервлета (нам не важно, что это не PDF)
        final var text = "{\"response\":\"any type will do\"}";
        doAnswer((Answer<Void>) invocation -> {
            final CopyInterface copyInterface = invocation.getArgument(1);
            copyInterface.setContent(MediaType.APPLICATION_JSON.toString(), text.length()).write(text.getBytes());
            return null;
        }).when(backendClient).copyInvoicePrintForm(eq(INVOICE_ID), any(CopyInterface.class));

        ResponseEntity<String> response = print();
        MbiAsserts.assertJsonEquals(text, response.getBody());

        // У нас нет настоящего бэкенда или эмуляции баланса для проверки - пользователь это или агент,
        // поэтому отдельного теста не будет
    }

    @DisplayName("Стандартный цикл операций - предвыставление, выставление, печать")
    @Test
    void testStandardCycle() {
        requestInvoice();
        makeInvoice();
        printInvoice();
    }

    private ResponseEntity<String> request(double amount) {
        return request(CAMPAIGN_ID, amount);
    }

    private ResponseEntity<String> request(long campaignId, double amount) {
        return FunctionalTestHelper.makeRequest(urlBasePrefix +
                        String.format("/campaigns/%s/invoice/paypreview?amount=%s", campaignId, amount),
                HttpMethod.POST, Format.JSON);
    }

    private ResponseEntity<String> make(MakeInvoiceRequestDTO request) {
        try {
            return FunctionalTestHelper.makeRequest(urlBasePrefix + "/invoice",
                    HttpMethod.POST, Format.JSON, MAPPER.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private ResponseEntity<String> print() {
        return print(CAMPAIGN_ID);
    }

    private ResponseEntity<String> print(long campaignId) {
        return FunctionalTestHelper.makeRequest(urlBasePrefix +
                        String.format("/campaigns/%s/invoices/%s", campaignId, INVOICE_ID),
                HttpMethod.GET, Format.JSON);
    }

}
