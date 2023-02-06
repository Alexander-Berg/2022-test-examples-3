package ru.yandex.market.partner.mvc.controller.billing;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.common.util.application.EnvironmentType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.model.ClientInfo;
import ru.yandex.market.common.balance.model.ClientType;
import ru.yandex.market.common.balance.xmlrpc.model.CardBindingURLStructure;
import ru.yandex.market.common.balance.xmlrpc.model.OrderRequest2Result;
import ru.yandex.market.common.balance.xmlrpc.model.PaymentMethodInfoStructure;
import ru.yandex.market.common.balance.xmlrpc.model.RequestPaymentMethodStructure;
import ru.yandex.market.mbi.environment.TestEnvironmentService;
import ru.yandex.market.partner.billing.dto.AutoPaymentSettingsDto;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.common.balance.xmlrpc.model.PaymentMethodInfoStructure.FIELD_ACCOUNT;
import static ru.yandex.market.common.balance.xmlrpc.model.PaymentMethodInfoStructure.FIELD_CARD_ID;
import static ru.yandex.market.common.balance.xmlrpc.model.PaymentMethodInfoStructure.FIELD_MAX_AMOUNT;
import static ru.yandex.market.common.balance.xmlrpc.model.RequestPaymentMethodStructure.FIELD_CURRENCY;
import static ru.yandex.market.common.balance.xmlrpc.model.RequestPaymentMethodStructure.FIELD_LEGAL_ENTITY;
import static ru.yandex.market.common.balance.xmlrpc.model.RequestPaymentMethodStructure.FIELD_PAYMENT_METHOD_ID;
import static ru.yandex.market.common.balance.xmlrpc.model.RequestPaymentMethodStructure.FIELD_PAYMENT_METHOD_INFO;
import static ru.yandex.market.common.balance.xmlrpc.model.RequestPaymentMethodStructure.FIELD_PAYMENT_METHOD_TYPE;
import static ru.yandex.market.common.balance.xmlrpc.model.RequestPaymentMethodStructure.FIELD_PERSON_ID;
import static ru.yandex.market.common.balance.xmlrpc.model.RequestPaymentMethodStructure.FIELD_PERSON_NAME;
import static ru.yandex.market.common.balance.xmlrpc.model.RequestPaymentMethodStructure.FIELD_RESIDENT;
import static ru.yandex.market.common.balance.xmlrpc.model.RequestPaymentMethodStructure.PAYMENT_METHOD_TYPE_CARD;

/**
 * Тесты {@link AutoPaymentController}.
 */
@ParametersAreNonnullByDefault
@DbUnitDataSet(before = "csv/AutoPaymentControllerTest.before.csv")
public class AutoPaymentControllerTest extends FunctionalTest {

    @Autowired
    @Qualifier("patientBalanceService")
    BalanceService balanceService;

    @Autowired
    TestEnvironmentService environmentService;

    private static final long UID = 887308675L;
    private static final int PERSON_ID = 10813395;
    private static final String PERSON_NAME = "Платежеспособный Кирилл";
    private static final long CLIENT_ID = 325076L;
    private static final long REQUEST_ID = 1613L;
    private static final String CARD_ID = "card-xe700fc1e77eff3e4828d94e7";
    private static final String ALIAS = "card-x2080";
    private static final String CARD_MASK = "510000****3425";
    private static final String CURRENCY = "RUB";
    private static final long CAMPAIGN_ID = 10774L;
    private static final String BINDING_URL =
            "https://trust-test.yandex.ru/web/binding?purchase_token=7886ef9613eb2c66a794e29f0b61eb8a";

    @BeforeEach
    void init() {
        environmentService.setEnvironmentType(EnvironmentType.TESTING);
        Mockito.when(balanceService.getRequestPaymentMethods(anyLong(), anyLong(), any()))
                .thenReturn(getRequestPaymentMethods());
        when(balanceService.createRequest2(eq(CLIENT_ID), any(), eq(UID)))
                .thenReturn(new OrderRequest2Result(Map.of(OrderRequest2Result.FIELD_REQUEST_ID, REQUEST_ID)));

        final CardBindingURLStructure cardBindingURL = new CardBindingURLStructure();
        cardBindingURL.setBindingUrl(BINDING_URL);
        cardBindingURL.setPurchaseToken("tkn");
        Mockito.when(balanceService.getCardBindingUrl(ArgumentMatchers.eq(UID), ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString()))
                .thenReturn(cardBindingURL);
        Mockito.when(balanceService.getClients(ArgumentMatchers.eq(List.of(CLIENT_ID))))
                .thenReturn(Map.of(CLIENT_ID, new ClientInfo(CLIENT_ID, ClientType.PHYSICAL)));
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("environment");
    }

