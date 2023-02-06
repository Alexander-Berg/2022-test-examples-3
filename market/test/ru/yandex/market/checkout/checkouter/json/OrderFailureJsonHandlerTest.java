package ru.yandex.market.checkout.checkouter.json;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.json.helper.EntityHelper;
import ru.yandex.market.checkout.checkouter.order.OrderFailure;

public class OrderFailureJsonHandlerTest extends AbstractJsonHandlerTestBase {

    @Test
    public void serialize() throws Exception {
        OrderFailure orderFailure = EntityHelper.getOrderFailure();

        String json = write(orderFailure);
        System.out.println(json);

        checkJson(json, "$." + Names.Error.ERROR, OrderFailure.Code.OUT_OF_DATE.name());
        checkJson(json, "$." + Names.Error.ERROR_REASON, OrderFailure.Reason.SHOP_IS_TRICKY.name());
        checkJson(json, "$." + Names.Error.ERROR_DETAILS, "errorDetails");
        checkJson(json, "$." + Names.Error.ERROR_DEV_DETAILS, "errorDevDetails");
    }

    @Test
    public void deserialize() throws Exception {
        OrderFailure orderFailure = read(OrderFailure.class, getClass().getResourceAsStream("orderFailure.json"));

        Assertions.assertEquals(OrderFailure.Code.OUT_OF_DATE, orderFailure.getErrorCode());
        Assertions.assertEquals(OrderFailure.Reason.SHOP_IS_TRICKY, orderFailure.getErrorReason());
        Assertions.assertEquals("errorDetails", orderFailure.getErrorDetails());
        Assertions.assertEquals("errorDevDetails", orderFailure.getErrorDevDetails());
        Assertions.assertNotNull(orderFailure.getOrder());
    }

}
