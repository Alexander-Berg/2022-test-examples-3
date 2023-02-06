package ru.yandex.market.checkout.checkouter.actualization.actualizers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.antifraud.orders.entity.AntifraudAction;
import ru.yandex.market.antifraud.orders.entity.AntifraudCheckResult;
import ru.yandex.market.antifraud.orders.entity.OrderVerdict;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.AntifraudFetcher;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.mutations.AntifraudPostPaidMethodsMutation;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.mutations.antifraud.CancelOrderActionMutation;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.mutations.antifraud.FraudItemChangeActionMutation;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.mutations.antifraud.RoboCallActionMutation;
import ru.yandex.market.checkout.checkouter.actualization.flow.context.CartFetchingContext;
import ru.yandex.market.checkout.checkouter.actualization.flow.context.MultiCartFetchingContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ActualizationContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableActualizationContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartParameters;
import ru.yandex.market.checkout.checkouter.actualization.model.MultiCartContext;
import ru.yandex.market.checkout.checkouter.actualization.model.PushApiCartResponse;
import ru.yandex.market.checkout.checkouter.actualization.services.PaymentOptionsService;
import ru.yandex.market.checkout.checkouter.actualization.stages.AntifraudStage;
import ru.yandex.market.checkout.checkouter.antifraud.AntifraudService;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureReader;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.immutable.ImmutableOrder;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.util.CheckouterProperties;
import ru.yandex.market.checkout.checkouter.util.CheckouterPropertiesImpl;
import ru.yandex.market.checkout.checkouter.validation.ValidationResult;
import ru.yandex.market.checkout.helpers.FlowSessionHelper;
import ru.yandex.market.checkout.providers.MultiCartProvider;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.sdk.userinfo.service.NoSideEffectUserService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.checkout.checkouter.antifraud.entity.FraudCheckResult.fraudAction;

@ExtendWith(MockitoExtension.class)
public class AntifraudActualizerTest {

    @InjectMocks
    private PaymentOptionsService paymentOptionsService;
    @InjectMocks
    private AntifraudFetcher antifraudFetcher;
    @InjectMocks
    private AntifraudStage antifraudStage;
    @Mock
    private AntifraudService antifraudService;
    @Mock
    private NoSideEffectUserService noSideEffectUserService;
    @Mock
    private CheckouterFeatureReader checkouterFeatureReader;
    @Mock
    private ImmutableMultiCartParameters cartParameters;
    @Spy
    private FraudItemChangeActionMutation itemChangeActionMutation;
    @Spy
    private CancelOrderActionMutation cancelOrderActionMutation;
    @Spy
    private RoboCallActionMutation roboCallActionMutation;
    @Spy
    private final CheckouterProperties checkouterProperties = new CheckouterPropertiesImpl(
            checkouterFeatureReader,
            null);
    private AntifraudPostPaidMethodsMutation antifraudPostPaidMethodsMutation;
    private Order order;
    private ActualizationContext.ActualizationContextBuilder actualizationContextBuilder;

    @BeforeEach
    public void setUp() throws Exception {
        antifraudPostPaidMethodsMutation = new AntifraudPostPaidMethodsMutation(paymentOptionsService);

        order = new Order();
        Delivery delivery = new Delivery();
        delivery.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        order.setDelivery(delivery);
        order.setBuyer(BuyerProvider.getBuyer());

        var multiCartContext = MultiCartContext.createBy(cartParameters, Map.of());
        var immutableMultiCartContext = ImmutableMultiCartContext.from(
                multiCartContext,
                MultiCartProvider.buildMultiCart(List.of()));
        actualizationContextBuilder = ActualizationContext.builder()
                .withImmutableMulticartContext(immutableMultiCartContext)
                .withCart(order)
                .withInitialCart(ImmutableOrder.from(order))
                .withOriginalBuyerCurrency(Currency.RUR);
        lenient().when(cartParameters.isUseLightActualization()).thenReturn(false);
        lenient().when(noSideEffectUserService.isNoSideEffectUid(anyLong())).thenReturn(false);
    }

