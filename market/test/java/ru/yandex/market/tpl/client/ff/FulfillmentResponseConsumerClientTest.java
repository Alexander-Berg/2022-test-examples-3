package ru.yandex.market.tpl.client.ff;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.HttpServerErrorException;

import ru.yandex.market.tpl.client.BaseClientTest;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * @author kukabara
 */
class FulfillmentResponseConsumerClientTest extends BaseClientTest {

    private static final long PARTNER_ID = 123L;
    private static final String ORDER_ID = "orderId";
    private static final String ORDER_ID2 = "orderId2";
    private static final long ORDER_ID_LONG = 123L;
    private static final String TRACK_ID = "trackId";
    private static final String REGISTER_ID = "registerId";

    @Autowired
    private FulfillmentResponseConsumerClient fulfillmentResponseConsumerClient;

    @Test
    void setCreateOrderSuccess() {
        mock.expect(method(HttpMethod.POST))
                .andExpect(requestTo(tplIntUrl + "/fulfillment/createOrderSuccess"))
                .andExpect(content().json(getFileContent("request/create_order_success.json"), true))
                .andRespond(withSuccess());

        fulfillmentResponseConsumerClient.setCreateOrderSuccess(ORDER_ID, TRACK_ID, PARTNER_ID);
    }

    @Test
    void setCreateOrderSuccessException() {
        mock.expect(method(HttpMethod.POST))
                .andExpect(requestTo(tplIntUrl + "/fulfillment/createOrderSuccess"))
                .andRespond(withServerError());

        Throwable thrown = catchThrowable(() -> fulfillmentResponseConsumerClient.setCreateOrderSuccess(ORDER_ID,
                TRACK_ID,
                PARTNER_ID));
        softly.assertThat(thrown)
                .isInstanceOf(HttpServerErrorException.class);
    }

    @Test
    void setCreateOrderError() {
        mock.expect(method(HttpMethod.POST))
                .andExpect(requestTo(tplIntUrl + "/fulfillment/createOrderError"))
                .andExpect(content().json(getFileContent("request/create_order_error.json"), true))
                .andRespond(withSuccess());

        fulfillmentResponseConsumerClient.setCreateOrderError(ORDER_ID, PARTNER_ID);
    }

    @Test
    void setCancelOrderSuccess() {
        mock.expect(method(HttpMethod.POST))
                .andExpect(requestTo(tplIntUrl + "/fulfillment/cancelOrderSuccess?orderId=" + ORDER_ID_LONG
                        + "&partnerId=" + PARTNER_ID))
                .andRespond(withSuccess());

        fulfillmentResponseConsumerClient.setCancelOrderSuccess(ORDER_ID_LONG, PARTNER_ID);
    }

    @Test
    void setUpdateOrderItemError() {
        mock.expect(method(HttpMethod.POST))
                .andExpect(requestTo(tplIntUrl + "/fulfillment/updateOrderItemsError"))
                .andExpect(content().json(getFileContent("request/create_order_error.json"), true))
                .andRespond(withSuccess());

        fulfillmentResponseConsumerClient.setUpdateOrderItemsError(ORDER_ID, PARTNER_ID);
    }

    @Test
    void setUpdateOrderItemSuccess() {
        mock.expect(method(HttpMethod.POST))
                .andExpect(requestTo(tplIntUrl + "/fulfillment/updateOrderItemsSuccess"))
                .andExpect(content().json(getFileContent("request/create_order_error.json"), true))
                .andRespond(withSuccess());

        fulfillmentResponseConsumerClient.setUpdateOrderItemsSuccess(ORDER_ID, PARTNER_ID);
    }

    @Test
    void setCancelOrderError() {
        mock.expect(method(HttpMethod.POST))
                .andExpect(requestTo(tplIntUrl + "/fulfillment/cancelOrderError?orderId=" + ORDER_ID_LONG
                        + "&partnerId=" + PARTNER_ID))
                .andRespond(withSuccess());

        fulfillmentResponseConsumerClient.setCancelOrderError(ORDER_ID_LONG, PARTNER_ID);
    }

    @Test
    void setCreateReturnRegisterSuccess() {
        mock.expect(method(HttpMethod.POST))
                .andExpect(requestTo(tplIntUrl + "/fulfillment/createReturnRegisterSuccess"))
                .andExpect(content().json(getFileContent("request/create_return_register_success.json"), true))
                .andRespond(withSuccess());

        fulfillmentResponseConsumerClient.setCreateReturnRegisterSuccess(REGISTER_ID, Arrays.asList(ORDER_ID,
                ORDER_ID2));
    }

}
