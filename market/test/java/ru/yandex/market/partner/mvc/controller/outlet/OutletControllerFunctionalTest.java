package ru.yandex.market.partner.mvc.controller.outlet;

import java.util.Collections;

import com.google.common.collect.ImmutableList;
import org.hamcrest.MatcherAssert;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.matchers.HttpClientErrorMatcher;
import ru.yandex.market.core.outlet.OutletType;
import ru.yandex.market.partner.mvc.controller.outlet.model.ManageOutletBatchRequestDTO;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

/**
 * Тесты для {@link OutletController}.
 *
 * @author serenitas
 */
@DbUnitDataSet(before = "csv/OutletControllerFunctionalTest.before.csv")
class OutletControllerFunctionalTest extends FunctionalTest {

    public static final Logger log = LoggerFactory.getLogger(OutletControllerFunctionalTest.class);

    //UPDATE

    private static void checkResult(String response, int total, int notNeeded, int notAllowed) {
        String expectedStr = String.format("{\"total\":%d,\"notNeeded\":%d,\"notAllowed\":%d}",
                total, notNeeded, notAllowed);
        JSONAssert.assertEquals(expectedStr,
                new JSONObject(response).getJSONObject("result"),
                JSONCompareMode.LENIENT);
    }

    @Test
    @DisplayName("Успешное обновление нескольких параметров у нескольких аутлетов")
    @DbUnitDataSet(after = "csv/OutletControllerFunctionalTest.testCommonUpdate.after.csv")
    void testCommonUpdate() {
        ManageOutletBatchRequestDTO dto = new ManageOutletBatchRequestDTO();
        dto.setOutletIds(ImmutableList.of(1L, 2L, 4L));
        dto.setHidden(false);
        dto.setType(OutletType.MIXED);
        dto.setInverted(false);
        final ResponseEntity<String> response = sendRequestUpdate(dto);
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        checkResult(response.getBody(), 3, 0, 0);
    }

    @Test
    @DisplayName("Успешное обновление одного параметра у нескольких аутлетов")
    @DbUnitDataSet(after = "csv/OutletControllerFunctionalTest.testUpdateOneParameter.after.csv")
    void testUpdateOneParameter() {
        ManageOutletBatchRequestDTO dto = new ManageOutletBatchRequestDTO();
        dto.setOutletIds(ImmutableList.of(2L, 3L));
        dto.setType(OutletType.MIXED);
        dto.setInverted(false);
        final ResponseEntity<String> response = sendRequestUpdate(dto);
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        checkResult(response.getBody(), 2, 0, 0);
    }

    @Test
    @DisplayName("Обновление срока хранения у нескольких аутлетов")
    @DbUnitDataSet(after = "csv/OutletControllerFunctionalTest.testUpdateStoragePeriod.after.csv")
    void testUpdateStoragePeriod() {
        ManageOutletBatchRequestDTO dto = new ManageOutletBatchRequestDTO();
        dto.setOutletIds(ImmutableList.of(2L, 3L, 4L));
        dto.setStoragePeriod(200L);
        dto.setInverted(false);
        final ResponseEntity<String> response = sendRequestUpdate(dto);
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        checkResult(response.getBody(), 3, 0, 0);
    }

    @Test
    @DisplayName("Успешное выключение точки поставщика")
    @DbUnitDataSet(after = "csv/OutletControllerFunctionalTest.testBlueUpdate.after.csv")
    void blueTest() {
        ManageOutletBatchRequestDTO dto = new ManageOutletBatchRequestDTO();
        dto.setOutletIds(Collections.singletonList(8L));
        dto.setHidden(true);
        final String url = baseUrl + "/outlets?id=102";
        final ResponseEntity<String> response = FunctionalTestHelper.put(url, dto);
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        checkResult(response.getBody(), 1, 0, 0);
    }

    @Test
    @DisplayName("Проверка, что параметры обновятся только для разрешенных статусов")
    @DbUnitDataSet(after = "csv/OutletControllerFunctionalTest.testCheckStatuses.after.csv")
    void testCheckStatuses() {
        ManageOutletBatchRequestDTO dto = new ManageOutletBatchRequestDTO();
        dto.setOutletIds(ImmutableList.of(2L, 3L, 5L, 6L, 7L));
        dto.setHidden(false);
        dto.setType(OutletType.MIXED);
        dto.setInverted(false);
        final ResponseEntity<String> response = sendRequestUpdate(dto);
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        checkResult(response.getBody(), 5, 0, 1);
    }

