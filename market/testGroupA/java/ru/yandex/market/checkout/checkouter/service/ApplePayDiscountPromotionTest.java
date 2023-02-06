package ru.yandex.market.checkout.checkouter.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureWriter;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.promo.ItemPromo;
import ru.yandex.market.checkout.checkouter.order.promo.PromoDefinition;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.balance.PaymentFormType;
import ru.yandex.market.checkout.checkouter.service.ApplePayDiscountPromotion.ApplePayDiscountRepayException;
import ru.yandex.market.loyalty.api.model.ItemPromoResponse;
import ru.yandex.market.loyalty.api.model.bundle.BundledOrderItemResponse;
import ru.yandex.market.loyalty.api.model.bundle.OrderWithBundlesResponse;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountResponse;

import static java.math.BigDecimal.ZERO;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplePayDiscountPromotionTest extends AbstractServicesTestBase {

    @Autowired
    private ApplePayDiscountPromotion applePayDiscountPromotion;

    @Autowired
    private CheckouterFeatureWriter featureWriter;

    // region loyaltyResponseProcessor_test_filter
    @Test
    @DisplayName("Если тоггл на акцию AP выключен, то должны убрать промокод из ответа лоялти")
    public void loyaltyResponseProcessor_test_filter_feature_off() {
        featureWriter.writeValue(ApplePayDiscountPromotion.FEATURE_TOGGLE, false);

        MultiCartWithBundlesDiscountResponse response = createMultiCartWithBundlesDiscountResponse();
        applePayDiscountPromotion.loyaltyResponseProcessor(response, true);

        assertFalse(response.getOrders().stream()
                .flatMap(order -> order.getItems().stream())
                .flatMap(item -> item.getPromos().stream())
                .anyMatch(ApplePayDiscountPromotion::isApplePayPromo));
    }

    @Test
    @DisplayName("Если тоггл на акцию AP включен, то промокод должен остаться в ответе")
    public void loyaltyResponseProcessor_test_filter_feature_on() {
        featureWriter.writeValue(ApplePayDiscountPromotion.FEATURE_TOGGLE, true);

        MultiCartWithBundlesDiscountResponse response = createMultiCartWithBundlesDiscountResponse();
        applePayDiscountPromotion.loyaltyResponseProcessor(response, true);

        assertTrue(response.getOrders().stream()
                .flatMap(order -> order.getItems().stream())
                .flatMap(item -> item.getPromos().stream())
                .anyMatch(ApplePayDiscountPromotion::isApplePayPromo));
    }

    @Test
    @DisplayName("Если в чекаутере включен toggle на акцию Apple Pay, но на клиенте нет, то убираем промокод из " +
            "ответа лоялти")
    public void loyaltyResponseProcessor_test_filter_feature_on_but_client_off() {
        featureWriter.writeValue(ApplePayDiscountPromotion.FEATURE_TOGGLE, true);

        MultiCartWithBundlesDiscountResponse response = createMultiCartWithBundlesDiscountResponse();
        applePayDiscountPromotion.loyaltyResponseProcessor(response, false);

        assertFalse(response.getOrders().stream()
                .flatMap(order -> order.getItems().stream())
                .flatMap(item -> item.getPromos().stream())
                .anyMatch(ApplePayDiscountPromotion::isApplePayPromo));
    }

    private MultiCartWithBundlesDiscountResponse createMultiCartWithBundlesDiscountResponse() {
        return new MultiCartWithBundlesDiscountResponse(
                List.of(new OrderWithBundlesResponse(
                        null,
                        null,
                        List.of(new BundledOrderItemResponse(
                                null, null, null, null, false,
                                new ArrayList<>(List.of(new ItemPromoResponse(
                                        ZERO,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        ApplePayDiscountPromotion.PROMOCODE,
                                        null,
                                        null
                                ))), null, null, null
                        )), null, null, null, null, null
                )),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                null,
                Collections.emptySet(),
                Collections.emptySet(),
                Collections.emptySet(),
                ZERO,
                ZERO,
                null,
                null,
                Collections.emptyMap(),
                null,
                null
        );
    }
    // endregion

    // region validateRepay
    @Test
    @DisplayName("Заказ с акцией Apple Pay оплачивается не через Apple Pay")
    public void validateRepay_throw_exception_test() {
        featureWriter.writeValue(ApplePayDiscountPromotion.FEATURE_TOGGLE, true);

        Order order = new Order(false, false, ZERO, ZERO, ZERO, ZERO, ZERO);
        OrderItem orderItem = new OrderItem();
        orderItem.setPromos(Set.of(createApplePayItemPromo()));
        order.setItems(List.of(orderItem));
        order.setPaymentMethod(PaymentMethod.GOOGLE_PAY);
        List<Order> orders = List.of(order);
        assertThrows(ApplePayDiscountRepayException.class, () -> {
            applePayDiscountPromotion.validateRepay(orders, null, null, null);
        });
    }

    @Test
    @DisplayName("Заказ с акцией Apple Pay оплачивается через Apple Pay")
    public void validateRepay_normal_test() {
        featureWriter.writeValue(ApplePayDiscountPromotion.FEATURE_TOGGLE, true);

        Order order = new Order(false, false, ZERO, ZERO, ZERO, ZERO, ZERO);
        OrderItem orderItem = new OrderItem();
        orderItem.setPromos(Set.of(createApplePayItemPromo()));
        order.setItems(List.of(orderItem));
        order.setPaymentMethod(PaymentMethod.APPLE_PAY);
        List<Order> orders = List.of(order);
        assertDoesNotThrow(() -> {
            applePayDiscountPromotion.validateRepay(orders, null, null, null);
        });
    }

    @Test
    @DisplayName("Заказ с акцией Apple Pay оплачивается с Desktop")
    public void validateRepay_throw_exception_desktop_test() {
        featureWriter.writeValue(ApplePayDiscountPromotion.FEATURE_TOGGLE, true);

        Order order = new Order(false, false, ZERO, ZERO, ZERO, ZERO, ZERO);
        OrderItem orderItem = new OrderItem();
        orderItem.setPromos(Set.of(createApplePayItemPromo()));
        order.setItems(List.of(orderItem));
        order.setPaymentMethod(PaymentMethod.GOOGLE_PAY);
        List<Order> orders = List.of(order);
        assertThrows(ApplePayDiscountRepayException.class, () -> {
            applePayDiscountPromotion.validateRepay(orders, PaymentFormType.DESKTOP, null, null);
        });
    }

    @Test
    @DisplayName("Заказ с акцией Apple Pay оплачивается с Touch-а")
    public void validateRepay_throw_exception_touch_test() {
        featureWriter.writeValue(ApplePayDiscountPromotion.FEATURE_TOGGLE, true);

        Order order = new Order(false, false, ZERO, ZERO, ZERO, ZERO, ZERO);
        OrderItem orderItem = new OrderItem();
        orderItem.setPromos(Set.of(createApplePayItemPromo()));
        order.setItems(List.of(orderItem));
        order.setPaymentMethod(PaymentMethod.GOOGLE_PAY);
        List<Order> orders = List.of(order);
        assertThrows(ApplePayDiscountRepayException.class, () -> {
            applePayDiscountPromotion.validateRepay(orders, PaymentFormType.MOBILE, null, "return");
        });
    }

    @Test
    @DisplayName("Заказ с акцией Apple Pay оплачивается картой")
    public void validateRepay_throw_exception_card_test() {
        featureWriter.writeValue(ApplePayDiscountPromotion.FEATURE_TOGGLE, true);

        Order order = new Order(false, false, ZERO, ZERO, ZERO, ZERO, ZERO);
        OrderItem orderItem = new OrderItem();
        orderItem.setPromos(Set.of(createApplePayItemPromo()));
        order.setItems(List.of(orderItem));
        order.setPaymentMethod(PaymentMethod.GOOGLE_PAY);
        List<Order> orders = List.of(order);
        assertThrows(ApplePayDiscountRepayException.class, () -> {
            applePayDiscountPromotion.validateRepay(orders, null, "card-x8af859f142289e3279eb0e4d", null);
        });
    }

    private ItemPromo createApplePayItemPromo() {
        PromoDefinition promoDefinition = PromoDefinition.builder()
                .type(PromoType.MARKET_PROMOCODE)
                .promoCode(ApplePayDiscountPromotion.PROMOCODE)
                .build();
        return ItemPromo.builder()
                .promoDefinition(promoDefinition)
                .buyerDiscount(ZERO)
                .subsidy(ZERO)
                .buyerSubsidy(ZERO)
                .cashbackAccrualAmount(ZERO)
                .cashbackSpendLimit(ZERO)
                .giftCount(ZERO)
                .marketCashbackPercent(ZERO)
                .partnerCashbackPercent(ZERO)
                .build();
    }
    // endregion
}
