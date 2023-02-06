package ru.yandex.market.checkout.helpers;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.client.OrderFilter;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvents;
import ru.yandex.market.checkout.checkouter.json.LongHolder;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.common.WebTestHelper;
import ru.yandex.market.checkout.helpers.utils.MockMvcAware;
import ru.yandex.market.checkout.util.CheckedConsumer;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebTestHelper
public class EventsQueueGetHelper extends MockMvcAware {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");

    @Autowired
    public EventsQueueGetHelper(WebApplicationContext webApplicationContext,
                                TestSerializationService testSerializationService) {
        super(webApplicationContext, testSerializationService);
    }

    public OrderHistoryEvents getOrderHistoryEvents() throws Exception {
        return getOrderHistoryEvents(ClientRole.SYSTEM, 0, null, null, null, null, null, null);
    }

    public OrderHistoryEvents getOrderHistoryEvents(int batchSize) throws Exception {
        return getOrderHistoryEvents(ClientRole.SYSTEM, 0, batchSize, null, null, null, null, null);
    }

    public OrderHistoryEvents getOrderHistoryEvents(long lastEventId) throws Exception {
        return getOrderHistoryEvents(ClientRole.SYSTEM, lastEventId, null, null, null, null, null, null);
    }

    public OrderHistoryEvents getOrderHistoryEvents(Set<Integer> buckets) throws Exception {
        return getOrderHistoryEvents(ClientRole.SYSTEM, 0, null, buckets, null, null, null, null);
    }

    public OrderHistoryEvents getOrderHistoryEvents(@Nullable Set<Integer> buckets,
                                                    @Nullable Set<HistoryEventType> eventTypes,
                                                    @Nullable Boolean ignoreEventTypes,
                                                    @Nullable OrderFilter orderFilter) throws Exception {
        return getOrderHistoryEvents(ClientRole.SYSTEM, 0, null, buckets, eventTypes,
                ignoreEventTypes, orderFilter,
                null);
    }

    public @Nullable
    Long getFirstEventIdAfterDate(Date fromDate) throws Exception {
        MockHttpServletRequestBuilder builder = get("/orders/events/first-by-date")
                .param("fromDate", DATE_FORMAT.format(fromDate));

        MvcResult result = mockMvc.perform(builder)
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        return testSerializationService.deserializeCheckouterObject(content, LongHolder.class).getValue();
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    public OrderHistoryEvents getOrderHistoryEvents(ClientRole clientRole,
                                                    long lastEventId,
                                                    Integer batchSize,
                                                    Set<Integer> buckets,
                                                    Set<HistoryEventType> eventTypes,
                                                    Boolean ignoreEventTypes,
                                                    OrderFilter orderFilter,
                                                    CheckedConsumer<ResultActions> customizer) throws Exception {
        MockHttpServletRequestBuilder builder = get("/orders/events")
                .param(CheckouterClientParams.CLIENT_ROLE, clientRole.name())
                .param("lastEventId", String.valueOf(lastEventId))
                .param("withWaitInterval", "false");

        if (batchSize != null) {
            builder.param("batchSize", String.valueOf(batchSize));
        }

        if (buckets != null) {
            builder.param("buckets", buckets.stream().map(String::valueOf).toArray(String[]::new));
        }

        if (eventTypes != null) {
            builder.param("eventTypes", eventTypes.stream().map(HistoryEventType::name).toArray(String[]::new));
        }

        if (ignoreEventTypes != null) {
            builder.param("ignoreEventTypes", Boolean.toString(ignoreEventTypes));
        }

        if (orderFilter != null) {
            if (orderFilter.getFulfilment() != null) {
                builder.param("fulfilment", String.valueOf(orderFilter.getFulfilment()));
            }

            if (orderFilter.getGlobal() != null) {
                builder.param("global", String.valueOf(orderFilter.getGlobal()));
            }

            if (orderFilter.getRgb() != null && orderFilter.getRgb().length > 0) {
                builder.param("rgb", Stream.of(orderFilter.getRgb()).map(Color::name).toArray(String[]::new));
            }

            if (ArrayUtils.isNotEmpty(orderFilter.getShopId())) {
                builder.param("shopId",
                        Arrays.stream(orderFilter.getShopId()).mapToObj(String::valueOf).toArray(String[]::new));
            }

            if (orderFilter.getNoAuth() != null) {
                builder.param("noAuth", String.valueOf(orderFilter.getNoAuth()));
            }
        }


        ResultActions resultActions = mockMvc.perform(builder);
        if (customizer != null) {
            customizer.consume(resultActions);
        }

        MvcResult result = resultActions
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn();
        return testSerializationService.deserializeCheckouterObject(result.getResponse().getContentAsString(),
                OrderHistoryEvents.class);
    }

}
