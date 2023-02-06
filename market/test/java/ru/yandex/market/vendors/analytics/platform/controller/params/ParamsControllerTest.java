package ru.yandex.market.vendors.analytics.platform.controller.params;

import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.vendors.analytics.core.utils.dbunit.ClickhouseDbUnitDataSet;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;

/**
 * Тесты на базовые случаи {@link ParamsController}.
 *
 * @author ogonek.
 */
@ClickhouseDbUnitDataSet(before = "ParamsControllerTest.ch.csv")
class ParamsControllerTest extends FunctionalTest {

    private static final long DEFAULT_HID = 91491L;

    @Test
    @DisplayName("Передана категория с кучей разных фильтров")
    void differentFilters() {
        String response = getParamValues(DEFAULT_HID);
        String expected = loadFromFile("differentFilters.response.json");
        JsonAssert.assertJsonEquals(expected, response);
    }

    private String getParamValues(long hid) {
        var paramValuesUrl = baseUrl() + "model/parameters?hid=" + hid;
        return FunctionalTestHelper.get(paramValuesUrl).getBody();
    }

}
