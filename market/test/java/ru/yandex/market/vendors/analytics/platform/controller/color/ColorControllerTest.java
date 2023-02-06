package ru.yandex.market.vendors.analytics.platform.controller.color;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;

/**
 * @author antipov93.
 */
public class ColorControllerTest extends FunctionalTest {

    @Test
    @DisplayName("Получение списка всех цветов")
    void name() {
        String actual = FunctionalTestHelper.get(baseUrl() + "/palette").getBody();
        String expected = loadFromFile("colors.json");
        JsonTestUtil.assertEquals(expected, actual);
    }
}
