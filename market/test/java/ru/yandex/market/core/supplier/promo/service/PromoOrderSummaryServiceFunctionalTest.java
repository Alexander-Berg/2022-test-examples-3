package ru.yandex.market.core.supplier.promo.service;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.OfferItemKey;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.promo.ItemPromo;
import ru.yandex.market.checkout.checkouter.order.promo.PromoDefinition;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;

@DbUnitDataSet(before = "promo-order-service-test.before.csv")
public class PromoOrderSummaryServiceFunctionalTest extends FunctionalTest {
    @Autowired
    PromoOrderSummaryService promoOrderSummaryService;

    @Test
    @DbUnitDataSet(after = "promo-order-service-add-test.after.csv")
    public void addPromoOrderSummaryTest() {
        // Обе позиции в заказе с акциями, обе новые
        OrderHistoryEvent event1 = new OrderHistoryEvent();
        event1.setType(HistoryEventType.ORDER_STATUS_UPDATED);
        Order orderAfter1 = new Order();
        orderAfter1.setShopId(111L);
        orderAfter1.setId(1234L);
        orderAfter1.setStatus(OrderStatus.PENDING);
        OrderItem item1 = new OrderItem();
        item1.setOfferItemKey(new OfferItemKey(new FeedOfferId("1", 1L), "1"));
        ItemPromo promo1 = new ItemPromo(
                PromoDefinition.marketPromocodePromo("Fasj124K_12d", "111_ASTRQ", 666L), null, null, null);
        item1.setPromos(Set.of(promo1));
        item1.setCount(4);
        orderAfter1.addItem(item1);
        OrderItem item3 = new OrderItem();
        item3.setOfferItemKey(new OfferItemKey(new FeedOfferId("3", 3L), "3"));
        ItemPromo promo3 = new ItemPromo(
                PromoDefinition.marketPromocodePromo("Mij1209jd", "111_RRRRR", 666L), null, null, null);
        item3.setPromos(Set.of(promo3));
        item3.setCount(40);
        orderAfter1.addItem(item3);
        event1.setOrderAfter(orderAfter1);

        promoOrderSummaryService.processPromoEvent(event1);
    }

    @Test
    @DbUnitDataSet(after = "promo-order-service-update-test.after.csv")
    public void updatePromoOrderSummaryTest() {
        // Одна позиция с акцией на обновление, другая без акции
        OrderHistoryEvent event = new OrderHistoryEvent();
        event.setType(HistoryEventType.ORDER_STATUS_UPDATED);
        Order orderAfter = new Order();
        orderAfter.setShopId(654987L);
        orderAfter.setId(124L);
        orderAfter.setStatus(OrderStatus.PENDING);
        OrderItem item1 = new OrderItem();
        item1.setOfferItemKey(new OfferItemKey(new FeedOfferId("2", 2L), "2"));
        ItemPromo promo1 = new ItemPromo(
                PromoDefinition.marketPromocodePromo("ag2112c1a", "654987_PROMO", 666L), null, null, null);
        item1.setPromos(Set.of(promo1));
        item1.setCount(45);
        orderAfter.addItem(item1);
        OrderItem item2 = new OrderItem();
        item2.setOfferItemKey(new OfferItemKey(new FeedOfferId("4", 4L), "4"));
        ItemPromo promo2 = new ItemPromo(
                PromoDefinition.marketCouponPromo("ag2112c1a", null, null, "coupon", null), null, null, null);
        item2.setPromos(Set.of(promo2));
        item2.setCount(45);
        orderAfter.addItem(item2);
        event.setOrderAfter(orderAfter);

        promoOrderSummaryService.processPromoEvent(event);
    }

