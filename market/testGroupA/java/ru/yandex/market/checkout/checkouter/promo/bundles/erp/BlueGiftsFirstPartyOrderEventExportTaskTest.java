package ru.yandex.market.checkout.checkouter.promo.bundles.erp;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.order.ApiSettings;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.SupplierType;
import ru.yandex.market.checkout.checkouter.order.promo.ReportPromoType;
import ru.yandex.market.checkout.common.tasks.ZooTask;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;
import ru.yandex.market.checkout.util.loyalty.LoyaltyParameters;
import ru.yandex.market.checkout.util.loyalty.response.OrderBundleBuilder;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;
import ru.yandex.market.common.report.model.FoundOffer;

import static java.util.stream.Collectors.toMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.NEW_ORDER;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERY;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;
import static ru.yandex.market.checkout.checkouter.pay.PaymentStatus.CLEARED;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.GIFT_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PRIMARY_OFFER;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PROMO_BUNDLE;
import static ru.yandex.market.checkout.checkouter.promo.PromoConfigurer.PROMO_KEY;
import static ru.yandex.market.checkout.checkouter.promo.bundles.utils.BlueGiftsOrderUtils.orderWithYandexDelivery;
import static ru.yandex.market.checkout.helpers.MultiPaymentHelper.checkBasketForClearTask;
import static ru.yandex.market.checkout.providers.MultiCartProvider.single;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.INTAKE_AVAILABLE_DATE;
import static ru.yandex.market.checkout.test.providers.OrderItemProvider.orderItemWithSortingCenter;
import static ru.yandex.market.checkout.util.items.OrderItemUtils.itemResponseFor;

public class BlueGiftsFirstPartyOrderEventExportTaskTest extends AbstractWebTestBase {

    @Autowired
    private ZooTask firstPartyOrderEventExportTask;
    @Autowired
    private JdbcTemplate erpJdbcTemplate;

    @BeforeEach
    public void setup() {
        setFixedTime(INTAKE_AVAILABLE_DATE);
    }

    @Test
    public void shouldExportShopPromoIdToErp() {
        Order order = createTypicalOrderWithBundles();

        trustMockConfigurer.mockCheckBasket(checkBasketForClearTask(List.of(order)));
        trustMockConfigurer.mockStatusBasket(checkBasketForClearTask(List.of(order)), null);

        order = orderStatusHelper.proceedOrderToStatus(order, PROCESSING);
        order = orderStatusHelper.proceedOrderToStatus(order, DELIVERY);

        assertThat(order.getStatus(), is(DELIVERY));
        assertThat(order.getPayment(), notNullValue());
        assertThat(order.getPayment().getStatus(), is(CLEARED));

        firstPartyOrderEventExportTask.runOnce();

        verifyEventExported(order, HistoryEventType.ORDER_STATUS_UPDATED, greaterThanOrEqualTo(1));
        verifyEventExported(order, NEW_ORDER, equalTo(0));
        verifyItemsExported(order, 3);

        verifyShopPromoIdExported(order, PROMO_KEY);
    }

    private void verifyItemsExported(Order order, Integer count) {
        Integer res = erpJdbcTemplate.queryForObject(
                "SELECT count(DISTINCT item_id) FROM coorderitem WHERE order_id=?",
                (rs, rowNum) -> rs.getInt(1),
                order.getId()
        );

        assertThat(res, equalTo(count));
    }

    private void verifyShopPromoIdExported(Order order, String expected) {
        final List<String> result = erpJdbcTemplate.queryForList(
                "SELECT shop_promo_id FROM COOrderItem WHERE ORDER_ID=?", String.class, order.getId());
        assertThat(result, hasSize(6));
        final List<String> nonNullResult = result.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        assertThat(nonNullResult, hasSize(2));
        assertThat(nonNullResult.get(0), equalTo(expected));
    }

    private void verifyEventExported(Order order, HistoryEventType type, Matcher<Integer> matcher) {
        Integer res = erpJdbcTemplate.queryForObject(
                "SELECT count(*) FROM coorderevent WHERE order_id=? AND event_type=?",
                (rs, rowNum) -> rs.getInt(1),
                order.getId(),
                type.name()
        );

        assertThat(res, matcher);
    }

    private Order createTypicalOrderWithBundles() {
        OrderItemProvider.OrderItemBuilder primaryOffer = orderItemWithSortingCenter()
                .label("some-id-1")
                .offer(PRIMARY_OFFER)
                .supplierType(SupplierType.FIRST_PARTY)
                .price(10000);

        OrderItemProvider.OrderItemBuilder secondaryOffer = orderItemWithSortingCenter()
                .label("some-id-2")
                .offer(GIFT_OFFER)
                .supplierType(SupplierType.FIRST_PARTY)
                .price(2000);

        return orderCreateHelper.createOrder(request(single(orderWithYandexDelivery()
                .itemBuilder(similar(primaryOffer)
                        .count(2))
                .itemBuilder(secondaryOffer)
        ), config ->
                config.expectPromoBundle(OrderBundleBuilder.create()
                        .bundleId(PROMO_BUNDLE)
                        .promo(PROMO_KEY)
                        .item(similar(primaryOffer).primaryInBundle(true), 1)
                        .item(similar(secondaryOffer).primaryInBundle(false), 1, 1999))
                        .expectResponseItems(
                                itemResponseFor(primaryOffer)
                                        .bundleId(PROMO_BUNDLE)
                                        .primaryInBundle(true),
                                itemResponseFor(secondaryOffer)
                                        .bundleId(PROMO_BUNDLE)
                                        .primaryInBundle(false),
                                itemResponseFor(primaryOffer)
                        )));
    }

    private Parameters request(
            MultiCart multiCart,
            Consumer<LoyaltyParameters> loyaltyConfigurerConsumer
    ) {
        assertThat(multiCart.getCarts(), hasSize(1));
        Parameters props = BlueParametersProvider.defaultBlueOrderParameters(multiCart.getCarts().get(0));
        props.setCheckCartErrors(false);
        props.setMockLoyalty(true);

        props.addShopMetaData(
                props.getOrder().getShopId(),
                ShopSettingsHelper.getDefaultMeta()
        );

        props.setApiSettings(ApiSettings.STUB);
        props.setColor(props.getOrder().getRgb());
        props.setDeliveryPartnerType(props.getOrder().getDelivery().getDeliveryPartnerType());
        props.setDeliveryServiceId(props.getOrder().getDelivery().getDeliveryServiceId());
        props.setPaymentMethod(props.getOrder().getPaymentMethod());

        props.getReportParameters().setOffers(new ArrayList<>(multiCart.getCarts().stream()
                .flatMap(order -> order.getItems().stream())
                .map(FoundOfferBuilder::createFrom)
                .peek(b -> b.shopId(props.getOrder().getShopId()))
                .peek(builder -> builder
                        .promoKey(PROMO_KEY)
                        .promoType(ReportPromoType.GENERIC_BUNDLE.getCode()))
                .map(FoundOfferBuilder::build)
                .distinct()
                .collect(toMap(FoundOffer::getFeedOfferId, Function.identity(), (o1, o2) -> o1)).values()));
        loyaltyConfigurerConsumer.accept(props.getLoyaltyParameters());

        return props;
    }

    private OrderItemProvider.OrderItemBuilder similar(OrderItemProvider.OrderItemBuilder item) {
        return item.clone();
    }
}
