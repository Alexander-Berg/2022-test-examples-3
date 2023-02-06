package ru.yandex.market.checkout.checkouter.promo;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.promo.ReportPromoType;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;
import ru.yandex.market.common.report.model.FoundOffer;
import ru.yandex.market.common.report.model.OfferPromo;
import ru.yandex.market.common.report.model.PromoDetails;

public final class PriceDropPromoHelper {

    private PriceDropPromoHelper() {
    }

    @SafeVarargs
    @Nonnull
    public static FoundOffer applyPriceDrop(
            @Nonnull OrderItem item,
            @Nonnull FoundOfferBuilder offerBuilder,
            @Nonnull BigDecimal discount,
            @Nonnull String promoKey,
            @Nonnull String anaplanId,
            Consumer<FoundOfferBuilder>... customizers
    ) {
        final PromoDetails promoDetails = PromoDetails.builder()
                .promoKey(promoKey)
                .anaplanId(anaplanId)
                .promoType(ReportPromoType.PRICE_DROP_AS_YOU_SHOP.getCode())
                .discount(discount)
                .build();

        offerBuilder
                .promoKey(promoDetails.getPromoKey())
                .promoType(promoDetails.getPromoType())
                .promoDetails(promoDetails);

        OfferPromo offerPromo = new OfferPromo();
        offerPromo.setPromoMd5(promoDetails.getPromoKey());
        offerPromo.setPromoType(promoDetails.getPromoType());
        offerPromo.setPromoDetails(promoDetails);
        offerBuilder
                .promo(offerPromo);

        final BigDecimal oldPrice = offerBuilder.build().getOldMin();
        final BigDecimal discountedPrice = item.getPrice().subtract(discount);

        if (oldPrice == null) {
            offerBuilder
                    .oldMin(item.getPrice())
                    .price(discountedPrice);
        } else {
            //уже наложена скидка старой цены
            offerBuilder
                    .oldMin(oldPrice)
                    .oldDiscountOldMin(item.getPrice())
                    .price(discountedPrice);
        }

        Arrays.stream(customizers)
                .forEach(c -> c.accept(offerBuilder));

        item.setPrice(discountedPrice);
        item.setQuantPrice(discountedPrice);
        item.setBuyerPrice(discountedPrice);

        return offerBuilder.build();
    }
}