    @Test
    public void nonFulfilmentOrderOldWay() {
        order.setFulfilment(false);
        order.setContext(Context.MARKET);
        when(checkouterFeatureReader.getBoolean(BooleanFeatureType.ENABLE_ANTIFRAUD_FOR_ALL_ORDERS)).thenReturn(false);

        assertFalse(antifraudStage.needToCheckInAntifraud()
                .test(ImmutableActualizationContext.of(actualizationContextBuilder.build())));
    }

    @Test
    public void fulfilmentOrderOldWay() {
        order.setFulfilment(true);
        order.setContext(Context.MARKET);
        when(checkouterFeatureReader.getBoolean(BooleanFeatureType.ENABLE_ANTIFRAUD_FOR_ALL_ORDERS)).thenReturn(false);

        assertTrue(antifraudStage.needToCheckInAntifraud()
                .test(ImmutableActualizationContext.of(actualizationContextBuilder.build())));
    }

    @Test
    public void nonFulfilmentOrderNewWay() {
        order.setFulfilment(false);
        order.setContext(Context.MARKET);
        when(checkouterFeatureReader.getBoolean(BooleanFeatureType.ENABLE_ANTIFRAUD_FOR_ALL_ORDERS)).thenReturn(true);

        assertTrue(antifraudStage.needToCheckInAntifraud()
                .test(ImmutableActualizationContext.of(actualizationContextBuilder.build())));
    }

    @Test
    public void fulfilmentOrderNewWay() {
        order.setFulfilment(true);
        order.setContext(Context.MARKET);
        when(checkouterFeatureReader.getBoolean(BooleanFeatureType.ENABLE_ANTIFRAUD_FOR_ALL_ORDERS)).thenReturn(true);

        assertTrue(antifraudStage.needToCheckInAntifraud()
                .test(ImmutableActualizationContext.of(actualizationContextBuilder.build())));
    }

    @Test
    public void wrongContextOrderNewWay() {
        order.setFulfilment(true);
        order.setContext(Context.SANDBOX);
        when(checkouterFeatureReader.getBoolean(BooleanFeatureType.ENABLE_ANTIFRAUD_FOR_ALL_ORDERS)).thenReturn(true);

        assertFalse(antifraudStage.needToCheckInAntifraud()
                .test(ImmutableActualizationContext.of(actualizationContextBuilder.build())));
    }

    @Test
    public void fakeOrderNewWay() {
        order.setFulfilment(true);
        order.setContext(Context.MARKET);
        order.setFake(true);
        when(checkouterFeatureReader.getBoolean(BooleanFeatureType.ENABLE_ANTIFRAUD_FOR_ALL_ORDERS)).thenReturn(true);

        assertFalse(antifraudStage.needToCheckInAntifraud()
                .test(ImmutableActualizationContext.of(actualizationContextBuilder.build())));
    }

    @Test
    public void shopDeliveryOrdersIgnorePrepaidOnlyAction() throws Throwable {
        order.setFulfilment(false);
        order.getDelivery().setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        order.setContext(Context.MARKET);

        mutate(order, mock(PushApiCartResponse.class));
    }

    @Test
    public void marketDeliveryOrdersApplyPrepaidOnlyActionIfPostpaidOptionsRemain() throws Throwable {
        order.setFulfilment(false);
        order.setContext(Context.MARKET);
        addPaymentOptionsToOrder(Set.of(PaymentMethod.CASH_ON_DELIVERY, PaymentMethod.YANDEX));

        PushApiCartResponse cartResponse = createPushApiCartResponse(Set.of(PaymentMethod.CASH_ON_DELIVERY,
                PaymentMethod.YANDEX));
        when(cartParameters.isShowSbp()).thenReturn(true);
        mutate(order, cartResponse);
        assertThat(order.getPaymentOptions().size(), equalTo(3));
    }

