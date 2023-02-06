package ru.yandex.market.checkout.checkouter.order;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.util.CastlingUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.checkout.common.pay.FinancialUtils.roundPrice;


class OrderItemCastlingTest {

    @Test
    void fromMeasurableToMeasurable() {
        BigDecimal originalQuantity = BigDecimal.valueOf(9.97);
        int originalCount = 1;

        BigDecimal originaQuantPrice = BigDecimal.valueOf(15.03);
        BigDecimal originalBuyerPrice = originaQuantPrice.multiply(originalQuantity);
        BigDecimal originalPrice = originaQuantPrice.multiply(originalQuantity);
        BigDecimal everyItemPrice = BigDecimal.valueOf(10.05);

        OrderItem item = buildItem(originalCount, originalQuantity, originaQuantPrice,
                originalBuyerPrice, originalPrice, everyItemPrice);

        BigDecimal expectedQuantity = BigDecimal.valueOf(4.34);
        int expectedCount = 1;
        BigDecimal expectedQuantPrice = originaQuantPrice;
        BigDecimal expectedBuyerPrice = roundPrice(expectedQuantPrice.multiply(expectedQuantity));
        BigDecimal expectedPrice = roundPrice(expectedQuantPrice.multiply(expectedQuantity));
        BigDecimal expectedEveryItemPrice = BigDecimal.valueOf(4.37);

        OrderItem expectedItem = buildItem(expectedCount, expectedQuantity, expectedQuantPrice,
                expectedBuyerPrice, expectedPrice, expectedEveryItemPrice);

        CastlingUtils.castle(expectedQuantity, item);

        assertThat(item.getQuantity(), is(expectedItem.getQuantity()));
        assertThat(item.getQuantPrice(), is(expectedItem.getQuantPrice()));
        assertThat(item.getBuyerPrice(), is(expectedItem.getBuyerPrice()));
        assertThat(item.getPrice(), is(expectedItem.getPrice()));
        assertThat(item.getCount(), is(expectedItem.getCount()));

        assertItemPrices(item, expectedItem);
    }

    @Test
    void fromIntegerToMeasurable() {
        BigDecimal originalQuantity = BigDecimal.valueOf(6);
        int originalCount = 6;

        BigDecimal originaQuantPrice = BigDecimal.valueOf(125.52);
        BigDecimal originalBuyerPrice = originaQuantPrice;
        BigDecimal originalPrice = originaQuantPrice;
        BigDecimal everyItemPrice = BigDecimal.valueOf(100.00);

        OrderItem item = buildItem(originalCount, originalQuantity, originaQuantPrice,
                originalBuyerPrice, originalPrice, everyItemPrice);

        BigDecimal expectedQuantity = BigDecimal.valueOf(7.21);
        int expectedCount = 1;
        BigDecimal expectedQuantPrice = originaQuantPrice;
        BigDecimal expectedBuyerPrice = roundPrice(expectedQuantPrice.multiply(expectedQuantity));
        BigDecimal expectedPrice = roundPrice(expectedQuantPrice.multiply(expectedQuantity));
        BigDecimal expectedEveryItemPrice = BigDecimal.valueOf(721).setScale(2);

        OrderItem expectedItem = buildItem(expectedCount, expectedQuantity, expectedQuantPrice,
                expectedBuyerPrice, expectedPrice, expectedEveryItemPrice);

        CastlingUtils.castle(expectedQuantity, item);

        assertThat(item.getQuantity(), is(expectedItem.getQuantity()));
        assertThat(item.getQuantPrice(), is(expectedItem.getQuantPrice()));
        assertThat(item.getBuyerPrice(), is(expectedItem.getBuyerPrice()));
        assertThat(item.getPrice(), is(expectedItem.getPrice()));
        assertThat(item.getCount(), is(expectedItem.getCount()));

        assertItemPrices(item, expectedItem);
    }

