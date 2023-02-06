package ru.yandex.market.partner.mvc.controller.feature;

import java.util.Optional;
import java.util.stream.Stream;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.client.CheckouterShopApi;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.matchers.HttpClientErrorMatcher;
import ru.yandex.market.core.message.PartnerNotificationMessageServiceTest;
import ru.yandex.market.id.MarketIdServiceGrpc;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.nesu.client.NesuClient;
import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.partner.notification.client.model.WebUINotificationResponse;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Функциональные тесты на {@link FeatureController}.
 *
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
@DbUnitDataSet(before = "testRestrictedCutoffClose.before.csv")
class PartnerFeatureControllerTest extends FunctionalTest {
    private static final int USER_ID = 10;
    @Autowired
    private LMSClient lmsClient;
    @Autowired
    private CheckouterClient checkouterClient;

    private final CheckouterShopApi shopApi = mock(CheckouterShopApi.class);

    @Autowired
    private NesuClient nesuClient;

    @Autowired
    private MarketIdServiceGrpc.MarketIdServiceImplBase marketIdServiceImplBase;

    static Stream<Arguments> shopFeatures() {
        long supplierId = 10103L;
        return Stream.of(
                //не должно включиться для поставщика
                Arguments.of(FeatureType.CPA_20, supplierId),
                Arguments.of(FeatureType.PROMO_CPC, supplierId),
                Arguments.of(FeatureType.SUBSIDIES, supplierId)
        );
    }

    @BeforeEach
    void initTest() {
        when(checkouterClient.shops()).thenReturn(shopApi);
        PartnerNotificationMessageServiceTest.mockPN(
                partnerNotificationClient,
                new WebUINotificationResponse()
                        .subject("subj1")
                        .body("body1")
                        .priority(2L)
                        .groupId(3L),
                new WebUINotificationResponse()
                        .subject("subj1")
                        .body("body1")
                        .priority(2L)
                        .groupId(7L)
        );
    }

    @Test
    void testRestrictedCutoffClose() {
        HttpClientErrorException e = assertThrows(HttpClientErrorException.class,
                () -> enableFeature(101, FeatureType.SUBSIDIES.getId()));
        assertThat(e.getResponseBodyAsString(), MbiMatchers.jsonPath("$.errors[0].messageCode", "feature-not" +
                "-available"));
    }

    @Test
    void testDropshipEnableFeature() {
        when(lmsClient.getPartner(eq(1002L))).thenReturn(
                Optional.of(EnhancedRandom.random(PartnerResponse.PartnerResponseBuilder.class)
                        .id(1002)
                        .status(PartnerStatus.INACTIVE)
                        .build()));
        ResponseEntity<String> response = enableFeature(10100, FeatureType.DROPSHIP.getId());
        String expected = "{\"shop-id\":100,\"feature-id\":\"112\",\"status\":\"NEW\",\"feature-name\":\"DROPSHIP\"," +
                "\"cutoffs\":[{\"type\":\"TESTING\"}]}";
        JsonTestUtil.assertEquals(response, expected);
        verify(lmsClient).changePartnerStatus(eq(1002L), eq(PartnerStatus.TESTING));
    }

    @Test
    void testDropshipEnableNotAllowedFeature() {
        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class,
                () -> enableFeature(10104, FeatureType.DROPSHIP.getId()));

