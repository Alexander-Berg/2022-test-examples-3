package ru.yandex.market.checkout.checkouter.actualization.fetchers.mutations;

import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.checkout.checkouter.actualization.actualizers.ItemsCountActualizer;
import ru.yandex.market.checkout.checkouter.actualization.actualizers.ItemsPriceActualizer;
import ru.yandex.market.checkout.checkouter.actualization.actualizers.MissingItemsActualizer;
import ru.yandex.market.checkout.checkouter.actualization.flow.context.CartFetchingContext;
import ru.yandex.market.checkout.checkouter.actualization.flow.context.MultiCartFetchingContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ActualizationContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartParameters;
import ru.yandex.market.checkout.checkouter.actualization.model.MultiCartContext;
import ru.yandex.market.checkout.checkouter.actualization.model.PushApiCartItemResponse;
import ru.yandex.market.checkout.checkouter.actualization.model.PushApiCartResponse;
import ru.yandex.market.checkout.checkouter.actualization.model.StocksItemKey;
import ru.yandex.market.checkout.checkouter.cart.ItemChange;
import ru.yandex.market.checkout.checkouter.color.ColorConfig;
import ru.yandex.market.checkout.checkouter.color.SingleColorConfig;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureReader;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.immutable.ImmutableOrder;
import ru.yandex.market.checkout.checkouter.pay.CurrencyRates;
import ru.yandex.market.checkout.checkouter.promo.blueset.BlueSetPromoFeatureSupportHelper;
import ru.yandex.market.checkout.checkouter.promo.flash.FlashPromoFeatureSupportHelper;
import ru.yandex.market.checkout.helpers.FlowSessionHelper;
import ru.yandex.market.checkout.providers.MultiCartProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.ENABLE_QUANTITY_LIMITS_ACTUALIZATION;
import static ru.yandex.market.checkout.checkouter.mocks.Mocks.createMock;

public class ItemsPriceAndCountMutationTest {

    private ItemsPriceAndCountMutation priceAndCountMutation;

    @BeforeEach
    void configure() {
        var checkouterFeatureReader = Mockito.mock(CheckouterFeatureReader.class);
        Mockito.doReturn(true).when(checkouterFeatureReader)
                .getBoolean(Mockito.eq(ENABLE_QUANTITY_LIMITS_ACTUALIZATION));
        var colorConfig = Mockito.mock(ColorConfig.class);
        var config = Mockito.mock(SingleColorConfig.class);
        Mockito.doReturn(config).when(colorConfig).getFor(Mockito.any(Order.class));
        Mockito.doAnswer(args -> {
            OrderItem i = args.getArgument(0);
            return i.getShopSku();
        }).when(config).getShopSku(Mockito.any(OrderItem.class));
        priceAndCountMutation = new ItemsPriceAndCountMutation(
                new ItemsCountActualizer(checkouterFeatureReader, colorConfig),
                new MissingItemsActualizer(),
                new ItemsPriceActualizer(
                        createMock(CurrencyRates.class),
                        new FlashPromoFeatureSupportHelper(),
                        new BlueSetPromoFeatureSupportHelper(),
                        checkouterFeatureReader)
        );
    }

    @Test
    void shouldSetCountToZeroIfEmptyStock() {
        var item = OrderItemProvider.orderItemBuilder()
                .offer("some offer")
                .count(3)
                .supplierId(1)
                .warehouseId(1)
                .build();
        var order = OrderProvider.orderBuilder()
                .item(item)
                .build();
        var multiCart = MultiCartProvider.single(order);
        var multiCartContext = MultiCartContext.createBy(ImmutableMultiCartParameters.builder().build(), multiCart);
        var fetchingContext = MultiCartFetchingContext.of(multiCartContext, multiCart);

        FlowSessionHelper.patchSession(fetchingContext,
                MultiCartFetchingContext::makeImmutableContext,
                (c, v) -> c.getMultiCartContext().setStocksStage(v), Map.of(
                        new StocksItemKey(item.getShopSku(), item.getSupplierId(), item.getWarehouseId(), false),
                        0
                ));

        var cartFetchingContext = CartFetchingContext.of(fetchingContext,
                ActualizationContext.builder()
                        .withInitialCart(ImmutableOrder.from(order))
                        .withCart(order)
                        .withImmutableMulticartContext(ImmutableMultiCartContext.from(multiCartContext, multiCart)),
                order);

        var pushApiResponse = new PushApiCartResponse();
        pushApiResponse.setItems(List.of(new PushApiCartItemResponse(
                item.getFeedId(),
                item.getOfferId(),
                item.getBundleId(),
                item.getCount(),
                item.getPrice(),
                item.getVat(),
                true,
                item.getSellerInn()
        )));

        FlowSessionHelper.patchSession(cartFetchingContext,
                CartFetchingContext::makeImmutableContext,
                (c, v) -> c.getActualizationContextBuilder().withPushApiCartStage(v), pushApiResponse);

        priceAndCountMutation.onSuccess(cartFetchingContext);

        assertThat(item.getChanges(), Matchers.hasItem(ItemChange.MISSING));
        assertThat(item.getCount(), Matchers.comparesEqualTo(0));
    }

