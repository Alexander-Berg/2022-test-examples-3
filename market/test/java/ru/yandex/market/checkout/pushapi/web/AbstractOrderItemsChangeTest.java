package ru.yandex.market.checkout.pushapi.web;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.pushapi.providers.OrderItemsUpdateProvider;
import ru.yandex.market.checkout.pushapi.settings.DataType;
import ru.yandex.market.checkout.util.PushApiTestSerializationService;
import ru.yandex.market.request.trace.RequestContextHolder;

import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

/**
 * @author mkasumov
 */
public abstract class AbstractOrderItemsChangeTest extends AbstractShopWebTestBase {
    @Autowired
    private PushApiTestSerializationService testSerializationService;

    public AbstractOrderItemsChangeTest(DataType dataType) {
        super(dataType);
    }

    protected ResultActions performOrderItemsChange(long shopId) throws Exception {
        Order order = OrderItemsUpdateProvider.buildOrderItemsUpdate();
        return performOrderItemsChange(shopId, order);
    }

    private ResultActions performOrderItemsChange(long shopId, Order order) throws Exception {
        shopadminStubMock.stubFor(post(urlPathEqualTo("/svn-shop/" + shopId + "/order/items"))
                .willReturn(ResponseDefinitionBuilder.responseDefinition().withStatus(200)));

        RequestContextHolder.createNewContext();
        var result = mockMvc.perform(MockMvcRequestBuilders.post("/shops/{shopId}/order/items", shopId)
                        .content(testSerializationService.serialize(order))
                        .contentType(MediaType.APPLICATION_XML))
                .andExpect(request().asyncStarted())
                .andReturn();
        return mockMvc.perform(asyncDispatch(result));
    }
}
