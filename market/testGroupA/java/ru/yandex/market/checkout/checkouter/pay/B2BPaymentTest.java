package ru.yandex.market.checkout.checkouter.pay;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.ActualDeliveryDraftFetcher;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.ActualDeliveryFetcher;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.DeliveryFetcher;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.DeliveryRouteFetcher;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.mutations.ActualDeliveryMutation;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.mutations.DeliveryRouteMutation;
import ru.yandex.market.checkout.checkouter.actualization.flow.CartFlowFactory;
import ru.yandex.market.checkout.checkouter.actualization.flow.ContextualFlowRuntimeSession;
import ru.yandex.market.checkout.checkouter.actualization.flow.context.CartFetchingContext;
import ru.yandex.market.checkout.checkouter.actualization.flow.context.MultiCartFetchingContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ActualizationContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableActualizationContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartParameters;
import ru.yandex.market.checkout.checkouter.actualization.model.MultiCartContext;
import ru.yandex.market.checkout.checkouter.actualization.model.PushApiCartItemResponse;
import ru.yandex.market.checkout.checkouter.actualization.model.PushApiCartResponse;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.ApiSettings;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.immutable.ImmutableOrder;
import ru.yandex.market.checkout.checkouter.trace.CheckoutContextHolder;
import ru.yandex.market.checkout.helpers.FlowSessionHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.B2bCustomersTestProvider;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.MultiCartProvider;
import ru.yandex.market.checkout.test.providers.DeliveryRouteProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.common.report.model.ActualDelivery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.checkout.checkouter.pay.PaymentMethod.B2B_ACCOUNT_PREPAYMENT;

public class B2BPaymentTest extends AbstractWebTestBase {

    @Test
    @DisplayName("Для B2B заказов доступен единственный метод оплаты B2B_ACCOUNT_PREPAYMENT")
    void shouldReturnOnlyB2BPaymentMethod() {
        Parameters parameters = B2bCustomersTestProvider.defaultB2bParameters();

        MultiCart cart = orderCreateHelper.cart(parameters);

        assertEquals(1, cart.getPaymentOptions().size());
        assertTrue(cart.getPaymentOptions().contains(B2B_ACCOUNT_PREPAYMENT));
    }

    @Test
    @DisplayName("Для B2B заказов не должен запрашиваться маршрут доставки")
    void shouldNotQueryDeliveryRouteForBusinessClient() {
        var order = OrderProvider.getBlueOrderForBusinessClient();
        order.setPreorder(false);

        var mockDeliveryFetcher = testDeliveryActualizer(order);

        Mockito.verify(mockDeliveryFetcher, Mockito.never())
                .fetchDeliveryRoute(Mockito.any(), Mockito.any());
    }

    @Test
    @DisplayName("Для остальных типов заказов маршрут доставки запрашиваться должен")
    void shouldQueryDeliveryRouteForOtherClients() {
        var order = OrderProvider.getBlueOrder();
        order.setPreorder(false);

        var mockDeliveryFetcher = testDeliveryActualizer(order);

        Mockito.verify(mockDeliveryFetcher, Mockito.times(1))
                .fetchDeliveryRoute(Mockito.any(), Mockito.any());
    }

    private DeliveryFetcher getMockDeliveryFetcher(Parameters parameters) {
        var mockDeliveryFetcher = mock(DeliveryFetcher.class);
        var route = DeliveryRouteProvider.fromActualDelivery(
                parameters.configuration().cart().mockConfigurations().values().stream().findFirst()
                        .get().getReportParameters().getActualDelivery(),
                DeliveryType.DELIVERY
        );

        when(mockDeliveryFetcher.fetchDeliveryRoute(Mockito.any(), Mockito.any())).thenReturn(route);

        when(mockDeliveryFetcher.fetchActualDelivery(Mockito.any(), Mockito.any()))
                .thenReturn(new ActualDelivery());

        return mockDeliveryFetcher;
    }

