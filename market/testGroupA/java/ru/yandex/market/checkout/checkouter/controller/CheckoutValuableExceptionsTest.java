package ru.yandex.market.checkout.checkouter.controller;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ru.yandex.market.antifraud.orders.entity.AntifraudAction;
import ru.yandex.market.antifraud.orders.entity.AntifraudCheckResult;
import ru.yandex.market.antifraud.orders.entity.OrderVerdict;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderItemResponseDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderResponseDto;
import ru.yandex.market.antifraud.orders.web.entity.OrderItemChange;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.ItemChange;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SSItem;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SSItemAmount;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.market.checkout.checkouter.order.OrderFailure.Code.OUT_OF_DATE;
import static ru.yandex.market.checkout.checkouter.order.OrderFailure.Code.SHOP_ERROR;
import static ru.yandex.market.checkout.checkouter.order.OrderFailure.SubCode.COUNT;
import static ru.yandex.market.checkout.checkouter.order.OrderFailure.SubCode.FRAUD_COUNT_FIXED;
import static ru.yandex.market.checkout.checkouter.order.OrderFailure.SubCode.MISSING;
import static ru.yandex.market.checkout.test.providers.OrderItemProvider.similar;

public class CheckoutValuableExceptionsTest extends AbstractWebTestBase {

    @Test
    void shouldFailCheckoutOnItemCountFraud() throws Exception {
        var item1 = OrderItemProvider.orderItemBuilder()
                .configure(OrderItemProvider::applyDefaults)
                .offer("some offer")
                .price(200);
        var item2 = OrderItemProvider.orderItemBuilder()
                .configure(OrderItemProvider::applyDefaults)
                .offer("another offer")
                .price(200);
        var params = BlueParametersProvider.defaultBlueOrderParameters(OrderProvider.orderBuilder()
                .configure(OrderProvider::applyDefaults)
                .itemBuilder(item1)
                .itemBuilder(item2)
                .build());

        params.turnOffErrorChecks();
        params.setUseErrorMatcher(false);

        var mulltiCart = orderCreateHelper.cart(params);
        var item = item1.build();

        mstatAntifraudConfigurer.mockVerdict(OrderVerdict.builder()
                .checkResults(Collections.singleton(
                        new AntifraudCheckResult(AntifraudAction.ORDER_ITEM_CHANGE, "", "")
                ))
                .fixedOrder(new OrderResponseDto(List.of(new OrderItemResponseDto(
                        null,
                        item.getFeedId(),
                        item.getOfferId(),
                        null,
                        1,
                        EnumSet.of(OrderItemChange.COUNT)
                ))))
                .build());

        var multiOrder = orderCreateHelper.checkout(mulltiCart, params);

        assertThat(multiOrder, notNullValue());
        assertThat(multiOrder.isValid(), is(false));
        assertThat(multiOrder.getCartFailures(), notNullValue());
        assertThat(multiOrder.getCartFailures(), hasItem(hasProperty("errorCode",
                is(OUT_OF_DATE))));
        assertThat(multiOrder.getCartFailures(), hasItem(hasProperty("errorSubCodes", is(Set.of(FRAUD_COUNT_FIXED)))));
        assertThat(multiOrder.getCartFailures(), hasItem(hasProperty("errorDetails",
                is("Actualization error: count was decreased by antifraud."))));

        assertThat(multiOrder.getCartFailures().get(0).getOrder().getItems().iterator().next().getChanges(),
                hasItem(ItemChange.COUNT));
    }

