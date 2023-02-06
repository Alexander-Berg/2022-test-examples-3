package ru.yandex.market.deliverycalculator.indexer.controller;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.deliverycalculator.indexer.FunctionalTest;

import static org.junit.Assert.assertEquals;
import static ru.yandex.market.common.test.util.JsonTestUtil.parseJson;
import static ru.yandex.market.common.test.util.StringTestUtil.getString;

/**
 * Тест для {@link YaDeliverySenderSettingsController}.
 */
@DbUnitDataSet(before = "data/db/importSenderSettings.before.csv")
class YaDeliverySenderSettingsControllerTest extends FunctionalTest {

    /**
     * Тест на успешную запись нескольких магазинных настроек.
     */
    @DbUnitDataSet(after = "data/db/importSenderSettings.after.csv")
    @Test
    void testSuccessfulStoreSetting() {
        storeSettings("data/rest/storeSenderSettingsRequest.json");
    }

    /**
     * Тест на успешную запись нескольких магазинных настроек.
     * Данные о модификаторах не приходят, только даные о связанных службах доставки.
     */
    @DbUnitDataSet(after = "data/db/importSenderSettingsOnlyCarrierLink.after.csv")
    @Test
    void testSuccessfulStoreOnlyCarrierLinks() {
        storeSettings("data/rest/storeSenderSettingsNoModifiersRequest.json");
    }

    /**
     * Тест на успешную запись настроек магазина, с указанными средними габаритами оффера.
     */
    @DbUnitDataSet(after = "data/db/importSenderSettingsWithOfferWeightDimensions.after.csv")
    @Test
    void testSuccessfulStoreSettingWithAverageOfferDimensions() {
        storeSettings("data/rest/storeSenderSettingsWithOfferDimensions.json");
    }

    /**
     * Тест на успешную запись настроек магазина с указанием маппинга служб доставки на регионы.
     */
    @DbUnitDataSet(after = "data/db/importSenderSettingsSeveralRegions.after.csv")
    @Test
    @DisplayName("Указание соответствия СД к регионам")
    void testWithSeveralRegionMapping() {
        storeSettings("data/rest/storeSenderSettingsSeveralRegions.json");
    }

    /**
     * Тест на случай, когда зануляются настройки магазина.
     */
    @DbUnitDataSet(after = "data/db/importSenderNullifiedSettings.after.csv")
    @Test
    void testSenderWithoutSettings() {
        storeSettings("data/rest/nullifySenderSettingsRequest.json");
    }

    @DbUnitDataSet(after = "data/db/importSenderNullifiedSettings.after.csv")
    @Test
    @DisplayName("Пустое соответствие зануляет настройки")
    void testSenderWithEmptyMapping() {
        storeSettings("data/rest/nullifySenderSettingsMappingRequest.json");
    }

    /**
     * Тест на неправильный формат запроса.
     */
    @ParameterizedTest
    @MethodSource("invalidInput")
    @DisplayName("Валидация тела запроса")
    void testStoreSettings_invalidJson(String requestPath, String responsePath) {
        HttpClientErrorException exception = Assertions.assertThrows(
            HttpClientErrorException.class,
            () -> storeSettings(requestPath)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals(
            parseJson(getString(this.getClass(), responsePath)),
            parseJson(exception.getResponseBodyAsString())
        );
    }

    private static Stream<Arguments> invalidInput() {
        return Stream.of(
            Arguments.of(
                "data/rest/storeSenderSettingsInvalidRequest.json",
                "data/rest/storeSenderSettingsInvalidResponse.json"
            ),
            Arguments.of(
                "data/rest/storeSenderSettingsInvalidMappingRequest.json",
                "data/rest/storeSenderSettingsInvalidMappingResponse.json"
            )
        );
    }

    private void storeSettings(String pathToRequestBody) {
        REST_TEMPLATE.exchange(
            baseUrl + "/updateYaDeliveryShopRules",
            HttpMethod.POST,
            JsonTestUtil.getJsonHttpEntity(this.getClass(), pathToRequestBody),
            String.class
        );
    }
}
