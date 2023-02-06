package ru.yandex.market.checkout.checkouter.order;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartParameters;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryCipherService;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.viewmodel.DeliveryActualizationRequest;
import ru.yandex.market.checkout.checkouter.viewmodel.MultiOrderActualizationRequest;
import ru.yandex.market.checkout.checkouter.viewmodel.OrderActualizationRequest;
import ru.yandex.market.checkout.checkouter.viewmodel.OrderItemActualizationRequest;
import ru.yandex.market.checkout.checkouter.views.services.ActualizationRequestMapper;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ActualizationRequestMapperTest extends AbstractWebTestBase {

    private static final String DEFAULT_LABEL = "48367_nMnSsGCSBuhWiUOQf94SyQ_0";
    private static final String DEFAULT_OFFER_ID = "1879047";
    private static final String DEFAULT_SHOW_INFO = "OTNDnItfwROQypMce5YPJzT51uFNhB" +
            "-53Z0_8Eo35bgpsDAdBlDN2i7BUCTjIhTK1ApkJxL41TkBA35wFpg1VaYHizvn" +
            "-HKtfL8IKf1cGoEiMmrM1hOorps9JdU24zsc0pHWTWIjOvfryxIUpwvJP6ddmK6UpQzTP0pxpt5FpMGL7sGGTrHwFU5Cv8QhqInh";
    private static final String DEFAULT_CART_INFO = "OTNDnItfwROQypMce5YPJzT51uFNhB" +
            "-53Z0_8Eo35bgpsDAdBlDN2i7BUCTjIhTK1ApkJxL41TkBA35wFpg1VaYHizvn" +
            "-HKtfL8IKf1cGoEiMmrM1hOorps9JdU24zsc0pHWTWIjOvfryxIUpwvJP6ddmK6UpQzTP0pxpt5FpMGL7sGGTrHwFU5Cv8QhqInh";
    private static final long DEFAULT_SHOP = 614403;
    private static final long DEFAULT_FEED_ID = 733627;
    private static final long DEFAULT_REGION = 213;

    @Autowired
    private ActualizationRequestMapper requestMapper;
    @Autowired
    private DeliveryCipherService deliveryCipherService;

    @Test
    void shouldHaveHashInDelivery() {
        var hash = "vujQrQNMAdM1cYADVSGnQEsID4jiUVQLSE6ZbeyHL7x6XZzjGH/QGjJuQ" +
                "/CFZpgOdC8yNL8596QPG5qPAIqTmkkqwI8Zf4azYYQB0UHuyswbZ4coCya0VA==";
        var delivery = new Delivery();
        delivery.setId(hash);
        deliveryCipherService.decipherDelivery(delivery);

        assertThat(delivery.safeGetHash(), Matchers.is(hash));
    }

    @Test
    void shouldNormalizePhoneInCartRequest() {
        var request = makeMultiOrderCartRequest();
        request.getBuyer().setPhone("+7(987) 654-32-10");

        var multicart = requestMapper.mapToMultiCart(request, ImmutableMultiCartParameters.builder()
                .withUid(BuyerProvider.getBuyer().getUid())
                .withBuyerRegionId(DEFAULT_REGION)
                .build());

        assertEquals("79876543210", multicart.getBuyer().getNormalizedPhone());
    }

    @Test
    void shouldNormalizePhoneInCheckoutRequest() {
        var multiorder = makeMultiOrderCheckoutRequest();
        multiorder.getBuyer().setPhone("+7(987) 654-32-10");

        requestMapper.prepareMultiOrder(ImmutableMultiCartParameters.builder()
                .withUid(BuyerProvider.getBuyer().getUid())
                .withBuyerRegionId(DEFAULT_REGION)
                .build(), multiorder, UserGroup.UNKNOWN, null);

        assertEquals("79876543210", multiorder.getBuyer().getNormalizedPhone());
    }

    @Test
    public void shouldSetQuantityToCountValueInCheckoutRequest() {
        MultiOrder multiOrder1 = makeMultiOrderCheckoutRequest();
        OrderItem orderItem1 = multiOrder1.getCarts().get(0).getItems().iterator().next();
        orderItem1.setCount(10);
        orderItem1.setQuantity(null);
        //
        MultiOrder multiOrder2 = makeMultiOrderCheckoutRequest();
        OrderItem orderItem2 = multiOrder2.getCarts().get(0).getItems().iterator().next();
        orderItem2.setCount(1);
        orderItem2.setQuantity(BigDecimal.TEN);

        requestMapper.prepareMultiOrder(
                ImmutableMultiCartParameters.builder().build(), multiOrder1, UserGroup.UNKNOWN, null);
        requestMapper.prepareMultiOrder(
                ImmutableMultiCartParameters.builder().build(), multiOrder2, UserGroup.UNKNOWN, null);

        assertEquals(10, orderItem1.getCount());
        assertThat(orderItem1.getQuantity(), comparesEqualTo(BigDecimal.TEN));
        assertEquals(1, orderItem2.getCount());
        assertThat(orderItem2.getQuantity(), comparesEqualTo(BigDecimal.ONE));
    }

    //TODO добавить итилиты созания
    @Nonnull
    private MultiOrderActualizationRequest makeMultiOrderCartRequest() {
        var buyer = BuyerProvider.getBuyer();
        return MultiOrderActualizationRequest.builder()
                .buyer(buyer)
                .buyerRegionId(DEFAULT_REGION)
                .buyerCurrency(Currency.RUR)
                .carts(List.of(OrderActualizationRequest.builder()
                        .label(DEFAULT_LABEL)
                        .shopId(DEFAULT_SHOP)
                        .buyer(buyer)
                        .delivery(DeliveryActualizationRequest.builder()
                                .regionId(DEFAULT_REGION)
                                .leaveAtTheDoor(false)
                                .build())
                        .properties(Map.of(
                                "wasSplitByCombinator", "false",
                                "yandexPlusUser", "true",
                                "allowYandexPay", "false",
                                "allowSpasibo", "0",
                                "isYandexPay", "0",
                                "purchaseReferrer", "null",
                                "platform", "undefined",
                                "directShopInShopItems", ""
                        ))
                        .items(List.of(OrderItemActualizationRequest.builder()
                                .label(DEFAULT_LABEL)
                                .feedId(DEFAULT_FEED_ID)
                                .offerId(DEFAULT_OFFER_ID)
                                .buyerPrice(BigDecimal.valueOf(809))
                                .showInfo(DEFAULT_SHOW_INFO)
                                .cartShowInfo(DEFAULT_CART_INFO)
                                .build()))
                        .build()))
                .build();
    }

    @Nonnull
    private MultiOrder makeMultiOrderCheckoutRequest() {
        var buyer = BuyerProvider.getBuyer();
        var multiOrder = new MultiOrder();
        multiOrder.setBuyer(buyer);
        multiOrder.setBuyerCurrency(Currency.RUR);
        multiOrder.setBuyerRegionId(DEFAULT_REGION);
        multiOrder.setPaymentMethod(PaymentMethod.GOOGLE_PAY);
        multiOrder.setPaymentType(PaymentMethod.GOOGLE_PAY.getPaymentType());
        multiOrder.addOrder(OrderProvider.orderBuilder()
                .shopId(DEFAULT_SHOP)
                .buyer(buyer)
                .label(DEFAULT_LABEL)
                .delivery(DeliveryProvider.yandexDelivery().regionId(DEFAULT_REGION).build())
                .item(OrderItemProvider.orderItemBuilder()
                        .label(DEFAULT_LABEL)
                        .feedId(DEFAULT_FEED_ID)
                        .offerId(DEFAULT_OFFER_ID)
                        .price(BigDecimal.valueOf(809))
                        .build()).build());
        return multiOrder;
    }

}
