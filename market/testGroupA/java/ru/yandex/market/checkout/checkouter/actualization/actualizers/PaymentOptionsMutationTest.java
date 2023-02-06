package ru.yandex.market.checkout.checkouter.actualization.actualizers;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.mutations.PaymentOptionsMutation;
import ru.yandex.market.checkout.checkouter.actualization.flow.context.CartFetchingContext;
import ru.yandex.market.checkout.checkouter.actualization.flow.context.MultiCartFetchingContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ActualizationContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartParameters;
import ru.yandex.market.checkout.checkouter.actualization.model.MultiCartContext;
import ru.yandex.market.checkout.checkouter.actualization.model.PushApiCartResponse;
import ru.yandex.market.checkout.checkouter.actualization.services.PaymentOptionsService;
import ru.yandex.market.checkout.checkouter.actualization.services.PostpaidMlDeciderService;
import ru.yandex.market.checkout.checkouter.color.ColorConfig;
import ru.yandex.market.checkout.checkouter.color.SingleColorConfig;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureReader;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.geo.GeoRegionService;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.immutable.ImmutableOrder;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.shop.ShopService;
import ru.yandex.market.checkout.checkouter.shop.SupplierMetaDataService;
import ru.yandex.market.checkout.checkouter.util.CheckouterProperties;
import ru.yandex.market.checkout.helpers.FlowSessionHelper;
import ru.yandex.market.checkout.providers.MultiCartProvider;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.sdk.userinfo.service.NoSideEffectUserService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.checkout.checkouter.feature.type.permanent.PermanentCollectionFeatureType.PAYMENT_DISABLE_METHOD;
import static ru.yandex.market.checkout.checkouter.feature.type.permanent.PermanentIntegerFeatureType.PAYMENT_DISABLE_USER_PERCENT;

@ExtendWith(MockitoExtension.class)
public class PaymentOptionsMutationTest {

    @Mock
    private SupplierMetaDataService supplierMetaDataService;
    @Mock
    private ShopService shopService;
    @Mock
    private ColorConfig colorConfig;
    @Mock
    private CheckouterFeatureReader checkouterFeatureReader;
    @Mock
    private PaymentOptionsService paymentOptionsService;
    @Mock
    private CheckouterProperties checkouterProperties;
    @Mock
    private GeoRegionService geoRegionService;
    @Mock
    private PostpaidMlDeciderService postpaidMlDeciderService;
    @Mock
    private NoSideEffectUserService noSideEffectUserService;

    @InjectMocks
    private PaymentOptionsMutation mutation;

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void actualize_ifB2bInPaymentOptionsThenSetPaymentMethodsToB2b(boolean enableYandexPaymentFeature) {
        if (enableYandexPaymentFeature) {
            when(checkouterFeatureReader.getBoolean(any()))
                    .thenAnswer(invocation -> {
                        BooleanFeatureType argument = invocation.getArgument(0, BooleanFeatureType.class);
                        return BooleanFeatureType.ENABLE_B2B_ORDER_YANDEX_PAYMENT.equals(argument);
                    });
        }

        // Given
        DeliveryResponse b2bOption = DeliveryProvider.buildShopDeliveryResponse(DeliveryResponse::new);
        b2bOption.setPaymentOptions(Set.of(PaymentMethod.B2B_ACCOUNT_PREPAYMENT));

        Order buyerCart = OrderProvider.getBlueOrder();
        configureMockToNotFailOnNPECalls();

        PushApiCartResponse cartResponse = new PushApiCartResponse();
        cartResponse.setPaymentMethods(List.of(PaymentMethod.YANDEX));
        cartResponse.setDeliveryOptions(List.of(b2bOption));

        // When
        mutate(buyerCart, cartResponse);

        // Then
        if (enableYandexPaymentFeature) {
            assertEquals(
                    List.of(PaymentMethod.B2B_ACCOUNT_PREPAYMENT, PaymentMethod.YANDEX),
                    cartResponse.getPaymentMethods()
            );
        } else {
            assertEquals(
                    List.of(PaymentMethod.B2B_ACCOUNT_PREPAYMENT),
                    cartResponse.getPaymentMethods());
        }
    }

    private void configureMockToNotFailOnNPECalls() {
        SingleColorConfig config = mock(SingleColorConfig.class);
        doReturn(config).when(colorConfig).getFor(any(Color.class));
    }

