package ru.yandex.market.checkout.checkouter.promo.bundles.utils;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.promo.ReportPromoType;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider.OrderItemBuilder;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;
import ru.yandex.market.common.report.model.FoundOffer;

import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.GIFT_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PRIMARY_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PROMO_KEY;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.THIRD_OFFER;

public abstract class CheapestAsGiftTestBase extends AbstractWebTestBase {

    protected final List<FoundOffer> reportOffers = new ArrayList<>();
    protected OrderItemBuilder firstOffer;
    protected OrderItemBuilder secondOffer;
    protected OrderItemBuilder offerWithoutPromo;

    @BeforeEach
    public void configure() {
        firstOffer = OrderItemProvider.orderItemWithSortingCenter()
                .offer(PRIMARY_OFFER)
                .price(1000)
                .count(2);

        secondOffer = OrderItemProvider.orderItemWithSortingCenter()
                .offer(GIFT_OFFER)
                .price(1000)
                .count(2);

        offerWithoutPromo = OrderItemProvider.orderItemWithSortingCenter()
                .offer(THIRD_OFFER)
                .price(1000)
                .count(2);

        reportOffers.clear();
        reportOffers.add(FoundOfferBuilder.createFrom(firstOffer.build())
                .promoKey(PROMO_KEY)
                .promoType(ReportPromoType.CHEAPEST_AS_GIFT.getCode())
                .build());
        reportOffers.add(FoundOfferBuilder.createFrom(secondOffer.build())
                .promoKey(PROMO_KEY)
                .promoType(ReportPromoType.CHEAPEST_AS_GIFT.getCode())
                .build());
    }
}
