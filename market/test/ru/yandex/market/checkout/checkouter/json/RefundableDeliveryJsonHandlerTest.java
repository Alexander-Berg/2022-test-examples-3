package ru.yandex.market.checkout.checkouter.json;

import java.math.BigDecimal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.json.helper.EntityHelper;
import ru.yandex.market.checkout.checkouter.pay.RefundableDelivery;

public class RefundableDeliveryJsonHandlerTest extends AbstractJsonHandlerTestBase {

    @Test
    public void deserialize() throws Exception {
        String json = "{\"type\":\"DELIVERY\",\"serviceName\":\"serviceName\",\"price\":12.34,\"buyerPrice\":34.56," +
                "\"refundable\":true}";

        RefundableDelivery refundableDelivery = read(RefundableDelivery.class, json);

        Assertions.assertEquals(DeliveryType.DELIVERY, refundableDelivery.getType());
        Assertions.assertEquals("serviceName", refundableDelivery.getServiceName());
        Assertions.assertEquals(new BigDecimal("12.34"), refundableDelivery.getPrice());
        Assertions.assertEquals(new BigDecimal("34.56"), refundableDelivery.getBuyerPrice());
        Assertions.assertTrue(refundableDelivery.isRefundable());
    }

    @Test
    public void serialize() throws Exception {
        RefundableDelivery refundableDelivery = EntityHelper.getRefundableDelivery();

        String json = write(refundableDelivery);
        System.out.println(json);

        checkJson(json, "$." + Names.Delivery.TYPE, DeliveryType.DELIVERY.name());
        checkJson(json, "$." + Names.Delivery.SERVICE_NAME, "serviceName");
        checkJson(json, "$." + Names.Delivery.PRICE, 12.34);
        checkJson(json, "$." + Names.Delivery.BUYER_PRICE, 34.56);
        checkJson(json, "$." + Names.RefundableDelivery.REFUNDABLE, true);
    }
}
