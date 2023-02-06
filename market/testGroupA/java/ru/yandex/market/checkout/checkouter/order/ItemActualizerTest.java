package ru.yandex.market.checkout.checkouter.order;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.checkout.checkouter.actual.ActualItem;
import ru.yandex.market.checkout.checkouter.actual.ActualizeItemRequest;
import ru.yandex.market.checkout.checkouter.actualization.CartActualizer;
import ru.yandex.market.checkout.checkouter.actualization.PreparingStageProcessor;
import ru.yandex.market.checkout.checkouter.actualization.flow.FetchingStage;
import ru.yandex.market.checkout.checkouter.actualization.flow.context.MultiCartFetchingContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ActualizationContext;
import ru.yandex.market.checkout.checkouter.actualization.model.MultiCartContext;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureReader;
import ru.yandex.market.checkout.checkouter.order.immutable.ImmutableOrder;
import ru.yandex.market.checkout.checkouter.report.Experiments;
import ru.yandex.market.checkout.checkouter.util.CheckouterProperties;
import ru.yandex.market.checkout.checkouter.util.CheckouterPropertiesImpl;
import ru.yandex.market.checkout.helpers.FlowSessionHelper;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class ItemActualizerTest {

    @Mock
    CartActualizer cartActualizer;
    @Spy
    CheckouterProperties checkouterProperties = new CheckouterPropertiesImpl();
    @Mock
    PreparingStageProcessor preparingStageProcessor;
    @Mock
    CheckouterFeatureReader checkouterFeatureReader;

    ItemActualizer itemActualizer;

    @BeforeEach
    public void setUp() {
        itemActualizer = new ItemActualizer(cartActualizer, null, null, preparingStageProcessor,
                checkouterFeatureReader);
    }

    @Test
    public void testItemActualizer() {
        ActualItem item = new ActualItem();
        item.setBuyerRegionId(2L);
        item.setShopId(383182L);
        item.setFeedId(383182L);
        item.setOfferId("3");
        item.setRgb(Color.BLUE);

        Mockito.when(cartActualizer.actualizeCart(any(), any()))
                .thenAnswer(invocation -> {
                    Order order = (Order) invocation.getArguments()[0];
                    order.setDeliveryOptions(createDelivery());
                    order.getItems().forEach(oi -> oi.setCount(1));

                    return true;
                });

        Mockito.when(preparingStageProcessor.actualizeResources(any(), any(), any()))
                .then(invocation -> {
                    MultiCartContext context = invocation.getArgument(0);
                    MultiCart multiCart = invocation.getArgument(1);
                    Order cart = multiCart.getCarts().get(0);
                    var fetchingContext = MultiCartFetchingContext.of(context, multiCart);

                    FlowSessionHelper.patchSession(
                            fetchingContext,
                            MultiCartFetchingContext::makeImmutableContext,
                            (c, v) -> c.getMultiCartContext().setActualizationContextBuildStage(v),
                            Map.of(
                                    cart.getLabel(),
                                    ActualizationContext.builder()
                                            .withCart(cart)
                                            .withInitialCart(ImmutableOrder.from(cart))
                            )
                    );
                    return FetchingStage.canceled(fetchingContext.getSession());
                });

        ActualItem actualItem = itemActualizer.actualizeItem(item, 123L, ApiSettings.PRODUCTION, true, null,
                Experiments.empty(),
                null, null, new ActualizeItemRequest());
        assertThat(actualItem.getOutletIds(), hasSize(2));
    }

    @Nonnull
    private List<Delivery> createDelivery() {
        DeliveryResponse response1 = new DeliveryResponse();
        response1.setType(DeliveryType.PICKUP);
        response1.setOutletIds(Set.of(69L));

        DeliveryResponse response2 = new DeliveryResponse();
        response2.setType(DeliveryType.PICKUP);
        response2.setOutletIds(Set.of(96L));

        DeliveryResponse response3 = new DeliveryResponse();
        response3.setType(DeliveryType.PICKUP);
        response3.setOutletIds(Set.of(69L, 96L));

        return Arrays.asList(response1, response2, response3);
    }

}
