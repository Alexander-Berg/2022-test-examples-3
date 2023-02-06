package ru.yandex.market.checkout.checkouter.controller;

import java.util.List;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import de.flapdoodle.embed.process.collections.Collections;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.stock.StockStorageConfigurer;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class StocksControllerTest extends AbstractWebTestBase {

    @Autowired
    private StockStorageConfigurer stockStorageConfigurer;

    @Test
    public void unfreezeStocks() throws Exception {
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        stockStorageConfigurer.resetRequests();
        stockStorageConfigurer.mockOkForUnfreeze();
        String response = unfreezeRequest(order.getId(), ClientRole.SYSTEM, 1L);
        List<ServeEvent> serveEvents = onlyWithOkResponse(stockStorageConfigurer.getServeEvents());
        assertThat(serveEvents, hasSize(1));
        assertUnfreezeOrderRequest(order, serveEvents);
        assertThat(response, equalTo("true"));
    }

    @Test
    public void unfreezeStocksScheduleOnFailure() throws Exception {
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        stockStorageConfigurer.resetRequests();
        stockStorageConfigurer.mockErrorForUnfreeze(order.getId());
        String response = unfreezeRequest(order.getId(), ClientRole.SYSTEM, 1L);
        List<ServeEvent> serveEvents = onlyWithOkResponse(stockStorageConfigurer.getServeEvents());
        assertThat(serveEvents, empty());
        assertThat(response, equalTo("false"));
    }

    @Test
    public void bulkUnfreezeStocks() throws Exception {
        Order order1 = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        Order order2 = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        Order order3 = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        stockStorageConfigurer.resetRequests();
        stockStorageConfigurer.mockOkForUnfreeze(order1.getId());
        stockStorageConfigurer.mockOkForUnfreeze(order2.getId());
        stockStorageConfigurer.mockOkForUnfreeze(order3.getId());
        String response = bulkUnfreezeRequest(
                Collections.newArrayList(order1.getId(), order2.getId(), order3.getId()),
                ClientRole.SYSTEM, 1L
        );
        List<ServeEvent> serveEvents = onlyWithOkResponse(stockStorageConfigurer.getServeEvents());
        assertThat(serveEvents, hasSize(3));
        assertUnfreezeOrderRequest(order1, serveEvents);
        assertUnfreezeOrderRequest(order2, serveEvents);
        assertUnfreezeOrderRequest(order3, serveEvents);
        assertThat(response, containsString(order3.getId().toString()));
        assertThat(response, containsString(order2.getId().toString()));
        assertThat(response, containsString(order1.getId().toString()));
    }

    @Test
    public void bulkUnfreezeStocksWithFails() throws Exception {
        Order order1 = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        Order order2 = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        stockStorageConfigurer.resetRequests();
        stockStorageConfigurer.mockOkForUnfreeze(order1.getId());
        stockStorageConfigurer.mockErrorForUnfreeze(order2.getId());
        String response = bulkUnfreezeRequest(
                Collections.newArrayList(order1.getId(), order2.getId()),
                ClientRole.SYSTEM, 1L
        );
        List<ServeEvent> serveEvents = onlyWithOkResponse(stockStorageConfigurer.getServeEvents());
        assertThat(serveEvents, hasSize(1));
        assertUnfreezeOrderRequest(order1, serveEvents);
        assertThat(response, containsString(order1.getId().toString()));
    }

    private List<ServeEvent> onlyWithOkResponse(List<ServeEvent> events) {
        return events
                .stream()
                .filter(e -> e.getResponse().getStatus() == 200)
                .collect(toList());
    }

    private String unfreezeRequest(long orderId, ClientRole clientRole, Long clientId) throws Exception {
        String response = mockMvc.perform(post("/orders/{orderId}/unfreeze-stocks", orderId)
                .param("clientRole", StringUtils.nullSafeToString(clientRole))
                .param("clientId", StringUtils.nullSafeToString(clientId)))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return response;
    }

    private String bulkUnfreezeRequest(List<Long> orderIds, ClientRole clientRole,
                                       Long clientId) throws Exception {
        MockHttpServletRequestBuilder request = post("/orders/unfreeze-stocks-bulk")
                .param("clientRole", StringUtils.nullSafeToString(clientRole))
                .param("clientId", StringUtils.nullSafeToString(clientId));

        orderIds.forEach(id -> request.param("orderId", id.toString()));

        String response = mockMvc.perform(request).andReturn().getResponse().getContentAsString();
        return response;
    }

    private void assertUnfreezeOrderRequest(Order order, List<ServeEvent> serveEvents) {
        assertThat(
                serveEvents.stream().map(s -> s.getRequest().getUrl()).collect(toList()),
                hasItem(containsString("order/" + order.getId() + "?cancel=false"))
        );
    }
}
