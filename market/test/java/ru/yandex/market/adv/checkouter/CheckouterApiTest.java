package ru.yandex.market.adv.checkouter;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.mockserver.model.Header;
import org.mockserver.model.MediaType;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderItems;
import ru.yandex.market.checkout.checkouter.order.OrderItemsException;
import ru.yandex.market.checkout.checkouter.order.fee.FeeChangeResolution;
import ru.yandex.market.checkout.checkouter.order.fee.OrderItemFeeChangeRequest;
import ru.yandex.market.checkout.checkouter.order.fee.OrderItemFeeChangeResponse;
import ru.yandex.market.checkout.checkouter.order.fee.OrderItemsChangeFeeRequest;
import ru.yandex.market.checkout.checkouter.request.BasicOrderRequest;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@MockServerSettings(ports = 12244)
class CheckouterApiTest extends AbstractCheckouterApiMockServerTest {

    private static final List<Header> HEADERS = List.of(
            new Header("X-Ya-Service-Ticket", "TEST-TICKET")
    );

    @Autowired
    private CheckouterClient checkouterApi;
    @Autowired
    private ObjectMapper objectMapper;

    CheckouterApiTest(MockServerClient server) {
        super(server);
    }

    @DisplayName("Возврат корректных данных")
    @Test
    void getOrderItems_exists_itemsReturned() throws JsonProcessingException {
        OrderItems expectedItems = getItems();

        server.when(
                        request()
                                .withMethod("GET")
                                .withPath("/orders/100/items")
                                .withQueryStringParameter("clientRole", "USER")
                                .withQueryStringParameter("clientId", "1")
                                .withQueryStringParameter("shopId", "")
                                .withQueryStringParameter("archived", "false")
                                .withHeaders(HEADERS)
                )
                .respond(
                        response()
                                .withContentType(MediaType.APPLICATION_JSON)
                                .withBody(objectMapper.writeValueAsString(expectedItems))
                                .withStatusCode(200)
                );

        OrderItems actualItems = checkouterApi.getOrderItems(
                RequestClientInfo.builder(ClientRole.USER)
                        .withClientId(1L)
                        .build(),
                BasicOrderRequest.builder(100L)
                        .build()
        );

        Assertions.assertThat(actualItems.getName())
                .isEqualTo("items");
        Assertions.assertThat(actualItems.getContent())
                .hasSize(2);
    }


    @DisplayName("Возврат корректных данных")
    @Test
    void putOrderItemsEditFee_ok_itemsReturned() throws JsonProcessingException {
        OrderItems expectedItems = getItems();

        List<OrderItemFeeChangeRequest> feeChangeRequests = expectedItems.getContent()
                .stream().map(i -> new OrderItemFeeChangeRequest(i.getOrderId(), i.getId(), 11))
                .collect(Collectors.toList());
        OrderItemsChangeFeeRequest clicks = new OrderItemsChangeFeeRequest(feeChangeRequests, true);

        OrderItemFeeChangeRequest orderItemFeeChangeRequest = feeChangeRequests.get(0);
        List<OrderItemFeeChangeResponse> expectedResponse = List.of(
                new OrderItemFeeChangeResponse(orderItemFeeChangeRequest.getOrderId(),
                        orderItemFeeChangeRequest.getItemId(), FeeChangeResolution.ORDER_DELIVERED)
        );

        server.when(
                        request()
                                .withMethod("PUT")
                                .withPath("/orders/items/edit-fee")
                                .withBody(objectMapper.writeValueAsString(clicks))
                                .withHeaders(HEADERS)
                )
                .respond(
                        response()
                                .withContentType(MediaType.APPLICATION_JSON)
                                .withBody(objectMapper.writeValueAsString(expectedResponse))
                                .withStatusCode(200)
                );

        List<OrderItemFeeChangeResponse> responses = checkouterApi.putOrderItemsEditFee(clicks);

        Assertions.assertThat(responses).hasSize(1);
        Assertions.assertThat(responses.get(0).getItemId()).isEqualTo(expectedResponse.get(0).getItemId());
        Assertions.assertThat(responses.get(0).getOrderId()).isEqualTo(expectedResponse.get(0).getOrderId());
        Assertions.assertThat(responses.get(0).getResult()).isEqualTo(expectedResponse.get(0).getResult());
    }


    @DisplayName("Возврат ошибки")
    @Test
    void putOrderItemsEditFee_failed_nothingReturned() throws JsonProcessingException {
        OrderItemsChangeFeeRequest request = new OrderItemsChangeFeeRequest(List.of(), false);

        server.when(
                        request()
                                .withMethod("PUT")
                                .withPath("/orders/items/edit-fee")
                                .withBody(objectMapper.writeValueAsString(request))
                                .withHeaders(HEADERS)
                )
                .respond(
                        response()
                                .withContentType(MediaType.APPLICATION_JSON)
                                .withBody("{\"code\": \"CANNOT_PERFORM_FEE_CHANGE\", \"message\": " +
                                        "\"Failed to perform fee change: lucky!\", \"statusCode\": 400}")
                                .withStatusCode(400)
                );

        Assertions.assertThatThrownBy(
                        () -> checkouterApi.putOrderItemsEditFee(request)
                )
                .isInstanceOf(ErrorCodeException.class)
                .hasMessage("Failed to perform fee change: lucky!");
    }


    @DisplayName("Возврат ошибки")
    @Test
    void putOrderItems_wrongStatus_exceptionReturned() {
        OrderItems expectedItems = new OrderItems();
        server.when(
                        request()
                                .withMethod("PUT")
                                .withPath("/orders/10/items")
                                .withQueryStringParameter("clientRole", "USER")
                                .withQueryStringParameter("clientId", "10")
                                .withHeaders(HEADERS)
                )
                .respond(
                        response()
                                .withContentType(MediaType.APPLICATION_JSON)
                                .withBody("{\"code\": \"" + OrderItemsException.ITEM_DUPLICATE_CODE +
                                        "\", \"message\": " +
                                        "\"Wrong status\", \"statusCode\": 400}")
                                .withStatusCode(400)
                );

        Assertions.assertThatThrownBy(
                        () -> checkouterApi.putOrderItems(
                                10,
                                expectedItems,
                                ClientRole.USER,
                                10
                        )
                )
                .isInstanceOf(ErrorCodeException.class)
                .hasMessage("Wrong status");
    }

    @Nonnull
    private OrderItems getItems() {
        OrderItems items = new OrderItems();
        items.setName("TestItems");
        items.setContent(
                List.of(
                        getOrderItem(1),
                        getOrderItem(2)
                )
        );
        return items;
    }

    @Nonnull
    private OrderItem getOrderItem(int id) {
        OrderItem item1 = new OrderItem();
        item1.setEnglishName("Item" + id);
        item1.setCount(1);
        item1.setPrice(new BigDecimal(10 + id));
        item1.setOrderId(id + 10L);
        item1.setId((long) id);
        return item1;
    }
}