    @Test
    void fromIntegerToInteger() {
        BigDecimal originalQuantity = BigDecimal.valueOf(6);
        int originalCount = 6;

        BigDecimal originaQuantPrice = BigDecimal.valueOf(72.1);
        BigDecimal originalBuyerPrice = originaQuantPrice;
        BigDecimal originalPrice = originaQuantPrice;
        BigDecimal everyItemPrice = BigDecimal.valueOf(99.99);

        OrderItem item = buildItem(originalCount, originalQuantity, originaQuantPrice,
                originalBuyerPrice, originalPrice, everyItemPrice);

        BigDecimal expectedQuantity = BigDecimal.valueOf(12);
        int expectedCount = 12;
        BigDecimal expectedQuantPrice = originaQuantPrice;
        BigDecimal expectedBuyerPrice = originaQuantPrice;
        BigDecimal expectedPrice = originaQuantPrice;
        BigDecimal expectedEveryItemPrice = BigDecimal.valueOf(99.99);

        OrderItem expectedItem = buildItem(expectedCount, expectedQuantity, expectedQuantPrice,
                expectedBuyerPrice, expectedPrice, expectedEveryItemPrice);

        CastlingUtils.castle(expectedQuantity, item);

        assertThat(item.getQuantity(), is(expectedItem.getQuantity()));
        assertThat(item.getQuantPrice(), is(expectedItem.getQuantPrice()));
        assertThat(item.getBuyerPrice(), is(expectedItem.getBuyerPrice()));
        assertThat(item.getPrice(), is(expectedItem.getPrice()));
        assertThat(item.getCount(), is(expectedItem.getCount()));

        assertItemPrices(item, expectedItem);
    }

    @Test
    void fromIntegerToIntegerForMagicOne() {
        BigDecimal originalQuantity = BigDecimal.valueOf(1);
        int originalCount = 1;

        BigDecimal originaQuantPrice = BigDecimal.valueOf(72.1);
        BigDecimal originalBuyerPrice = originaQuantPrice;
        BigDecimal originalPrice = originaQuantPrice;
        BigDecimal everyItemPrice = BigDecimal.valueOf(99.99);

        OrderItem item = buildItem(originalCount, originalQuantity, originaQuantPrice,
                originalBuyerPrice, originalPrice, everyItemPrice);

        BigDecimal expectedQuantity = BigDecimal.valueOf(5);
        int expectedCount = 5;
        BigDecimal expectedQuantPrice = originaQuantPrice;
        BigDecimal expectedBuyerPrice = originaQuantPrice;
        BigDecimal expectedPrice = originaQuantPrice;
        BigDecimal expectedEveryItemPrice = BigDecimal.valueOf(99.99);

        OrderItem expectedItem = buildItem(expectedCount, expectedQuantity, expectedQuantPrice,
                expectedBuyerPrice, expectedPrice, expectedEveryItemPrice);

        CastlingUtils.castle(expectedQuantity, item);

        assertThat(item.getCount(), is(expectedItem.getCount()));
        assertThat(item.getQuantity(), is(expectedItem.getQuantity()));
        assertThat(item.getQuantPrice(), is(expectedItem.getQuantPrice()));
        assertThat(item.getBuyerPrice(), is(expectedItem.getBuyerPrice()));
        assertThat(item.getPrice(), is(expectedItem.getPrice()));

        assertItemPrices(item, expectedItem);
    }


