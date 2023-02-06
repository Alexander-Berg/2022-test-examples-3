package ru.yandex.market.checkout.backbone.config;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ru.yandex.market.checkout.checkouter.order.AdditionalCartInfo;
import ru.yandex.market.checkout.checkouter.order.AdditionalInfo;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.promo.ReportPromoDiscount;
import ru.yandex.market.checkout.checkouter.order.promo.ReportPromoType;
import ru.yandex.market.checkout.checkouter.promo.loyalty.client.DiscountRequestFactoryTest;
import ru.yandex.market.checkout.providers.MultiOrderProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;
import ru.yandex.market.common.report.model.ExtraChargeParameters;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.common.report.model.PayByYaPlus;
import ru.yandex.market.common.report.model.PromoDetails;

import static ru.yandex.market.checkout.checkouter.order.promo.ReportPromoType.GENERIC_BUNDLE;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.FIRST_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PROMO_KEY;
import static ru.yandex.market.checkout.checkouter.promo.bundles.utils.BlueGiftsOrderUtils.offerPromo;

public final class MarketOmsTestUtils {
    private MarketOmsTestUtils() {

    }

    public static MultiOrder generateMultiOrderWithAdditionalInfo() {
        var multiOrder = generateMultiOrder(1);
        var firstOffer = OrderItemProvider.orderItemBuilder()
                .configure(OrderItemProvider::applyDefaults)
                .weight(null)
                .supplierId(OrderProvider.SHOP_ID)
                .offer(FIRST_OFFER)
                .price(1000);

        Map<FeedOfferId, Set<ReportPromoDiscount>> offersDiscounts = new HashMap<>();
        offersDiscounts.put(new FeedOfferId("123", 123L), Set.of(ReportPromoDiscount.builder()
                .feedOfferId(FeedOfferId.from(DiscountRequestFactoryTest.FEED, FIRST_OFFER))
                .reportPromoType(GENERIC_BUNDLE)
                .promoDetails(PromoDetails.builder()
                        .promoKey(DiscountRequestFactoryTest.PROMO_KEY)
                        .promoType(GENERIC_BUNDLE.getCode())
                        .build())
                .build()));

        AdditionalCartInfo additionalCartInfo = new AdditionalCartInfo();
        additionalCartInfo.setWidth(1L);
        additionalCartInfo.setHeight(1L);
        additionalCartInfo.setWeight(1L);
        additionalCartInfo.setDepth(2L);

        AdditionalInfo additionalInfo = new AdditionalInfo(
                Map.of(new FeedOfferId("123", 123L), FoundOfferBuilder
                        .createFrom(firstOffer.build())
                                .payByYaPlus(PayByYaPlus.of(null))
                        .promo(offerPromo(PROMO_KEY, ReportPromoType.BLUE_SET))
                        .build()
                ),
                offersDiscounts,
                Map.of("221", List.of(additionalCartInfo)),
                Map.of("223", true),
                Map.of("321", buildExtraChargeParameters()),
                true,
                true
        );
        multiOrder.setAdditionalInfo(additionalInfo);

        return multiOrder;
    }

    private static MultiOrder generateMultiOrder(int size) {
        List<Order> orders = new ArrayList<>();
        while (orders.size() < size) {
            Order order = OrderProvider.getPostPaidOrder();
            order.setDelivery(DeliveryProvider.getYandexMarketDelivery(true));
            order.getDelivery().setHash(DeliveryProvider.DELIVERY_HASH);
            orders.add(order);
        }
        return MultiOrderProvider.buildMultiOrder(orders);
    }

    private static ExtraChargeParameters buildExtraChargeParameters() {
        ExtraChargeParameters extraChargeParameters = new ExtraChargeParameters();
        extraChargeParameters.setMinCharge(BigDecimal.valueOf(1));
        extraChargeParameters.setMaxCharge(BigDecimal.valueOf(2));
        extraChargeParameters.setChargeQuant(BigDecimal.valueOf(3));
        extraChargeParameters.setVatMultiplier(BigDecimal.valueOf(4));
        extraChargeParameters.setMinChargeOfGmv(BigDecimal.valueOf(5));
        return extraChargeParameters;
    }
}
