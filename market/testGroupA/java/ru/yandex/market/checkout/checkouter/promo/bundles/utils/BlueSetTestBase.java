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

import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.FIRST_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.FOURTH_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PROMO_KEY;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.SECOND_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.THIRD_OFFER;

public abstract class BlueSetTestBase extends AbstractWebTestBase {

    protected final List<FoundOffer> reportOffers = new ArrayList<>();
    protected OrderItemBuilder firstOffer;
    protected OrderItemBuilder secondOffer;
    protected OrderItemBuilder thirdOffer;
    protected OrderItemBuilder fourthOffer;

    @BeforeEach
    public void configure() {
        firstOffer = OrderItemProvider.orderItemWithSortingCenter()
                .offer(FIRST_OFFER)
                .price(1000)
                .count(2);

        secondOffer = OrderItemProvider.orderItemWithSortingCenter()
                .offer(SECOND_OFFER)
                .price(2000)
                .count(2);

        thirdOffer = OrderItemProvider.orderItemWithSortingCenter()
                .offer(THIRD_OFFER)
                .price(3000)
                .count(2);

        fourthOffer = OrderItemProvider.orderItemWithSortingCenter()
                .offer(FOURTH_OFFER)
                .price(4000)
                .count(2);

        reportOffers.clear();
        reportOffers.add(FoundOfferBuilder.createFrom(firstOffer.build())
                .promoKey(PROMO_KEY)
                .promoType(ReportPromoType.BLUE_SET.getCode())
                .build());
        reportOffers.add(FoundOfferBuilder.createFrom(secondOffer.build())
                .promoKey(PROMO_KEY)
                .promoType(ReportPromoType.BLUE_SET.getCode())
                .build());
        reportOffers.add(FoundOfferBuilder.createFrom(thirdOffer.build())
                .promoKey(PROMO_KEY)
                .promoType(ReportPromoType.BLUE_SET.getCode())
                .build());
        reportOffers.add(FoundOfferBuilder.createFrom(fourthOffer.build())
                .promoKey(PROMO_KEY)
                .promoType(ReportPromoType.BLUE_SET_SECONDARY.getCode())
                .build());
    }
}
