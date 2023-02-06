package ru.yandex.market.partner.mvc.controller.shipping;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterShopApi;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.mockito.Mockito.when;

@DbUnitDataSet(before = "database/shipmentDateCalculationRule.before.csv")
public class ShipmentDateCalculationControllerTest extends FunctionalTest {

    @Autowired
    private CheckouterAPI checkouterClient;

    @Autowired
    private CheckouterShopApi checkouterShopApi;

    @BeforeEach
    public void setup() {
        when(checkouterClient.shops()).thenReturn(checkouterShopApi);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getArgumentsForGetSuccessfulTest")
    void testSuccessfulGet(String testDescription, long datasourceId, String expectedJsonFilePath) {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/partner/shipment/date-calculation-rule?datasourceId={id}",
                datasourceId
        );
        JsonTestUtil.assertEquals(response, this.getClass(), expectedJsonFilePath);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getArgumentsForFailureTest")
    void testErroneousGet(String testDescription, long datasourceId, HttpStatus expectedStatus) {
        HttpClientErrorException httpClientErrorException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(
                        baseUrl + "/partner/shipment/date-calculation-rule?datasourceId={id}",
                        datasourceId
                )
        );
        Assertions.assertEquals(expectedStatus, httpClientErrorException.getStatusCode());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getArgumentsForFailureTest")
    void testErroneousPatch(String testDescription, long datasourceId, HttpStatus expectedStatus) {
        HttpClientErrorException httpClientErrorException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.patch(
                        baseUrl + "/partner/shipment/date-calculation-rule?datasourceId={id}",
                        StringTestUtil.getString(this.getClass(), "json/getStoredCalculationRulesForShop1.json"),
                        datasourceId
                )
        );
        Assertions.assertEquals(expectedStatus, httpClientErrorException.getStatusCode());
    }

    @Test
    @DbUnitDataSet(after = "database/shipmentDateCalculationRule.patchExisting.all.after.csv")
    void testSuccessPatch_allFieldsUpdated() {
        FunctionalTestHelper.patch(
                baseUrl + "/partner/shipment/date-calculation-rule?datasourceId={id}",
                StringTestUtil.getString(this.getClass(), "json/updateForShop1_allFieldsUpdated.json"),
                1L
        );
    }

    @Test
    @DbUnitDataSet(after = "database/shipmentDateCalculationRule.patchExisting.partial.after.csv")
    void testSuccessPatch_onlyDaysInLocalRegionAndHourBeforeChanged() {
        FunctionalTestHelper.patch(
                baseUrl + "/partner/shipment/date-calculation-rule?datasourceId={id}",
                StringTestUtil.getString(this.getClass(), "json/updateForShop1_partialUpdate.json"),
                1L
        );
    }

    @Test
    @DbUnitDataSet(after = "database/shipmentDateCalculationRule.patchExisting.partial2.after.csv")
    void testSuccessPatch_onlyBaseForLocalShop2() {
        FunctionalTestHelper.patch(
                baseUrl + "/partner/shipment/date-calculation-rule?datasourceId={id}",
                StringTestUtil.getString(this.getClass(), "json/updateForShop2_partialUpdate.json"),
                2L
        );
    }

    private static Stream<Arguments> getArgumentsForGetSuccessfulTest() {
        return Stream.of(
                Arguments.of("Возвращение для сохраненных в базе правил", 1L, "json/getStoredCalculationRulesForShop1" +
                        ".json"),
                Arguments.of("Возвращение несохраненных в базе правил", 2L, "json/getStoredCalculationRulesForShop2" +
                        ".json")
        );
    }

    private static Stream<Arguments> getArgumentsForFailureTest() {
        return Stream.of(
                Arguments.of("Нет магазина", 10L, HttpStatus.NOT_FOUND),
                Arguments.of("Не ДСБС магазин", 3L, HttpStatus.FORBIDDEN)
        );
    }
}
