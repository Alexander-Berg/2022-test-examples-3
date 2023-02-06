package ru.yandex.market.checkout.helpers;

import java.util.Set;

import javax.annotation.Nullable;

import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.json.IntHolder;
import ru.yandex.market.checkout.common.WebTestHelper;
import ru.yandex.market.checkout.helpers.utils.MockMvcAware;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@WebTestHelper
public class EventCountHelper extends MockMvcAware {

    public EventCountHelper(WebApplicationContext webApplicationContext,
                            TestSerializationService testSerializationService) {
        super(webApplicationContext, testSerializationService);
    }

    public int getOrderEventsCount(long firstEventId,
                                   long lastEventId,
                                   @Nullable Set<HistoryEventType> eventTypes,
                                   @Nullable Boolean ignoreEventTypes) throws Exception {
        MockHttpServletRequestBuilder builder = get("/orders/events-count")
                .param("firstEventId", String.valueOf(firstEventId))
                .param("lastEventId", String.valueOf(lastEventId));

        if (eventTypes != null) {
            eventTypes.forEach(het -> builder.param("eventTypes", het.name()));
        }

        if (ignoreEventTypes != null) {
            builder.param("ignoreEventTypes", Boolean.toString(ignoreEventTypes));
        }


        return performApiRequest(builder, IntHolder.class).getValue();

    }
}
