package ru.yandex.market.checkout.checkouter.service.business;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.promo.ItemPromo;
import ru.yandex.market.checkout.checkouter.order.promo.PromoDefinition;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.checkout.checkouter.util.CastlingUtils;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;

import static java.math.BigDecimal.ZERO;
import static java.math.BigDecimal.valueOf;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.number.BigDecimalCloseTo.closeTo;
import static ru.yandex.market.checkout.checkouter.order.promo.PromoDefinition.marketCouponPromo;
import static ru.yandex.market.checkout.checkouter.order.promo.PromoDefinition.marketDealPromo;

/**
 * @author Nicolai Iusiumbeli <mailto:armor@yandex-team.ru>
 * date: 26/05/2017
 */
public class OrderFinancialServiceImplTest extends AbstractServicesTestBase {

    private BigDecimal error = new BigDecimal("0.000000000000001");

    @Autowired
    private OrderFinancialServiceImpl financialService;

    @Test
    public void calculateItemsSubsidies() {
        Order order = new Order();
        OrderItem item = OrderItemProvider.getOrderItem();
        List<OrderItem> orderItems = singletonList(item);
        order.setItems(orderItems);
        financialService.calculateAndFillItemsTotals(order);
        assertThat(item.getPrices().getSubsidy(), nullValue());
        assertThat(item.getPrices().getBuyerDiscount(), nullValue());

        item.addOrReplacePromo(new ItemPromo(marketDealPromo("promoKey1"), null, null, null));
        financialService.calculateAndFillItemsTotals(order);
        assertThat(item.getPrices().getSubsidy(), closeTo(ZERO, error));
        assertThat(item.getPrices().getBuyerDiscount(), closeTo(ZERO, error));

        item.addOrReplacePromo(new ItemPromo(marketCouponPromo(), null, new BigDecimal("123.44"), null));
        financialService.calculateAndFillItemsTotals(order);
        assertThat(item.getPrices().getSubsidy(), closeTo(new BigDecimal("123.44"), error));
        assertThat(item.getPrices().getBuyerDiscount(), closeTo(ZERO, error));


        ItemPromo promo = new ItemPromo(marketDealPromo("promoKey2"), new BigDecimal("55.57"), new BigDecimal("123" +
                ".44"), null);
        item.addOrReplacePromo(promo);
        financialService.calculateAndFillItemsTotals(order);
        assertThat(item.getPrices().getSubsidy(), closeTo(new BigDecimal("246.88"), error));
        assertThat(item.getPrices().getBuyerDiscount(), closeTo(new BigDecimal("55.57"), error));
    }

    @Test
    public void calculateItemsSubsidiesMultiple() {
        OrderItem item1 = OrderItemProvider.getOrderItem();
        OrderItem item2 = OrderItemProvider.getOrderItem();
        Order order = new Order();
        order.setItems(Arrays.asList(item1, item2));

        item1.addOrReplacePromo(new ItemPromo(marketCouponPromo(), new BigDecimal("123.44"), new BigDecimal("55.57"),
                null));
        item1.addOrReplacePromo(new ItemPromo(marketDealPromo("key1"), new BigDecimal("123.44"),
                new BigDecimal("55.57"), null));

        item2.addOrReplacePromo(new ItemPromo(marketDealPromo("key2"), new BigDecimal("1.23"),
                new BigDecimal("2.44"), null));
        item2.addOrReplacePromo(new ItemPromo(marketCouponPromo(), new BigDecimal("4.34"),
                new BigDecimal("7.00"), null));

        financialService.calculateAndFillItemsTotals(order);

        assertThat(item1.getPrices().getSubsidy(), closeTo(new BigDecimal("111.14"), error));
        assertThat(item1.getPrices().getBuyerDiscount(), closeTo(new BigDecimal("246.88"), error));

        assertThat(item2.getPrices().getSubsidy(), closeTo(new BigDecimal("9.44"), error));
        assertThat(item2.getPrices().getBuyerDiscount(), closeTo(new BigDecimal("5.57"), error));
    }

