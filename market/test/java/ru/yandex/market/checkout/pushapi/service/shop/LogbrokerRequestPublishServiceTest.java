package ru.yandex.market.checkout.pushapi.service.shop;

import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.order.ApiSettings;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.pushapi.client.error.ErrorSubCode;
import ru.yandex.market.checkout.pushapi.proto.PushApiRequest;
import ru.yandex.market.checkout.pushapi.shop.ShopApiResponse;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class LogbrokerRequestPublishServiceTest extends AbstractLogTestBase {

    private String message = "OLOLOLO";
    private ErrorSubCode errorSubCode = ErrorSubCode.CONNECTION_TIMED_OUT;
    private String request = "/cart";
    private String successRequestId = "SUCCESS_REQUEST_ID";
    private String errorRequestId = "ERROR_REQUEST_ID";
    RequestContext requestContext = new RequestContext(1L, false, Context.MARKET, ApiSettings.PRODUCTION, null, 123L);
    private ShopApiResponse response = ShopApiResponse.fromException(null)
            .setUid(123456L)
            .populateBodies(new MockHttpBodies("qwerty", "asdfgh", "zxcvbn", "qazwsx"))
            .setHost("host1")
            .setResponseTime(1111L)
            .setUrl("https://qwerty")
            .setArgs("args")
            .setHttpMethod("GET");

    private void storeSuccessThenError() {
        requestPublishService.publishSuccess(requestContext, request, response, successRequestId, true);
        requestPublishService.publishError(requestContext, request, errorSubCode, message, response, errorRequestId);
        requestPublishService.init();
    }

    private void storeErrorThenSuccess() {
        requestPublishService.publishError(requestContext, request, errorSubCode, message, response, errorRequestId);
        requestPublishService.publishSuccess(requestContext, request, response, successRequestId, true);
        requestPublishService.init();
    }

    @Test
    public void shouldSendSuccessThenErrorToLB() throws Exception {
        shouldSendLogToLB(this::storeSuccessThenError, successRequestId, errorRequestId);
    }

    @Test
    public void shouldSendErrorThenSuccessToLB() throws Exception {
        shouldSendLogToLB(this::storeErrorThenSuccess, errorRequestId, successRequestId);
    }

    @Test
    public void shouldNotFailWhenRequestsQueueIsEmpty() {
        requestPublishService.init();
        List<byte[]> values = messageCaptor.getAllValues();
        assertThat(values.size(), equalTo(0));
    }

    private void shouldSendLogToLB(Runnable storeRequests, String firstRequestId, String secondRequestId) throws Exception {
        storeRequests.run();

        List<byte[]> values = messageCaptor.getAllValues();
        assertThat(values.size(), equalTo(2));

        PushApiRequest request = PushApiRequest.parseFrom(values.get(0));
        assertNotNull(request);
        assertEquals(firstRequestId, request.getRequestId());

        request = PushApiRequest.parseFrom(values.get(1));
        assertNotNull(request);
        assertEquals(secondRequestId, request.getRequestId());
    }
}
