package ru.yandex.market.partner.mvc.controller.timezone;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

/**
 * Функциональные тесты для {@link TimezoneController}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
public class TimezoneControllerFunctionalTest extends FunctionalTest {

    @Test
    @DisplayName("Получение списка всех таймзон")
    @DbUnitDataSet(before = "csv/TimezoneController.before.csv")
    public void testGetAllTimezones() {
        final ResponseEntity<String> entity = FunctionalTestHelper.get(getAllTimezonesUrl());
        JsonTestUtil.assertEquals(entity, this.getClass(), "json/all_timezones.json");
    }

    private String getAllTimezonesUrl() {
        return baseUrl + "/timezones";
    }
}
