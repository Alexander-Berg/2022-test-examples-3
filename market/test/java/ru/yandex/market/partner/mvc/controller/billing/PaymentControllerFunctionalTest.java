package ru.yandex.market.partner.mvc.controller.billing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.model.ClientInfo;
import ru.yandex.market.common.balance.model.ClientType;
import ru.yandex.market.common.balance.xmlrpc.model.OrderRequest2Result;
import ru.yandex.market.common.balance.xmlrpc.model.OverdraftInfoStructure;
import ru.yandex.market.common.balance.xmlrpc.model.PayRequestResultStructure;
import ru.yandex.market.common.balance.xmlrpc.model.PcpListStructure;
import ru.yandex.market.common.balance.xmlrpc.model.PcpPaysysStructure;
import ru.yandex.market.common.balance.xmlrpc.model.PcpPersonStructure;
import ru.yandex.market.common.balance.xmlrpc.model.ResponseChoicesStructure;
import ru.yandex.market.partner.billing.PaymentDao;
import ru.yandex.market.partner.billing.dto.PersonPaymentMethod;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static ru.yandex.market.common.balance.xmlrpc.model.PcpPaysysStructure.REGION_RUSSIA;
import static ru.yandex.market.common.balance.xmlrpc.model.PcpPaysysStructure.REGION_SWITZERLAND;
import static ru.yandex.market.common.balance.xmlrpc.model.PcpPersonStructure.TYPE_LEGAL_KAZAKHSTAN;
import static ru.yandex.market.common.balance.xmlrpc.model.PcpPersonStructure.TYPE_LEGAL_RUSSIA;
import static ru.yandex.market.common.balance.xmlrpc.model.PcpPersonStructure.TYPE_NATURAL_KAZAKHSTAN;
import static ru.yandex.market.common.balance.xmlrpc.model.PcpPersonStructure.TYPE_NATURAL_RUSSIA;
import static ru.yandex.market.common.balance.xmlrpc.model.PcpPersonStructure.TYPE_NATURAL_SNG;
import static ru.yandex.market.common.balance.xmlrpc.model.PcpPersonStructure.TYPE_NATURAL_SWITZERLAND_NON_RESIDENT;
import static ru.yandex.market.common.balance.xmlrpc.model.PcpPersonStructure.TYPE_NATURAL_SWITZERLAND_RESIDENT;

/**
 * Тесты {@link PaymentController}.
 */
@ParametersAreNonnullByDefault
class PaymentControllerFunctionalTest extends FunctionalTest {
    private static final long UID = 67282295L;
    private static final long REQUEST_ID = 111222333L;
    private static final long INVALID_REQUEST_ID = -1L;
    private static final long INVOICE_ID = 555666777L;
    private static final long CLIENT_ID = 325076L;
    private static final ClientInfo CLIENT_INFO = new ClientInfo(CLIENT_ID, ClientType.PHYSICAL);
    private static final String MOCKED_PARTNER_URL = "https://partner.market.yandex.ru";
    private static final String MOCKED_TRUST_URL = "https://trust-test.yandex.ru/web/payment?purchase_token=mock";
    private static final String MOCKED_BALANCE_URL = "" +
            "https://passport.yandex.ru/passport?mode=subscribe&from=balance&retpath=" +
            "https%3A%2F%2Fuser-balance.greed-tm.paysys.yandex.ru%2Fpaypreview.xml" +
            "%3Frequest_id%3D111222333%26ref_service_id%3D11";
    private static final List<String> BRIEF_PAYSYS_FIELDS = List.of(
            PcpPaysysStructure.FIELD_ID,
            PcpPaysysStructure.FIELD_NAME,
            PcpPaysysStructure.FIELD_CC
    );

    @Autowired
    @Qualifier("patientBalanceService")
    private BalanceService balanceService;

    @Autowired
    private PaymentDao paymentDao;

    @BeforeEach
    private void setUpBalanceMock() {
        when(balanceService.createRequest2(eq(CLIENT_ID), any(), eq(UID)))
                .thenReturn(orderRequest2Result());
        when(balanceService.getClients(any())).thenReturn(Map.of(CLIENT_ID, CLIENT_INFO));
        when(balanceService.getClient(CLIENT_ID)).thenReturn(CLIENT_INFO);
        when(balanceService.getClientByUid(UID)).thenReturn(CLIENT_INFO);
    }

    @Test
    @DbUnitDataSet(before = "csv/testPayment.before.csv")
    void testPaymentWithoutPaysysPassed() {
        when(balanceService.getRequestChoices(eq(UID), eq(REQUEST_ID), nullable(Long.class)))
                .thenReturn(responseChoicesStructure());

        final String url = UriComponentsBuilder.fromUriString(baseUrl + "/payment/proceed")
                .queryParam("_user_id", UID)
                .queryParam("format=json")
                .build().toString();

        final HttpEntity request = JsonTestUtil.getJsonHttpEntity(
                getClass(),
                "json/PaymentController.testPaymentRequest.json"
        );
        final ResponseEntity<String> response = FunctionalTestHelper.post(url, request);
        JsonTestUtil.assertEquals(
                response,
                getClass(),
                "json/PaymentController.testPaymentBalanceRedirectWithoutPaysys.json"
        );
    }

