package ru.yandex.market.checkout.helpers;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.CancellationRequest;
import ru.yandex.market.checkout.checkouter.order.CancellationRules;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderCancellationListResult;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.common.WebTestHelper;
import ru.yandex.market.checkout.helpers.utils.MockMvcAware;
import ru.yandex.market.checkout.helpers.utils.ResultActionsContainer;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * @author mmetlov
 */
@WebTestHelper
public class CancellationRequestHelper extends MockMvcAware {

    public CancellationRequestHelper(WebApplicationContext webApplicationContext,
                                     TestSerializationService testSerializationService) {
        super(webApplicationContext, testSerializationService);
    }

    public Order createCancellationRequest(long orderId,
                                           CancellationRequest cancellationRequest,
                                           ClientInfo clientInfo) throws Exception {
        return createCancellationRequest(orderId, cancellationRequest, clientInfo, null, null);
    }

    public Order createCancellationRequest(long orderId,
                                           CancellationRequest cancellationRequest,
                                           ClientInfo clientInfo,
                                           ResultActionsContainer container) throws Exception {
        return createCancellationRequest(orderId, cancellationRequest, clientInfo, null, container);

    }

    public Order createCancellationRequest(long orderId,
                                           CancellationRequest cancellationRequest,
                                           ClientInfo clientInfo,
                                           HttpHeaders headers,
                                           ResultActionsContainer container) throws Exception {
        String content = testSerializationService.serializeCheckouterObject(cancellationRequest);
        MockHttpServletRequestBuilder builder = post("/orders/{orderId}/cancellation-request", orderId);
        ClientRoleHelper.addClientInfoParameters(clientInfo, builder);
        if (headers != null) {
            builder.headers(headers);
        }

        return performApiRequest(
                builder.contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                        .content(content),
                Order.class,
                container);
    }

    public void createCancellationRequestByEditApi(
            long orderId,
            CancellationRequest cancellationRequest,
            ClientInfo clientInfo
    ) throws Exception {
        createCancellationRequestByEditApi(orderId, cancellationRequest, clientInfo, null);
    }

    public void createCancellationRequestByEditApi(
            long orderId,
            CancellationRequest cancellationRequest,
            ClientInfo clientInfo,
            ResultActionsContainer container
    ) throws Exception {
        OrderEditRequest orderEditRequest = new OrderEditRequest();
        orderEditRequest.setCancellationRequest(mapCancellationRequest(cancellationRequest));

        String content = testSerializationService.serializeCheckouterObject(orderEditRequest);
        MockHttpServletRequestBuilder builder = post("/orders/{orderId}/edit", orderId);
        ClientRoleHelper.addClientInfoParameters(clientInfo, builder);
        builder.param(CheckouterClientParams.RGB, Color.BLUE.name(), Color.WHITE.name());

        performApiRequest(
                builder.contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                        .content(content),
                Void.class,
                container);
    }

    private ru.yandex.market.checkout.checkouter.order.changerequest.CancellationRequest
    mapCancellationRequest(CancellationRequest cancellationRequest) {
        return new ru.yandex.market.checkout.checkouter.order.changerequest.CancellationRequest(
                cancellationRequest.getSubstatus(),
                cancellationRequest.getNotes()
        );
    }

    public CancellationRules getCancellationRules(ClientRole clientRole) throws Exception {
        MockHttpServletRequestBuilder builder = get("/orders/cancellation-substatuses");
        builder.param("clientRole", clientRole.name());

        return performApiRequest(builder.contentType(MediaType.APPLICATION_JSON_UTF8_VALUE), CancellationRules.class);
    }

    public OrderCancellationListResult getCancellationList(Long orderId) throws Exception {
        MockHttpServletRequestBuilder builder = get("/orders/{orderId}/cancellation-list", orderId);

        return performApiRequest(builder.contentType(MediaType.APPLICATION_JSON_UTF8_VALUE),
                OrderCancellationListResult.class);
    }
}
