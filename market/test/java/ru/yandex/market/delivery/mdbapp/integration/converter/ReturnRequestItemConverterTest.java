package ru.yandex.market.delivery.mdbapp.integration.converter;

import java.math.BigDecimal;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.market.checkout.checkouter.order.OfferItemKey;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderItemsException;
import ru.yandex.market.checkout.checkouter.returns.ReturnItem;
import ru.yandex.market.checkout.checkouter.returns.ReturnReasonType;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.ReturnRequestItem;

import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.ITEM_ID_1;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.ITEM_NAME;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.ITEM_SKU_1;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.ITEM_SUPPLIER_1;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.PRICE_1;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.RETURN_ITEM_REASON;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.item;

public class ReturnRequestItemConverterTest {

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    public ReturnRequestItemConverter converter = new ReturnRequestItemConverter(
        new ReturnTypeConverter(),
        new EnumConverter()
    );

    @Test
    public void shouldSuccessfullyConvert() {
        // given:
        final ReturnItem returnItem = returnItem();
        final Order order = new Order();
        order.addItem(orderItem());

        // and:
        final ReturnRequestItem expected = item(ITEM_ID_1, PRICE_1, ITEM_SKU_1, ITEM_SUPPLIER_1).setCount(3);

        // given:
        final var actual = converter.convert(returnItem, order);

        // then:
        softly.assertThat(actual).isEqualToComparingOnlyGivenFields(expected);
    }

    @Test(expected = OrderItemsException.class)
    public void shouldThrowOrderItemsException() {
        // when:
        converter.convert(returnItem(), new Order());
    }

    private ReturnItem returnItem() {
        final ReturnItem returnItem = new ReturnItem();
        returnItem.setItemId(ITEM_ID_1);
        returnItem.setItemTitle(ITEM_NAME);
        returnItem.setReasonType(ReturnReasonType.BAD_QUALITY);
        returnItem.setReturnReason(RETURN_ITEM_REASON);
        returnItem.setCount(3);
        return returnItem;
    }

    private OrderItem orderItem() {
        OrderItem orderItem = new OrderItem();
        orderItem.setId(ITEM_ID_1);
        orderItem.setShopSku(ITEM_SKU_1);
        orderItem.setPrice(BigDecimal.valueOf(PRICE_1));
        orderItem.setOfferItemKey(OfferItemKey.of(null, ITEM_ID_1, null));
        return orderItem;
    }
}
