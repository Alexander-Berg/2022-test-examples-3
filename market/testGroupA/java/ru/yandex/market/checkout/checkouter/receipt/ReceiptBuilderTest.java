package ru.yandex.market.checkout.checkouter.receipt;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Unit тесты для {@link ReceiptBuilder}.
 *
 * @author avetokhin 08/06/17.
 */
public class ReceiptBuilderTest {

    private static final long ORDER_ID = 11;

    private static final long ITEM_ID_1 = 1;
    private static final long ITEM_ID_2 = 2;

    private static final String ITEM_TITLE_1 = "title1";
    private static final String ITEM_TITLE_2 = "title2";
    private static final int ITEM_COUNT_1 = 15;
    private static final int ITEM_COUNT_2 = 10;
    private static final BigDecimal ITEM_PRICE_1 = new BigDecimal(1000);
    private static final BigDecimal ITEM_PRICE_2 = new BigDecimal(500);
    private static final BigDecimal ITEM_AMOUNT_1 = new BigDecimal(15000).setScale(2);
    private static final BigDecimal ITEM_AMOUNT_2 = new BigDecimal(5000).setScale(2);

    private static final long DELIVERY_ID = 10;
    private static final BigDecimal DELIVERY_PRICE = new BigDecimal(100500);

    private static final ReceiptType RECEIPT_TYPE = ReceiptType.INCOME;
    private static final Long PAYMENT_ID = 10L;

    /**
     * Проверить, что сформировался корректный чек.
     */
    @Test
    public void testBuild() {
        final Order order = initOrder();

        final Receipt receipt = ReceiptBuilder.newPrintable()
                .withType(RECEIPT_TYPE)
                .withPaymentId(PAYMENT_ID)
                .buildFromOrders(Collections.singletonList(order));

        assertThat(receipt, notNullValue());
        assertThat(receipt.getType(), equalTo(RECEIPT_TYPE));
        assertThat(receipt.getPaymentId(), equalTo(PAYMENT_ID));
        assertThat(receipt.getRefundId(), nullValue());
        assertThat(receipt.getStatus(), equalTo(ReceiptStatus.NEW));
        assertThat(receipt.isPrintable(), equalTo(true));

        final List<ReceiptItem> items = receipt.getItems();
        assertThat(items, notNullValue());
        assertThat(items, hasSize(3));

        checkReceiptItem(items.get(0), ITEM_ID_1, null, null, ITEM_TITLE_1, ITEM_COUNT_1, ITEM_PRICE_1, ITEM_AMOUNT_1);
        checkReceiptItem(items.get(1), ITEM_ID_2, null, null, ITEM_TITLE_2, ITEM_COUNT_2, ITEM_PRICE_2, ITEM_AMOUNT_2);
        checkReceiptItem(items.get(2), null, null, DELIVERY_ID, "Доставка", 1, DELIVERY_PRICE, DELIVERY_PRICE);
    }

    @Test
    public void liftingTest() {
        final Order freeLiftingOrder = initOrder();
        final Order notFreeLiftingOrder = initOrder();

        freeLiftingOrder.getDelivery().setLiftPrice(BigDecimal.ZERO);
        freeLiftingOrder.getDelivery().setPrice(BigDecimal.ZERO);
        freeLiftingOrder.setInternalDeliveryId(666L);
        freeLiftingOrder.setItems(Collections.emptySet());

        notFreeLiftingOrder.getDelivery().setLiftPrice(BigDecimal.ONE);
        notFreeLiftingOrder.getDelivery().setPrice(BigDecimal.ZERO);
        notFreeLiftingOrder.setInternalDeliveryId(777L);
        notFreeLiftingOrder.setItems(Collections.emptySet());

        final Receipt receipt = ReceiptBuilder.newPrintable()
                .buildFromOrders(Arrays.asList(freeLiftingOrder, notFreeLiftingOrder));

        // элемент один, пришедший от notFreeLiftingOrder
        assertThat(receipt.getItems(), hasSize(1));

        assertThat(receipt.getItems().get(0).getDeliveryId(), equalTo(777L));
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private void checkReceiptItem(final ReceiptItem item, final Long itemId, final Long itemServiceId,
                                  final Long deliveryId, final String title,
                                  final int count, final BigDecimal price, final BigDecimal amount) {
        assertThat(item.getOrderId(), equalTo(ORDER_ID));
        assertThat(item.getItemId(), equalTo(itemId));
        assertThat(item.getItemServiceId(), equalTo(itemServiceId));
        assertThat(item.getDeliveryId(), equalTo(deliveryId));
        assertThat(item.getItemTitle(), equalTo(title));
        assertThat(item.getCount(), equalTo(count));
        assertThat(item.getPrice(), equalTo(price));
        assertThat(item.getAmount(), equalTo(amount));
    }

    private Order initOrder() {
        final Order order = new Order();
        order.setId(ORDER_ID);

        order.setInternalDeliveryId(DELIVERY_ID);
        final Delivery delivery = new Delivery();
        delivery.setPrice(DELIVERY_PRICE);
        delivery.setBuyerPrice(DELIVERY_PRICE);
        order.setDelivery(delivery);

        final OrderItem item1 = item(ITEM_ID_1, ITEM_TITLE_1, ITEM_PRICE_1, ITEM_COUNT_1);
        final OrderItem item2 = item(ITEM_ID_2, ITEM_TITLE_2, ITEM_PRICE_2, ITEM_COUNT_2);

        order.setItems(Arrays.asList(item1, item2));
        return order;
    }

    private static OrderItem item(final long id, final String title, final BigDecimal price, final int count) {
        final OrderItem item = new OrderItem();
        item.setId(id);
        item.setOfferId("" + id);
        item.setOfferName(title);
        item.setPrice(price.add(new BigDecimal(1)));
        item.setBuyerPrice(price);
        item.setQuantPrice(price);
        item.setCount(count);

        return item;
    }
}