    @Test
    void fromMeasurableToInteger() {
        BigDecimal originalQuantity = BigDecimal.valueOf(6.12);
        int originalCount = 1;
        BigDecimal originaQuantPrice = BigDecimal.valueOf(98.32);
        BigDecimal originalBuyerPrice = originaQuantPrice.multiply(originalQuantity);
        BigDecimal originalPrice = originaQuantPrice.multiply(originalQuantity);
        BigDecimal everyItemPrice = BigDecimal.valueOf(99.99);

        OrderItem item = buildItem(originalCount, originalQuantity, originaQuantPrice,
                originalBuyerPrice, originalPrice, everyItemPrice);

        BigDecimal expectedQuantity = BigDecimal.valueOf(9);
        int expectedCount = 1;
        BigDecimal expectedQuantPrice = originaQuantPrice;
        BigDecimal expectedBuyerPrice = originaQuantPrice.multiply(expectedQuantity);
        BigDecimal expectedPrice = roundPrice(originaQuantPrice.multiply(expectedQuantity));
        BigDecimal expectedEveryItemPrice = BigDecimal.valueOf(147.04);

        OrderItem expectedItem = buildItem(expectedCount, expectedQuantity, expectedQuantPrice,
                expectedBuyerPrice, expectedPrice, expectedEveryItemPrice);

        CastlingUtils.castle(expectedQuantity, item);

        assertThat(item.getQuantity(), is(expectedItem.getQuantity()));
        assertThat(item.getQuantPrice(), is(expectedItem.getQuantPrice()));
        assertThat(item.getBuyerPrice(), is(expectedItem.getBuyerPrice()));
        assertThat(item.getPrice(), is(expectedItem.getPrice()));
        assertThat(item.getCount(), is(expectedItem.getCount()));

        assertItemPrices(item, expectedItem);
    }

    @Test
    void doubleCastlingForItemWithEmptyQuantity() {
        BigDecimal originalQuantity = null;
        int originalCount = 1;
        BigDecimal originaQuantPrice = BigDecimal.valueOf(98.32);
        BigDecimal originalBuyerPrice = originaQuantPrice;
        BigDecimal originalPrice = originaQuantPrice;
        BigDecimal everyItemPrice = BigDecimal.valueOf(99.99);

        OrderItem item = buildItem(originalCount, originalQuantity, originaQuantPrice,
                originalBuyerPrice, originalPrice, everyItemPrice);

        CastlingUtils.castle(BigDecimal.valueOf(4.22), item);
        CastlingUtils.castle(BigDecimal.valueOf(9.12), item);

        BigDecimal expectedQuantity = BigDecimal.valueOf(9.12);
        int expectedCount = 1;
        BigDecimal expectedQuantPrice = originaQuantPrice;
        BigDecimal expectedBuyerPrice = roundPrice(expectedQuantPrice.multiply(expectedQuantity));
        BigDecimal expectedPrice = roundPrice(expectedQuantPrice.multiply(expectedQuantity));
        BigDecimal expectedEveryItemPrice = BigDecimal.valueOf(911.91);

        OrderItem expectedItem = buildItem(expectedCount, expectedQuantity, expectedQuantPrice,
                expectedBuyerPrice, expectedPrice, expectedEveryItemPrice);

        CastlingUtils.castle(expectedQuantity, item);

        assertThat(item.getQuantity(), is(expectedItem.getQuantity()));
        assertThat(item.getQuantPrice(), is(expectedItem.getQuantPrice()));
        assertThat(item.getBuyerPrice(), is(expectedItem.getBuyerPrice()));
        assertThat(item.getPrice(), is(expectedItem.getPrice()));
        assertThat(item.getCount(), is(expectedItem.getCount()));

        assertItemPrices(item, expectedItem);
    }