    @Test
    void shouldFailCheckoutOnItemCountChangeBecauseOfStockStorage() throws Exception {
        var item1 = OrderItemProvider.orderItemBuilder()
                .configure(OrderItemProvider::applyDefaults)
                .offer("some offer")
                .price(200)
                .build();
        var item2 = OrderItemProvider.orderItemBuilder()
                .configure(OrderItemProvider::applyDefaults)
                .offer("another offer")
                .price(200)
                .build();
        var params = BlueParametersProvider.defaultBlueOrderParameters(OrderProvider.orderBuilder()
                .configure(OrderProvider::applyDefaults)
                .item(item1)
                .item(item2)
                .build());

        item1.setShopSku("some offer");
        item2.setShopSku("another offer");
        params.getReportParameters().overrideItemInfo(item1.getFeedOfferId()).setFulfilment(null);
        params.getReportParameters().overrideItemInfo(item2.getFeedOfferId()).setFulfilment(null);

        params.turnOffErrorChecks();
        params.setUseErrorMatcher(false);

        var mulltiCart = orderCreateHelper.cart(params);

        var actualized = mulltiCart.getCarts().get(0).getItems().iterator().next();

        stockStorageConfigurer.mockGetAvailableCount(SSItemAmount.of(
                SSItem.of(item1.getShopSku(), item1.getSupplierId(), item1.getWarehouseId()),
                1), SSItemAmount.of(
                SSItem.of(item2.getShopSku(), item2.getSupplierId(), item2.getWarehouseId()),
                2));
        actualized.setCount(2);

        var multiOrder = orderCreateHelper.checkout(mulltiCart, params);

        assertThat(multiOrder, notNullValue());
        assertThat(multiOrder.isValid(), is(false));
        assertThat(multiOrder.getCartFailures(), notNullValue());
        assertThat(multiOrder.getCartFailures(), hasItem(hasProperty("errorCode",
                is(OUT_OF_DATE))));
        assertThat(multiOrder.getCartFailures(), hasItem(hasProperty("errorSubCodes", is(Set.of(COUNT)))));
        assertThat(multiOrder.getCartFailures(), hasItem(hasProperty("errorDetails",
                is("Actualization error: item count mismatch."))));

        assertThat(multiOrder.getCartFailures().get(0).getOrder().getItems().iterator().next().getChanges(),
                hasItem(ItemChange.COUNT));
    }

    @Test
    void shouldFailCheckoutOnItemMissingChangeBecauseOfStockStorage() throws Exception {
        var item1 = OrderItemProvider.orderItemBuilder()
                .configure(OrderItemProvider::applyDefaults)
                .count(2)
                .offer("some offer")
                .price(200)
                .build();
        var item2 = OrderItemProvider.orderItemBuilder()
                .configure(OrderItemProvider::applyDefaults)
                .offer("another offer")
                .count(2)
                .price(200)
                .build();
        var params = BlueParametersProvider.defaultBlueOrderParameters(OrderProvider.orderBuilder()
                .configure(OrderProvider::applyDefaults)
                .item(item1)
                .item(item2)
                .build());

        item1.setShopSku("some offer");
        item2.setShopSku("another offer");
        params.getReportParameters().overrideItemInfo(item1.getFeedOfferId()).setFulfilment(null);
        params.getReportParameters().overrideItemInfo(item2.getFeedOfferId()).setFulfilment(null);

        //ugly design
        params.configuration().cart(params.getOrder())
                .getReportParameters().overrideItemInfo(item1.getFeedOfferId()).setFulfilment(null);

        params.configuration().cart(params.getOrder())
                .getReportParameters().overrideItemInfo(item2.getFeedOfferId()).setFulfilment(null);

        params.turnOffErrorChecks();
        params.setUseErrorMatcher(false);

        var mulltiCart = orderCreateHelper.cart(params);

        stockStorageConfigurer.mockGetAvailableCount(SSItemAmount.of(
                SSItem.of(item1.getShopSku(), item1.getSupplierId(), item1.getWarehouseId()),
                0), SSItemAmount.of(
                SSItem.of(item2.getShopSku(), item2.getSupplierId(), item2.getWarehouseId()),
                2));

        var multiOrder = orderCreateHelper.checkout(mulltiCart, params);

        assertThat(multiOrder, notNullValue());
        assertThat(multiOrder.isValid(), is(false));
        assertThat(multiOrder.getCartFailures(), notNullValue());
        assertThat(multiOrder.getCartFailures(), hasItem(hasProperty("errorCode",
                is(OUT_OF_DATE))));
        assertThat(multiOrder.getCartFailures(), hasItem(hasProperty("errorSubCodes", is(Set.of(MISSING)))));
        assertThat(multiOrder.getCartFailures(), hasItem(hasProperty("errorDetails",
                is("Actualization error: item is missing."))));

        assertThat(multiOrder.getCartFailures().get(0).getOrder().getItems().iterator().next().getChanges(),
                hasItem(ItemChange.MISSING));
    }