    @Test
    public void actualize_ifNoB2bInPaymentOptionsThenNoChangesToPaymentOptions() {
        // Given
        DeliveryResponse noB2bOption = DeliveryProvider.buildShopDeliveryResponse(DeliveryResponse::new);

        Order buyerCart = OrderProvider.getBlueOrder();
        configureMockToNotFailOnNPECalls();

        PushApiCartResponse cartResponse = new PushApiCartResponse();
        cartResponse.setPaymentMethods(List.of(PaymentMethod.YANDEX, PaymentMethod.SBP));
        cartResponse.setDeliveryOptions(List.of(noB2bOption));

        // When
        mutate(buyerCart, new PushApiCartResponse());

        // Then
        assertEquals(List.of(PaymentMethod.YANDEX, PaymentMethod.SBP), cartResponse.getPaymentMethods());
    }

    @Test
    public void actualize_doNotAddPostpaidPaymentOptionsForRegularUser() {
        // Given
        doReturn(false).when(noSideEffectUserService).isNoSideEffectUid(anyLong());
        Order buyerCart = OrderProvider.getBlueOrder();
        buyerCart.setPaymentOptions(Set.of());

        configureMockToNotFailOnNPECalls();

        // When
        mutate(buyerCart, new PushApiCartResponse());

        // Then
        verify(paymentOptionsService, never()).enablePostpaidMethods(any());
    }

    @Test
    public void actualize_addPostpaidPaymentOptionsForTesterUser() {
        // Given
        when(checkouterFeatureReader.getInteger(eq(PAYMENT_DISABLE_USER_PERCENT))).thenReturn(100);
        doReturn(true).when(noSideEffectUserService).isNoSideEffectUid(anyLong());
        Order buyerCart = OrderProvider.getBlueOrder();
        buyerCart.setPaymentOptions(Set.of());

        configureMockToNotFailOnNPECalls();

        // When
        mutate(buyerCart, new PushApiCartResponse());

        // Then
        verify(paymentOptionsService).enablePostpaidMethods(buyerCart);
    }

    @Test
    public void actualize_checkDisabledMethodsForRegularUser() {
        // Given
        when(checkouterFeatureReader.getInteger(eq(PAYMENT_DISABLE_USER_PERCENT))).thenReturn(100);
        doReturn(false).when(noSideEffectUserService).isNoSideEffectUid(anyLong());
        Order buyerCart = OrderProvider.getBlueOrder();
        buyerCart.setPaymentOptions(Set.of());

        configureMockToNotFailOnNPECalls();

        // When
        mutate(buyerCart, new PushApiCartResponse());

        // Then
        verify(checkouterFeatureReader).getSet(eq(PAYMENT_DISABLE_METHOD), eq(String.class));
    }

    @Test
    public void actualize_doNotCheckDisabledMethodsForTesterUser() {
        // Given
        when(checkouterFeatureReader.getInteger(eq(PAYMENT_DISABLE_USER_PERCENT))).thenReturn(100);
        doReturn(true).when(noSideEffectUserService).isNoSideEffectUid(anyLong());
        Order buyerCart = OrderProvider.getBlueOrder();
        buyerCart.setPaymentOptions(Set.of());

        configureMockToNotFailOnNPECalls();

        // When
        mutate(buyerCart, new PushApiCartResponse());

        // Then
        verify(checkouterFeatureReader, never()).getSet(eq(PAYMENT_DISABLE_METHOD), eq(String.class));
    }

    private void mutate(Order order, PushApiCartResponse shopCart) {
        var multiCartContext = MultiCartContext.createBy(
                ImmutableMultiCartParameters.builder().build(),
                Map.of());
        var immutableMultiCartContext = ImmutableMultiCartContext.from(multiCartContext,
                MultiCartProvider.buildMultiCart(List.of()));
        var actualizationContextBuilder = ActualizationContext.builder()
                .withImmutableMulticartContext(immutableMultiCartContext)
                .withCart(order)
                .withInitialCart(ImmutableOrder.from(order))
                .withOriginalBuyerCurrency(Currency.RUR);
        var multiCart = MultiCartProvider.single(order);
        var multiCartFetchingContext = MultiCartFetchingContext.of(MultiCartContext.createBy(
                ImmutableMultiCartParameters.builder().build(), multiCart), multiCart);
        var fetchingContext = CartFetchingContext.of(
                multiCartFetchingContext, actualizationContextBuilder, order);

        FlowSessionHelper.patchSession(fetchingContext, CartFetchingContext::makeImmutableContext,
                (c, v) -> c.getActualizationContextBuilder().withPushApiCartStage(v), shopCart);
        mutation.onSuccess(fetchingContext);
    }
}
