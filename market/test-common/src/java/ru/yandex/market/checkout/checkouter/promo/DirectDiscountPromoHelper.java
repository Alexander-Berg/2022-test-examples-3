package ru.yandex.market.checkout.checkouter.promo;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.promo.ReportPromoType;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;
import ru.yandex.market.common.report.model.FoundOffer;
import ru.yandex.market.common.report.model.OfferPromo;
import ru.yandex.market.common.report.model.PromoDetails;

public final class DirectDiscountPromoHelper {

    private DirectDiscountPromoHelper() {
    }

    /**
     * Установить 'Прямую скидку' на офер.
     *
     * @param item         Позиция заказа
     * @param offerBuilder Офер
     * @param discount     Скидка
     * @param subsidy      Субсидия
     * @param promoKey     Уникальный идентификатор промо
     * @param anaplanId    Идентификатор промо в Анаплане
     * @param shopPromoId  Идентификатор промо от магазина
     * @param customizers  декораторы
     * @return офер
     */
    @SafeVarargs
    @Nonnull
    @SuppressWarnings("checkstyle:ParameterNumber")
    public static FoundOffer applyDirectDiscount(
            @Nonnull OrderItem item,
            @Nonnull FoundOfferBuilder offerBuilder,
            @Nonnull BigDecimal discount,
            @Nullable BigDecimal subsidy,
            @Nonnull String promoKey,
            @Nonnull String anaplanId,
            @Nonnull String shopPromoId,
            boolean fillDeprecatedPromoFields,
            boolean fillMultiPromoFields,
            Consumer<FoundOfferBuilder>... customizers
    ) {
        final BigDecimal discountedPrice = item.getPrice().subtract(discount);
        final PromoDetails promoDetails = PromoDetails.builder()
                .promoKey(promoKey)
                .anaplanId(anaplanId)
                .shopPromoId(shopPromoId)
                .promoType(ReportPromoType.DIRECT_DISCOUNT.getCode())
                .promoFixedSubsidy(subsidy)
                .discount(discount)
                .hasDcoSubsidy(subsidy != null)
                .build();

        if (fillDeprecatedPromoFields) {
            offerBuilder
                    .promoKey(promoDetails.getPromoKey())
                    .promoType(promoDetails.getPromoType())
                    .promoDetails(promoDetails);
        }

        if (fillMultiPromoFields) {
            OfferPromo offerPromo = new OfferPromo();
            offerPromo.setPromoMd5(promoDetails.getPromoKey());
            offerPromo.setPromoType(promoDetails.getPromoType());
            offerPromo.setPromoDetails(promoDetails);

            offerBuilder
                    .promo(offerPromo);
        }

        offerBuilder
                .oldMin(item.getPrice())
                .price(discountedPrice);

        Arrays.stream(customizers)
                .forEach(c -> c.accept(offerBuilder));

        item.setPrice(discountedPrice);
        item.setQuantPrice(discountedPrice);
        item.setBuyerPrice(discountedPrice);

        return offerBuilder.build();
    }

    /**
     * Установить 'Старую цену' на офер.
     * Когда нибудь будет заменена на механику 'Прямая скидка'.
     *
     * @param item         Позиция заказа
     * @param offerBuilder Офер
     * @param discount     Процент Скидка
     * @param customizers  декораторы
     * @return офер
     */
    @Deprecated
    @SafeVarargs
    @Nonnull
    public static FoundOffer applyBlueDiscount(
            @Nonnull OrderItem item,
            @Nonnull FoundOfferBuilder offerBuilder,
            @Nonnull BigDecimal discount,
            Consumer<FoundOfferBuilder>... customizers
    ) {
        final BigDecimal discountedPrice = item.getPrice().subtract(discount);

        offerBuilder
                .oldMin(item.getPrice())
                .price(discountedPrice);

        Arrays.stream(customizers)
                .forEach(c -> c.accept(offerBuilder));

        item.setPrice(discountedPrice);
        item.setQuantPrice(discountedPrice);
        item.setBuyerPrice(discountedPrice);

        return offerBuilder.build();
    }
}
