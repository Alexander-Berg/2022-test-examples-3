package ru.yandex.market.checkout.checkouter.actualization.fetchers.mutations;

import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.checkout.checkouter.actualization.flow.context.CartFetchingContext;
import ru.yandex.market.checkout.checkouter.actualization.flow.context.MultiCartFetchingContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ActualizationContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartParameters;
import ru.yandex.market.checkout.checkouter.actualization.model.MultiCartContext;
import ru.yandex.market.checkout.checkouter.actualization.model.StocksItemKey;
import ru.yandex.market.checkout.checkouter.cart.ItemChange;
import ru.yandex.market.checkout.checkouter.color.ColorConfig;
import ru.yandex.market.checkout.checkouter.color.SingleColorConfig;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.immutable.ImmutableOrder;
import ru.yandex.market.checkout.helpers.FlowSessionHelper;
import ru.yandex.market.checkout.providers.MultiCartProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static org.hamcrest.MatcherAssert.assertThat;

public class ItemsStockCountMutationTest {

    private ItemsStockCountMutation stockCountMutation;

    @BeforeEach
    void configure() {
        var colorConfig = Mockito.mock(ColorConfig.class);
        var config = Mockito.mock(SingleColorConfig.class);
        Mockito.doReturn(config).when(colorConfig).getFor(Mockito.any(Order.class));
        Mockito.doAnswer(args -> {
            OrderItem i = args.getArgument(0);
            return i.getShopSku();
        }).when(config).getShopSku(Mockito.any(OrderItem.class));
        stockCountMutation = new ItemsStockCountMutation(colorConfig);
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

        stockCountMutation.onSuccess(CartFetchingContext.of(fetchingContext,
                ActualizationContext.builder()
                        .withInitialCart(ImmutableOrder.from(order))
                        .withCart(order)
                        .withImmutableMulticartContext(ImmutableMultiCartContext.from(multiCartContext, multiCart)),
                order));

        assertThat(item.getChanges(), Matchers.hasItem(ItemChange.MISSING));
        assertThat(item.getCount(), Matchers.comparesEqualTo(0));
    }

    @Test
    void shouldReduceCountToZeroIfNotEnough() {
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

        stockCountMutation.onSuccess(CartFetchingContext.of(fetchingContext,
                ActualizationContext.builder()
                        .withInitialCart(ImmutableOrder.from(order))
                        .withCart(order)
                        .withImmutableMulticartContext(ImmutableMultiCartContext.from(multiCartContext, multiCart)),
                order));

        assertThat(item.getChanges(), Matchers.hasItem(ItemChange.COUNT));
        assertThat(item.getCount(), Matchers.comparesEqualTo(1));
    }

    @Test
    void shouldChangeNothingIfStockEnough() {
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
                        10
                ));

        stockCountMutation.onSuccess(CartFetchingContext.of(fetchingContext, ActualizationContext.builder()
                        .withInitialCart(ImmutableOrder.from(order))
                        .withCart(order)
                        .withImmutableMulticartContext(ImmutableMultiCartContext.from(multiCartContext, multiCart)),
                order));

        assertThat(item.getChanges(), Matchers.nullValue());
        assertThat(item.getCount(), Matchers.comparesEqualTo(3));
    }
}
