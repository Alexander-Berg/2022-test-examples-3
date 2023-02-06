package ru.yandex.market.checkout.checkouter.service.combinator.redeliverypickuppointoption;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.JsonExpectationsHelper;

import ru.yandex.market.checkout.checkouter.json.AbstractJsonHandlerTestBase;

public class RedeliveryPickupPointOptionJsonHandlerTest extends AbstractJsonHandlerTestBase {

    @Test
    public void serializeRequestTest() throws Exception {
        var request = RedeliveryPickupPointOptionDummyUtils.getRequest();
        var json = write(request);
        new JsonExpectationsHelper().assertJsonEqual(json, RedeliveryPickupPointOptionDummyUtils.getRequestAsJson());
    }

    @Test
    public void deserializeResponseTest() throws Exception {
        var response = read(RedeliveryPickupPointOptionResponse.class,
                RedeliveryPickupPointOptionDummyUtils.getResponseAsJson());
        Assertions.assertEquals(RedeliveryPickupPointOptionDummyUtils.getResponse(), response);
    }
}
