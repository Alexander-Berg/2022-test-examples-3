package ru.yandex.market.checkout.checkouter.actualization.fetchers.mutations;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.checkout.checkouter.actualization.flow.context.CartFetchingContext;
import ru.yandex.market.checkout.checkouter.actualization.flow.context.MultiCartFetchingContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ActualizationContext;
import ru.yandex.market.checkout.checkouter.cart.ItemChange;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureReader;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.common.report.model.QuantityLimits;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.ENABLE_QUANTITY_LIMITS_ACTUALIZATION;

public class ItemsQuantityLimitsMutationTest {

    private ItemsQuantityLimitsMutation quantityLimitsMutation;

    @BeforeEach
    void configure() {
        var checkouterFeatureReader = Mockito.mock(CheckouterFeatureReader.class);
        Mockito.doReturn(true).when(checkouterFeatureReader)
                .getBoolean(Mockito.eq(ENABLE_QUANTITY_LIMITS_ACTUALIZATION));
        quantityLimitsMutation = new ItemsQuantityLimitsMutation(checkouterFeatureReader);
    }

    @Test
    void shouldSetCountToZeroIfItLessThenLimit() {
        var context = CartFetchingContext.of(Mockito.mock(MultiCartFetchingContext.class),
                ActualizationContext.builder(),
                OrderProvider.orderBuilder()
                        .itemBuilder(OrderItemProvider.orderItemBuilder()
                                .offer("some offer")
                                .count(1)
                                .quantityLimits(new QuantityLimits(3, 3))
                        )
                        .build());
        quantityLimitsMutation.onSuccess(context);

        var item = context.getOrder().getItems().iterator().next();

        assertThat(item.getChanges(), Matchers.hasItem(ItemChange.MISSING));
        assertThat(item.getCount(), Matchers.comparesEqualTo(0));
    }

    @Test
    void shouldReduceCountToZeroIfItNotProportional() {
        var context = CartFetchingContext.of(Mockito.mock(MultiCartFetchingContext.class),
                ActualizationContext.builder(),
                OrderProvider.orderBuilder()
                        .itemBuilder(OrderItemProvider.orderItemBuilder()
                                .offer("some offer")
                                .count(8)
                                .quantityLimits(new QuantityLimits(3, 3))
                        )
                        .build());
        quantityLimitsMutation.onSuccess(context);

        var item = context.getOrder().getItems().iterator().next();

        assertThat(item.getChanges(), Matchers.hasItem(ItemChange.COUNT));
        assertThat(item.getCount(), Matchers.comparesEqualTo(6));
    }

    @Test
    void shouldNotChangeCountIfItAlreadyHas() {
        var context = CartFetchingContext.of(Mockito.mock(MultiCartFetchingContext.class),
                ActualizationContext.builder(),
                OrderProvider.orderBuilder()
                        .itemBuilder(OrderItemProvider.orderItemBuilder()
                                .offer("some offer")
                                .count(8)
                                .change(ItemChange.COUNT)
                                .quantityLimits(new QuantityLimits(3, 3))
                        )
                        .build());
        quantityLimitsMutation.onSuccess(context);

        var item = context.getOrder().getItems().iterator().next();

        assertThat(item.getCount(), Matchers.comparesEqualTo(8));
    }
}
