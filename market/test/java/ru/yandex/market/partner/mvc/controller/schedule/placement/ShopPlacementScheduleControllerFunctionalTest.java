package ru.yandex.market.partner.mvc.controller.schedule.placement;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.matchers.HttpClientErrorMatcher;
import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

/**
 * Тесты для {@link ShopPlacementScheduleController}
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
public class ShopPlacementScheduleControllerFunctionalTest extends FunctionalTest {

    @Test
    @DisplayName("Получение дефолтного расписания размещения")
    @DbUnitDataSet(before = "csv/PlacementSchedule.default.before.csv")
    public void testGetDefaultSchedule() {
        final ResponseEntity<String> response = FunctionalTestHelper.get(getUrl(1L));
        checkResponse(response, "default.json");
    }

    @Test
    @DisplayName("Получение пользовательского расписания размещения: дефолтная таймзона")
    @DbUnitDataSet(before = "csv/PlacementSchedule.custom.default_tz.before.csv")
    public void testGetScheduleDefaultTimezone() {
        final ResponseEntity<String> response = FunctionalTestHelper.get(getUrl(1L));
        checkResponse(response, "default_tz.json");
    }

    @Test
    @DisplayName("Получение пользовательского расписания размещения: кастомная таймзона")
    @DbUnitDataSet(before = "csv/PlacementSchedule.custom.custom_tz.before.csv")
    public void testGetScheduleTimezone() {
        final ResponseEntity<String> response = FunctionalTestHelper.get(getUrl(1L));
        checkResponse(response, "custom_tz.json");
    }

    @Test
    @DisplayName("Получение расписания размещения при отключенном статусе")
    @DbUnitDataSet(before = "csv/PlacementSchedule.switched_off.before.csv")
    public void testGetSwitchedOffSchedule() {
        final ResponseEntity<String> response = FunctionalTestHelper.get(getUrl(1L));
        checkResponse(response, "switched_off.json");
    }

    @Test
    @DisplayName("Получение пользовательского расписания размещения: кастомная таймзона. Заканчивается в полночь")
    @DbUnitDataSet(before = "csv/PlacementSchedule.custom.custom_tz.midnight.before.csv")
    public void testGetScheduleTimezoneMidnight() {
        final ResponseEntity<String> response = FunctionalTestHelper.get(getUrl(1L));
        checkResponse(response, "custom_tz.midnight.json");
    }

    @Test
    @DisplayName("Сброс пользовательского расписания размещения")
    @DbUnitDataSet(before = "csv/PlacementSchedule.reset.before.csv", after = "csv/PlacementSchedule.reset.after.csv")
    public void testResetScheduleTimezone() {
        FunctionalTestHelper.post(getResetUrl(1L));
    }

    @Test
    @DisplayName("Выключение размещения")
    @DbUnitDataSet(before = "csv/PlacementSchedule.custom.custom_tz.before.csv", after = "csv/PlacementSchedule.switch_off.after.csv")
    public void testSwitchOffScheduleTimezone() {
        FunctionalTestHelper.post(getSwitchOffUrl(1L));
    }

    @Test
    @DisplayName("Установка пользовательского расписания размещения: дефолтная таймзона")
    @DbUnitDataSet(before = "csv/PlacementSchedule.default.before.csv", after = "csv/PlacementSchedule.set.default_tz.after.csv")
    public void testSetScheduleDefaultTimezone() {
        final HttpEntity request = getRequest("01.schedule.default_tz.json");
        FunctionalTestHelper.put(getUrl(1L), request);

        final ResponseEntity<String> response = FunctionalTestHelper.get(getUrl(1L));
        checkResponse(response, "01.schedule.default_tz.json");
    }

    @Test
    @DisplayName("Установка пользовательского расписания размещения: null таймзона")
    @DbUnitDataSet(before = "csv/PlacementSchedule.default.before.csv", after = "csv/PlacementSchedule.set.default_tz.after.csv")
    public void testSetScheduleNullTimezone() {
        final HttpEntity request = getRequest("02.schedule.null_tz.json");
        FunctionalTestHelper.put(getUrl(1L), request);

        final ResponseEntity<String> response = FunctionalTestHelper.get(getUrl(1L));
        checkResponse(response, "01.schedule.default_tz.json");
    }

    @Test
    @DisplayName("Установка пользовательского расписания размещения: кастомная таймзона")
    @DbUnitDataSet(before = "csv/PlacementSchedule.set.custom_tz.before.csv", after = "csv/PlacementSchedule.set.custom_tz.after.csv")
    public void testSetScheduleCustomTimezone() {
        final HttpEntity request = getRequest("03.schedule.custom_tz.json");
        FunctionalTestHelper.put(getUrl(1L), request);

        final ResponseEntity<String> response = FunctionalTestHelper.get(getUrl(1L));
        checkResponse(response, "03.schedule.custom_tz.json");
    }

    @Test
    @DisplayName("Установка пользовательского расписания размещения: изменение существующего")
    @DbUnitDataSet(before = "csv/PlacementSchedule.upd.before.csv", after = "csv/PlacementSchedule.upd.not_same_tz.after.csv")
    public void testUpdScheduleNotSameTimezone() {
        final HttpEntity request = getRequest("05.schedule.not_same_tz.json");
        FunctionalTestHelper.put(getUrl(1L), request);

        final ResponseEntity<String> response = FunctionalTestHelper.get(getUrl(1L));
        checkResponse(response, "05.schedule.not_same_tz.json");
    }

    @Test
    @DisplayName("Установка пользовательского расписания размещения: после смены таймзоны расписание разобьется на несколько")
    @DbUnitDataSet(before = "csv/PlacementSchedule.set.big.before.csv")
    public void testSetScheduleBig() {
        final HttpEntity request = getRequest("10.schedule.big.json");
        FunctionalTestHelper.put(getUrl(1L), request);

        final ResponseEntity<String> response = FunctionalTestHelper.get(getUrl(1L));
        checkResponse(response, "10.schedule.big.json");
    }

    @Test
    @DisplayName("Установка пользовательского расписания показа телефона: обратный порядок дней")
    @DbUnitDataSet(before = "csv/PlacementSchedule.default.before.csv")
    public void testSetScheduleInvalidDayInterval() {
        final HttpEntity request = getRequest("06.schedule.error.day_interval.json");
        HttpClientErrorException HttpClientErrorException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.put(getUrl(1L), request)
        );
        MatcherAssert.assertThat(
                HttpClientErrorException,
                Matchers.allOf(
                        HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST),
                        HttpClientErrorMatcher.bodyMatches(
                                MbiMatchers.jsonPropertyMatches(
                                        "errors",
                                        MbiMatchers.jsonArrayEquals(
                                                "" +
                                                        "{" +
                                                        "    \"code\":\"BAD_PARAM\"," +
                                                        "    \"details\":{" +
                                                        "        \"subcode\":\"INVALID\"," +
                                                        "        \"field\":\"day-interval\"" +
                                                        "    }" +
                                                        "}"
                                        )
                                )
                        )
                )
        );
    }

    @Test
    @DisplayName("Установка пользовательского расписания показа телефона: неизвестная таймзона")
    @DbUnitDataSet(before = "csv/PlacementSchedule.default.before.csv")
    public void testSetScheduleInvalidTimezone() {
        final HttpEntity request = getRequest("07.schedule.error.timezone.json");
        HttpClientErrorException httpClientErrorException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.put(getUrl(1L), request)
        );
        MatcherAssert.assertThat(
                httpClientErrorException,
                Matchers.allOf(
                        HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST),
                        HttpClientErrorMatcher.bodyMatches(
                                MbiMatchers.jsonPropertyMatches(
                                        "errors",
                                        MbiMatchers.jsonArrayEquals(
                                                "" +
                                                        "{" +
                                                        "    \"code\":\"BAD_PARAM\"," +
                                                        "    \"details\":{" +
                                                        "        \"subcode\":\"INVALID\"," +
                                                        "        \"field\":\"timezoneName\"" +
                                                        "    }" +
                                                        "}"
                                        )
                                )
                        )
                )
        );
    }

    @Test
    @DisplayName("Установка пользовательского расписания показа телефона: отрицательное время")
    @DbUnitDataSet(before = "csv/PlacementSchedule.default.before.csv")
    public void testSetScheduleInvalidStartTime() {
        final HttpEntity request = getRequest("08.schedule.error.start_time.json");
        HttpClientErrorException httpClientErrorException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.put(getUrl(1L), request)
        );
        MatcherAssert.assertThat(
                httpClientErrorException,
                Matchers.allOf(
                        HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST),
                        HttpClientErrorMatcher.bodyMatches(
                                MbiMatchers.jsonPropertyMatches(
                                        "errors",
                                        MbiMatchers.jsonArrayEquals(
                                                "" +
                                                        "{" +
                                                        "    \"code\":\"BAD_PARAM\"," +
                                                        "    \"details\":{" +
                                                        "        \"subcode\":\"INVALID\"," +
                                                        "        \"field\":\"start-time\"" +
                                                        "    }" +
                                                        "}"
                                        )
                                )
                        )
                )
        );
    }

    @Test
    @DisplayName("Установка пользовательского расписания показа телефона: большой интервал")
    @DbUnitDataSet(before = "csv/PlacementSchedule.default.before.csv")
    public void testSetScheduleInvalidTimeInterval() {
        final HttpEntity request = getRequest("09.schedule.error.time_interval.json");
        HttpClientErrorException httpClientErrorException = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.put(getUrl(1L), request)
        );
        MatcherAssert.assertThat(
                httpClientErrorException,
                Matchers.allOf(
                        HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST),
                        HttpClientErrorMatcher.bodyMatches(
                                MbiMatchers.jsonPropertyMatches(
                                        "errors",
                                        MbiMatchers.jsonArrayEquals(
                                                "" +
                                                        "{" +
                                                        "    \"code\":\"BAD_PARAM\"," +
                                                        "    \"details\":{" +
                                                        "        \"subcode\":\"INVALID\"," +
                                                        "        \"field\":\"time-interval\"" +
                                                        "    }" +
                                                        "}"
                                        )
                                )
                        )
                )
        );
    }

    private String getUrl(final long datasourceId) {
        return String.format("%s/schedule/placement?datasourceId=%d&_user_id=12345", baseUrl, datasourceId);
    }

    private String getResetUrl(final long datasourceId) {
        return String.format("%s/schedule/placement/reset?datasourceId=%d&_user_id=12345", baseUrl, datasourceId);
    }

    private String getSwitchOffUrl(final long datasourceId) {
        return String.format("%s/schedule/placement/switch-off?datasourceId=%d&_user_id=12345", baseUrl, datasourceId);
    }

    private void checkResponse(final ResponseEntity<String> response, final String file) {
        JsonTestUtil.assertEquals(response, this.getClass(), "/mvc/schedule/placement/response/" + file);
    }

    private HttpEntity getRequest(final String file) {
        return JsonTestUtil.getJsonHttpEntity(this.getClass(), "/mvc/schedule/placement/request/" + file);
    }
}
