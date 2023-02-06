package ru.yandex.market.checkout.checkouter.returns.domain.buyers;

import java.time.LocalTime;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureReader;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.returns.ReturnDelivery;
import ru.yandex.market.checkout.checkouter.returns.ReturnItem;
import ru.yandex.market.checkout.checkouter.returns.ReturnReasonType;
import ru.yandex.market.checkout.checkouter.returns.domain.ReturnIdempotentKey;
import ru.yandex.market.checkout.test.providers.AddressProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryType.DELIVERY;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryType.PICKUP;
import static ru.yandex.market.checkout.checkouter.returns.ReturnReasonType.BAD_QUALITY;
import static ru.yandex.market.checkout.checkouter.returns.ReturnReasonType.CONTENT_FAIL;

class ReturnIdempotentKeyTest {

    private final CheckouterFeatureReader checkouterFeatureReader = Mockito.mock(CheckouterFeatureReader.class);

    @Test
    void withOneItem() {
        Order order = makeOrder(20, 123);
        Return aReturn = makeReturn(PICKUP);
        aReturn.setItems(List.of(makeItem(12, 2, BAD_QUALITY)));

        ReturnIdempotentKey key = ReturnIdempotentKey.generateFrom(checkouterFeatureReader, aReturn, order);

        assertThat(key.toString()).isNotEmpty();
    }

    @Test
    void withOneItemAndCourierDelivery() {
        Order order = makeOrder(20, 123);
        Return aReturn = makeReturn(DELIVERY);
        aReturn.setItems(List.of(makeItem(12, 2, BAD_QUALITY)));

        ReturnIdempotentKey key = ReturnIdempotentKey.generateFrom(checkouterFeatureReader, aReturn, order);

        assertThat(key.toString()).isNotEmpty();
    }

    @Test
    void withOneItemAndReasonComment() {
        Order order = makeOrder(20, 123);
        Return aReturn = makeReturn(PICKUP);
        ReturnItem item = makeItem(12, 2, BAD_QUALITY);
        String comment = "Все поломалось, верните деньги!";
        item.setReturnReason(comment);
        aReturn.setItems(List.of(item));

        ReturnIdempotentKey key = ReturnIdempotentKey.generateFrom(checkouterFeatureReader, aReturn, order);

        assertThat(key.toString()).isNotEmpty();
    }

    @Test
    void withTwoItems() {
        Order order = makeOrder(20, 123);
        Return aReturn = makeReturn(PICKUP);
        aReturn.setItems(List.of(makeItem(12, 2, BAD_QUALITY),
                makeItem(13, 1, CONTENT_FAIL)));
        ReturnIdempotentKey key = ReturnIdempotentKey.generateFrom(checkouterFeatureReader, aReturn, order);

        assertThat(key.toString()).isNotEmpty();
    }

    @Test
    void withDeliveryItem() {
        Order order = makeOrder(20, 123);
        Return aReturn = makeReturn(PICKUP);
        aReturn.setItems(List.of(makeItem(12, 2, BAD_QUALITY),
                makeDeliveryItem(321)));

        ReturnIdempotentKey key = ReturnIdempotentKey.generateFrom(checkouterFeatureReader, aReturn, order);

        assertThat(key.toString()).isNotEmpty();
    }

    @Test
    void withServiceItem() {
        Order order = makeOrder(20, 123);
        Return aReturn = makeReturn(PICKUP);
        aReturn.setItems(List.of(makeItem(12, 2, BAD_QUALITY),
                makeServiceItem(323)));

        ReturnIdempotentKey key = ReturnIdempotentKey.generateFrom(checkouterFeatureReader, aReturn, order);

        assertThat(key.toString()).isNotEmpty();
    }

    private ReturnItem makeItem(long orderItemId, int count, ReturnReasonType type) {
        ReturnItem item = new ReturnItem();
        item.setItemId(orderItemId);
        item.setCount(count);
        item.setReasonType(type);
        return item;
    }

    private ReturnItem makeDeliveryItem(long orderDeliveryServiceId) {
        ReturnItem item = new ReturnItem();
        item.setOrderDeliveryId(orderDeliveryServiceId);
        item.setCount(1);
        return item;
    }

    private ReturnItem makeServiceItem(long orderServiceId) {
        ReturnItem item = new ReturnItem();
        item.setItemServiceId(orderServiceId);
        item.setCount(1);
        return item;
    }

    private Order makeOrder(long id, long userId) {
        Order order = new Order();
        order.setId(id);
        order.setBuyer(new Buyer(userId));
        return order;
    }

    private Return makeReturn(DeliveryType deliveryType) {
        Return newReturn = new Return();
        newReturn.setDelivery(makeReturnDelivery(deliveryType));
        return newReturn;
    }

    private ReturnDelivery makeReturnDelivery(DeliveryType type) {
        ReturnDelivery delivery = new ReturnDelivery();
        delivery.setType(type);
        switch (type) {
            case DELIVERY:
                delivery.setDates(makeDeliveryDates());
                delivery.setSenderAddress(AddressProvider.getSenderAddress());
                break;
            case PICKUP:
            case POST:
                delivery.setOutletId(12345876L);
                break;
            default:
                throw new IllegalArgumentException("unsupported delivery type " + type);
        }
        return delivery;
    }

    private DeliveryDates makeDeliveryDates() {
        return new DeliveryDates(new Date(), new Date(), LocalTime.of(10, 30), LocalTime.of(15, 30));
    }
}