    @Test
    void castlingForItemWithEmptyQuantPrice() {
        BigDecimal originalQuantity = BigDecimal.valueOf(1);
        int originalCount = 1;
        BigDecimal originaQuantPrice = null;
        BigDecimal originalBuyerPrice = BigDecimal.valueOf(34.21);
        BigDecimal originalPrice = BigDecimal.valueOf(34.21);
        BigDecimal everyItemPrice = BigDecimal.valueOf(99.99);

        OrderItem item = buildItem(originalCount, originalQuantity, originaQuantPrice,
                originalBuyerPrice, originalPrice, everyItemPrice);

        CastlingUtils.castle(BigDecimal.valueOf(4.22), item);

        BigDecimal expectedQuantity = BigDecimal.valueOf(4.22);
        int expectedCount = 1;
        BigDecimal expectedQuantPrice = originalBuyerPrice;
        BigDecimal expectedBuyerPrice = roundPrice(expectedQuantPrice.multiply(expectedQuantity));
        BigDecimal expectedPrice = roundPrice(expectedQuantPrice.multiply(expectedQuantity));
        BigDecimal expectedEveryItemPrice = BigDecimal.valueOf(421.96);

        OrderItem expectedItem = buildItem(expectedCount, expectedQuantity, expectedQuantPrice,
                expectedBuyerPrice, expectedPrice, expectedEveryItemPrice);

        CastlingUtils.castle(expectedQuantity, item);

        assertThat(item.getQuantity(), is(expectedItem.getQuantity()));
        assertThat(item.getQuantPrice(), is(expectedItem.getQuantPrice()));
        assertThat(item.getBuyerPrice(), is(expectedItem.getBuyerPrice()));
        assertThat(item.getPrice(), is(expectedItem.getPrice()));
        assertThat(item.getCount(), is(expectedItem.getCount()));

        assertItemPrices(item, expectedItem);
    }

    private OrderItem buildItem(int count, BigDecimal quantity, BigDecimal quantPrice,
                                BigDecimal buyerPrice, BigDecimal price, BigDecimal itemPrice) {
        OrderItem item = new OrderItem();
        item.setQuantPrice(quantPrice);
        item.setBuyerPrice(buyerPrice);
        item.setPrice(price);
        item.setQuantity(quantity);
        item.setCount(count);
        setPrices(item, itemPrice);
        return item;
    }

    private void setPrices(OrderItem item, BigDecimal price) {
        var itemPrices = item.getPrices();
        itemPrices.setBuyerDiscount(price);
        itemPrices.setPriceWithoutVat(price);
        itemPrices.setFeedPrice(price);
        itemPrices.setClientBuyerPrice(price);
        itemPrices.setPartnerPrice(price);
        itemPrices.setBuyerPriceBeforeDiscount(price);
        itemPrices.setReportPrice(price);
        itemPrices.setBuyerSubsidy(price);
        itemPrices.setSubsidy(price);
        itemPrices.setBuyerPriceNominal(price);
        itemPrices.setOldMin(price);
        itemPrices.setOldDiscountOldMin(price);
    }

    private void assertItemPrices(OrderItem actualItem, OrderItem expectedItem) {
        ItemPrices actualPrices = actualItem.getPrices();
        ItemPrices expectedPrices = expectedItem.getPrices();

        assertThat(actualPrices.getSubsidy(), is(expectedPrices.getSubsidy()));
        assertThat(actualPrices.getBuyerDiscount(), is(expectedPrices.getBuyerDiscount()));
        assertThat(actualPrices.getFeedPrice(), is(expectedPrices.getFeedPrice()));
        assertThat(actualPrices.getPartnerPrice(), is(expectedPrices.getPartnerPrice()));
        assertThat(actualPrices.getReportPrice(), is(expectedPrices.getReportPrice()));
        assertThat(actualPrices.getBuyerSubsidy(), is(expectedPrices.getBuyerSubsidy()));
        assertThat(actualPrices.getBuyerPriceBeforeDiscount(), is(expectedPrices.getBuyerPriceBeforeDiscount()));
        assertThat(actualPrices.getBuyerPriceNominal(), is(expectedPrices.getBuyerPriceNominal()));
        assertThat(actualPrices.getPriceWithoutVat(), is(expectedPrices.getPriceWithoutVat()));
        assertThat(actualPrices.getOldMin(), is(expectedPrices.getOldMin()));
        assertThat(actualPrices.getOldDiscountOldMin(), is(expectedPrices.getOldDiscountOldMin()));
        assertThat(actualPrices.getClientBuyerPrice(), is(expectedPrices.getClientBuyerPrice()));
    }

}
