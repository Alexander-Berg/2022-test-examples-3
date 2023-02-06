package ru.yandex.market.crm.operatorwindow.domain;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.checkout.checkouter.order.OfferItemKey;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.promo.OrderPromo;
import ru.yandex.market.checkout.checkouter.order.promo.PromoDefinition;
import ru.yandex.market.ocrm.module.checkouter.old.OrderBundleInformation;

public class OrderBundleInformationTest {

    private static final Long initialItemId = 0L;
    private static final String BUNDLE_ID = "bundleId";
    private static final String BUNDLE_ID_TWO = "bundleId_TWO";

    @Test
    public void getItemTitle() {
        Order order = prepareOrder();
        var itemOne = order.getItem(initialItemId);
        var itemTwo = order.getItem(initialItemId + 1);
        var itemThree = order.getItem(initialItemId + 2);

        OrderBundleInformation bundleInformation = new OrderBundleInformation(order);

        var bundlePrefix = "комплект ";
        Assertions.assertEquals(bundlePrefix + 1, bundleInformation.getItemTitle(itemOne));
        Assertions.assertEquals(bundlePrefix + 1, bundleInformation.getItemTitle(itemTwo));
        Assertions.assertEquals(bundlePrefix + 2, bundleInformation.getItemTitle(itemThree));
    }

    @Test
    public void hasBundleReturnRestrict() {
        Order order = prepareOrder();
        var itemOne = order.getItem(initialItemId);
        var itemTwo = order.getItem(initialItemId + 1);
        var itemThree = order.getItem(initialItemId + 2);

        OrderBundleInformation bundleInformation = new OrderBundleInformation(order);

        Assertions.assertTrue(bundleInformation.hasBundleReturnRestrict(itemOne));
        Assertions.assertTrue(bundleInformation.hasBundleReturnRestrict(itemTwo));
        Assertions.assertFalse(bundleInformation.hasBundleReturnRestrict(itemThree));
    }


    private Order prepareOrder() {
        Order order = new Order();

        var orderItemOne = Mockito.mock(OrderItem.class);
        var orderItemTwo = Mockito.mock(OrderItem.class);
        var orderItemThree = Mockito.mock(OrderItem.class);
        Mockito.when(orderItemOne.getId()).thenReturn(initialItemId);
        Mockito.when(orderItemTwo.getId()).thenReturn(initialItemId + 1);
        Mockito.when(orderItemThree.getId()).thenReturn(initialItemId + 2);

        Mockito.when(orderItemOne.getBundleId()).thenReturn(BUNDLE_ID);
        Mockito.when(orderItemTwo.getBundleId()).thenReturn(BUNDLE_ID);
        Mockito.when(orderItemThree.getBundleId()).thenReturn(BUNDLE_ID_TWO);

        // Первые 2 итема относятся к 1 бандлу, третий - ко второму
        Mockito.when(orderItemOne.getOfferItemKey()).thenReturn(new OfferItemKey("offerId1", 0L, BUNDLE_ID));
        Mockito.when(orderItemTwo.getOfferItemKey()).thenReturn(new OfferItemKey("offerId2", 1L, BUNDLE_ID));
        Mockito.when(orderItemThree.getOfferItemKey()).thenReturn(new OfferItemKey("offerId3", 2L, BUNDLE_ID_TWO));

        var promoDefinition = Mockito.mock(PromoDefinition.class);
        // Первый бандл можно возвратить только вместе
        Mockito.when(promoDefinition.getBundleId()).thenReturn(BUNDLE_ID);
        Mockito.when(promoDefinition.isBundleReturnRestrict()).thenReturn(true);

        var orderPromo = new OrderPromo(promoDefinition);

        order.addItem(orderItemOne);
        order.addItem(orderItemTwo);
        order.addItem(orderItemThree);

        order.setPromos(List.of(orderPromo));

        return order;
    }
}
