package ru.yandex.market.partner.mvc.controller.agency;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

/**
 * Тесты для {@link LegacyAgencyController}
 */
@DbUnitDataSet(before = "csv/LegacyAgencyController_getEmployees.before.csv")
public class LegacyAgencyControllerFunctionalTest extends FunctionalTest {
    @Test
    @DisplayName("Получить список менеджеров. Тест ручки /listEmployees без group")
    void testListEmployeesNoGroup() {
        ResponseEntity<String> responseEntity = FunctionalTestHelper.get(getUrl(""));
        JsonTestUtil.assertEquals(responseEntity, this.getClass(),
                "json/LegacyAgencyController_getEmployeesNoGroup.json");
    }

    @Test
    @DisplayName("Получить список менеджеров. Тест ручки /listEmployees")
    void testListEmployees() {
        ResponseEntity<String> responseEntity = FunctionalTestHelper.get(getUrl("&group=manager"));
        JsonTestUtil.assertEquals(responseEntity, this.getClass(),
                "json/LegacyAgencyController_getEmployees.json");
    }

    private String getUrl(String group) {
        return baseUrl + "/listEmployees?&format=json" + group;
    }
}