        assertThat(
                ex,
                Matchers.allOf(
                        HttpClientErrorMatcher.hasErrorCode(HttpStatus.UNPROCESSABLE_ENTITY),
                        HttpClientErrorMatcher.bodyMatches(
                                MbiMatchers.jsonPropertyMatches(
                                        "errors",
                                        MbiMatchers.jsonArrayEquals(
                                                /*language=json*/ "" +
                                                        "{\"messageCode\":\"preconditions-failed\"," +
                                                        "\"statusCode\":422," +
                                                        "\"args\":[\"dropship-enabled\",\"api-not-ready\"]}"
                                        )
                                )
                        )
                )
        );
    }

    @Test
    void testDropshipDisableFeature() {
        when(lmsClient.getPartner(eq(1001L))).thenReturn(
                Optional.of(EnhancedRandom.random(PartnerResponse.PartnerResponseBuilder.class)
                        .id(1001)
                        .status(PartnerStatus.ACTIVE)
                        .build()));
        ResponseEntity<String> response = disableFeature(10102, FeatureType.DROPSHIP.getId());
        String expected = "{\"shop-id\":102,\"feature-id\":\"112\",\"status\":\"DONT_WANT\",\"can-enable\":true," +
                "\"feature-name\":\"DROPSHIP\",\"cutoffs\":[{\"type\":\"PARTNER\"}]}";
        verify(lmsClient).changePartnerStatus(eq(1001L), eq(PartnerStatus.TESTING));
        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    @DbUnitDataSet(before = "testScreenshots.before.csv")
    void testScreenshots() {
        ResponseEntity<String> response = featureInfo(105, FeatureType.CASHBACK.getId());
        JsonTestUtil.assertEquals(response, getClass(), "testScreenshots.json");
    }

    @Test
    void testNoScreenshots() {
        ResponseEntity<String> response = featureInfo(105, FeatureType.CASHBACK.getId());
        JsonTestUtil.assertEquals(response, getClass(), "testNoScreenshots.json");
    }

    @Test
    void testDropshipFeatureInfo() {
        ResponseEntity<String> response = featureInfo(10101, FeatureType.DROPSHIP.getId());
        String expected = "{\"shop-id\":101,\"feature-id\":\"112\",\"status\":\"DONT_WANT\",\"can-enable\":true," +
                "\"feature-name\":\"DROPSHIP\"}";
        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    void testDropshipFeatureInfoWithoutParam() {
        ResponseEntity<String> response = featureInfo(10105, FeatureType.DROPSHIP.getId());
        String expected = /*language=json*/
                "{\"shop-id\":105,\"feature-id\":\"112\",\"feature-name\":\"DROPSHIP\"," +
                        "\"status\":\"DONT_WANT\",\"can-enable\":false,\"failed-precondition\":[\"dropship-enabled\"," +
                        "\"api-not-ready\"]," +
                        "\"cutoffs\":[{\"type\":\"PARTNER\"}]}";
        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    void testDropshipFeatureInfoShop() {
        ResponseEntity<String> response = featureInfo(10106, FeatureType.DROPSHIP.getId());
        String expected = /*language=json*/
                "{\"shop-id\":106,\"feature-id\":\"112\",\"feature-name\":\"DROPSHIP\"," +
                        "\"status\":\"DONT_WANT\",\"can-enable\":false,\"failed-precondition\":[\"dropship-enabled\"," +
                        "\"api-not-ready\",\"dropship-has-not-fulfilment\"]," +
                        "\"cutoffs\":[{\"type\":\"PARTNER\"}]}";
        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    void testDropshipFeatureInfoNotAllowed() {
        ResponseEntity<String> response = featureInfo(10104, FeatureType.DROPSHIP.getId());
        String expected = /*language=json*/
                "{\"shop-id\":104,\"feature-id\":\"112\",\"feature-name\":\"DROPSHIP\"," +
                        "\"status\":\"DONT_WANT\",\"can-enable\":false,\"failed-precondition\":[\"dropship-enabled\"," +
                        "\"api-not-ready\"],\"cutoffs\":[{\"type\":\"PARTNER\"}]}";
        JsonTestUtil.assertEquals(response, expected);
    }

    /**
     * Проверяем, что нельзя включить субсидии поставщикам и DROPSHIP магазинам.
     */
    @ParameterizedTest
    @MethodSource("shopFeatures")
    void testEnableSubsidiesForSupplierFeature(FeatureType featureType, long campaignId) {
        HttpClientErrorException response = assertThrows(
                HttpClientErrorException.class,
                () -> enableFeature(campaignId, featureType.getId())
        );

        JsonElement jsonElement = JsonTestUtil.parseJson(response.getResponseBodyAsString());

        JsonElement firstError = jsonElement.getAsJsonObject().getAsJsonArray("errors").get(0);
        JsonObject jsonObject = firstError.getAsJsonObject();
        assertEquals("preconditions-failed", jsonObject.getAsJsonPrimitive("messageCode").getAsString());
        assertEquals("not-allowed-partner-type", jsonObject.getAsJsonArray("args").get(0).getAsString());
    }

    @Test
    @DisplayName("Открытие фичи MARKETPLACE_SELF_DELIVERY для магазина")
    void testShowFeatureMarketplaceSelfDelivery() {
        ResponseEntity<String> response = featureInfo(101L, FeatureType.MARKETPLACE_SELF_DELIVERY.getId());
        // language=json
        String expected = "{\"shop-id\":1,\"feature-id\":\"1015\",\"feature-name\":\"MARKETPLACE_SELF_DELIVERY\"," +
                "\"status\":\"DONT_WANT\",\"can-enable\":false," +
                "\"failed-precondition\":[\"delivery-not-configured\"],\"cutoffs\":[{\"type\":\"PARTNER\"}]}";
        JsonTestUtil.assertEquals(response, expected);
        verify(nesuClient, never()).registerShop(any());
        verify(lmsClient, never()).updatePartnerSettings(anyLong(), any());
        verify(marketIdServiceImplBase, never()).getByPartner(any(), any());
    }

    @Test
    @DisplayName("Проверка снятия катоффа MARKETPLACE_ORDER_NOT_ACCEPTED на фиче в SUCCESS")
    @DbUnitDataSet(
            before = "PartnerFeatureControllerTest.testCloseManualCutoffWithSuccessFeature.before.csv",
            after = "PartnerFeatureControllerTest.testCloseManualCutoffWithSuccessFeature.after.csv"
    )
    void testCloseManualCutoffWithSuccessFeature() {
        ResponseEntity<String> response = enableFeature(999, FeatureType.DROPSHIP.getId());
        //language=json
        String expected = "" +
                "{\"shop-id\":999,\"feature-id\":\"112\",\"feature-name\":\"DROPSHIP\",\"status\":\"SUCCESS\"}";
        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    @DisplayName("Проверка возвращаемого катофа MARKETPLACE_ORDER_NOT_ACCEPTED в /featureInfo")
    @DbUnitDataSet(before = "PartnerFeatureControllerTest.testCloseManualCutoffWithSuccessFeature.before.csv")
    void testOrderNotAcceptedCutoffFeatureInfo() {
        ResponseEntity<String> response = featureInfo(999, FeatureType.DROPSHIP.getId());
        //language=json
        String expected = "" +
                "{\n" +
                "  \"shop-id\": 999,\n" +
                "  \"feature-id\": \"112\",\n" +
                "  \"feature-name\": \"DROPSHIP\",\n" +
                "  \"status\": \"SUCCESS\",\n" +
                "  \"cutoffs\": [\n" +
                "    {\n" +
                "      \"type\": \"MARKETPLACE_ORDER_NOT_ACCEPTED\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    @DisplayName("Включение/отключение фичи ORDER_AUTO_ACCEPT")
    void testEnableFeatureOrderAutoAccept() {
        ResponseEntity<String> response = enableFeature(10102, FeatureType.ORDER_AUTO_ACCEPT.getId());
        //language=json
        String expected = "" +
                "{\n" +
                "  \"shop-id\": 102,\n" +
                "  \"feature-id\": \"1018\",\n" +
                "  \"feature-name\": \"ORDER_AUTO_ACCEPT\",\n" +
                "  \"status\": \"SUCCESS\"\n" +
                "}";
        JsonTestUtil.assertEquals(response, expected);
        response = disableFeature(10102, FeatureType.ORDER_AUTO_ACCEPT.getId());
        //language=json
        expected = "{\n" +
                "  \"shop-id\": 102,\n" +
                "  \"feature-id\": \"1018\",\n" +
                "  \"feature-name\": \"ORDER_AUTO_ACCEPT\",\n" +
                "  \"status\": \"DONT_WANT\",\n" +
                "  \"can-enable\": true,\n" +
                "  \"cutoffs\": [\n" +
                "    {\n" +
                "      \"type\": \"PARTNER\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    @DisplayName("Проверка включения фичи DROPSHIP в эксперименте КЗ")
    @DbUnitDataSet(
            before = "PartnerFeatureControllerTest.testSuccessFeatureExperiment.before.csv"
    )
    void testSuccessFeatureExperiment() {
        when(lmsClient.getPartner(eq(99991L))).thenReturn(
                Optional.of(EnhancedRandom.random(PartnerResponse.PartnerResponseBuilder.class)
                        .id(99991L)
                        .status(PartnerStatus.INACTIVE)
                        .build()));
        ResponseEntity<String> response = successFeature(19990, FeatureType.DROPSHIP.getId());
        //language=json
        String expected = "" +
                "{" +
                "\"shop-id\":9990," +
                "\"feature-id\":\"112\"," +
                "\"feature-name\":\"DROPSHIP\"," +
                "\"status\":\"SUCCESS\"," +
                "\"cutoffs\":[{\"type\": \"EXPERIMENT\"}]" +
                "}";
        JsonTestUtil.assertEquals(response, expected);
        verify(lmsClient).changePartnerStatus(eq(99991L), eq(PartnerStatus.TESTING));
    }

    @Test
    @DisplayName("Проверка включения фичи DROPSHIP в эксперименте КЗ когда поставщик не попал в эксперимент")
    void testSuccessFeatureNotExperiment() {
        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class,
                () -> successFeature(10104, FeatureType.DROPSHIP.getId()));

        assertThat(
                ex,
                Matchers.allOf(
                        HttpClientErrorMatcher.hasErrorCode(HttpStatus.UNPROCESSABLE_ENTITY),
                        HttpClientErrorMatcher.bodyMatches(
                                MbiMatchers.jsonPropertyMatches(
                                        "errors",
                                        MbiMatchers.jsonArrayEquals(
                                                /*language=json*/ "" +
                                                        "{\"messageCode\":\"not-experiment\"," +
                                                        "\"statusCode\":422}"
                                        )
                                )
                        )
                )
        );
    }

    @Test
    @DisplayName("Проверка включения фичи DROPSHIP в эксперименте КЗ когда предусловия не выполнены")
    @DbUnitDataSet(
            before = "PartnerFeatureControllerTest.testSuccessFeatureExperiment.before.csv"
    )
    void testSuccessFeatureFailedPreconditions() {
        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class,
                () -> successFeature(199990, FeatureType.DROPSHIP.getId()));
        String expected = /*language=json*/ "" +
                "{\"messageCode\":\"preconditions-failed\",\"statusCode\":422,\"args\":[\"api-not-ready\"," +
                "\"dropship-has-not-fulfilment\"]}";

        assertThat(
                ex,
                Matchers.allOf(
                        HttpClientErrorMatcher.hasErrorCode(HttpStatus.UNPROCESSABLE_ENTITY),
                        HttpClientErrorMatcher.bodyMatches(
                                MbiMatchers.jsonPropertyMatches(
                                        "errors",
                                        MbiMatchers.jsonArrayEquals(expected)
                                )
                        )
                )
        );
    }

    @Test
    @DisplayName("Проверка включения фичи DROPSHIP, минуя NEW")
    @DbUnitDataSet(
            before = "PartnerFeatureControllerTest.testSuccessDropshipFromDontWant.before.csv",
            after = "PartnerFeatureControllerTest.testSuccessDropshipFromDontWant.after.csv"
    )
    void testSuccessDropshipFeatureFromDontWant() {
        when(lmsClient.getPartner(eq(333L))).thenReturn(
                Optional.of(EnhancedRandom.random(PartnerResponse.PartnerResponseBuilder.class)
                        .id(333333L)
                        .status(PartnerStatus.INACTIVE)
                        .build()));
        successFeature(10333, FeatureType.DROPSHIP.getId());
    }

    @Test
    @DisplayName("Проверка включения фичи FULFILLMENT")
    @DbUnitDataSet(after = "PartnerFeatureControllerTest.testSuccessFulfillmentFromDontWant.after.csv")
    void testSuccessFulfillmentFeature() {
        successFeature(40001, FeatureType.FULFILLMENT.getId());
    }

    @Test
    @DisplayName("Проверка включения фичи FULFILLMENT AS A SERVICE")
    @DbUnitDataSet(after = "PartnerFeatureControllerTest.testFulfillmentAsAService.after.csv")
    void testSuccessFulfillmentAsAServiceFeature() {
        successFeature(40006, FeatureType.FULFILLMENT_AS_A_SERVICE.getId());
    }

    @Test
    @DisplayName("Проверка перевода фичи CIS в SUCCESS при вызове /enableFeature")
    @DbUnitDataSet(
            before = "PartnerFeatureControllerTest.testDbsCisListener.before.csv"
    )
    void testDbsCisListener() {
        ResponseEntity<String> entity = enableFeature(10036L, 1014L);
        String expected = /*language=json*/ "" +
                "{\"shop-id\":1036,\"feature-id\":\"1014\",\"feature-name\":\"CIS\",\"status\":\"SUCCESS\"}";

        JsonTestUtil.assertEquals(entity, expected);
    }

    @Test
    @DisplayName("Проверка включения фичи B2B_SELLER")
    @DbUnitDataSet(after = "PartnerFeatureControllerTest.testSuccessB2BSellerFromDontWant.after.csv")
    void testSuccessB2BSellerFeature() {
        successFeature(40006, FeatureType.B2B_SELLER.getId());
    }

    @Test
    @DisplayName("Проверка включения фичи B2C_SELLER из дефолтного SUCCESS")
    @DbUnitDataSet(after = "PartnerFeatureControllerTest.testSuccessB2CSellerFromDefaultSuccess.after.csv")
    void testSuccessB2CSellerFeature() {
        successFeature(40006, FeatureType.B2C_SELLER.getId());
    }

    @Test
    @DisplayName("Включение фичи B2B_SELLER")
    void testEnableFeatureB2BSeller() {
        ResponseEntity<String> response = enableFeature(40006, FeatureType.B2B_SELLER.getId());
        //language=json
        String expected = "" +
                "{\n" +
                "  \"shop-id\": 406,\n" +
                "  \"feature-id\": \"2006\",\n" +
                "  \"feature-name\": \"B2B_SELLER\",\n" +
                "  \"status\": \"SUCCESS\"\n" +
                "}";
        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    @DisplayName("Включение фичи B2C_SELLER из дефолтного SUCCESS")
    void testEnableFeatureB2CSeller() {
        ResponseEntity<String> response = enableFeature(40006, FeatureType.B2C_SELLER.getId());
        //language=json
        String expected = "" +
                "{\n" +
                "  \"shop-id\": 406,\n" +
                "  \"feature-id\": \"2008\",\n" +
                "  \"feature-name\": \"B2C_SELLER\",\n" +
                "  \"status\": \"SUCCESS\"\n" +
                "}";
        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    @DisplayName("Проверка выключения фичи B2B_SELLER")
    void testDisableFeatureB2BSeller() {
        ResponseEntity<String> response = enableFeature(40006, FeatureType.B2B_SELLER.getId());
        //language=json
        String expected = "" +
                "{\n" +
                "  \"shop-id\": 406,\n" +
                "  \"feature-id\": \"2006\",\n" +
                "  \"feature-name\": \"B2B_SELLER\",\n" +
                "  \"status\": \"SUCCESS\"\n" +
                "}";
        JsonTestUtil.assertEquals(response, expected);

        response = disableFeature(40006, FeatureType.B2B_SELLER.getId());
        //language=json
        expected = "" +
                "{\n" +
                "  \"shop-id\": 406,\n" +
                "  \"feature-id\": \"2006\",\n" +
                "  \"feature-name\": \"B2B_SELLER\",\n" +
                "  \"status\": \"DONT_WANT\",\n" +
                "\"can-enable\":true,\n" +
                "\"cutoffs\":[{\"type\":\"PARTNER\"}]\n" +
                "}";
        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    @DisplayName("Проверка выключения фичи B2C_SELLER")
    void testDisableFeatureB2CSeller() {
        ResponseEntity<String> response = disableFeature(40006, FeatureType.B2C_SELLER.getId());
        //language=json
        String expected = "" +
                "{\n" +
                "  \"shop-id\": 406,\n" +
                "  \"feature-id\": \"2008\",\n" +
                "  \"feature-name\": \"B2C_SELLER\",\n" +
                "  \"status\": \"DONT_WANT\",\n" +
                "\"can-enable\":true,\n" +
                "\"cutoffs\":[{\"type\":\"PARTNER\"}]\n" +
                "}";
        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    @DisplayName("Выключение отсутствующей фичи B2B_SELLER")
    void testDisableFeatureB2BSeller2() {
        assertThrows(HttpClientErrorException.BadRequest.class, () ->
                disableFeature(40006, FeatureType.B2B_SELLER.getId()));
    }

    @Test
    @DisplayName("Проверка выключения и последующего включения фичи B2C_SELLER")
    void testEnableAfterDisableFeatureB2CSeller() {
        ResponseEntity<String> response = disableFeature(40006, FeatureType.B2C_SELLER.getId());
        //language=json
        String expected = "" +
                "{\n" +
                "  \"shop-id\": 406,\n" +
                "  \"feature-id\": \"2008\",\n" +
                "  \"feature-name\": \"B2C_SELLER\",\n" +
                "  \"status\": \"DONT_WANT\",\n" +
                "\"can-enable\":true,\n" +
                "\"cutoffs\":[{\"type\":\"PARTNER\"}]\n" +
                "}";
        JsonTestUtil.assertEquals(response, expected);

        response = enableFeature(40006, FeatureType.B2C_SELLER.getId());
        //language=json
        expected = "" +
                "{\n" +
                "  \"shop-id\": 406,\n" +
                "  \"feature-id\": \"2008\",\n" +
                "  \"feature-name\": \"B2C_SELLER\",\n" +
                "  \"status\": \"SUCCESS\"\n" +
                "}";
        JsonTestUtil.assertEquals(response, expected);

    }

    @Test
    @DisplayName("Проверка включения DBS'а в статусе SUCCESS с MARKETPLACE_PLACEMENT")
    @DbUnitDataSet(before = "testEnableFeature.marketplaceSelfDelivery.before.csv")
    void testEnableDbsWithMarketplacePlacement() {
        ResponseEntity<String> response = enableFeature(70001, FeatureType.MARKETPLACE_SELF_DELIVERY.getId());
        String expected = "" +
                "{\n" +
                "  \"shop-id\": 701,\n" +
                "  \"feature-id\": \"1015\",\n" +
                "  \"feature-name\": \"MARKETPLACE_SELF_DELIVERY\",\n" +
                "  \"status\": \"SUCCESS\"\n" +
                "}";

        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    @DisplayName("Проверка включения DBS'а в статусе SUCCESS с DAILY_ORDER_LIMIT")
    @DbUnitDataSet(
            before = "testEnableFeature.marketplaceSelfDelivery.before.csv"
    )
    void testEnableDbsWithDailyOrderLimit() {
        ResponseEntity<String> response = enableFeature(70002, FeatureType.MARKETPLACE_SELF_DELIVERY.getId());
        String expected = "" +
                "{\n" +
                "  \"shop-id\": 702,\n" +
                "  \"feature-id\": \"1015\",\n" +
                "  \"feature-name\": \"MARKETPLACE_SELF_DELIVERY\",\n" +
                "  \"status\": \"SUCCESS\",\n" +
                "  \"cutoffs\":[{\"type\":\"DAILY_ORDER_LIMIT\"}]\n" +
                "}";

        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    @DisplayName("Проверка включения DBS'а в статусе SUCCESS без катофов")
    @DbUnitDataSet(
            before = "testEnableFeature.marketplaceSelfDelivery.before.csv"
    )
    void testEnableDbsWithoutCutoffs() {
        ResponseEntity<String> response = enableFeature(70003, FeatureType.MARKETPLACE_SELF_DELIVERY.getId());
        String expected = "" +
                "{\n" +
                "  \"shop-id\": 703,\n" +
                "  \"feature-id\": \"1015\",\n" +
                "  \"feature-name\": \"MARKETPLACE_SELF_DELIVERY\",\n" +
                "  \"status\": \"SUCCESS\"\n" +
                "}";

        JsonTestUtil.assertEquals(response, expected);
    }

    @Test
    @DisplayName("Проверка включения DBS в статусе FAIL")
    @DbUnitDataSet(before = "testEnableFeature.marketplaceSelfDelivery.before.csv")
    void testEnableFailDbsForbidden() {
        var exception = assertThrows(
                HttpServerErrorException.InternalServerError.class,
                () -> enableFeature(70004, FeatureType.MARKETPLACE_SELF_DELIVERY.getId())
        );

        assertThat(
                exception.getResponseBodyAsString(),
                MbiMatchers.jsonPath("$.errors[0].message",
                        "Can't enable feature for DBS. Feature should be enabled with moderation"));
    }

    private ResponseEntity<String> disableFeature(long campaignId, long featureId) {
        return FunctionalTestHelper.post(baseUrl + "/disableFeature?_user_id={userId}&id={campaignId}&feature-id" +
                        "={featureId}",
                null,
                USER_ID,
                campaignId,
                featureId
        );
    }

    private ResponseEntity<String> enableFeature(long campaignId, long featureId) {
        return FunctionalTestHelper.post(baseUrl + "/enableFeature?_user_id={userId}&id={campaignId}&feature-id" +
                        "={featureId}",
                null,
                USER_ID,
                campaignId,
                featureId
        );
    }

    private ResponseEntity<String> featureInfo(long campaignId, long featureId) {
        return FunctionalTestHelper.get(baseUrl + "/featureInfo?_user_id={userId}&id={campaignId}&feature-id" +
                        "={featureId}",
                USER_ID,
                campaignId,
                featureId
        );
    }

    private ResponseEntity<String> showFeature(long campaignId, long featureId) {
        return FunctionalTestHelper.post(baseUrl + "/show-feature?_user_id={userId}&id={campaignId}&feature-id" +
                        "={featureId}",
                null,
                USER_ID,
                campaignId,
                featureId
        );
    }

    private ResponseEntity<String> hideFeature(long campaignId, long featureId) {
        return FunctionalTestHelper.post(baseUrl + "/hide-feature?_user_id={userId}&id={campaignId}&feature-id" +
                        "={featureId}",
                null,
                USER_ID,
                campaignId,
                featureId
        );
    }

    private boolean getParam(long campaignId) {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/manageParam?id={campaignId}&format=json&type=CPA_IS_PARTNER_INTERFACE&value=1",
                campaignId);
        assertEquals(response.getStatusCode(), HttpStatus.OK);
        assertNotNull(response.getBody());
        return response.getBody().contains("\"param-value\":true");
    }

    private ResponseEntity<String> successFeature(long campaignId, long featureId) {
        return FunctionalTestHelper.post(baseUrl + "/successFeature?_user_id={userId}&id={campaignId}&feature-id" +
                        "={featureId}",
                null,
                USER_ID,
                campaignId,
                featureId
        );
    }
}
