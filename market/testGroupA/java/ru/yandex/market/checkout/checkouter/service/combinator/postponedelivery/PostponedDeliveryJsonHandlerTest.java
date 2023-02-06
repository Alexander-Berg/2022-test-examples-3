package ru.yandex.market.checkout.checkouter.service.combinator.postponedelivery;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.JsonExpectationsHelper;

import ru.yandex.market.checkout.checkouter.json.AbstractJsonHandlerTestBase;

public class PostponedDeliveryJsonHandlerTest extends AbstractJsonHandlerTestBase {

    @Test
    public void serializeRequestTest() throws Exception {
        var request = PostponeDeliveryDummyUtils.getRequest();
        var json = write(request);
        new JsonExpectationsHelper().assertJsonEqual(json, PostponeDeliveryDummyUtils.getRequestAsJson());
    }

    @Test
    public void deserializeResponseTest() throws Exception {
        var response = read(PostponeDeliveryResponse.class, PostponeDeliveryDummyUtils.getResponseAsJson());
        Assertions.assertEquals(PostponeDeliveryDummyUtils.getResponse(), response);
    }
}
