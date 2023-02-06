package ru.yandex.market.checkout.checkouter.json;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.JsonPathExpectationsHelper;

import ru.yandex.market.checkout.checkouter.cart.Filters;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItems;

public class FiltersJsonHandlerTest extends AbstractJsonHandlerTestBase {

    @Test
    public void deserialize() throws Exception {
        String json = "{\"glfilter\":[\"a\",\"b\",\"c\"]}";

        Filters filters = read(Filters.class, json);

        assertThat(filters.getGlfilter(), hasItems("a", "b", "c"));
    }

    @Test
    public void serialize() throws Exception {
        Filters filters = new Filters(Arrays.asList("a", "b", "c"));

        String json = write(filters);

        checkJson(json, "$.glfilter", JsonPathExpectationsHelper::assertValueIsArray);
        checkJsonMatcher(json, "$.glfilter", hasItems("a", "b", "c"));
    }
}