    @Test
    void shouldFailCheckoutOnItemCountChangeBecauseOfPushApi() throws Exception {
        var item1 = OrderItemProvider.orderItemBuilder()
                .configure(OrderItemProvider::applyDefaults)
                .offer("some offer")
                .price(200);
        var item2 = OrderItemProvider.orderItemBuilder()
                .configure(OrderItemProvider::applyDefaults)
                .offer("another offer")
                .price(200);
        var params = BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters(
                OrderProvider.orderBuilder()
                        .configure(OrderProvider::applyDefaults)
                        .pushApi()
                        .itemBuilder(item1)
                        .itemBuilder(item2)
                        .build());

        params.turnOffErrorChecks();
        params.setUseErrorMatcher(false);

        var mulltiCart = orderCreateHelper.cart(params);

        params.setMockPushApi(false);
        pushApiConfigurer.mockCart(
                mulltiCart.getCarts().get(0).getItems(),
                params.getOrder().getShopId(),
                null,
                params.getOrder().getAcceptMethod(),
                false
        );

        mulltiCart.getCarts().get(0).getItems().iterator().next().setCount(2);

        var multiOrder = orderCreateHelper.checkout(mulltiCart, params);

        assertThat(multiOrder, notNullValue());
        assertThat(multiOrder.isValid(), is(false));
        assertThat(multiOrder.getCartFailures(), notNullValue());
        assertThat(multiOrder.getCartFailures(), hasItem(hasProperty("errorCode",
                is(OUT_OF_DATE))));
        assertThat(multiOrder.getCartFailures(), hasItem(hasProperty("errorSubCodes", is(Set.of(COUNT)))));
        assertThat(multiOrder.getCartFailures(), hasItem(hasProperty("errorDetails",
                is("Actualization error: item count mismatch."))));

        assertThat(multiOrder.getCartFailures().get(0).getOrder().getItems().iterator().next().getChanges(),
                hasItem(ItemChange.COUNT));
    }

    @Test
    void shouldFailCheckoutOnPushApiError() throws Exception {
        var item1 = OrderItemProvider.orderItemBuilder()
                .configure(OrderItemProvider::applyDefaults)
                .offer("some offer")
                .price(200);
        var item2 = OrderItemProvider.orderItemBuilder()
                .configure(OrderItemProvider::applyDefaults)
                .offer("another offer")
                .price(200);
        var params = BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters(
                OrderProvider.orderBuilder()
                        .configure(OrderProvider::applyDefaults)
                        .pushApi()
                        .itemBuilder(item1)
                        .itemBuilder(item2)
                        .build());

        params.turnOffErrorChecks();
        params.setUseErrorMatcher(false);

        var mulltiCart = orderCreateHelper.cart(params);

        params.setMockPushApi(false);
        pushApiConfigurer.mockCartShopFailure(params.getOrder().getShopId(), true);

        var multiOrder = orderCreateHelper.checkout(mulltiCart, params);

        assertThat(multiOrder, notNullValue());
        assertThat(multiOrder.isValid(), is(false));
        assertThat(multiOrder.getCartFailures(), notNullValue());
        assertThat(multiOrder.getCartFailures(), hasItem(hasProperty("errorCode",
                is(SHOP_ERROR))));
        assertThat(multiOrder.getCartFailures(), hasItem(hasProperty("errorDetails",
                is("HTTP"))));
    }

    @Test
    void shouldFailCheckoutOnItemMissingChangeBecauseOfPushApi() throws Exception {
        var item1 = OrderItemProvider.orderItemBuilder()
                .configure(OrderItemProvider::applyDefaults)
                .offer("some offer")
                .price(200);
        var item2 = OrderItemProvider.orderItemBuilder()
                .configure(OrderItemProvider::applyDefaults)
                .offer("another offer")
                .price(200);
        var params = BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters(
                OrderProvider.orderBuilder()
                        .configure(OrderProvider::applyDefaults)
                        .pushApi()
                        .itemBuilder(item1)
                        .itemBuilder(item2)
                        .build());

        params.turnOffErrorChecks();
        params.setUseErrorMatcher(false);

        var mulltiCart = orderCreateHelper.cart(params);

        params.setMockPushApi(false);
        pushApiConfigurer.mockCart(
                List.of(similar(item1).count(0).build(), item2.build()),
                params.getOrder().getShopId(),
                null,
                params.getOrder().getAcceptMethod(),
                false
        );

        var multiOrder = orderCreateHelper.checkout(mulltiCart, params);

        assertThat(multiOrder, notNullValue());
        assertThat(multiOrder.isValid(), is(false));
        assertThat(multiOrder.getCartFailures(), notNullValue());
        assertThat(multiOrder.getCartFailures(), hasItem(hasProperty("errorCode",
                is(OUT_OF_DATE))));
        assertThat(multiOrder.getCartFailures(), hasItem(hasProperty("errorSubCodes", is(Set.of(MISSING)))));
        assertThat(multiOrder.getCartFailures(), hasItem(hasProperty("errorDetails",
                containsString("Actualization error: item is missing."))));

        assertThat(multiOrder.getCartFailures().get(0).getOrder().getItems().iterator().next().getChanges(),
                hasItem(ItemChange.MISSING));
    }

}
