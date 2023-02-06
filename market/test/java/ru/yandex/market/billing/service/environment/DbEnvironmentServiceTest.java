package ru.yandex.market.billing.service.environment;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

class DbEnvironmentServiceTest extends FunctionalTest {

    @Autowired
    EnvironmentService environmentService;

    @Test
    @DbUnitDataSet(before = "DbEnvironmentServiceTest.removeValuesTest.before.csv",
            after = "DbEnvironmentServiceTest.removeValuesTest.after.csv")
    void removeValuesTest() {
        List<String> values = List.of("value1", "value2");
        String name = "name1";
        environmentService.removeValues(name, values);
    }

    @Test
    @DbUnitDataSet(before = "DbEnvironmentServiceTest.removeValuesTest.before.csv",
            after = "DbEnvironmentServiceTest.removeAllValuesTest.after.csv")
    void removeAllValuesTest() {
        String name = "name1";
        environmentService.removeAllValues(name);
    }

    @Test
    @DbUnitDataSet(before = "DbEnvironmentServiceTest.removeValuesTest.before.csv")
    void getFirstCntValuesTest() {
        String name = "name1";
        int cnt = 2;
        Assertions.assertEquals(List.of("value1", "value2"), environmentService.getFirstCntValues(name, cnt));
    }

    @Test
    @DisplayName("Тест на проверку того, что itemId сохранится в env, если для него не найдется совпадающее значение")
    @DbUnitDataSet(before = "DbEnvironmentServiceTest.addValueWithoutReplaceTest.before.csv",
            after = "DbEnvironmentServiceTest.addValueWithoutReplaceTest.after.csv")
    void addValueWithoutReplaceTest() {
        String name = "name1";
        environmentService.addValueWithoutReplace(name, "value3");
    }

    @Test
    @DisplayName("Тест на проверку того, что itemId не изменит env, если для него найдется совпадающее значение")
    @DbUnitDataSet(before = "DbEnvironmentServiceTest.addValueWithoutReplaceTest.before.csv",
            after = "DbEnvironmentServiceTest.addValueWithoutReplaceTest.before.csv")
    void addValueWithReplaceTest() {
        String name = "name1";
        environmentService.addValueWithoutReplace(name, "value2");
    }
}
