package ru.yandex.market.notifier.xml;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.returns.ReturnDelivery;
import ru.yandex.market.checkout.checkouter.returns.ReturnItem;
import ru.yandex.market.checkout.checkouter.returns.ReturnReasonType;
import ru.yandex.market.checkout.checkouter.returns.ReturnStatus;

class AboInfoFormatterTest {

    @Test
    void testAboInfoMarshalling() {
        long orderId = 123L;
        long total = 100500L;
        Calendar c = Calendar.getInstance();
        c.set(2021, Calendar.FEBRUARY, 28, 15, 26);
        Date creationDate = c.getTime();

        Order order = new Order();
        order.setId(orderId);
        order.setTotal(BigDecimal.valueOf(total));
        order.setCreationDate(creationDate);

        String actual = AboInfoFormatter.getXml(order);
        String expected = "<abo-info order-id=\"123\" order-sum=\"100500\" order-date=\"28.02.2021\" " +
                "dbs-confirmation-time=\"17:26\" dbs-confirmation-datetime=\"17:26 28 февраля\"/>";

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void testAboInfoMarshallingWithConfirmationDate() {
        long orderId = 123L;
        long total = 100500L;
        Calendar c = Calendar.getInstance();
        c.set(2021, Calendar.FEBRUARY, 28, 15, 26);
        Date creationDate = c.getTime();

        Order order = new Order();
        order.setId(orderId);
        order.setTotal(BigDecimal.valueOf(total));
        order.setCreationDate(creationDate);

        Date confirmationDate = DateUtil.addHour(order.getCreationDate(), 5);

        String actual = AboInfoFormatter.getXml(order, confirmationDate);
        String expected = "<abo-info order-id=\"123\" order-sum=\"100500\" order-date=\"28.02.2021\" " +
                "dbs-confirmation-time=\"20:26\" dbs-confirmation-datetime=\"20:26 28 февраля\"/>";

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void testAboInfoMarshallingWithReturnItems() {
        long orderId = 123L;
        long total = 100500L;
        Calendar c = Calendar.getInstance();
        c.set(2021, Calendar.FEBRUARY, 28, 15, 26);
        Date creationDate = c.getTime();

        Order order = new Order();
        order.setId(orderId);
        order.setTotal(BigDecimal.valueOf(total));
        order.setCreationDate(creationDate);

        Track track = new Track();
        track.setTrackCode("RR123456788RU");

        ReturnDelivery returnDelivery = new ReturnDelivery();
        returnDelivery.setTrack(track);

        Return orderReturn = new Return();
        orderReturn.setStatus(ReturnStatus.STARTED_BY_USER);
        orderReturn.setId(orderId);
        orderReturn.setDelivery(returnDelivery);

        List<Pair<OrderItem, ReturnItem>> items = List.of(
                createItemPair("Item 1", "123", "1.123", 10,
                        ReturnReasonType.WRONG_ITEM, "comment 1"),
                createItemPair("Item 2", "345", "2.345", 1,
                        null,  null),
                createItemPair("Item 3", "567", "3.567", 100,
                        ReturnReasonType.DAMAGE_DELIVERY, null)
        );

        String actual = AboInfoFormatter.getXml(order, orderReturn, items);
        String expected = "<abo-info order-id=\"123\" order-sum=\"100500\" order-date=\"28.02.2021\" " +
                "dbs-confirmation-time=\"17:26\" dbs-confirmation-datetime=\"17:26 28 февраля\" return-id=\"123\" " +
                "return-track-code=\"RR123456788RU\" reason=\"comment 1\">\n" +
                "  <offers>\n" +
                "    <offer title=\"Item 1\" sku=\"123\" offer-id=\"1.123\" count=\"10\">\n" +
                "      <return>\n" +
                "        <reason-type disable-markdown-escaping=\"true\">WRONG_ITEM</reason-type>\n" +
                "        <reason-comment>comment 1</reason-comment>\n" +
                "      </return>\n" +
                "    </offer>\n" +
                "    <offer title=\"Item 2\" sku=\"345\" offer-id=\"2.345\" count=\"1\">\n" +
                "      <return/>\n" +
                "    </offer>\n" +
                "    <offer title=\"Item 3\" sku=\"567\" offer-id=\"3.567\" count=\"100\">\n" +
                "      <return>\n" +
                "        <reason-type disable-markdown-escaping=\"true\">DAMAGE_DELIVERY</reason-type>\n" +
                "      </return>\n" +
                "    </offer>\n" +
                "  </offers>\n" +
                "</abo-info>";

        Assertions.assertEquals(expected, actual);
    }

    private Pair<OrderItem, ReturnItem> createItemPair(String title, String sku, String offerId, int count,
                                                       ReturnReasonType reasonType, String returnReasonComment) {
        OrderItem orderItem = new OrderItem();
        orderItem.setOfferId(offerId);
        orderItem.setShopSku(sku);
        orderItem.setCount(count);
        orderItem.setOfferName(title);

        ReturnItem returnItem = new ReturnItem();
        returnItem.setReasonType(reasonType);
        returnItem.setReturnReason(returnReasonComment);

        return new Pair<>(orderItem, returnItem);
    }
}