    @Test
    @DbUnitDataSet(before = "csv/testPayment.before.csv")
    void testPaymentWithIncorrectPaysysPassed() {
        when(balanceService.getRequestChoices(eq(UID), eq(REQUEST_ID), nullable(Long.class)))
                .thenReturn(responseChoicesStructure());

        final String url = UriComponentsBuilder.fromUriString(baseUrl + "/payment/proceed")
                .queryParam("_user_id", UID)
                .queryParam("format=json")
                .build().toString();

        final HttpEntity request = JsonTestUtil.getJsonHttpEntity(
                getClass(),
                "json/PaymentController.testPaymentRequestWithIncorrectPaysysId.json"
        );

        Assertions.assertThrows(Exception.class, () -> FunctionalTestHelper.post(url, request));
    }

    @ParameterizedTest(name = "Сценарий оплаты через интерфейс Баланса (плательщик типа {0}, терминал {1}")
    @MethodSource("paymentInfos")
    @DbUnitDataSet(before = "csv/testPayment.before.csv")
    void testPayment(final String personType,
                     final Long paysysId,
                     final Long personId,
                     final Boolean directPayment) throws IOException {

        when(balanceService.getRequestChoices(eq(UID), eq(REQUEST_ID), nullable(Long.class)))
                .thenReturn(responseChoicesStructure(personType));

        final String url = UriComponentsBuilder.fromUriString(baseUrl + "/payment/proceed")
                .queryParam("_user_id", UID)
                .queryParam("format=json")
                .build().toString();
        final String json = createJsonByTemplate(paysysId, personId, directPayment);

        final HttpEntity request = JsonTestUtil.getJsonHttpEntity(json);
        final ResponseEntity<PaymentResponse> response = FunctionalTestHelper.post(url, request, PaymentResponse.class);

        final String result = response.getBody().getResult();
        assertTrue(result.startsWith(MOCKED_BALANCE_URL));
        if (paysysId != null && personId != null) {
            assertTrue(result.contains("%26paysys_id%3D" + paysysId));
            assertTrue(result.contains("%26person_id%3D" + personId));
            Mockito.verify(paymentDao)
                    .saveLastPaymentMethodForClient(eq(CLIENT_ID), eq(PersonPaymentMethod.of(paysysId, personId)));
            Mockito.verifyNoMoreInteractions(paymentDao);
        } else {
            Mockito.verifyNoMoreInteractions(paymentDao);
        }
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.NONE)
    private static class PaymentResponse {
        @XmlElement(name = "result", required = true)
        private String result;

        public String getResult() {
            return result;
        }
    }

    private static Stream<Arguments> paymentInfos() {
        return Stream.of(
                Arguments.of(TYPE_NATURAL_RUSSIA, 11101001L, 111L, false),
                Arguments.of(TYPE_NATURAL_SWITZERLAND_NON_RESIDENT, 1069L, 112L, false),
                Arguments.of(TYPE_NATURAL_SWITZERLAND_NON_RESIDENT, 1070L, null, false),
                Arguments.of(TYPE_NATURAL_SWITZERLAND_NON_RESIDENT, null, 112L, false),
                Arguments.of(TYPE_NATURAL_SWITZERLAND_RESIDENT, null, null, false),
                Arguments.of(TYPE_NATURAL_SWITZERLAND_RESIDENT, 1080L, 111L, false),
                Arguments.of(TYPE_LEGAL_RUSSIA, 11101003L, 111L, false),
                Arguments.of(TYPE_LEGAL_RUSSIA, 11101033L, 112L, false)
        );
    }

    private String createJsonByTemplate(final Long paysysId,
                                        final Long personId,
                                        final boolean directPayment) throws IOException {
        final byte[] rawJsonTemplate = getClass()
                .getResourceAsStream("json/PaymentController.testPaymentRequestWithWildcardPaysysId.json-template")
                .readAllBytes();

        return String.format(new String(rawJsonTemplate), paysysId, personId, directPayment);
    }

