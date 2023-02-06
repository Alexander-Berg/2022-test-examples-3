package ru.yandex.market.checkout.checkouter.service;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.CheckoutParameters;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureWriter;
import ru.yandex.market.checkout.checkouter.order.BasicOrder;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditOptions;
import ru.yandex.market.checkout.checkouter.order.OrderEditOptionsRequest;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestType;
import ru.yandex.market.checkout.checkouter.order.promo.ItemPromo;
import ru.yandex.market.checkout.checkouter.order.promo.PromoDefinition;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.helpers.ChangeOrderItemsHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.util.loyalty.LoyaltyDiscount;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;
import static ru.yandex.market.checkout.checkouter.pay.PaymentMethod.APPLE_PAY;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;

@Import(ApplePayDiscountPromotionIntegrationTest.Configuration.class)
class ApplePayDiscountPromotionIntegrationTest extends AbstractWebTestBase {

    @TestConfiguration
    public static class Configuration {

        @Bean
        @Primary
        public ApplePayDiscountPromotion applePayDiscountPromotionSpy(
                ApplePayDiscountPromotion applePayDiscountPromotion
        ) {
            return Mockito.spy(applePayDiscountPromotion);
        }
    }

    @Autowired
    private ChangeOrderItemsHelper changeOrderItemsHelper;

    @Autowired
    private ApplePayDiscountPromotion applePayDiscountPromotionSpy;

    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;

    @Autowired
    private CheckouterFeatureWriter featureWriter;

    @Test
    @DisplayName("Должен вернуться только Apple Pay в качестве метода оплаты для заказов с акцией Apple Pay")
    public void testPaymentOptionsFeatureOn() throws Exception {
        featureWriter.writeValue(ApplePayDiscountPromotion.FEATURE_TOGGLE, true);
        doReturn(true).when(applePayDiscountPromotionSpy)
                .isApplePayDiscountEnabledOnClient(Mockito.any());

        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(BLUE)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .buildParameters();
        parameters.setPaymentMethod(APPLE_PAY);
        parameters.setMockLoyalty(true);
        LoyaltyDiscount loyaltyDiscount = new LoyaltyDiscount(ONE, PromoType.MARKET_PROMOCODE, null);
        loyaltyDiscount.setPromocode(ApplePayDiscountPromotion.PROMOCODE);
        parameters.getLoyaltyParameters().addLoyaltyDiscount(parameters.getItems().iterator().next(), loyaltyDiscount);
        parameters.getReportParameters().getActualDelivery().getResults().get(0).getDelivery().forEach(o ->
                o.setTimeIntervals(null));
        parameters.getItems().stream()
                .map(OrderItem::getPromos)
                .forEach(promos -> promos.add(
                        new ItemPromo(
                                new PromoDefinition(
                                        PromoType.MARKET_PROMOCODE,
                                        null, ApplePayDiscountPromotion.PROMOCODE, 0L
                                ),
                                ZERO, ZERO, ZERO
                        )
                ));
        Order order = orderCreateHelper.createOrder(parameters);

        OrderEditOptionsRequest request = new OrderEditOptionsRequest();
        request.setChangeRequestTypes(Set.of(ChangeRequestType.PAYMENT_METHOD));
        OrderEditOptions orderEditOptions = client.getOrderEditOptions(order.getId(), ClientRole.USER,
                order.getBuyer().getUid(), List.of(BLUE), request);

        Set<PaymentMethod> paymentOptions = orderEditOptions.getPaymentOptions();
        Assertions.assertEquals(Set.of(APPLE_PAY), paymentOptions);
    }

    @Test
    @DisplayName("Акция Apple Pay со скидкой 0 должна возвращаться в /cart, но убираться в /checkout")
    public void test_zero_apple_pay_discount_remove() throws Exception {
        featureWriter.writeValue(ApplePayDiscountPromotion.FEATURE_TOGGLE, true);
        doReturn(true).when(applePayDiscountPromotionSpy)
                .isApplePayDiscountEnabledOnClient(Mockito.any());

        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(BLUE)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .buildParameters();
        parameters.setPaymentMethod(APPLE_PAY);
        parameters.setMockLoyalty(true);
        LoyaltyDiscount loyaltyDiscount = new LoyaltyDiscount(ZERO, PromoType.MARKET_PROMOCODE, null);
        loyaltyDiscount.setPromocode(ApplePayDiscountPromotion.PROMOCODE);
        parameters.getLoyaltyParameters().addLoyaltyDiscount(parameters.getItems().iterator().next(), loyaltyDiscount);
        parameters.getReportParameters().getActualDelivery().getResults().get(0).getDelivery().forEach(o ->
                o.setTimeIntervals(null));
        parameters.getItems().stream()
                .map(OrderItem::getPromos)
                .forEach(promos -> promos.add(
                        new ItemPromo(
                                new PromoDefinition(
                                        PromoType.MARKET_PROMOCODE,
                                        null, ApplePayDiscountPromotion.PROMOCODE, 0L
                                ),
                                ZERO, ZERO, ZERO
                        )
                ));

        MultiCart cartResponse = orderCreateHelper.cart(parameters);
        MultiOrder multiOrderMapped = orderCreateHelper.mapCartToOrder(cartResponse, parameters);
        MultiOrder checkoutResponse = client.checkout(multiOrderMapped, CheckoutParameters.builder()
                .withUid(multiOrderMapped.getBuyer().getUid())
                .build());

        assertTrue(cartResponse.getCarts().stream()
                .flatMap(o -> o.getItems().stream())
                .flatMap(i -> i.getPromos().stream())
                .anyMatch(ApplePayDiscountPromotion::isApplePayPromo));
        assertTrue(checkoutResponse.getCarts().stream()
                .flatMap(o -> o.getItems().stream())
                .flatMap(i -> i.getPromos().stream())
                .noneMatch(ApplePayDiscountPromotion::isApplePayPromo));
        assertTrue(checkoutResponse.getOrders().stream().map(BasicOrder::getId)
                .map(orderService::getOrder)
                .flatMap(o -> o.getItems().stream())
                .flatMap(i -> i.getPromos().stream())
                .noneMatch(ApplePayDiscountPromotion::isApplePayPromo));
    }
}
