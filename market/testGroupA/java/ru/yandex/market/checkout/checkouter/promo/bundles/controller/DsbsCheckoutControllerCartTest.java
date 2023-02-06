package ru.yandex.market.checkout.checkouter.promo.bundles.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.CartChange;
import ru.yandex.market.checkout.checkouter.cart.ItemChange;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
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
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.FIRST_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.SECOND_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.THIRD_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.bundles.utils.BlueGiftsOrderUtils.orderWithShopDelivery;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.shopSelfDelivery;
import static ru.yandex.market.checkout.util.OrderUtils.firstOrder;
import static ru.yandex.market.checkout.util.items.OrderItemUtils.similar;

public class DsbsCheckoutControllerCartTest extends AbstractWebTestBase {

    public static final long MOSCOW_ID = 213L;
    public static final long ROSTOV_ON_DON_ID = 39L;

    private static final long SHOP_ID = 774L;
    private static final DeliveryDates PUSH_API_DATES = new DeliveryDates(
            Date.from(LocalDate.now().plus(1, ChronoUnit.DAYS).atStartOfDay(ZoneId.systemDefault()).toInstant()),
            Date.from(LocalDate.now().plus(2, ChronoUnit.DAYS).atStartOfDay(ZoneId.systemDefault()).toInstant()));
    private static final BigDecimal PUSH_API_PRICE = BigDecimal.valueOf(100);
    private static final DeliveryDates ACTUAL_DELIVERY_DATES = new DeliveryDates(
            Date.from(LocalDate.now().plus(3, ChronoUnit.DAYS).atStartOfDay(ZoneId.systemDefault()).toInstant()),
            Date.from(LocalDate.now().plus(4, ChronoUnit.DAYS).atStartOfDay(ZoneId.systemDefault()).toInstant()));
    private static final BigDecimal ACTUAL_DELIVERY_PRICE = BigDecimal.valueOf(200);
    protected final List<FoundOffer> reportOffers = new ArrayList<>();
    protected OrderItemProvider.OrderItemBuilder firstOffer;
    protected OrderItemProvider.OrderItemBuilder secondOffer;
    protected OrderItemProvider.OrderItemBuilder thirdOffer;

    public static Parameters requestFor(
            MultiCart multiCart,
            Collection<FoundOffer> offerToReport
    ) {
        Parameters props = new Parameters(multiCart);
        props.turnOffErrorChecks();
        props.setUseErrorMatcher(false);

        props.addShopMetaData(
                props.getOrder().getShopId(),
                ShopSettingsHelper.getDsbsShopPrepayMeta()
        );
        props.getReportParameters().setDeliveryPartnerTypes(List.of("SHOP"));
        props.setApiSettings(ApiSettings.PRODUCTION);
        props.setColor(props.getOrder().getRgb());
        props.setPushApiDeliveryResponse(DeliveryProvider.createFrom(props.getOrder().getDelivery())
                // замена настройки
                .price(PUSH_API_PRICE)
                .dates(PUSH_API_DATES)
                .buildResponse(DeliveryResponse::new));
        props.setDeliveryPartnerType(props.getOrder().getDelivery().getDeliveryPartnerType());
        props.setDeliveryServiceId(props.getOrder().getDelivery().getDeliveryServiceId());
        props.setPaymentMethod(null);
        ActualDelivery actualDelivery = ActualDeliveryProvider.builder()
                .addDelivery(DeliveryProvider.createFrom(props.getOrder().getDelivery())
                        .price(ACTUAL_DELIVERY_PRICE)
                        .dates(ACTUAL_DELIVERY_DATES)
                        .serviceId(Delivery.SELF_DELIVERY_SERVICE_ID)
                        .paymentOptions(Collections.emptyList())
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
    public void cartDsbsFromActualDelivery() {
        MultiCart cart = MultiCartProvider.single(orderWithShopDelivery()
                .deliveryBuilder(shopSelfDelivery()
                        .price(PUSH_API_PRICE)
                        .dates(PUSH_API_DATES)
                        .regionId(MOSCOW_ID))
                .color(Color.WHITE)
                .itemBuilder(similar(firstOffer).price(900))
                .itemBuilder(similar(secondOffer).price(1800))
                .itemBuilder(similar(thirdOffer).price(2700))
        );
        Parameters props = requestFor(cart, reportOffers);


        MultiCart multiCart = orderCreateHelper.cart(props);

        assertThat(multiCart, hasProperty("valid", is(true)));
        assertThat(firstOrder(multiCart), hasProperty("changes", hasItem(CartChange.DELIVERY)));
        assertThat(firstOrder(multiCart), hasProperty("validationErrors", nullValue()));
        assertThat(firstOrder(multiCart).getItems(), everyItem(allOf(
                hasProperty("bundleId", nullValue()),
                hasProperty("changes", hasItem(ItemChange.PRICE)),
                hasProperty("promos", empty())
        )));
        assertThat(multiCart.getCostLimitInformation().getErrors(), empty());
        assertThat(firstOrder(multiCart).getDeliveryOptions(), Matchers.hasSize(1));
        Delivery delivery = firstOrder(multiCart).getDeliveryOptions().get(0);
        assertThat(delivery.getPrice(), is(ACTUAL_DELIVERY_PRICE));
        assertThat(delivery.getDeliveryDates(), is(ACTUAL_DELIVERY_DATES));
        assertThat(delivery.getPaymentOptions(), Matchers.hasSize(5));
    }

    @Test
    @Disabled("Непонятно что тестирует кейс")
    public void cartDsbsFromPushApi() {
        MultiCart cart = MultiCartProvider.single(orderWithShopDelivery()
                .deliveryBuilder(shopSelfDelivery()
                        .price(PUSH_API_PRICE)
                        .dates(PUSH_API_DATES)
                        .regionId(ROSTOV_ON_DON_ID))
                .color(Color.WHITE)
                .itemBuilder(similar(firstOffer).price(900))
                .itemBuilder(similar(secondOffer).price(1800))
                .itemBuilder(similar(thirdOffer).price(2700))
        );
        Parameters props = requestFor(cart, reportOffers);
        MultiCart multiCart = orderCreateHelper.cart(props);

        assertThat(multiCart, hasProperty("valid", is(true)));
        assertThat(firstOrder(multiCart), hasProperty("changes", hasItem(CartChange.DELIVERY)));
        assertThat(firstOrder(multiCart), hasProperty("validationErrors", nullValue()));
        assertThat(firstOrder(multiCart).getItems(), everyItem(allOf(
                hasProperty("bundleId", nullValue()),
                hasProperty("changes", hasItem(ItemChange.PRICE)),
                hasProperty("promos", empty())
        )));
        assertThat(multiCart.getCostLimitInformation().getErrors(), empty());
        assertThat(firstOrder(multiCart).getDeliveryOptions(), Matchers.hasSize(1));
        Delivery delivery = firstOrder(multiCart).getDeliveryOptions().get(0);
        assertThat(delivery.getPrice(), is(PUSH_API_PRICE));
        assertThat(delivery.getDeliveryDates(), is(PUSH_API_DATES));
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