    @ParameterizedTest(name = "Сценарий оплаты мимо интерфейсов Баланса (плательщик типа {0}, терминал {1}")
    @MethodSource("directPaymentInfos")
    @DbUnitDataSet(before = "csv/testPayment.before.csv")
    void testDirectPayment(final String personType,
                           final long paysysId,
                           final long personId,
                           final boolean directPayment) throws IOException {
        when(balanceService.getRequestChoices(eq(UID), eq(REQUEST_ID), nullable(Long.class)))
                .thenReturn(responseChoicesStructure(personType));
        when(balanceService.payRequest(eq(UID), any()))
                .thenReturn(payRequestResultStructure(false));

        final String url = UriComponentsBuilder.fromUriString(baseUrl + "/payment/proceed")
                .queryParam("_user_id", UID)
                .queryParam("format=json")
                .build().toString();

        final String json = createJsonByTemplate(paysysId, personId, directPayment);
        final HttpEntity request = JsonTestUtil.getJsonHttpEntity(json);
        final ResponseEntity<String> response = FunctionalTestHelper.post(url, request);
        JsonTestUtil.assertEquals(
                response,
                getClass(),
                "json/PaymentController.testPaymentTrustRedirect.json"
        );
        Mockito.verify(paymentDao)
                .saveLastPaymentMethodForClient(eq(CLIENT_ID), eq(PersonPaymentMethod.of(paysysId, personId)));
        Mockito.verifyNoMoreInteractions(paymentDao);
    }

    private static Stream<Arguments> promoOrOverdraft() {
        return Stream.of(
                Arguments.of(true, true),
                Arguments.of(true, false),
                Arguments.of(false, true)
        );
    }


    private String createJsonByTemplatePromoOrOverdraft(boolean promocode, boolean overdraft) throws IOException {
        final byte[] rawJsonTemplate = getClass()
                .getResourceAsStream("json/PaymentController.testPaymentRequestWithWildcardPaysysIdFull.json-template")
                .readAllBytes();

        return String.format(new String(rawJsonTemplate), promocode, overdraft);
    }

    @ParameterizedTest(name = "Если выбирают промо или овердрафт, нужно редиректить в баланс")
    @MethodSource("promoOrOverdraft")
    @DbUnitDataSet(before = "csv/testPayment.before.csv")
    void testDirectPayment(boolean promocode, boolean overdraft) throws IOException {
        when(balanceService.getRequestChoices(eq(UID), eq(REQUEST_ID), nullable(Long.class)))
                .thenReturn(responseChoicesStructure(TYPE_NATURAL_RUSSIA));
        when(balanceService.payRequest(eq(UID), any()))
                .thenReturn(payRequestResultStructure(false));

        final String url = UriComponentsBuilder.fromUriString(baseUrl + "/payment/proceed")
                .queryParam("_user_id", UID)
                .queryParam("format=json")
                .build().toString();

        final String json = createJsonByTemplatePromoOrOverdraft(promocode, overdraft);
        final HttpEntity request = JsonTestUtil.getJsonHttpEntity(json);
        final ResponseEntity<PaymentResponse> response = FunctionalTestHelper.post(url, request, PaymentResponse.class);
        final String result = response.getBody().getResult();
        assertTrue(result.startsWith(MOCKED_BALANCE_URL));
        assertTrue(result.contains("%26paysys_id%3D" + 11101002));
        assertTrue(result.contains("%26person_id%3D" + 1613));
        Mockito.verify(paymentDao)
                .saveLastPaymentMethodForClient(eq(CLIENT_ID), eq(PersonPaymentMethod.of(11101002, 1613)));
        Mockito.verifyNoMoreInteractions(paymentDao);
    }

    private static Stream<Arguments> directPaymentInfos() {
        return Stream.of(
                Arguments.of(TYPE_NATURAL_RUSSIA, 11101002L, 1613L, true),
                Arguments.of(TYPE_NATURAL_SNG, 1075L, 1613L, true),
                Arguments.of(TYPE_NATURAL_SWITZERLAND_NON_RESIDENT, 1076L, 1613L, true),
                Arguments.of(TYPE_NATURAL_SWITZERLAND_NON_RESIDENT, 1077L, 1613L, true),
                Arguments.of(TYPE_NATURAL_SWITZERLAND_NON_RESIDENT, 1079L, 1613L, true)
        );
    }

    @Test
    @DbUnitDataSet(before = "csv/testPayment.before.csv")
    void testBackUrlForDirectPayment() {
        when(balanceService.getRequestChoices(eq(UID), eq(REQUEST_ID), nullable(Long.class)))
                .thenReturn(responseChoicesStructure());
        when(balanceService.payRequest(eq(UID), any()))
                .thenReturn(payRequestResultStructure(true));

        final var url = UriComponentsBuilder.fromUriString(baseUrl + "/payment/proceed")
                .queryParam("_user_id", UID)
                .queryParam("format", "json")
                .build().toString();

        final HttpEntity request = JsonTestUtil.getJsonHttpEntity(
                getClass(),
                "json/PaymentController.testPaymentRequestWithBackUrl.json"
        );
        final ResponseEntity<String> response = FunctionalTestHelper.post(url, request);
        JsonTestUtil.assertEquals(
                response,
                getClass(),
                "json/PaymentController.testPaymentTrustRedirect.json"
        );
    }

    /**
     * Проверка выдачи {@code /payment/methods}, когда в базе есть
     * информация о последнем использованном способе оплаты.
     */
    @Test
    @DbUnitDataSet(before = {"csv/testGetPaymentMethods.csv", "csv/lastPaymentMethods.csv", "csv/goodOverdraft.csv"})
    void testGetPaymentMethodsWithLastPaymentMethodRecorded() {
        testGetPaymentMethodsCorrectInput(
                "json/PaymentController.testGetPaymentMethodsCorrectResponse.json",
                "RUB"
        );
    }

