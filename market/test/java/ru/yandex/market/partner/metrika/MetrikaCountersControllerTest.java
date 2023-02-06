package ru.yandex.market.partner.metrika;

import com.google.gson.JsonElement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

@DbUnitDataSet(before = "MetrikaControllerTest.before.csv")
class MetrikaCountersControllerTest extends FunctionalTest {

    @Test
    @DisplayName("Запрашивается список целей для конкретного счетчика.")
    void getGoals() {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "/metrika/counters/checkout?datasourceId=1");
        JsonElement expectResult = JsonTestUtil.parseJson("[{\"id\":1,\"shopId\":1,\"counterId\":\"cnt\",\"goalId\":\"gl\",\"type\":\"CHECKOUT\"}]");
        JsonTestUtil.assertEquals(response, expectResult);
    }

    @Test
    @DisplayName("Сохраняется список целей для конкретного счетчика.")
    @DbUnitDataSet(after = "MetrikaControllerTestSave.after.csv")
    void putGoals() {
        FunctionalTestHelper.put(
                baseUrl + "/metrika/counters/checkout?datasourceId=2",
                JsonTestUtil.getJsonHttpEntity("[{\"counterId\": \"cnt2\", \"goalId\": \"gl2\"}]"));
    }

    @Test
    @DisplayName("Удаляется цель конкретного счетчика.")
    @DbUnitDataSet(after = "MetrikaControllerTestDelete.after.csv")
    void deleteGoals() {
        FunctionalTestHelper.delete(baseUrl + "/metrika/counters/checkout?datasourceId=1&id=1");
    }
}
