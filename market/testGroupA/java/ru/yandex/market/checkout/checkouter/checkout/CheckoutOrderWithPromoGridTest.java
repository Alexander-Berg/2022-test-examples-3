package ru.yandex.market.checkout.checkouter.checkout;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentRecord;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.util.loyalty.LoyaltyDiscount;
import ru.yandex.market.checkout.util.loyalty.LoyaltyParameters;
import ru.yandex.market.loyalty.api.model.PaymentType;
import ru.yandex.market.loyalty.api.model.delivery.DeliveryDiscountWithPromoType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.checkout.test.providers.ActualDeliveryProvider.PICKUP_PRICE;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;

public class CheckoutOrderWithPromoGridTest extends AbstractWebTestBase {

    public static final BigDecimal DELIVERY_DISCOUNT = new BigDecimal(100);
    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    private Parameters parameters;

    @BeforeEach
    public void setUp() throws Exception {
        parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withColor(Color.BLUE)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withShipmentDay(1)
                .withActualDelivery(
                        ActualDeliveryProvider.builder()
                                .addPickup(MOCK_DELIVERY_SERVICE_ID)
                                .addDelivery(MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID, 1, DELIVERY_DISCOUNT)
                                .addCommonProblem("NO_POST_OFFICE_FOR_POST_CODE")
                                .withPaymentMethods(Set.of(
                                        PaymentMethod.YANDEX,
                                        PaymentMethod.BANK_CARD,
                                        PaymentMethod.CASH_ON_DELIVERY))
                                .build()
                )

                .buildParameters();

        enrichPromoGrid(parameters);
        parameters.setDeliveryType(DeliveryType.PICKUP);
        parameters.setMockLoyalty(true);
        parameters.getReportParameters().setShopSupportsSubsidies(false);
    }

    @Test
    public void shouldFillPaymentRecordsByPromos() throws Exception {
        orderCreateHelper.initializeMock(parameters);

        MultiCart cart = orderCreateHelper.cart(parameters);
        Order order = cart.getCarts().get(0);

        Delivery pickupDeliveryOption = order.getDeliveryOptions()
                .stream()
                .filter(s -> s.getType().equals(DeliveryType.PICKUP))
                .findFirst().orElse(null);

        Assertions.assertNotNull(pickupDeliveryOption);

        PaymentRecord paymentRecord = pickupDeliveryOption.getPaymentRecords()
                .stream()
                .filter(s -> s.getPaymentMethod().equals(PaymentMethod.APPLE_PAY))
                .findAny()
                .orElse(null);

        Assertions.assertNotNull(paymentRecord);

        Assertions.assertEquals(0, paymentRecord.getDeliveryPrice().intValue());
        Assertions.assertEquals(PromoType.FREE_PICKUP, paymentRecord.getPromoType());

        LoyaltyDiscount deliveryCoin = new LoyaltyDiscount(DELIVERY_DISCOUNT, PromoType.FREE_PICKUP);
        deliveryCoin.setPromoKey(PromoType.FREE_PICKUP.getCode());
        deliveryCoin.setCoinId(1111L);

        parameters.getLoyaltyParameters().addDeliveryDiscount(
                ru.yandex.market.loyalty.api.model.delivery.DeliveryType.PICKUP, deliveryCoin);
        cart.setPaymentMethod(PaymentMethod.APPLE_PAY);
        cart.setPaymentType(ru.yandex.market.checkout.checkouter.pay.PaymentType.PREPAID);
        cart.getCarts().forEach(s -> {
            s.setPaymentMethod(PaymentMethod.APPLE_PAY);
            s.setPaymentType(ru.yandex.market.checkout.checkouter.pay.PaymentType.PREPAID);
        });

        MultiOrder checkout = orderCreateHelper.checkout(cart, parameters);
        assertThat("Скидка на доставку не применилась. ",
                checkout.getCarts().get(0).getDelivery().getPrice(), is(BigDecimal.ZERO));
    }

    private void enrichPromoGrid(Parameters parameters) {
        DeliveryDiscountWithPromoType deliveryDiscountWithPromoType =
                new DeliveryDiscountWithPromoType(PICKUP_PRICE,
                        ru.yandex.market.loyalty.api.model.PromoType.FREE_PICKUP);

        Map<ru.yandex.market.loyalty.api.model.PaymentType,
                DeliveryDiscountWithPromoType> deliveryDiscountByPaymentType = new HashMap<>();
        deliveryDiscountByPaymentType.put(PaymentType.APPLE_PAY, deliveryDiscountWithPromoType);

        LoyaltyParameters loyaltyParameters = parameters.getLoyaltyParameters();

        LoyaltyDiscount loyaltyDiscount = new LoyaltyDiscount();
        loyaltyDiscount.setDiscount(BigDecimal.ZERO);
        loyaltyDiscount.setPromoKey("promo");
        loyaltyDiscount.setPromoType(PromoType.FREE_PICKUP);
        loyaltyDiscount.setDeliveryDiscountByPaymentType(deliveryDiscountByPaymentType);
        loyaltyParameters.addDeliveryDiscount(ru.yandex.market.loyalty.api.model.delivery.DeliveryType.PICKUP,
                loyaltyDiscount);
    }
}
