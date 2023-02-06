package ru.yandex.market.checkout.checkouter.promo.bundles.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.ItemChange;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.order.ApiSettings;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.MultiCartProvider;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;
import ru.yandex.market.common.report.model.ActualDelivery;
import ru.yandex.market.common.report.model.FoundOffer;

import static java.util.stream.Collectors.toMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.market.checkout.checkouter.feature.type.common.MapFeatureType.MULTI_CART_MIN_COSTS_BY_REGION;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.FIRST_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.SECOND_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.THIRD_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.bundles.utils.BlueGiftsOrderUtils.orderWithYandexDelivery;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.yandexDelivery;
import static ru.yandex.market.checkout.util.OrderUtils.firstOrder;
import static ru.yandex.market.checkout.util.items.OrderItemUtils.similar;

public class TurboCheckoutControllerCartTest extends AbstractWebTestBase {

    private static final long NOVOCHERKASSK_REGION = 238L;
    private static final long RUSSIA_REGION = 225L;

    private static final long SHOP_ID = 774L;

    protected final List<FoundOffer> reportOffers = new ArrayList<>();

    protected OrderItemProvider.OrderItemBuilder firstOffer;
    protected OrderItemProvider.OrderItemBuilder secondOffer;
    protected OrderItemProvider.OrderItemBuilder thirdOffer;

    public static Parameters stubRequestFor(
            MultiCart multiCart,
            Collection<FoundOffer> offerToReport
    ) {
        Parameters props = new Parameters(multiCart);
        props.turnOffErrorChecks();
        props.setUseErrorMatcher(false);

        props.addShopMetaData(
                props.getOrder().getShopId(),
                ShopSettingsHelper.getDefaultMeta()
        );
        props.setApiSettings(ApiSettings.STUB);
        props.setColor(props.getOrder().getRgb());
        props.setPushApiDeliveryResponse(DeliveryProvider.createFrom(props.getOrder().getDelivery())
                .buildResponse(DeliveryResponse::new));
        props.setDeliveryPartnerType(props.getOrder().getDelivery().getDeliveryPartnerType());
        props.setDeliveryServiceId(props.getOrder().getDelivery().getDeliveryServiceId());
        props.setPaymentMethod(null);
        ActualDelivery actualDelivery = ActualDeliveryProvider.builder()
                .addDelivery(DeliveryProvider.createFrom(props.getOrder().getDelivery())
                        .serviceId(99L)
                        .partnerType(DeliveryPartnerType.SHOP)
                        .buildActualDeliveryOption())
                .addWeight(null)
                .addDimensions(null)
                .build();
        actualDelivery.getResults().forEach(actualDeliveryResult -> {
                    actualDeliveryResult.setWeight(null);
                    actualDeliveryResult.setDimensions(null);
                }
        );

        props.getReportParameters().setActualDelivery(actualDelivery);
        props.getReportParameters().setOffers(new ArrayList<>(offerToReport.stream()
                .peek(b -> b.setShopId(props.getOrder().getShopId()))
                .collect(toMap(FoundOffer::getFeedOfferId, Function.identity(), (o1, o2) -> o1)).values()));
        // нет ВГХ
        props.hideDimensions(true);
        props.hideWeight(true);
        props.setDeliveryRegionReportParameters(null);
        return props;
    }

    @Test
    public void cart() {
        BigDecimal minCostInNovocherkassk = BigDecimal.valueOf(200000);
        checkouterFeatureWriter.writeValue(MULTI_CART_MIN_COSTS_BY_REGION, Map.of(
                RUSSIA_REGION, BigDecimal.ZERO,
                NOVOCHERKASSK_REGION, minCostInNovocherkassk
        ));
        MultiCart cart = MultiCartProvider.single(orderWithYandexDelivery()
                .deliveryBuilder(yandexDelivery().regionId(NOVOCHERKASSK_REGION))
                .color(Color.TURBO_PLUS)
                .itemBuilder(similar(firstOffer).price(900))
                .itemBuilder(similar(secondOffer).price(1800))
                .itemBuilder(similar(thirdOffer).price(2700))
        );

        MultiCart multiCart = orderCreateHelper.cart(stubRequestFor(cart, reportOffers));

        assertThat(multiCart, hasProperty("valid", is(true)));
        assertThat(firstOrder(multiCart), hasProperty("changes", nullValue()));
        assertThat(firstOrder(multiCart), hasProperty("validationErrors", nullValue()));
        assertThat(firstOrder(multiCart).getItems(), everyItem(allOf(
                hasProperty("bundleId", nullValue()),
                hasProperty("changes", hasItem(ItemChange.PRICE)),
                hasProperty("promos", empty())
        )));
        assertThat(multiCart.getCostLimitInformation().getErrors(), empty());
    }

    @BeforeEach
    public void configure() {

        firstOffer = OrderItemProvider.orderItemBuilder()
                .configure(OrderItemProvider::applyDefaults)
                .weight(null)
                .supplierId(SHOP_ID)
                .offer(FIRST_OFFER)
                .price(1000);

        secondOffer = OrderItemProvider.orderItemBuilder()
                .configure(OrderItemProvider::applyDefaults)
                .weight(null)
                .supplierId(SHOP_ID)
                .offer(SECOND_OFFER)
                .price(2000);

        thirdOffer = OrderItemProvider.orderItemBuilder()
                .configure(OrderItemProvider::applyDefaults)
                .weight(null)
                .supplierId(SHOP_ID)
                .offer(THIRD_OFFER)
                .price(3000);

        reportOffers.clear();
        reportOffers.add(FoundOfferBuilder.createFrom(firstOffer.build())
                .build());
        reportOffers.add(FoundOfferBuilder.createFrom(secondOffer.build())
                .build());
        reportOffers.add(FoundOfferBuilder.createFrom(thirdOffer.build())
                .build());

        for (FoundOffer foundOffer : reportOffers) {
            foundOffer.setSupplierId(null);
            foundOffer.setSupplierType(null);
            foundOffer.setSupplierDescription(null);
            foundOffer.setSupplierWorkSchedule(null);
            foundOffer.setCpa(null);
            foundOffer.setWeight(null);
            foundOffer.setHeight(null);
            foundOffer.setWidth(null);
            foundOffer.setDepth(null);
        }
    }
}
