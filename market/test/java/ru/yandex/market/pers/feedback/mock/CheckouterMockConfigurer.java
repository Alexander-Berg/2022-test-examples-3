package ru.yandex.market.pers.feedback.mock;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.Stubbing;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderNotFoundException;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

@Component
public class CheckouterMockConfigurer {
    private final Stubbing checkouterMock;
    private final ObjectMapper checkouterAnnotationObjectMapper;

    public CheckouterMockConfigurer(Stubbing checkouterMock,
                                    @Qualifier("checkouterAnnotationObjectMapper")
                                            ObjectMapper checkouterAnnotationObjectMapper) {
        this.checkouterMock = checkouterMock;
        this.checkouterAnnotationObjectMapper = checkouterAnnotationObjectMapper;
    }

    public void mockGetOrderNotFound(long orderId, ClientRole clientRole, long clientId) throws JsonProcessingException {
        checkouterMock.stubFor(get(urlPathEqualTo("/orders/" + orderId))
                .withQueryParam(CheckouterClientParams.CLIENT_ROLE, WireMock.equalTo(clientRole.name()))
                .withQueryParam(CheckouterClientParams.CLIENT_ID, WireMock.equalTo(String.valueOf(clientId)))
                .willReturn(new ResponseDefinitionBuilder()
                        .withStatus(404)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE)
                        .withBody(checkouterAnnotationObjectMapper.writeValueAsString(Map.of("code",
                                OrderNotFoundException.ERROR_CODE, "message", "not found")))
                )
        );
    }

    public void mockGetOrder(long orderId, ClientRole clientRole, long clientId, Order order) throws JsonProcessingException {
        checkouterMock.stubFor(get(urlPathEqualTo("/orders/" + orderId))
                .withQueryParam(CheckouterClientParams.CLIENT_ROLE, WireMock.equalTo(clientRole.name()))
                .withQueryParam(CheckouterClientParams.CLIENT_ID, WireMock.equalTo(String.valueOf(clientId)))
                .willReturn(new ResponseDefinitionBuilder()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE)
                        .withBody(checkouterAnnotationObjectMapper.writeValueAsBytes(order))
                ));
    }

    public void mockPostGetOrders(ClientRole role, long clientId, PagedOrders orders) throws JsonProcessingException {
        checkouterMock.stubFor(post(urlPathEqualTo("/get-orders"))
                .withQueryParam(CheckouterClientParams.CLIENT_ROLE, WireMock.equalTo(role.name()))
                .withQueryParam(CheckouterClientParams.CLIENT_ID, WireMock.equalTo(String.valueOf(clientId)))
                .willReturn(new ResponseDefinitionBuilder()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE)
                        .withBody(checkouterAnnotationObjectMapper.writeValueAsString(orders)))
        );
    }

    public void mockPostGetAccessibleOrderIds(ClientRole role, long clientId, Set<Long> orderIds) throws JsonProcessingException {
        checkouterMock.stubFor(post(urlPathEqualTo("/get-accessible-order-ids"))
                .withQueryParam(CheckouterClientParams.CLIENT_ROLE, WireMock.equalTo(role.name()))
                .withQueryParam(CheckouterClientParams.CLIENT_ID, WireMock.equalTo(String.valueOf(clientId)))
                .willReturn(new ResponseDefinitionBuilder()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_UTF8_VALUE)
                        .withBody(checkouterAnnotationObjectMapper.writeValueAsString(orderIds)))
        );
    }
}
