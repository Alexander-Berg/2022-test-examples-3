package ru.yandex.market.checkout.checkouter.json;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.JsonPathExpectationsHelper;

import ru.yandex.market.checkout.checkouter.json.helper.EntityHelper;
import ru.yandex.market.checkout.checkouter.order.ItemParameter;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class ItemParametersJsonHandlerTest extends AbstractJsonHandlerTestBase {

    @Test
    public void serialize() throws Exception {
        ItemParameter itemParameter = EntityHelper.getItemParameter();

        String json = write(itemParameter);

        checkJson(json, "$.type", "type");
        checkJson(json, "$.subType", "subType");
        checkJson(json, "$.name", "name");
        checkJson(json, "$.value", "value");
        checkJson(json, "$.unit", "unit");
        checkJson(json, "$.code", "code");
        checkJson(json, "$.units", JsonPathExpectationsHelper::assertValueIsArray);
        checkJsonMatcher(json, "$.units", hasSize(1));
        checkJson(json, "$.specifiedForOffer", true);
    }

    @Test
    public void deserialize() throws Exception {
    }

}