    private static List<RequestPaymentMethodStructure> getRequestPaymentMethods() {
        return List.of(new RequestPaymentMethodStructure(Map.of(
                FIELD_PAYMENT_METHOD_INFO, new PaymentMethodInfoStructure(Map.of(
                        FIELD_CARD_ID, CARD_ID,
                        FIELD_ACCOUNT, CARD_MASK,
                        FIELD_MAX_AMOUNT, 50000
                )),
                FIELD_CURRENCY, CURRENCY,
                FIELD_CARD_ID, ALIAS,
                FIELD_LEGAL_ENTITY, 0,
                FIELD_RESIDENT, 1,
                FIELD_PERSON_NAME, "Cherdakov",
                FIELD_PERSON_ID, PERSON_ID,
                FIELD_PAYMENT_METHOD_ID, CARD_ID,
                FIELD_PAYMENT_METHOD_TYPE, PAYMENT_METHOD_TYPE_CARD)));
    }

    @Test
    @DisplayName("Получение списка способов оплаты доступных для подключения авто пополнения")
    void getAutoPaymentMethodsTest() {
        final var url = UriComponentsBuilder.fromUriString(baseUrl + "/auto-payment/methods")
                .queryParam("_user_id", UID)
                .queryParam("campaign_id", CAMPAIGN_ID)
                .queryParam("format", "json")
                .build().toString();
        final ResponseEntity<String> entity = FunctionalTestHelper.get(url);
        final InputStream expected = this.getClass()
                .getResourceAsStream("json/AutoPaymentControllerTest.getAutoPaymentMethodsTest.json");
        JsonTestUtil.assertEquals(entity, expected);
    }

    @Test
    @DisplayName("Получение ссылки для привязки банковской карты на маркет")
    void getCardBindingUrlTest() {
        final var url = UriComponentsBuilder.fromUriString(baseUrl + "/auto-payment/bind-card")
                .queryParam("_user_id", UID)
                .queryParam("return_path", "https://ya.ru")
                .queryParam("currency", "RUB")
                .queryParam("format", "json")
                .build().toString();
        JsonTestUtil.assertEquals(FunctionalTestHelper.get(url), this.getClass(),
                "json/AutoPaymentControllerTest.getCardBindingUrlTest.json");
    }

    @Test
    @DisplayName("Получение настроек авто пополнения, у магазина они есть")
    @DbUnitDataSet(before = "csv/AutoPaymentControllerTest.getAutoPaymentSettingsTest.before.csv")
    void getAutoPaymentSettingsTest() {
        JsonTestUtil.assertEquals(getAutoPaymentSettings(CAMPAIGN_ID), this.getClass(),
                "json/AutoPaymentControllerTest.getAutoPaymentSettingsTest.json");
    }


    @Test
    @DisplayName("Получение настроек авто пополнения, их нет у магазина")
    void getAutoPaymentNoSettingsTest() {
        JsonTestUtil.assertEquals(getAutoPaymentSettings(CAMPAIGN_ID), new JsonArray());
    }

    private ResponseEntity<String> getAutoPaymentSettings(long campaignId) {
        final var url = UriComponentsBuilder.fromUriString(baseUrl + "/auto-payment/settings-batch")
                .queryParam("_user_id", UID)
                .queryParam("campaign_id", campaignId)
                .queryParam("format", "json")
                .build().toString();
        return FunctionalTestHelper.get(url);
    }

    @Test
    @DisplayName("Задать настройки авто пополнения, впервые")
    @DbUnitDataSet(after = "csv/AutoPaymentControllerTest.setAutoPaymentSettingsTest.after.csv")
    void setAutoPaymentSettingsTest() throws JsonProcessingException {
        setAutoPaymentSettings();
    }

    @Test
    @DisplayName("Обновить уже имеющиеся настройки авто пополнения")
    @DbUnitDataSet(before = "csv/AutoPaymentControllerTest.updateAutoPaymentSettingsTest.before.csv",
            after = "csv/AutoPaymentControllerTest.updateAutoPaymentSettingsTest.after.csv")
    void updateAutoPaymentSettingsTest() throws JsonProcessingException {
        setAutoPaymentSettings();
    }

    private void setAutoPaymentSettings() throws JsonProcessingException {
        final var url = UriComponentsBuilder.fromUriString(baseUrl + "/auto-payment/settings")
                .queryParam("_user_id", UID)
                .queryParam("campaign_id", CAMPAIGN_ID)
                .build().toString();
        final String body = new ObjectMapper().writeValueAsString(getTestSettings());
        final ResponseEntity<String> response = FunctionalTestHelper.post(url, body);
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    private static AutoPaymentSettingsDto getTestSettings() {
        return AutoPaymentSettingsDto.builder()
                .campaignId(CAMPAIGN_ID)
                .payerUid(UID)
                .personId(PERSON_ID)
                .personName(PERSON_NAME)
                .enabled(true)
                .currency("RUB")
                .remainingSum(100)
                .paymentSum(1000)
                .paymentMethodType("card")
                .paymentMethodLabel("510000****3425")
                .paymentMethodId("card-xe700fc1e77eff3e4828d94e7")
                .build();
    }

}
