package ru.yandex.market.partner.mvc.controller.dictionary;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

/**
 * Тесты для {@link LegacyDictionaryController}.
 */
public class LegacyDictionaryControllerFunctionalTest extends FunctionalTest {
    @Test
    @DisplayName("Получение содержимого словарей. Тест для ручки /getDictionaries")
    @DbUnitDataSet(before = "LegacyDictionaryController_getDictionaries.before.csv")
    void testDeleteRegionDeliveryGroup() {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl +
                "/getDictionaries?format=json&ADDED_INFO=placement-group,placement-point");
        JsonTestUtil.assertEquals(response, this.getClass(), "LegacyDictionaryController_getDictionaries.json");
    }
}
