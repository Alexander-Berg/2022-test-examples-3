package ru.yandex.market.checkout.checkouter.actualization.processors.postprocessors;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.checkout.checkouter.actualization.fetchers.OrderAssessorFetcher;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.mutations.OrderAssessorMutation;
import ru.yandex.market.checkout.checkouter.actualization.flow.ContextualFlowRuntimeSession;
import ru.yandex.market.checkout.checkouter.actualization.flow.context.MultiCartFetchingContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartParameters;
import ru.yandex.market.checkout.checkouter.actualization.model.MultiCartContext;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.service.assessor.AssessorService;
import ru.yandex.market.checkout.providers.MultiCartProvider;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OrderAssessorMutationTest {

    private final OrderAssessorMutation orderAssessorMutation = new OrderAssessorMutation();
    @Mock
    private AssessorService assessorService;
    @InjectMocks
    private OrderAssessorFetcher orderAssessorFetcher;

    @AfterEach
    public void tearDown() {
        Mockito.reset(assessorService);
    }

    @Test
    public void shouldSetAssessorFlag() {
        Buyer buyer = new Buyer();
        buyer.setUid(123L);

        Order order = new Order();
        order.setBuyer(buyer);

        when(assessorService.checkAssessor(123L)).thenReturn(true);

        var multiCartContext = MultiCartContext.createBy(ImmutableMultiCartParameters.builder().build(),
                Map.of());
        var fetchingContext = MultiCartFetchingContext.of(multiCartContext,
                MultiCartProvider.single(order));
        multiCartContext.setFlowRuntimeSession(ContextualFlowRuntimeSession.empty(fetchingContext));
        orderAssessorMutation.onSuccess(orderAssessorFetcher.fetch(ImmutableMultiCartContext.from(multiCartContext,
                MultiCartProvider.single(order))), fetchingContext);

        Assertions.assertTrue(order.getBuyer().getAssessor());
    }

    @Test
    public void shouldNotSetAssessorFlagIfNotAssessor() {
        Buyer buyer = new Buyer();
        buyer.setUid(123L);

        Order order = new Order();
        order.setBuyer(buyer);

        var multiCartContext = MultiCartContext.createBy(ImmutableMultiCartParameters.builder().build(),
                Map.of());
        var fetchingContext = MultiCartFetchingContext.of(multiCartContext,
                MultiCartProvider.single(order));
        multiCartContext.setFlowRuntimeSession(ContextualFlowRuntimeSession.empty(fetchingContext));
        orderAssessorMutation.onSuccess(orderAssessorFetcher.fetch(ImmutableMultiCartContext.from(multiCartContext,
                MultiCartProvider.single(order))), fetchingContext);

        Assertions.assertFalse(order.getBuyer().getAssessor());
    }
}