    @Test
    void shouldDecreaseCountToIfNotEnoughStock() {
        var item = OrderItemProvider.orderItemBuilder()
                .offer("some offer")
                .count(3)
                .supplierId(1)
                .warehouseId(1)
                .build();
        var order = OrderProvider.orderBuilder()
                .item(item)
                .build();
        var multiCart = MultiCartProvider.single(order);
        var multiCartContext = MultiCartContext.createBy(ImmutableMultiCartParameters.builder().build(), multiCart);
        var fetchingContext = MultiCartFetchingContext.of(multiCartContext, multiCart);
        FlowSessionHelper.patchSession(fetchingContext,
                MultiCartFetchingContext::makeImmutableContext,
                (c, v) -> c.getMultiCartContext().setStocksStage(v), Map.of(
                        new StocksItemKey(item.getShopSku(), item.getSupplierId(), item.getWarehouseId(), false),
                        1
                ));

        var cartFetchingContext = CartFetchingContext.of(fetchingContext,
                ActualizationContext.builder()
                        .withInitialCart(ImmutableOrder.from(order))
                        .withCart(order)
                        .withImmutableMulticartContext(ImmutableMultiCartContext.from(multiCartContext, multiCart)),
                order);

        var pushApiResponse = new PushApiCartResponse();
        pushApiResponse.setItems(List.of(new PushApiCartItemResponse(
                item.getFeedId(),
                item.getOfferId(),
                item.getBundleId(),
                item.getCount(),
                item.getPrice(),
                item.getVat(),
                true,
                item.getSellerInn()
        )));

        FlowSessionHelper.patchSession(cartFetchingContext,
                CartFetchingContext::makeImmutableContext,
                (c, v) -> c.getActualizationContextBuilder().withPushApiCartStage(v), pushApiResponse);

        priceAndCountMutation.onSuccess(cartFetchingContext);

        assertThat(item.getChanges(), Matchers.hasItem(ItemChange.COUNT));
        assertThat(item.getCount(), Matchers.comparesEqualTo(1));
    }

    @Test
    void shouldSetCountToZeroIfNoItemInPushApi() {
        var item = OrderItemProvider.orderItemBuilder()
                .offer("some offer")
                .count(3)
                .supplierId(1)
                .warehouseId(1)
                .build();
        var order = OrderProvider.orderBuilder()
                .item(item)
                .build();
        var multiCart = MultiCartProvider.single(order);
        var multiCartContext = MultiCartContext.createBy(ImmutableMultiCartParameters.builder().build(), multiCart);
        var fetchingContext = MultiCartFetchingContext.of(multiCartContext, multiCart);
        FlowSessionHelper.patchSession(fetchingContext,
                MultiCartFetchingContext::makeImmutableContext,
                (c, v) -> c.getMultiCartContext().setStocksStage(v), Map.of(
                        new StocksItemKey(item.getShopSku(), item.getSupplierId(), item.getWarehouseId(), false),
                        1
                ));

        var cartFetchingContext = CartFetchingContext.of(fetchingContext,
                ActualizationContext.builder()
                        .withInitialCart(ImmutableOrder.from(order))
                        .withCart(order)
                        .withImmutableMulticartContext(ImmutableMultiCartContext.from(multiCartContext, multiCart)),
                order);

        var pushApiResponse = new PushApiCartResponse();
        pushApiResponse.setItems(List.of());

        FlowSessionHelper.patchSession(cartFetchingContext,
                CartFetchingContext::makeImmutableContext,
                (c, v) -> c.getActualizationContextBuilder().withPushApiCartStage(v), pushApiResponse);

        priceAndCountMutation.onSuccess(cartFetchingContext);

        assertThat(item.getChanges(), Matchers.hasItem(ItemChange.MISSING));
        assertThat(item.getCount(), Matchers.comparesEqualTo(0));
    }

    @Test
    void shouldDecreaseCountIfNotEnoughItemInPushApi() {
        var item = OrderItemProvider.orderItemBuilder()
                .offer("some offer")
                .count(3)
                .supplierId(1)
                .warehouseId(1)
                .build();
        var order = OrderProvider.orderBuilder()
                .item(item)
                .build();
        var multiCart = MultiCartProvider.single(order);
        var multiCartContext = MultiCartContext.createBy(ImmutableMultiCartParameters.builder().build(), multiCart);
        var fetchingContext = MultiCartFetchingContext.of(multiCartContext, multiCart);


        FlowSessionHelper.patchSession(fetchingContext,
                MultiCartFetchingContext::makeImmutableContext,
                (c, v) -> c.getMultiCartContext().setStocksStage(v), Map.of(
                        new StocksItemKey(item.getShopSku(), item.getSupplierId(), item.getWarehouseId(), false),
                        2
                ));

        var cartFetchingContext = CartFetchingContext.of(fetchingContext,
                ActualizationContext.builder()
                        .withInitialCart(ImmutableOrder.from(order))
                        .withCart(order)
                        .withImmutableMulticartContext(ImmutableMultiCartContext.from(multiCartContext, multiCart)),
                order);

        var pushApiResponse = new PushApiCartResponse();
        pushApiResponse.setItems(List.of(new PushApiCartItemResponse(
                item.getFeedId(),
                item.getOfferId(),
                item.getBundleId(),
                1,
                item.getPrice(),
                item.getVat(),
                true,
                item.getSellerInn()
        )));

        FlowSessionHelper.patchSession(cartFetchingContext,
                CartFetchingContext::makeImmutableContext,
                (c, v) -> c.getActualizationContextBuilder().withPushApiCartStage(v), pushApiResponse);

        priceAndCountMutation.onSuccess(cartFetchingContext);

        assertThat(item.getChanges(), Matchers.hasItem(ItemChange.COUNT));
        assertThat(item.getCount(), Matchers.comparesEqualTo(1));
    }
}
