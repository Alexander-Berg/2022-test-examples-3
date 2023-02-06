package ru.yandex.market.checkout.checkouter.json;

import java.io.IOException;
import java.text.ParseException;
import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.JsonPathExpectationsHelper;

import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.json.helper.EntityHelper;
import ru.yandex.market.checkout.common.rest.Pager;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class PagedEventsJsonHandlerTest extends AbstractJsonHandlerTestBase {

    @Test
    public void serialize() throws IOException, ParseException {
        PagedEvents pagedEvents = new PagedEvents(
                Collections.singletonList(EntityHelper.getOrderHistoryEvent()),
                Pager.atPage(1, 10)
        );

        String json = write(pagedEvents);
        System.out.println(json);

        checkJson(json, "$.pager", JsonPathExpectationsHelper::assertValueIsMap);
        checkJson(json, "$." + Names.History.EVENTS, JsonPathExpectationsHelper::assertValueIsArray);
        checkJsonMatcher(json, "$." + Names.History.EVENTS, hasSize(1));
    }

    @Test
    public void deserialize() throws IOException {
        PagedEvents pagedEvents = read(PagedEvents.class, getClass().getResourceAsStream("pagedEvents.json"));

        Assertions.assertNotNull(pagedEvents.getPager());
        Assertions.assertNotNull(pagedEvents.getItems());
        assertThat(pagedEvents.getItems(), hasSize(1));
    }
}