    @Test
    public void testItemSubsidiesRoundUp() throws Exception {
        ItemPromo coupon = new ItemPromo(marketCouponPromo(), new BigDecimal("222.4499999999"), new BigDecimal("123" +
                ".4499999999"), new BigDecimal("55.573333333333"));
        OrderItem item = OrderItemProvider.getOrderItem();
        item.addOrReplacePromo(coupon);
        List<OrderItem> items = singletonList(item);
        Order order = new Order();
        order.setItems(items);
        financialService.calculateAndFillItemsTotals(order);
        financialService.roundUpItemsPrices(items);

        assertThat(coupon.getSubsidy(), closeTo(new BigDecimal("123.4499999999"), error));
        assertThat(coupon.getBuyerSubsidy(), closeTo(new BigDecimal("55.573333333333"), error));
        assertThat(coupon.getBuyerDiscount(), closeTo(new BigDecimal("222.4499999999"), error));

        assertThat(item.getPrices().getSubsidy(), closeTo(new BigDecimal("123.45"), error));
        assertThat(item.getPrices().getBuyerSubsidy(), closeTo(new BigDecimal("55.57"), error));
        assertThat(item.getPrices().getBuyerDiscount(), closeTo(new BigDecimal("222.45"), error));
    }

    @Test
    public void testDeliveryPromoTotals() {
        Delivery delivery = DeliveryProvider.getYandexMarketPickupDelivery();
        ItemPromo deliveryPromo = new ItemPromo(PromoDefinition.builder()
                .type(PromoType.FREE_DELIVERY_THRESHOLD)
                .build(),
                delivery.getBuyerPrice(), ZERO, ZERO);
        delivery.addOrReplacePromo(deliveryPromo);
        financialService.calculateAndFillDeliveryTotals(delivery, Function.identity());
        assertThat(delivery.getBuyerPrice(), Matchers.comparesEqualTo(ZERO));
    }

    @Test
    public void calculateAndSetVatTotal_ifItemHasPriceWithoutVatThenVatIsTotalSubtractPriceWithoutVat() {
        BigDecimal total = valueOf(100L);
        BigDecimal priceWithoutVat = valueOf(11L);
        BigDecimal price = valueOf(12L);
        Order order = makeOrder(total, priceWithoutVat, price);

        financialService.calculateAndSetVatTotal(order);

        Assertions.assertEquals(
                valueOf(100L - 11L).doubleValue(),
                order.getVatTotal().doubleValue(),
                error.doubleValue());
    }

    @NotNull
    private Order makeOrder(BigDecimal total, BigDecimal priceWithoutVat, BigDecimal price) {
        Order order = new Order();
        order.setBuyerTotal(total);
        order.setDelivery(null);
        OrderItem oi = getOrderItem(priceWithoutVat, price);
        order.setItems(List.of(oi));
        return order;
    }

    @NotNull
    private OrderItem getOrderItem(BigDecimal priceWithoutVat, BigDecimal price) {
        OrderItem oi = getOrderItem(priceWithoutVat);
        if (price != null) {
            oi.setPrice(price);
            oi.setBuyerPrice(price);
            oi.getPrices().setBuyerPriceBeforeDiscount(price);
        }
        CastlingUtils.castle(BigDecimal.ONE, oi);
        return oi;
    }

    @NotNull
    private OrderItem getOrderItem(BigDecimal priceWithoutVat) {
        OrderItem orderItem = new OrderItem(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                priceWithoutVat,
                null
        );
        orderItem.getPrices().setReportPriceWithoutVat(priceWithoutVat);
        return orderItem;
    }

    @Test
    public void calculateAndSetVatTotal_ifItemDoesNotHasPriceWithoutVatThenVatIsTotalSubtractPrice() {
        BigDecimal total = valueOf(100L);
        BigDecimal priceWithoutVat = null;
        BigDecimal price = valueOf(12L);
        Order order = makeOrder(total, priceWithoutVat, price);

        financialService.calculateAndSetVatTotal(order);

        Assertions.assertEquals(
                valueOf(100L - 12L).doubleValue(),
                order.getVatTotal().doubleValue(),
                error.doubleValue());
    }