    private ActualDeliveryDraftFetcher getMockActualDeliveryDraftFetcher(Parameters parameters) {
        var mockDeliveryFetcher = mock(ActualDeliveryDraftFetcher.class);

        when(mockDeliveryFetcher.fetch(Mockito.any(ImmutableActualizationContext.class)))
                .thenReturn(new ActualDelivery());

        return mockDeliveryFetcher;
    }

    private PushApiCartResponse getCartResponse(Parameters parameters, Order order) {
        OrderItem orderItem = parameters.getOrder().getItems().iterator().next();

        var itemFromOrder = order.getItems().stream().findFirst();
        itemFromOrder.ifPresent(i -> orderItem.setOfferId(i.getOfferId()));

        var cartResponse = new PushApiCartResponse();
        cartResponse.setItems(List.of(
                new PushApiCartItemResponse(
                        orderItem.getFeedId(),
                        orderItem.getOfferId(),
                        orderItem.getBundleId(),
                        1,
                        orderItem.getPrice(),
                        orderItem.getVat(),
                        orderItem.getDelivery(),
                        orderItem.getSellerInn()
                )
        ));

        return cartResponse;
    }

    private ActualizationContext.ActualizationContextBuilder getActualizationContextBuilder(Order order) {
        var multiCartParameters = ImmutableMultiCartParameters.builder()
                .withBuyerRegionId(10L)
                .withIsMultiCart(false)
                .withApiSettings(ApiSettings.PRODUCTION)
                .withActionId("actionId")
                .build();

        var multiCart = MultiCartProvider.single(order);
        var multiCartContext = MultiCartContext.createBy(
                multiCartParameters,
                multiCart
        );
        var contextBuilder = ActualizationContext.builder()
                .withOriginalBuyerCurrency(Currency.RUR)
                .withImmutableMulticartContext(ImmutableMultiCartContext.from(multiCartContext, multiCart))
                .withCart(order)
                .withInitialCart(ImmutableOrder.from(order))
                .withCombinatorFlowEnabled(true);
        contextBuilder.withFlowRuntimeSession(ContextualFlowRuntimeSession.empty(
                CartFetchingContext.of(
                        MultiCartFetchingContext.of(multiCartContext, multiCart),
                        contextBuilder, order)));
        return contextBuilder;
    }

    private DeliveryFetcher testDeliveryActualizer(Order order) {
        CheckoutContextHolder.setCheckoutOperation(true);

        Parameters parameters = BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters();

        var mockDeliveryFetcher = getMockDeliveryFetcher(parameters);
        var mockActualDeliveryDraftFetcher = getMockActualDeliveryDraftFetcher(parameters);
        var cartResponse = getCartResponse(parameters, order);
        var actualizationContextBuilder = getActualizationContextBuilder(order);
        var multiCart = MultiCartProvider.single(order);
        var multiCartFetchingContext = MultiCartFetchingContext.of(MultiCartContext.createBy(
                ImmutableMultiCartParameters.builder().build(), multiCart), multiCart);
        var fetchingContext = CartFetchingContext.of(
                multiCartFetchingContext, actualizationContextBuilder, order);

        FlowSessionHelper.patchSession(fetchingContext, CartFetchingContext::makeImmutableContext,
                (c, v) -> c.getActualizationContextBuilder().withPushApiCartStage(v), cartResponse);

        var deliveryRouteFetcher = new DeliveryRouteFetcher(mockDeliveryFetcher, checkouterFeatureReader);
        var deliveryRouteMutation = new DeliveryRouteMutation(checkouterFeatureReader);
        var actualDeliveryFetcher = new ActualDeliveryFetcher(
                null,
                checkouterFeatureReader,
                mockActualDeliveryDraftFetcher, deliveryRouteFetcher, personalDataService);
        var actualDeliveryMutation = new ActualDeliveryMutation();

        CartFlowFactory
                .fetch(deliveryRouteFetcher)
                .mutate(deliveryRouteMutation)
                .whenSuccess(CartFlowFactory
                        .fetch(actualDeliveryFetcher)
                        .mutate(actualDeliveryMutation))
                .apply(fetchingContext);

        return mockDeliveryFetcher;
    }
}
