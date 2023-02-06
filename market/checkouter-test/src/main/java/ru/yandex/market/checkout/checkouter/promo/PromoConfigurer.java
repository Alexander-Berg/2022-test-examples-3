package ru.yandex.market.checkout.checkouter.promo;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Scope;

import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.common.report.model.FoundOffer;

@TestComponent
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public final class PromoConfigurer {

    public static final String PROMO_KEY = "some promo";
    public static final String ANAPLAN_ID = "some anaplan id";
    public static final String SHOP_PROMO_KEY = "some promo";
    public static final Long CLIENT_ID = 19980119L;
    public static final String PROMO_BUNDLE = "some generic bundle id";
    public static final String PRIMARY_OFFER = "some primary offer";
    public static final String GIFT_OFFER = "some gift offer";
    public static final String FIRST_OFFER = "first offer";
    public static final String SECOND_OFFER = "second offer";
    public static final String THIRD_OFFER = "third offer";
    public static final String FOURTH_OFFER = "fourth offer";

    private Map<OrderItem, FoundOfferBuilder> foundOfferByItem = new HashMap<>();

    public PromoConfigurer() {
    }

    public void importFrom(@Nonnull Parameters parameters) {
        Order order = parameters.getOrder();

        Map<FeedOfferId, FoundOffer> foundOfferMap = parameters.getReportParameters().getOffers().stream()
                .collect(Collectors.toUnmodifiableMap(FoundOffer::getFeedOfferId, Function.identity()));

        for (OrderItem item : order.getItems()) {
            FoundOffer offer = foundOfferMap.get(item.getFeedOfferId());
            foundOfferByItem.put(item, offer == null ? FoundOfferBuilder.createFrom(item) :
                    FoundOfferBuilder.createFrom(offer));
        }
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    @SafeVarargs
    @Nonnull
    public final FoundOffer applyDirectDiscount(
            @Nonnull OrderItem item,
            @Nonnull String promoKey,
            @Nonnull String anaplanId,
            @Nonnull String shopPromoId,
            @Nonnull BigDecimal discount,
            @Nullable BigDecimal subsidy,
            boolean fillDeprecatedPromoFields,
            boolean fillMultiPromoFields,
            Consumer<FoundOfferBuilder>... customizers
    ) {

        final FoundOfferBuilder offerBuilder = foundOfferByItem.computeIfAbsent(item, i ->
                FoundOfferBuilder.createFrom(item));

        DirectDiscountPromoHelper.applyDirectDiscount(
                item,
                offerBuilder,
                discount,
                subsidy,
                promoKey,
                anaplanId,
                shopPromoId,
                fillDeprecatedPromoFields,
                fillMultiPromoFields,
                customizers
        );

        return offerBuilder.build();
    }

    @SafeVarargs
    @Nonnull
    public final FoundOffer applyBlueDiscount(
            @Nonnull OrderItem item,
            @Nonnull BigDecimal discountPercent,
            Consumer<FoundOfferBuilder>... customizers
    ) {

        final FoundOfferBuilder offerBuilder = foundOfferByItem.computeIfAbsent(item, i ->
                FoundOfferBuilder.createFrom(item));

        DirectDiscountPromoHelper.applyBlueDiscount(
                item,
                offerBuilder,
                discountPercent,
                customizers
        );

        return offerBuilder.build();
    }


    @SafeVarargs
    @Nonnull
    public final FoundOffer applyPriceDropDiscount(
            @Nonnull OrderItem item,
            @Nonnull String promoKey,
            @Nonnull String anaplanId,
            @Nonnull BigDecimal discount,
            Consumer<FoundOfferBuilder>... customizers
    ) {

        final FoundOfferBuilder offerBuilder = foundOfferByItem.computeIfAbsent(item, i ->
                FoundOfferBuilder.createFrom(item));

        PriceDropPromoHelper.applyPriceDrop(
                item,
                offerBuilder,
                discount,
                promoKey,
                anaplanId,
                customizers
        );

        return offerBuilder.build();
    }

    @Nonnull
    public Parameters applyTo(@Nonnull Parameters parameters) {
        parameters.getReportParameters().setOffers(foundOfferByItem.values().stream()
                .map(FoundOfferBuilder::build)
                .collect(Collectors.toUnmodifiableList()));
        return parameters;
    }
}