    @Test
    @DbUnitDataSet(after = "promo-order-service-test.before.csv")
    public void processPromoOrderSummaryWithoutPartnerPromosTest() {
        // Заказ без акций или с непартнерскими акциями
        OrderHistoryEvent event = new OrderHistoryEvent();
        event.setType(HistoryEventType.ORDER_STATUS_UPDATED);
        Order orderAfter = new Order();
        orderAfter.setShopId(654987L);
        orderAfter.setId(124L);
        orderAfter.setStatus(OrderStatus.PENDING);
        OrderItem item1 = new OrderItem();
        item1.setOfferItemKey(new OfferItemKey(new FeedOfferId("5", 5L), "5"));
        item1.setCount(45);
        orderAfter.addItem(item1);
        OrderItem item2 = new OrderItem();
        item2.setOfferItemKey(new OfferItemKey(new FeedOfferId("6", 6L), "6"));
        ItemPromo promo = new ItemPromo(
                PromoDefinition.marketBundlePromo("ag2112c1a", "afa", null, null, "gsdf", false, null), null, null, null);
        item2.setPromos(Set.of(promo));
        item2.setCount(45);
        orderAfter.addItem(item2);
        event.setOrderAfter(orderAfter);

        promoOrderSummaryService.processPromoEvent(event);
    }

    @Test
    @DbUnitDataSet(after = "promo-order-service-test.before.csv")
    public void processPromoOrderSummaryWithPartialPartnerPromosTest() {
        OrderHistoryEvent event = new OrderHistoryEvent();
        Order orderAfter = new Order();
        orderAfter.setShopId(654987L);
        orderAfter.setId(124423L);
        orderAfter.setStatus(OrderStatus.PENDING);
        OrderItem item = new OrderItem();
        ItemPromo promo1 = new ItemPromo(
                PromoDefinition.directDiscountPromo("ag2112c1a", null, null, null), null, null, null);
        ItemPromo promo2 = new ItemPromo(
                PromoDefinition.marketPromocodePromo("ag2112c1a", "654987_PROMO", 666L), null, null, null);
        ItemPromo promo3 = new ItemPromo(
                PromoDefinition.marketCoinPromo("ag2112c1a", null, null, 123L, null, null), null, null, null);
        item.setPromos(Set.of(promo1, promo2, promo3));

        promoOrderSummaryService.processPromoEvent(event);
    }

    @Test
    @DbUnitDataSet(after = "promo-order-service-test.before.csv")
    public void processEmptyEventTest() {
        OrderHistoryEvent event = new OrderHistoryEvent();
        Order orderAfter = new Order();
        event.setOrderAfter(orderAfter);

        promoOrderSummaryService.processPromoEvent(event);
    }

    @Test
    public void getTotalItemsForPromosTest() {
        Map<String, Long> totalItemsForPromo = promoOrderSummaryService.getTotalItemsForPromos(654321L,
                Set.of("654321_PROMO", "654321_AAAAA", "654321_ZZZZZ"));
        // Суммируем записи из нескольких заказов
        Assertions.assertEquals(totalItemsForPromo.get("654321_PROMO"), 10);
        // Один заказ
        Assertions.assertEquals(totalItemsForPromo.get("654321_AAAAA"), 32);
        // Не было заказов
        Assertions.assertNull(totalItemsForPromo.get("654321_ZZZZZ"));
    }

    @Test
    @DbUnitDataSet(after = "promo-order-service-update-test.after.csv")
    public void processEmptyPromoId() {
        // Одна позиция с акцией на обновление, другая с некорректной акцией (пустой shop_promo_id)
        OrderHistoryEvent event = new OrderHistoryEvent();
        event.setType(HistoryEventType.ORDER_STATUS_UPDATED);
        Order orderAfter = new Order();
        orderAfter.setShopId(654987L);
        orderAfter.setId(124L);
        orderAfter.setStatus(OrderStatus.PENDING);
        OrderItem item1 = new OrderItem();
        item1.setOfferItemKey(new OfferItemKey(new FeedOfferId("2", 2L), "2"));
        ItemPromo promo1 = new ItemPromo(
                PromoDefinition.marketPromocodePromo("ag2112c1a", "654987_PROMO", 666L), null, null, null);
        item1.setPromos(Set.of(promo1));
        item1.setCount(45);
        orderAfter.addItem(item1);
        OrderItem item2 = new OrderItem();
        item2.setOfferItemKey(new OfferItemKey(new FeedOfferId("4", 4L), "4"));
        ItemPromo promo2 = new ItemPromo(
                PromoDefinition.marketPromocodePromo("cavad", "", 666L), null, null, null);
        item2.setPromos(Set.of(promo2));
        item2.setCount(45);
        orderAfter.addItem(item2);
        event.setOrderAfter(orderAfter);

        promoOrderSummaryService.processPromoEvent(event);
    }
}
