package ru.yandex.market.checkout.checkouter.json;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.JsonPathExpectationsHelper;

import ru.yandex.market.checkout.checkouter.json.helper.EntityHelper;
import ru.yandex.market.checkout.checkouter.order.UnitValue;

public class UnitValuesJsonHandlerTest extends AbstractJsonHandlerTestBase {

    @Test
    public void serialize() throws Exception {
        UnitValue unitValue = EntityHelper.createUnitValue();

        String json = write(unitValue);

        checkJson(json, "$.unitId", "unitId");
        checkJson(json, "$.values", JsonPathExpectationsHelper::assertValueIsArray);
        checkJsonMatcher(json, "$.values", CoreMatchers.hasItems("a", "b", "c"));
        checkJsonMatcher(json, "$.shopValues", CoreMatchers.hasItems("d", "e", "f"));
        checkJson(json, "$.defaultUnit", true);
    }

    @Test
    public void deserialize() throws Exception {
    }

}