    @Test
    public void calculateAndSetVatTotal_ifItemDoesNotHasPriceThenVatIsTotalSubtractZero() {
        BigDecimal total = valueOf(100L);
        BigDecimal priceWithoutVat = null;
        BigDecimal price = null;
        Order order = makeOrder(total, priceWithoutVat, price);

        financialService.calculateAndSetVatTotal(order);

        Assertions.assertEquals(
                valueOf(100L).doubleValue(),
                order.getVatTotal().doubleValue(),
                error.doubleValue());
    }

    @Test
    public void calculateAndSetVatTotal_ifItemHasPromoDiscountThenTotalVatIncludesVatFromPromoDiscount() {
        BigDecimal total = valueOf(180L);
        BigDecimal priceWithoutVat = valueOf(80L);
        BigDecimal price = valueOf(100L);
        BigDecimal priceAfterPromoDiscount = valueOf(90L);
        BigDecimal promoDiscount = valueOf(10);

        OrderItem oi = getOrderItem(priceWithoutVat);
        oi.setPrice(priceAfterPromoDiscount);
        oi.getPrices().setReportPrice(price);  // цена из репорта
        oi.getPrices().setBuyerPriceBeforeDiscount(price);  // у товара нет скидки от репорта
        oi.getPrices().setBuyerDiscount(promoDiscount);
        oi.setCount(2);

        Order order = makeOrder(total, oi);

        financialService.calculateAndSetVatTotal(order);

        // 200 - исходная сумма за 2 штуки товара
        // 200 - 20 - сумма после скидки за 2 штуки товара (10 - скидка на 1 товар)
        // 80 * 2 - цена без НДС (согласно Репорту) за 2 штуки товара
        // (1 - 80 / 100 ) - НДС за товар
        // 10 * (1 - 80 / 100) - сумма НДС со скидки
        // (10 - 10 * (1 - 80 / 100)) * 2 - сумма скидки без НДС
        Assertions.assertEquals(
                valueOf((200d - 20d) - (80d * 2) + (10d - 10d * (1 - 80d / 100d)) * 2).doubleValue(),
                order.getVatTotal().doubleValue(),
                error.doubleValue());
    }

    @NotNull
    private Order makeOrder(BigDecimal total, OrderItem oi) {
        Order order = new Order();
        order.setBuyerTotal(total);
        order.setDelivery(null);
        order.setItems(List.of(oi));
        return order;
    }

    @Test
    public void calculateAndSetVatTotal_ifItemHasPromoAndReportDiscountThenTotalVatIncludesVatFromPromoDiscount() {
        BigDecimal total = valueOf(180L);
        BigDecimal priceWithoutVat = valueOf(80L);
        BigDecimal price = valueOf(100L);
        BigDecimal priceAfterPromoDiscount = valueOf(90L);
        BigDecimal promoDiscount = valueOf(10);
        BigDecimal reportDiscount = valueOf(5L);

        OrderItem oi = getOrderItem(priceWithoutVat);
        oi.setPrice(priceAfterPromoDiscount);
        oi.getPrices().setReportPrice(price);  // цена из репорта
        oi.getPrices().setBuyerPriceBeforeDiscount(price.add(reportDiscount));  // у товара есть скидка от репорта
        oi.getPrices().setBuyerDiscount(promoDiscount.add(reportDiscount));  // скидка от репорта учтена в общей скидке
        oi.setCount(2);

        Order order = makeOrder(total, oi);

        financialService.calculateAndSetVatTotal(order);

        // 200 - исходная сумма за 2 штуки товара
        // 200 - 20 - сумма после скидки за 2 штуки товара (10 - скидка на 1 товар)
        // 80 * 2 - цена без НДС (согласно Репорту) за 2 штуки товара
        // (1 - 80 / 100 ) - НДС за товар
        // 10 * (1 - 80 / 100) - сумма НДС со скидки
        // (10 + 5) - (105 - 100) - скидка от Промо;
        // (10 - 10 * (1 - 80 / 100)) * 2 - сумма скидки без НДС
        Assertions.assertEquals(
                valueOf((200d - 20d) - (80d * 2) + ((10d + 5d) - (105d - 100d) - 10d * (1 - 80d / 100d)) * 2)
                        .doubleValue(),
                order.getVatTotal().doubleValue(),
                error.doubleValue());
    }
}
