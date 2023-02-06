package ru.yandex.market.checkout.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.order.ApiSettings;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.loyalty.LoyaltyParameters;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.common.report.model.FoundOffer;
import ru.yandex.market.common.report.model.LocalDeliveryOption;

import static java.util.stream.Collectors.toMap;

public final class CheckoutRequestUtils {

    private CheckoutRequestUtils() {
    }

    @Nonnull
    public static Parameters requestFor(
            @Nonnull MultiCart multiCart,
            @Nonnull Collection<FoundOffer> offerToReport,
            @Nonnull Supplier<ShopMetaData> shopMetaDataSupplier,
            @Nullable Consumer<LocalDeliveryOptionsConfigurer> localDeliveryOptionsConfigurer,
            @Nullable Consumer<DeliveryProvider.DeliveryBuilder> shopDeliveryConfigurer,
            @Nullable Consumer<ActualDeliveryProvider.ActualDeliveryBuilder> actualDeliveryConfigurer,
            @Nullable Consumer<LoyaltyParameters> loyaltyConfigurer
    ) {
        Parameters props = new Parameters(multiCart);
        //disable ugly checking
        props.turnOffErrorChecks();
        props.setUseErrorMatcher(false);

        props.addShopMetaData(
                props.getOrder().getShopId(),
                shopMetaDataSupplier.get()
        );

        props.setApiSettings(ApiSettings.PRODUCTION);
        props.setColor(props.getOrder().getRgb());

        props.setPushApiDeliveryResponse(
                change(DeliveryProvider.createFrom(props.getOrder().getDelivery()), shopDeliveryConfigurer)
                        .buildResponse(DeliveryResponse::new));

        props.setDeliveryPartnerType(props.getOrder().getDelivery().getDeliveryPartnerType());
        props.setDeliveryServiceId(props.getOrder().getDelivery().getDeliveryServiceId());
        props.setPaymentMethod(props.getOrder().getPaymentMethod());
        props.getReportParameters()
                .setActualDelivery(
                        change(ActualDeliveryProvider.builder()
                                        .addDelivery(DeliveryProvider.createFrom(props.getOrder().getDelivery())
                                                .buildActualDeliveryOption()),
                                actualDeliveryConfigurer)
                                .build()
                );

        if (localDeliveryOptionsConfigurer != null) {
            Multimap<FeedOfferId, LocalDeliveryOption> localDeliveryOptions
                    = HashMultimap.create();
            localDeliveryOptionsConfigurer.accept(localDeliveryOptions::put);
            props.getReportParameters().setLocalDeliveryOptions(localDeliveryOptions.entries().stream()
                    .collect(Collectors.groupingBy(Map.Entry::getKey,
                            Collectors.mapping(Map.Entry::getValue, Collectors.toList()))));
        }

        props.getReportParameters().setOffers(new ArrayList<>(offerToReport.stream()
                .peek(b -> b.setShopId(props.getOrder().getShopId()))
                .collect(toMap(FoundOffer::getFeedOfferId, Function.identity(), (o1, o2) -> o1)).values()));

        if (loyaltyConfigurer != null) {
            loyaltyConfigurer.accept(props.getLoyaltyParameters());
        }
        return props;
    }

    @Nonnull
    public static Parameters shopRequestFor(
            @Nonnull MultiCart multiCart,
            @Nonnull Collection<FoundOffer> offerToReport,
            @Nonnull Supplier<ShopMetaData> shopMetaDataSupplier,
            @Nullable Consumer<LocalDeliveryOptionsConfigurer> localDeliveryOptionsConfigurer,
            @Nullable Consumer<DeliveryProvider.DeliveryBuilder> shopDeliveryConfigurer,
            @Nullable Consumer<ActualDeliveryProvider.ActualDeliveryBuilder> reportDeliveryConfigurer,
            @Nullable Consumer<LoyaltyParameters> loyaltyConfigurer
    ) {
        var parameters = requestFor(multiCart, offerToReport, shopMetaDataSupplier,
                localDeliveryOptionsConfigurer, shopDeliveryConfigurer, reportDeliveryConfigurer, loyaltyConfigurer);
        parameters.setAcceptMethod(OrderAcceptMethod.PUSH_API);
        return parameters;
    }

    @Nonnull
    public static Parameters marketRequestFor(
            @Nonnull MultiCart multiCart,
            @Nonnull Collection<FoundOffer> offerToReport,
            @Nonnull Supplier<ShopMetaData> shopMetaDataSupplier,
            @Nullable Consumer<LocalDeliveryOptionsConfigurer> localDeliveryOptionsConfigurer,
            @Nullable Consumer<ActualDeliveryProvider.ActualDeliveryBuilder> reportDeliveryConfigurer,
            @Nullable Consumer<LoyaltyParameters> loyaltyConfigurer
    ) {
        return requestFor(multiCart, offerToReport, shopMetaDataSupplier,
                localDeliveryOptionsConfigurer, null, reportDeliveryConfigurer, loyaltyConfigurer);
    }

    @Nonnull
    public static Parameters shopRequestFor(
            @Nonnull MultiCart multiCart,
            @Nonnull Collection<FoundOffer> offerToReport,
            @Nonnull Supplier<ShopMetaData> shopMetaDataSupplier,
            @Nullable Consumer<LocalDeliveryOptionsConfigurer> localDeliveryOptionsConfigurer,
            @Nullable Consumer<ActualDeliveryProvider.ActualDeliveryBuilder> reportDeliveryConfigurer,
            @Nullable Consumer<LoyaltyParameters> loyaltyConfigurer
    ) {
        return requestFor(multiCart, offerToReport, shopMetaDataSupplier,
                localDeliveryOptionsConfigurer, null, reportDeliveryConfigurer, loyaltyConfigurer);
    }

    @Nonnull
    private static <T> T change(
            @Nonnull T deliveryBuilder,
            @Nullable Consumer<T> shopDeliveryConfigurer
    ) {
        if (shopDeliveryConfigurer != null) {
            shopDeliveryConfigurer.accept(deliveryBuilder);
        }
        return deliveryBuilder;
    }

    @FunctionalInterface
    public interface LocalDeliveryOptionsConfigurer {

        void accept(@Nonnull FeedOfferId offerId, @Nonnull LocalDeliveryOption deliveryOption);

        @Nonnull
        default LocalDeliveryOptionsConfigurer add(@Nonnull FeedOfferId offerId,
                                                   @Nonnull LocalDeliveryOption deliveryOption) {
            accept(offerId, deliveryOption);
            return this;
        }

        @Nonnull
        default LocalDeliveryOptionsConfigurer add(@Nonnull OrderItemProvider.OrderItemBuilder itemBuilder,
                                                   @Nonnull LocalDeliveryOption deliveryOption) {
            return add(itemBuilder.build().getFeedOfferId(), deliveryOption);
        }

        @Nonnull
        default LocalDeliveryOptionsConfigurer add(@Nonnull OrderItem item,
                                                   @Nonnull LocalDeliveryOption deliveryOption) {
            return add(item.getFeedOfferId(), deliveryOption);
        }
    }
}
