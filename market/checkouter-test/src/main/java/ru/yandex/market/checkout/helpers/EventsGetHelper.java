package ru.yandex.market.checkout.helpers;

import java.util.EnumSet;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.controllers.oms.OrderHistoryEventsController;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.common.WebTestHelper;
import ru.yandex.market.checkout.helpers.utils.MockMvcAware;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.ARCHIVED;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.CLIENT_ID;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.CLIENT_ROLE;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.OPTIONAL_PARTS;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.PAGE_SIZE;
import static ru.yandex.market.checkout.checkouter.client.ClientUtils.mapToUrlParams;

/**
 * Хелпер для
 * {@link OrderHistoryEventsController#getOrderHistoryEvents(long, ru.yandex.market.checkout.checkouter.client
 * .ClientRole, java.lang.Long, java.lang.Integer, java.lang.Integer, java.lang.Long, java.lang.Boolean, java.util
 * .Set, boolean)}
 */
@WebTestHelper
public class EventsGetHelper extends MockMvcAware {

    @Autowired
    public EventsGetHelper(WebApplicationContext webApplicationContext, TestSerializationService
            testSerializationService) {
        super(webApplicationContext, testSerializationService);
    }

    public PagedEvents getOrderHistoryEvents(long orderId) throws Exception {
        return getOrderHistoryEvents(orderId, ClientInfo.SYSTEM);
    }

    public PagedEvents getOrderHistoryEvents(long orderId, int pageSize) throws Exception {
        return getOrderHistoryEvents(orderId, pageSize, ClientInfo.SYSTEM);
    }

    public PagedEvents getOrderHistoryEvents(long orderId, Integer pageSize, ClientInfo clientInfo) throws Exception {
        MockHttpServletRequestBuilder httpServletRequestBuilder = get("/orders/{orderId}/events", orderId)
                .param(CLIENT_ROLE, clientInfo.getRole().name())
                .param(CLIENT_ID, String.valueOf(clientInfo.getId()))
                .param(OPTIONAL_PARTS, mapToUrlParams(EnumSet.allOf(OptionalOrderPart.class)));

        if (pageSize != null) {
            httpServletRequestBuilder.param(PAGE_SIZE, String.valueOf(pageSize));
        }

        MvcResult result = mockMvc.perform(httpServletRequestBuilder)
                .andDo(log())
                .andReturn();

        return testSerializationService.deserializeCheckouterObject(result.getResponse().getContentAsString(),
                PagedEvents.class);
    }

    public PagedEvents getOrderHistoryEvents(long orderId, ClientInfo clientInfo) throws Exception {
        return getOrderHistoryEvents(orderId, null, clientInfo);
    }

    public PagedEvents getOrderHistoryEvents(Set<Long> orderIds,
                                             ClientInfo clientInfo,
                                             @Nullable Boolean onlyImportant,
                                             @Nullable Boolean onlyUnread) throws Exception {
        if (CollectionUtils.isEmpty(orderIds)) {
            throw new IllegalArgumentException("orderIds");
        }

        MockHttpServletRequestBuilder builder = get("/orders/events/by-order-id");
        orderIds.forEach(orderId -> {
            builder.param("orderId", String.valueOf(orderId));
        });
        builder.param(CLIENT_ROLE, clientInfo.getRole().name());
        builder.param(CLIENT_ID, String.valueOf(clientInfo.getId()));
        if (onlyImportant != null) {
            builder.param("onlyImportant", onlyImportant ? "true" : "false");
        }
        if (onlyUnread != null) {
            builder.param("onlyUnread", onlyUnread ? "true" : "false");
        }

        MvcResult result = mockMvc.perform(builder)
                .andExpect(status().isOk())
                .andDo(log())
                .andReturn();

        return testSerializationService.deserializeCheckouterObject(
                result.getResponse().getContentAsString(), PagedEvents.class
        );
    }

    public ResultActions getOrderEventsForActions(long orderId, ClientInfo clientInfo, boolean archived)
            throws Exception {
        return mockMvc.perform(get("/orders/{orderId}/events", orderId)
                .param(CLIENT_ROLE, clientInfo.getRole().name())
                .param(CLIENT_ID, String.valueOf(clientInfo.getId()))
                .param(ARCHIVED, String.valueOf(archived)));
    }
}
