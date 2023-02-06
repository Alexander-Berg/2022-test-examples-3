package ru.yandex.market.checkout.checkouter.json;

import java.io.IOException;
import java.text.ParseException;
import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.JsonPathExpectationsHelper;

import ru.yandex.market.checkout.checkouter.json.helper.EntityHelper;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.checkout.common.rest.Pager;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class PagedOrdersJsonHandlerTest extends AbstractJsonHandlerTestBase {

    @Test
    public void serialize() throws IOException, ParseException {
        PagedOrders pagedOrders = new PagedOrders(
                Collections.singletonList(EntityHelper.getOrder()),
                Pager.atPage(1, 10)
        );

        String json = write(pagedOrders);
        System.out.println(json);

        checkJson(json, "$.orders", JsonPathExpectationsHelper::assertValueIsArray);
        checkJsonMatcher(json, "$.orders", hasSize(1));
        checkJson(json, "$.pager", JsonPathExpectationsHelper::assertValueIsMap);
    }

    @Test
    public void deserialize() throws IOException {
        PagedOrders pagedOrders = read(PagedOrders.class, getClass().getResourceAsStream("pagedOrders.json"));

        Assertions.assertNotNull(pagedOrders.getPager());
        Assertions.assertNotNull(pagedOrders.getItems());
        assertThat(pagedOrders.getItems(), hasSize(1));
    }
}