    @Test
    public void marketDeliveryOrdersIgnorePrepaidOnlyActionIfPostpaidOptionsNotRemain() throws Throwable {
        order.setFulfilment(false);
        order.setContext(Context.MARKET);
        addPaymentOptionsToOrder(Set.of(PaymentMethod.CASH_ON_DELIVERY));

        PushApiCartResponse cartResponse = createPushApiCartResponse(Set.of(PaymentMethod.CASH_ON_DELIVERY));
        mutate(order, cartResponse);
        assertThat(order.getPaymentOptions().size(), equalTo(1));
    }

    @ParameterizedTest
    @CsvSource(
            delimiter = ':',
            value = {
                    "123:answer_text:reason:answer_text",
                    ":answer_text:reason:answer_text, reason",
            })
    public void checkAntiFraudValidationMessage(Long businessBalanceId,
                                                String answerText,
                                                String reason,
                                                String expectedValidationMessage) throws Throwable {
        // для бизнес-клиентов должно возвращать только текст ответа (answer_text),
        // для остальных - текст ответа и причину (answer_text, reason)

        order.setFulfilment(false);
        order.setContext(Context.MARKET);
        order.setBuyer(new Buyer());
        order.getBuyer().setBusinessBalanceId(businessBalanceId);

        addPaymentOptionsToOrder(Set.of(PaymentMethod.CASH_ON_DELIVERY));

        var cartFetchingContext = mock(CartFetchingContext.class);
        when(cartFetchingContext.getOrder()).thenReturn(order);
        cancelOrderActionMutation.onSuccess(List.of(fraudAction(OrderVerdict.EMPTY, Set.of(new AntifraudCheckResult(
                AntifraudAction.CANCEL_ORDER, answerText, reason)))), cartFetchingContext);

        var actualValidationMessage =
                order.getValidationErrors()
                        .stream()
                        .map(ValidationResult::getMessage)
                        .findFirst()
                        .orElse(null);

        assertThat(actualValidationMessage, equalTo(expectedValidationMessage));
    }

    private void addPaymentOptionsToOrder(Set<PaymentMethod> paymentMethods) {
        order.setPaymentOptions(new HashSet<>(paymentMethods));
        order.setDeliveryOptions(List.of(order.getDelivery()));
        order.getDelivery().setPaymentOptions(new HashSet<>(paymentMethods));
    }

    @Nonnull
    private PushApiCartResponse createPushApiCartResponse(Set<PaymentMethod> paymentMethods) {
        PushApiCartResponse cartResponse = new PushApiCartResponse();
        cartResponse.setPaymentMethods(new ArrayList<>(paymentMethods));
        cartResponse.setDeliveryOptions(List.of(new DeliveryResponse()));
        cartResponse.getDeliveryOptions().forEach(d -> d.setPaymentOptions(new HashSet<>(paymentMethods)));
        return cartResponse;
    }

    private void mutate(Order order, PushApiCartResponse shopCart) {
        var multiCart = MultiCartProvider.single(order);
        var multiCartFetchingContext = MultiCartFetchingContext.of(MultiCartContext.createBy(
                ImmutableMultiCartParameters.builder().build(), multiCart), multiCart);
        var fetchingContext = CartFetchingContext.of(
                multiCartFetchingContext, actualizationContextBuilder, order);

        FlowSessionHelper.patchSession(fetchingContext, CartFetchingContext::makeImmutableContext,
                (c, v) -> c.getActualizationContextBuilder().withPushApiCartStage(v), shopCart);
        FlowSessionHelper.patchSession(fetchingContext, CartFetchingContext::makeImmutableContext,
                (c, v) -> c.getActualizationContextBuilder().withAntifraudCheckStage(v),
                List.of(fraudAction(OrderVerdict.EMPTY,
                        Set.of(new AntifraudCheckResult(AntifraudAction.PREPAID_ONLY, "", "")))));
        antifraudPostPaidMethodsMutation.onSuccess(fetchingContext);
    }
}