    /**
     * Проверка выдачи {@code /payment/methods}, когда в базе отсутствует
     * информация о последнем использованном способе оплаты.
     * В таком случае работает сортировка способов оплаты,
     * и оплата картой физлица должна вернуться как способ по умолчанию.
     */
    @Test
    @DbUnitDataSet(before = "csv/testGetPaymentMethods.csv")
    void testGetPaymentMethodsWithNoLastPaymentMethodRecorded() {
        testGetPaymentMethodsCorrectInput(
                "json/PaymentController.testGetPaymentMethodsNoLast.json", "RUB");
    }

    private void testGetPaymentMethodsCorrectInput(String jsonFileName, String currency) {
        when(balanceService.getRequestChoices(eq(UID), eq(INVALID_REQUEST_ID), eq(CLIENT_ID), any(), any()))
                .thenReturn(responseChoicesStructure());
        when(balanceService.getRequestChoicesAsync(eq(UID), eq(INVALID_REQUEST_ID), eq(CLIENT_ID), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(responseChoicesStructure()));

        final String url = UriComponentsBuilder.fromUriString(baseUrl + "/payment/methods")
                .queryParam("_user_id", UID)
                .queryParam("lt", true)
                .queryParam("campaign_id", 10774L, 10775L)
                .queryParam("currency", currency)
                .build().toString();

        final ResponseEntity<String> response = FunctionalTestHelper.get(url);
        JsonTestUtil.assertEquals(
                response,
                this.getClass(),
                jsonFileName
        );
    }

    /**
     * Проверка выдачи {@code /payment/methods} в случае плательщика
     * с доступными способами оплаты в иностранных валютах - доллары, евро, франки.
     */
    @Test
    @DbUnitDataSet(before = {"csv/testGetPaymentMethods.csv", "csv/spentOverdraft.csv"})
    void testGetPaymentMethodswithForeignCurrencies() {
        when(balanceService.getRequestChoices(eq(UID), eq(INVALID_REQUEST_ID), eq(CLIENT_ID), any(), any()))
                .thenReturn(responseChoicesStructure(TYPE_NATURAL_SWITZERLAND_NON_RESIDENT));
        when(balanceService.getRequestChoicesAsync(eq(UID), eq(INVALID_REQUEST_ID), eq(CLIENT_ID), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(responseChoicesStructure(TYPE_NATURAL_SWITZERLAND_NON_RESIDENT)));

        final String url = UriComponentsBuilder.fromUriString(baseUrl + "/payment/methods")
                .queryParam("_user_id", UID)
                .queryParam("lt", true)
                .queryParam("campaign_id", 10774L)
                .build().toString();

        final ResponseEntity<String> response = FunctionalTestHelper.get(url);
        JsonTestUtil.assertEquals(
                response,
                this.getClass(),
                "json/PaymentController.testGetPaymentMethodsWithForeignCurrencies.json"
        );
    }

    private static OrderRequest2Result orderRequest2Result() {
        return new OrderRequest2Result(Map.of(
                OrderRequest2Result.FIELD_REQUEST_ID, REQUEST_ID,
                OrderRequest2Result.FIELD_USER_PATH, MOCKED_BALANCE_URL
        ));
    }

    private static ResponseChoicesStructure responseChoicesStructure() {
        return responseChoicesStructure(TYPE_NATURAL_RUSSIA);
    }

    /**
     * @param personType тип  плательщика-физлица, уже существующего в Балансе.
     *                   Оплата должна быть произведена в любом случае -
     *                   при отсутствии поддходящего плательщика будет создан новый.
     */
    private static ResponseChoicesStructure responseChoicesStructure(@Nullable final String personType) {

        final var physRusCardPaysys = new PcpPaysysStructure(Map.of(
                PcpPaysysStructure.FIELD_ID, 11101002L,
                PcpPaysysStructure.FIELD_CC, "as",
                PcpPaysysStructure.FIELD_NAME, "Кредитной картой",
                PcpPaysysStructure.FIELD_CURRENCY, "RUB",
                PcpPaysysStructure.FIELD_PAYMENT_LIMIT, "49999.99",
                PcpPaysysStructure.FIELD_LEGAL_ENTITY, 0,
                PcpPaysysStructure.FIELD_PAYMENT_METHOD_CODE, "card",
                PcpPaysysStructure.FIELD_REGION_ID, REGION_RUSSIA,
                PcpPaysysStructure.FIELD_RESIDENT, 1
        ));

        final var physRusBankPaysys = new PcpPaysysStructure(Map.of(
                PcpPaysysStructure.FIELD_ID, 11101001L,
                PcpPaysysStructure.FIELD_CC, "ph",
                PcpPaysysStructure.FIELD_NAME, "Банк для физических лиц",
                PcpPaysysStructure.FIELD_CURRENCY, "RUB",
                PcpPaysysStructure.FIELD_LEGAL_ENTITY, 0,
                PcpPaysysStructure.FIELD_PAYMENT_METHOD_CODE, "bank",
                PcpPaysysStructure.FIELD_REGION_ID, REGION_RUSSIA,
                PcpPaysysStructure.FIELD_RESIDENT, 1
        ));

        final var physSngCardPaysys = new PcpPaysysStructure(Map.of(
                PcpPaysysStructure.FIELD_ID, 1075L,
                PcpPaysysStructure.FIELD_CC, "cc_yt_rur",
                PcpPaysysStructure.FIELD_NAME, "Кредитной картой для нерезидентов РФ - рубли",
                PcpPaysysStructure.FIELD_CURRENCY, "RUB",
                PcpPaysysStructure.FIELD_PAYMENT_LIMIT, "100000",
                PcpPaysysStructure.FIELD_LEGAL_ENTITY, 0,
                PcpPaysysStructure.FIELD_PAYMENT_METHOD_CODE, "card",
                PcpPaysysStructure.FIELD_REGION_ID, REGION_SWITZERLAND,
                PcpPaysysStructure.FIELD_RESIDENT, 0
        ));

        final var physNonSwUsdCardPaysys = new PcpPaysysStructure(Map.of(
                PcpPaysysStructure.FIELD_ID, 1076L,
                PcpPaysysStructure.FIELD_CC, "cc_yt_usd",
                PcpPaysysStructure.FIELD_NAME, "Кредитной картой для нерезидентов Швейцарии - доллары",
                PcpPaysysStructure.FIELD_CURRENCY, "USD",
                PcpPaysysStructure.FIELD_PAYMENT_LIMIT, "3500",
                PcpPaysysStructure.FIELD_LEGAL_ENTITY, 0,
                PcpPaysysStructure.FIELD_PAYMENT_METHOD_CODE, "card",
                PcpPaysysStructure.FIELD_REGION_ID, REGION_SWITZERLAND,
                PcpPaysysStructure.FIELD_RESIDENT, 0
        ));

        final var physNonSwUsdBankPaysys = new PcpPaysysStructure(Map.of(
                PcpPaysysStructure.FIELD_ID, 1070L,
                PcpPaysysStructure.FIELD_CC, "sw_ytph_usd",
                PcpPaysysStructure.FIELD_NAME, "Банк для физических лиц, доллары (нерезиденты, Швейцария)",
                PcpPaysysStructure.FIELD_CURRENCY, "USD",
                PcpPaysysStructure.FIELD_LEGAL_ENTITY, 0,
                PcpPaysysStructure.FIELD_PAYMENT_METHOD_CODE, "bank",
                PcpPaysysStructure.FIELD_REGION_ID, REGION_SWITZERLAND,
                PcpPaysysStructure.FIELD_RESIDENT, 0
        ));

        final var physNonSwEurCardPaysys = new PcpPaysysStructure(Map.of(
                PcpPaysysStructure.FIELD_ID, 1077L,
                PcpPaysysStructure.FIELD_CC, "cc_yt_eur",
                PcpPaysysStructure.FIELD_NAME, "Кредитной картой для нерезидентов Швейцарии - евро",
                PcpPaysysStructure.FIELD_CURRENCY, "EUR",
                PcpPaysysStructure.FIELD_PAYMENT_LIMIT, "2500",
                PcpPaysysStructure.FIELD_LEGAL_ENTITY, 0,
                PcpPaysysStructure.FIELD_PAYMENT_METHOD_CODE, "card",
                PcpPaysysStructure.FIELD_REGION_ID, REGION_SWITZERLAND,
                PcpPaysysStructure.FIELD_RESIDENT, 0
        ));

        final var physNonSwEurBankPaysys = new PcpPaysysStructure(Map.of(
                PcpPaysysStructure.FIELD_ID, 1069L,
                PcpPaysysStructure.FIELD_CC, "sw_ytph_eur",
                PcpPaysysStructure.FIELD_NAME, "Банк для физических лиц, евро (нерезиденты, Швейцария)",
                PcpPaysysStructure.FIELD_CURRENCY, "EUR",
                PcpPaysysStructure.FIELD_LEGAL_ENTITY, 0,
                PcpPaysysStructure.FIELD_PAYMENT_METHOD_CODE, "bank",
                PcpPaysysStructure.FIELD_REGION_ID, REGION_SWITZERLAND,
                PcpPaysysStructure.FIELD_RESIDENT, 0
        ));

        final var physNonSwChfCardPaysys = new PcpPaysysStructure(Map.of(
                PcpPaysysStructure.FIELD_ID, 1079L,
                PcpPaysysStructure.FIELD_CC, "cc_yt_chf",
                PcpPaysysStructure.FIELD_NAME, "Кредитной картой для нерезидентов Швейцарии - франки",
                PcpPaysysStructure.FIELD_CURRENCY, "CHF",
                PcpPaysysStructure.FIELD_PAYMENT_LIMIT, "3500",
                PcpPaysysStructure.FIELD_LEGAL_ENTITY, 0,
                PcpPaysysStructure.FIELD_PAYMENT_METHOD_CODE, "card",

                PcpPaysysStructure.FIELD_REGION_ID, REGION_SWITZERLAND,
                PcpPaysysStructure.FIELD_RESIDENT, 0
        ));

        final var physNonSwChfBankPaysys = new PcpPaysysStructure(Map.of(
                PcpPaysysStructure.FIELD_ID, 1071L,
                PcpPaysysStructure.FIELD_CC, "sw_ytph_chf",
                PcpPaysysStructure.FIELD_NAME, "Банк для физических лиц, франки (нерезиденты, Швейцария)",
                PcpPaysysStructure.FIELD_CURRENCY, "CHF",
                PcpPaysysStructure.FIELD_LEGAL_ENTITY, 0,
                PcpPaysysStructure.FIELD_PAYMENT_METHOD_CODE, "bank",
                PcpPaysysStructure.FIELD_REGION_ID, REGION_SWITZERLAND,
                PcpPaysysStructure.FIELD_RESIDENT, 0
        ));

        final var physSwUsdCardPaysys = new PcpPaysysStructure(Map.of(
                PcpPaysysStructure.FIELD_ID, 1080L,
                PcpPaysysStructure.FIELD_CC, "cc_sw_usd",
                PcpPaysysStructure.FIELD_NAME, "Кредитной картой для резидентов Швейцарии - доллары",
                PcpPaysysStructure.FIELD_CURRENCY, "USD",
                PcpPaysysStructure.FIELD_PAYMENT_LIMIT, "3500",
                PcpPaysysStructure.FIELD_LEGAL_ENTITY, 0,
                PcpPaysysStructure.FIELD_PAYMENT_METHOD_CODE, "card",
                PcpPaysysStructure.FIELD_REGION_ID, REGION_SWITZERLAND,
                PcpPaysysStructure.FIELD_RESIDENT, 1
        ));

        final var physSwUsdBankPaysys = new PcpPaysysStructure(Map.of(
                PcpPaysysStructure.FIELD_ID, 1067L,
                PcpPaysysStructure.FIELD_CC, "sw_ph_usd",
                PcpPaysysStructure.FIELD_NAME, "Банк для физических лиц, доллары (Швейцария)",
                PcpPaysysStructure.FIELD_CURRENCY, "USD",
                PcpPaysysStructure.FIELD_LEGAL_ENTITY, 0,
                PcpPaysysStructure.FIELD_PAYMENT_METHOD_CODE, "bank",
                PcpPaysysStructure.FIELD_REGION_ID, REGION_SWITZERLAND,
                PcpPaysysStructure.FIELD_RESIDENT, 1
        ));

        final var legalRusCardPaysys = new PcpPaysysStructure(Map.of(
                PcpPaysysStructure.FIELD_ID, 11101033L,
                PcpPaysysStructure.FIELD_CC, "cc_ur",
                PcpPaysysStructure.FIELD_NAME, "Кредитная карта (Юр. Лица)",
                PcpPaysysStructure.FIELD_CURRENCY, "RUB",
                PcpPaysysStructure.FIELD_PAYMENT_LIMIT, "49999.99",
                PcpPaysysStructure.FIELD_LEGAL_ENTITY, 1,
                PcpPaysysStructure.FIELD_PAYMENT_METHOD_CODE, "card",
                PcpPaysysStructure.FIELD_REGION_ID, REGION_RUSSIA,
                PcpPaysysStructure.FIELD_RESIDENT, 1
        ));

        final var legalRusBankPaysys = new PcpPaysysStructure(Map.of(
                PcpPaysysStructure.FIELD_ID, 11101003L,
                PcpPaysysStructure.FIELD_CC, "ur",
                PcpPaysysStructure.FIELD_NAME, "Банк для юридических лиц",
                PcpPaysysStructure.FIELD_CURRENCY, "RUB",
                PcpPaysysStructure.FIELD_LEGAL_ENTITY, 1,
                PcpPaysysStructure.FIELD_PAYMENT_METHOD_CODE, "bank",
                PcpPaysysStructure.FIELD_REGION_ID, REGION_RUSSIA,
                PcpPaysysStructure.FIELD_RESIDENT, 1
        ));

        final var physKztBankPaysys = new PcpPaysysStructure(Map.of(
                PcpPaysysStructure.FIELD_ID, 11101061L,
                PcpPaysysStructure.FIELD_CC, "yt_ur_kzt",
                PcpPaysysStructure.FIELD_NAME, "Банк для физических лиц без НДС (тенге)",
                PcpPaysysStructure.FIELD_CURRENCY, "KZT",
                PcpPaysysStructure.FIELD_LEGAL_ENTITY, 0,
                PcpPaysysStructure.FIELD_PAYMENT_METHOD_CODE, "bank",
                PcpPaysysStructure.FIELD_REGION_ID, REGION_RUSSIA,
                PcpPaysysStructure.FIELD_RESIDENT, 0
        ));

        final var legalKztBankPaysys = new PcpPaysysStructure(Map.of(
                PcpPaysysStructure.FIELD_ID, 11101060L,
                PcpPaysysStructure.FIELD_CC, "yt_ph_kzt",
                PcpPaysysStructure.FIELD_NAME, "Банк для юридических лиц без НДС (тенге)",
                PcpPaysysStructure.FIELD_CURRENCY, "KZT",
                PcpPaysysStructure.FIELD_LEGAL_ENTITY, 1,
                PcpPaysysStructure.FIELD_PAYMENT_METHOD_CODE, "bank",
                PcpPaysysStructure.FIELD_REGION_ID, REGION_RUSSIA,
                PcpPaysysStructure.FIELD_RESIDENT, 0
        ));

        final var pcpList = new ArrayList<PcpListStructure>();

        pcpList.add(new PcpListStructure(Map.of(
                PcpListStructure.FIELD_PERSON, new PcpPersonStructure(Map.of(
                        PcpPersonStructure.FIELD_LEGAL_ENTITY, 0,
                        PcpPersonStructure.FIELD_RESIDENT, 1,
                        PcpPersonStructure.FIELD_REGION_ID, REGION_RUSSIA,
                        PcpPersonStructure.FIELD_TYPE, TYPE_NATURAL_RUSSIA
                )),
                PcpListStructure.FIELD_PAYSYSES,
                List.of(physRusCardPaysys, physRusBankPaysys).toArray()
        )));

        pcpList.add(new PcpListStructure(Map.of(
                PcpListStructure.FIELD_PERSON, new PcpPersonStructure(Map.of(
                        PcpPersonStructure.FIELD_LEGAL_ENTITY, 1,
                        PcpPersonStructure.FIELD_RESIDENT, 1,
                        PcpPersonStructure.FIELD_REGION_ID, REGION_RUSSIA,
                        PcpPersonStructure.FIELD_TYPE, TYPE_LEGAL_RUSSIA
                )),
                PcpListStructure.FIELD_PAYSYSES, List.of(legalRusCardPaysys, legalRusBankPaysys).toArray()
        )));

        pcpList.add(new PcpListStructure(Map.of(
                PcpListStructure.FIELD_PERSON, new PcpPersonStructure(Map.of(
                        PcpPersonStructure.FIELD_LEGAL_ENTITY, 0,
                        PcpPersonStructure.FIELD_RESIDENT, 0,
                        PcpPersonStructure.FIELD_REGION_ID, REGION_RUSSIA,
                        PcpPersonStructure.FIELD_TYPE, TYPE_NATURAL_KAZAKHSTAN
                )),
                PcpListStructure.FIELD_PAYSYSES, List.of(physKztBankPaysys).toArray()
        )));

        pcpList.add(new PcpListStructure(Map.of(
                PcpListStructure.FIELD_PERSON, new PcpPersonStructure(Map.of(
                        PcpPersonStructure.FIELD_LEGAL_ENTITY, 1,
                        PcpPersonStructure.FIELD_RESIDENT, 0,
                        PcpPersonStructure.FIELD_REGION_ID, REGION_RUSSIA,
                        PcpPersonStructure.FIELD_TYPE, TYPE_LEGAL_KAZAKHSTAN
                )),
                PcpListStructure.FIELD_PAYSYSES, List.of(legalKztBankPaysys).toArray()
        )));

        if (TYPE_NATURAL_RUSSIA.equals(personType)) {

            pcpList.add(new PcpListStructure(Map.of(
                    PcpListStructure.FIELD_PERSON, new PcpPersonStructure(Map.of(
                            PcpPersonStructure.FIELD_ID, 111L,
                            PcpPersonStructure.FIELD_NAME, "Vasily Pupkin",
                            PcpPersonStructure.FIELD_LEGAL_ENTITY, 0,
                            PcpPersonStructure.FIELD_RESIDENT, 1,
                            PcpPersonStructure.FIELD_REGION_ID, REGION_RUSSIA,
                            PcpPersonStructure.FIELD_TYPE, TYPE_NATURAL_RUSSIA
                    )),

                    PcpListStructure.FIELD_PAYSYSES, List.of(physRusCardPaysys, physRusBankPaysys).toArray()
            )));

            pcpList.add(new PcpListStructure(Map.of(
                    PcpListStructure.FIELD_PERSON, new PcpPersonStructure(Map.of(
                            PcpPersonStructure.FIELD_ID, 112L,
                            PcpPersonStructure.FIELD_NAME, "Vasily Pupkin2",
                            PcpPersonStructure.FIELD_LEGAL_ENTITY, 0,
                            PcpPersonStructure.FIELD_RESIDENT, 1,
                            PcpPersonStructure.FIELD_REGION_ID, REGION_RUSSIA,
                            PcpPersonStructure.FIELD_TYPE, TYPE_NATURAL_RUSSIA
                    )),

                    PcpListStructure.FIELD_PAYSYSES, List.of(physRusCardPaysys, physRusBankPaysys).toArray()
            )));
        } else if (TYPE_NATURAL_SNG.equals(personType)) {
            pcpList.add(new PcpListStructure(Map.of(
                    PcpListStructure.FIELD_PERSON, new PcpPersonStructure(Map.of(
                            PcpPersonStructure.FIELD_ID, 111L,
                            PcpPersonStructure.FIELD_NAME, "Vasily Pupkin",
                            PcpPersonStructure.FIELD_LEGAL_ENTITY, 0,
                            PcpPersonStructure.FIELD_RESIDENT, 0,
                            PcpPersonStructure.FIELD_REGION_ID, REGION_SWITZERLAND,
                            PcpPersonStructure.FIELD_TYPE, TYPE_NATURAL_SNG
                    )),

                    PcpListStructure.FIELD_PAYSYSES, List.of(physSngCardPaysys).toArray()
            )));
        } else if (TYPE_NATURAL_SWITZERLAND_NON_RESIDENT.equals(personType)) {
            pcpList.add(new PcpListStructure(Map.of(
                    PcpListStructure.FIELD_PERSON, new PcpPersonStructure(Map.of(
                            PcpPersonStructure.FIELD_ID, 111L,
                            PcpPersonStructure.FIELD_NAME, "Vasily Pupkin",
                            PcpPersonStructure.FIELD_LEGAL_ENTITY, 0,
                            PcpPersonStructure.FIELD_RESIDENT, 0,
                            PcpPersonStructure.FIELD_REGION_ID, REGION_SWITZERLAND,
                            PcpPersonStructure.FIELD_TYPE, TYPE_NATURAL_SWITZERLAND_NON_RESIDENT
                    )),

                    PcpListStructure.FIELD_PAYSYSES,
                    List.of(
                            physNonSwUsdCardPaysys,
                            physNonSwUsdBankPaysys,
                            physNonSwEurCardPaysys,
                            physNonSwEurBankPaysys,
                            physNonSwChfCardPaysys,
                            physNonSwChfBankPaysys
                    ).toArray()
            )));
        } else if (TYPE_NATURAL_SWITZERLAND_RESIDENT.equals(personType)) {
            pcpList.add(new PcpListStructure(Map.of(
                    PcpListStructure.FIELD_PERSON, new PcpPersonStructure(Map.of(
                            PcpPersonStructure.FIELD_ID, 111L,
                            PcpPersonStructure.FIELD_NAME, "Vasily Pupkin",
                            PcpPersonStructure.FIELD_LEGAL_ENTITY, 0,
                            PcpPersonStructure.FIELD_RESIDENT, 1,
                            PcpPersonStructure.FIELD_REGION_ID, REGION_SWITZERLAND,
                            PcpPersonStructure.FIELD_TYPE, TYPE_NATURAL_SWITZERLAND_RESIDENT
                    )),

                    PcpListStructure.FIELD_PAYSYSES, List.of(physSwUsdCardPaysys, physSwUsdBankPaysys).toArray()
            )));
        }

        final var paysysList = Stream
                .of(physRusBankPaysys, physRusCardPaysys, legalRusBankPaysys, legalRusCardPaysys)
                .map(paysys -> paysys.entrySet()
                        .stream()
                        .filter(entry -> BRIEF_PAYSYS_FIELDS.contains(entry.getKey()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                )
                .toArray();

        final OverdraftInfoStructure overdrafts = new OverdraftInfoStructure(Map.of(
                OverdraftInfoStructure.FIELD_IS_AVAILABLE, true));

        return new ResponseChoicesStructure(Map.of(
                ResponseChoicesStructure.FIELD_PCP_LIST, pcpList.toArray(),
                ResponseChoicesStructure.FIELD_PAYSYS_LIST, paysysList,
                ResponseChoicesStructure.FIELD_OVERDRAFTS, overdrafts
        ));
    }

    private static PayRequestResultStructure payRequestResultStructure(final boolean withBackUrl) {
        final var structure = new HashMap<>(Map.of(
                PayRequestResultStructure.FIELD_PAYMENT_URL, MOCKED_TRUST_URL,
                PayRequestResultStructure.FIELD_REDIRECT_URL, MOCKED_PARTNER_URL,
                PayRequestResultStructure.FIELD_CURRENCY, "RUB",
                PayRequestResultStructure.FIELD_AMOUNT, 600.0,
                PayRequestResultStructure.FIELD_REQUEST_ID, REQUEST_ID,
                PayRequestResultStructure.FIELD_INVOICE_ID, INVOICE_ID
        ));

        if (withBackUrl) {
            structure.put(PayRequestResultStructure.FIELD_REDIRECT_URL, MOCKED_PARTNER_URL);
        }

        return new PayRequestResultStructure(structure);
    }
}