    @Test
    @DisplayName("Проверка инверсии идентификаторов (применится ко всем, кроме переданных)")
    @DbUnitDataSet(after = "csv/OutletControllerFunctionalTest.testInversion.after.csv")
    void testInversion() {
        ManageOutletBatchRequestDTO dto = new ManageOutletBatchRequestDTO();
        dto.setOutletIds(ImmutableList.of(1L, 4L));
        dto.setHidden(true);
        dto.setInverted(true);
        final ResponseEntity<String> response = sendRequestUpdate(dto);
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        checkResult(response.getBody(), 5, 1, 1);
    }

    @Test
    @DisplayName("Ошибка: в запросе нет ни идентификаторов аутлетов, ни признака инверсии")
    @DbUnitDataSet(after = "csv/OutletControllerFunctionalTest.before.csv")
    void testNoOutletsSpecified() {
        ManageOutletBatchRequestDTO dto = new ManageOutletBatchRequestDTO();
        dto.setHidden(true);
        dto.setInverted(false);
        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class, () -> sendRequestUpdate(dto));
        MatcherAssert.assertThat(exception, HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST));
        MatcherAssert.assertThat(exception, HttpClientErrorMatcher.errorListMatchesInAnyOrder(
                HttpClientErrorMatcher.errorMatches("BAD_PARAM", "outletIds", "MISSING")));
    }

    //DELETE

    @Test
    @DisplayName("Ошибка: в запросе нет обновлений параметров")
    @DbUnitDataSet(after = "csv/OutletControllerFunctionalTest.before.csv")
    void testNoUpdatesSpecified() {
        ManageOutletBatchRequestDTO dto = new ManageOutletBatchRequestDTO();
        dto.setOutletIds(ImmutableList.of(1L, 4L));
        dto.setInverted(false);
        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class, () -> sendRequestUpdate(dto));
        MatcherAssert.assertThat(exception, HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST));
        MatcherAssert.assertThat(exception, HttpClientErrorMatcher.errorListMatchesInAnyOrder(
                HttpClientErrorMatcher.errorMatches("BAD_PARAM", "type", "MISSING"),
                HttpClientErrorMatcher.errorMatches("BAD_PARAM", "hidden", "MISSING"),
                HttpClientErrorMatcher.errorMatches("BAD_PARAM", "storagePeriod", "MISSING")));
    }

    @Test
    @DisplayName("Успешное удаление нескольких аутлетов")
    @DbUnitDataSet(after = "csv/OutletControllerFunctionalTest.testCommonDelete.after.csv")
    void testCommonDelete() {
        ManageOutletBatchRequestDTO dto = new ManageOutletBatchRequestDTO();
        dto.setOutletIds(ImmutableList.of(1L, 2L, 4L));
        dto.setInverted(false);
        final ResponseEntity<String> response = sendRequestDelete(dto);
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("Успешное удаление нескольких аутлетов")
    @DbUnitDataSet(after = "csv/OutletControllerFunctionalTest.testBlueDelete.after.csv")
    void testBlueDelete() {
        ManageOutletBatchRequestDTO dto = new ManageOutletBatchRequestDTO();
        dto.setOutletIds(Collections.singletonList(8L));
        dto.setInverted(false);
        final String url = baseUrl + "/outlets?id=102";
        final ResponseEntity<String> response = FunctionalTestHelper.deleteWithBody(url, dto);
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("Проверка инверсии идентификаторов (удалятся все, кроме переданных)")
    @DbUnitDataSet(after = "csv/OutletControllerFunctionalTest.testInversionDelete.after.csv")
    void testInversionDelete() {
        ManageOutletBatchRequestDTO dto = new ManageOutletBatchRequestDTO();
        dto.setOutletIds(ImmutableList.of(1L, 4L));
        dto.setInverted(true);
        final ResponseEntity<String> response = sendRequestDelete(dto);
        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("Ошибка удаления: в запросе нет ни идентификаторов аутлетов, ни признака инверсии")
    @DbUnitDataSet(after = "csv/OutletControllerFunctionalTest.before.csv")
    void testNoOutletsSpecifiedDelete() {
        ManageOutletBatchRequestDTO dto = new ManageOutletBatchRequestDTO();
        dto.setInverted(false);
        HttpClientErrorException exception = Assertions.assertThrows(HttpClientErrorException.class, () -> sendRequestDelete(dto));
        MatcherAssert.assertThat(exception, HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST));
        MatcherAssert.assertThat(exception, HttpClientErrorMatcher.errorListMatchesInAnyOrder(
                HttpClientErrorMatcher.errorMatches("BAD_PARAM", "outletIds", "MISSING")));
    }

    private ResponseEntity<String> sendRequestUpdate(Object body) {
        final String url = baseUrl + "/outlets?id=101";
        return FunctionalTestHelper.put(url, body);
    }

    private ResponseEntity<String> sendRequestDelete(Object body) {
        final String url = baseUrl + "/outlets?id=101";
        return FunctionalTestHelper.deleteWithBody(url, body);
    }
}
