package ru.yandex.market.checkout.referee.entity.json;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Date;

import org.junit.jupiter.api.Test;

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.referee.entity.AbstractJsonHandlerTest;
import ru.yandex.market.checkout.referee.entity.CheckoutRefereeHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author kukabara
 */
public class OrderJsonTest extends AbstractJsonHandlerTest {

    @Test
    public void test() throws Exception {
        Order expectedObj = CheckoutRefereeHelper.getOrder();
        String json = write(expectedObj);

        checkJson(json, "$.id", CheckoutRefereeHelper.ID);
        checkJson(json, "$.shopId", CheckoutRefereeHelper.SHOP_ID);
        checkJson(json, "$.creationDate", "27-01-2014 00:26:05");
        checkJson(json, "$.deliveryOptions[0].dates.fromDate", "24-01-2014");
        checkJson(json, "$.deliveryOptions[0].dates.toDate", "27-01-2014");

        InputStream is = new ByteArrayInputStream(json.getBytes());
        Order obj = read(Order.class, is);
        assertEquals(expectedObj.getCreationDate().getTime(), obj.getCreationDate().getTime());
        Date expFromDate = expectedObj.getDeliveryOptions().iterator().next().getDeliveryDates().getFromDate();
        Date fromDate = obj.getDeliveryOptions().iterator().next().getDeliveryDates().getFromDate();
        assertEquals(DateUtil.truncDay(expFromDate), DateUtil.truncDay(fromDate));

        Date expToDate = expectedObj.getDeliveryOptions().iterator().next().getDeliveryDates().getToDate();
        Date toDate = obj.getDeliveryOptions().iterator().next().getDeliveryDates().getToDate();
        assertEquals(DateUtil.truncDay(expToDate), DateUtil.truncDay(toDate));
    }
}
