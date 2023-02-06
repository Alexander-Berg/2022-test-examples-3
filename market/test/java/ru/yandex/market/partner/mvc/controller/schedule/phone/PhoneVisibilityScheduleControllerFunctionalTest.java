package ru.yandex.market.partner.mvc.controller.schedule.phone;

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
 * Тесты для {@link PhoneVisibilityScheduleController}
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
public class PhoneVisibilityScheduleControllerFunctionalTest extends FunctionalTest {

    @Test
    @DisplayName("Получение дефолтного расписания показа телефона")
    @DbUnitDataSet(before = "csv/PhoneSchedule.default.before.csv")
    public void testGetDefaultSchedule() {
        final ResponseEntity<String> response = FunctionalTestHelper.get(getUrl(1L));
        checkResponse(response, "default.json");
    }

    @Test
    @DisplayName("Получение пользовательского расписания показа телефона: дефолтная таймзона")
    @DbUnitDataSet(before = "csv/PhoneSchedule.custom.default_tz.before.csv")
    public void testGetScheduleDefaultTimezone() {
        final ResponseEntity<String> response = FunctionalTestHelper.get(getUrl(1L));
        checkResponse(response, "default_tz.json");
    }

    @Test
    @DisplayName("Получение пользовательского расписания показа телефона: кастомная таймзона")
    @DbUnitDataSet(before = "csv/PhoneSchedule.custom.custom_tz.before.csv")
    public void testGetScheduleTimezone() {
        final ResponseEntity<String> response = FunctionalTestHelper.get(getUrl(1L));
        checkResponse(response, "custom_tz.json");
    }

    @Test
    @DisplayName("Установка пользовательского расписания показа телефона: дефолтная таймзона")
    @DbUnitDataSet(before = "csv/PhoneSchedule.default.before.csv", after = "csv/PhoneSchedule.set.default_tz.after.csv")
    public void testSetScheduleDefaultTimezone() {
        final HttpEntity request = getRequest("01.schedule.default_tz.json");
        FunctionalTestHelper.put(getUrl(1L), request);

        final ResponseEntity<String> response = FunctionalTestHelper.get(getUrl(1L));
        checkResponse(response, "01.schedule.default_tz.json");
    }

    @Test
    @DisplayName("Установка пользовательского расписания показа телефона: null таймзона")
    @DbUnitDataSet(before = "csv/PhoneSchedule.default.before.csv", after = "csv/PhoneSchedule.set.default_tz.after.csv")
    public void testSetScheduleNullTimezone() {
        final HttpEntity request = getRequest("02.schedule.null_tz.json");
        FunctionalTestHelper.put(getUrl(1L), request);

        final ResponseEntity<String> response = FunctionalTestHelper.get(getUrl(1L));
        checkResponse(response, "01.schedule.default_tz.json");
    }

    @Test
    @DisplayName("Установка пользовательского расписания показа телефона: кастомная таймзона")
    @DbUnitDataSet(before = "csv/PhoneSchedule.set.custom_tz.before.csv", after = "csv/PhoneSchedule.set.custom_tz.after.csv")
    public void testSetScheduleCustomTimezone() {
        final HttpEntity request = getRequest("03.schedule.custom_tz.json");
        FunctionalTestHelper.put(getUrl(1L), request);

        final ResponseEntity<String> response = FunctionalTestHelper.get(getUrl(1L));
        checkResponse(response, "03.schedule.custom_tz.json");
    }

    @Test
    @DisplayName("Установка пользовательского расписания показа телефона: изменение существующего, та же таймзона")
    @DbUnitDataSet(before = "csv/PhoneSchedule.upd.same_tz.before.csv", after = "csv/PhoneSchedule.upd.same_tz.after.csv")
    public void testUpdScheduleSameTimezone() {
        final HttpEntity request = getRequest("04.schedule.same_tz.json");
        FunctionalTestHelper.put(getUrl(1L), request);

        final ResponseEntity<String> response = FunctionalTestHelper.get(getUrl(1L));
        checkResponse(response, "04.schedule.same_tz.json");
    }

    @Test
    @DisplayName("Установка пользовательского расписания показа телефона: изменение существующего, другая таймзона")
    @DbUnitDataSet(before = "csv/PhoneSchedule.upd.same_tz.before.csv", after = "csv/PhoneSchedule.upd.not_same_tz.after.csv")
    public void testUpdScheduleNotSameTimezone() {
        final HttpEntity request = getRequest("05.schedule.not_same_tz.json");
        FunctionalTestHelper.put(getUrl(1L), request);

        final ResponseEntity<String> response = FunctionalTestHelper.get(getUrl(1L));
        checkResponse(response, "05.schedule.not_same_tz.json");
    }

    @Test
    @DisplayName("Установка пользовательского расписания показа телефона: после смены таймзоны расписание разобьется на несколько")
    @DbUnitDataSet(before = "csv/PhoneSchedule.set.big.before.csv")
    public void testSetScheduleBig() {
        final HttpEntity request = getRequest("10.schedule.big.json");
        FunctionalTestHelper.put(getUrl(1L), request);

        final ResponseEntity<String> response = FunctionalTestHelper.get(getUrl(1L));
        checkResponse(response, "10.schedule.big.json");
    }

    @Test
    @DisplayName("Установка пользовательского расписания показа телефона: недостаточно времени")
    @DbUnitDataSet(before = "csv/PhoneSchedule.default.before.csv")
    public void testSetScheduleNotEnoughTime() {
        final HttpEntity request = getRequest("06.schedule.error.duration.json");
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
                                                        "    \"code\":\"VISIBILITY_DURATION\"," +
                                                        "    \"details\":{" +
                                                        "        \"duration\":12600" +
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
    @DbUnitDataSet(before = "csv/PhoneSchedule.default.before.csv")
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
    @DisplayName("Установка пользовательского расписания показа телефона: пересекаются промежутки")
    @DbUnitDataSet(before = "csv/PhoneSchedule.default.before.csv")
    public void testSetScheduleWeeklyScheduleInterception() {
        final HttpEntity request = getRequest("08.schedule.error.interception.json");
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
                                                        "    \"code\":\"WEEKLY_SCHEDULE_INTERCEPTION\"," +
                                                        "    \"details\":{}" +
                                                        "}"
                                        )
                                )
                        )
                )
        );
    }

    private String getUrl(final long datasourceId) {
        return String.format("%s/schedule/phone?datasourceId=%d&_user_id=12345", baseUrl, datasourceId);
    }

    private void checkResponse(final ResponseEntity<String> response, final String file) {
        JsonTestUtil.assertEquals(response, this.getClass(), "/mvc/schedule/phone/response/" + file);
    }

    private HttpEntity getRequest(final String file) {
        return JsonTestUtil.getJsonHttpEntity(this.getClass(), "/mvc/schedule/phone/request/" + file);
    }
}
