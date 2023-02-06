package ru.yandex.market.checkout.checkouter.order;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Test;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.ShopMetaDataBuilder;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.mutations.PushApiPaymentMethodsMutation;
import ru.yandex.market.checkout.checkouter.actualization.flow.context.CartFetchingContext;
import ru.yandex.market.checkout.checkouter.actualization.flow.context.MultiCartFetchingContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ActualizationContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartParameters;
import ru.yandex.market.checkout.checkouter.actualization.model.MultiCartContext;
import ru.yandex.market.checkout.checkouter.actualization.model.PushApiCartResponse;
import ru.yandex.market.checkout.checkouter.actualization.services.PaymentMethodValidator;
import ru.yandex.market.checkout.checkouter.order.immutable.ImmutableOrder;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentOption;
import ru.yandex.market.checkout.checkouter.pay.PaymentOptionHiddenReason;
import ru.yandex.market.checkout.checkouter.shop.MemCachingShopServiceWrapper;
import ru.yandex.market.checkout.checkouter.shop.PaymentClass;
import ru.yandex.market.checkout.checkouter.shop.PrepayType;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.checkout.checkouter.shop.ShopService;
import ru.yandex.market.checkout.checkouter.shop.SupplierMetaDataService;
import ru.yandex.market.checkout.helpers.FlowSessionHelper;
import ru.yandex.market.checkout.providers.MultiCartProvider;
import ru.yandex.market.checkout.pushapi.client.entity.CartResponse;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.checkout.checkouter.actualization.utils.PushApiUtils.mapToPushApiCartResponse;

public class PushApiPaymentMethodsMutationTest {

    public static final ShopMetaData TEST_SHOP_METADATA = ShopMetaDataBuilder.createTestDefault()
            .withCampaiginId(0)
            .withClientId(0)
            .withSandboxClass(PaymentClass.SHOP)
            .withProdClass(PaymentClass.SHOP)
            .withPrepayType(PrepayType.YANDEX_MONEY)
            .build();
    private static final boolean KNOWN_USER = false;
    private static final boolean UNKNOWN_USER = true;
    // Валидирует все оплаты
    private final PaymentMethodValidator trueValidator = new PaymentMethodValidator(null) {

        public List<PaymentOption> getAndMarkForShop(List<PaymentMethod> paymentMethods,
                                                     Set<PaymentClass> paymentClasses,
                                                     Long shopId) {
            return getPaymentOptions(paymentMethods);
        }

        public List<PaymentOption> getAndMarkForDelivery(DeliveryResponse deliveryOption,
                                                         Set<PaymentClass> paymentClasses) {
            return getPaymentOptions(deliveryOption.getPaymentOptions());
        }
    };
    private final PaymentMethodValidator falseValidator = new PaymentMethodValidator(null) {

        public List<PaymentOption> getAndMarkForShop(List<PaymentMethod> paymentMethods,
                                                     Set<PaymentClass> paymentClasses,
                                                     Long shopId) {
            return emptyList();
        }

        public List<PaymentOption> getAndMarkForDelivery(DeliveryResponse deliveryOption,
                                                         Set<PaymentClass> paymentClasses) {
            return emptyList();
        }
    };
    private final PaymentMethodValidator antiYandexValidator = new PaymentMethodValidator(null) {

        public List<PaymentOption> getAndMarkForShop(List<PaymentMethod> paymentMethods,
                                                     Set<PaymentClass> paymentClasses,
                                                     Long shopId) {
            List<PaymentOption> shopPaymentOptions = getPaymentOptions(paymentMethods);
            return getMarkedForUser(shopPaymentOptions);
        }

        public List<PaymentOption> getAndMarkForDelivery(DeliveryResponse deliveryOption,
                                                         Set<PaymentClass> paymentClasses) {
            List<PaymentOption> deliveryPaymentOptions = getPaymentOptions(deliveryOption.getPaymentOptions());
            return getMarkedForUser(deliveryPaymentOptions);
        }

        public List<PaymentOption> getMarkedForUser(List<PaymentOption> shopPaymentOptions) {
            List<PaymentOption> marked = new ArrayList<>();
            for (PaymentOption po : shopPaymentOptions) {
                PaymentOption newOption = new PaymentOption(po);
                if (newOption.getPaymentMethod() == PaymentMethod.YANDEX) {
                    newOption.setHiddenReason(PaymentOptionHiddenReason.MUID);
                }
                marked.add(newOption);
            }
            return marked;
        }

    };

    @Test
    public void shouldAddDefaultPaymentMethodToAbsentDeliveryPayments() {
        PushApiPaymentMethodsMutation actualizer = new PushApiPaymentMethodsMutation(
                new SupplierMetaDataService(getShopService(), 0), trueValidator
        );
        List<PaymentMethod> paymentMethods = Arrays.asList(PaymentMethod.SHOP_PREPAID, PaymentMethod.CASH_ON_DELIVERY);
        PushApiCartResponse shopCart = mapToPushApiCartResponse(getCartResponseWithPaymentMethods(paymentMethods));

        mutate(actualizer, mockOrder(KNOWN_USER), shopCart);

        assertEquals(2, paymentMethods.size(), "Original list was changed");
        assertEquals(paymentMethods, shopCart.getPaymentMethods(), "Default payment was list changed");
        assertEquals(new HashSet<>(paymentMethods), shopCart.getDeliveryOptions().get(0).getPaymentOptions(),
                "Empty payment list was not inited");
    }

    @Test
    public void shouldNotRemoveAllValidPaymentMethodsForKnownUser() {
        PushApiPaymentMethodsMutation actualizer = new PushApiPaymentMethodsMutation(
                new SupplierMetaDataService(getShopService(), 0), trueValidator
        );
        List<PaymentMethod> paymentMethods = Arrays.asList(PaymentMethod.SHOP_PREPAID, PaymentMethod.CASH_ON_DELIVERY);
        PushApiCartResponse shopCart = mapToPushApiCartResponse(getCartResponseWithPaymentMethods(paymentMethods));

        mutate(actualizer, mockOrder(KNOWN_USER), shopCart);

        assertEquals(paymentMethods, shopCart.getPaymentMethods(), "Cart payments were changed");
        assertEquals(new HashSet<>(paymentMethods), shopCart.getDeliveryOptions().get(0).getPaymentOptions(),
                "Delivery payments were changed");
    }

    @Test
    public void shouldRemoveAllPaymentMethodsForKnownUserUsingFalsePaymentValidator() {
        PushApiPaymentMethodsMutation actualizer = new PushApiPaymentMethodsMutation(
                new SupplierMetaDataService(getShopService(), 0), falseValidator
        );
        List<PaymentMethod> paymentMethods = Arrays.asList(PaymentMethod.SHOP_PREPAID, PaymentMethod
                .CASH_ON_DELIVERY, PaymentMethod.YANDEX);
        PushApiCartResponse shopCart = mapToPushApiCartResponse(getCartResponseWithPaymentMethods(paymentMethods));
        mutate(actualizer, mockOrder(KNOWN_USER), shopCart);

        assertTrue(shopCart.getDeliveryOptions().get(0).getPaymentOptions().isEmpty(),
                "Delivery payments were not cleared");
        assertTrue(shopCart.getPaymentMethods().isEmpty(), "Cart's payment was not cleared");
    }

    @Test
    public void
    shouldNotRemoveYandexPaymentMethodFromDeliveryOptionPaymentsForUnknownUserWhenYandexIsNotTheOnlyPaymentMethod() {
        PushApiPaymentMethodsMutation actualizer = new PushApiPaymentMethodsMutation(
                new SupplierMetaDataService(getShopService(), 0), antiYandexValidator
        );
        List<PaymentMethod> paymentMethods = Arrays.asList(PaymentMethod.SHOP_PREPAID, PaymentMethod
                .CASH_ON_DELIVERY, PaymentMethod.YANDEX);
        PushApiCartResponse shopCart = mapToPushApiCartResponse(getCartResponseWithPaymentMethods(paymentMethods));

        mutate(actualizer, mockOrder(UNKNOWN_USER), shopCart);

        List<PaymentMethod> expectedPaymentMethods = Arrays.asList(PaymentMethod.SHOP_PREPAID, PaymentMethod
                .CASH_ON_DELIVERY);
        assertEquals(expectedPaymentMethods, shopCart.getPaymentMethods(), "Cart payments were changed");
        assertEquals(new HashSet<>(expectedPaymentMethods), shopCart.getDeliveryOptions().get(0).getPaymentOptions(),
                "Delivery payments must contain a Yandex payment method for an unknown user");
    }

    @Test
    public void
    shouldRemoveYandexPaymentMethodFromDeliveryOptionPaymentsForUnknownUserWhenYandexIsTheOnlyPaymentMethod() {
        PushApiPaymentMethodsMutation actualizer = new PushApiPaymentMethodsMutation(
                new SupplierMetaDataService(getShopService(), 0), antiYandexValidator
        );
        List<PaymentMethod> paymentMethods = Collections.singletonList(PaymentMethod.YANDEX);
        PushApiCartResponse shopCart = mapToPushApiCartResponse(getCartResponseWithPaymentMethods(paymentMethods));

        mutate(actualizer, mockOrder(UNKNOWN_USER), shopCart);

        assertEquals(Collections.emptyList(), shopCart.getPaymentMethods(), "Cart payments were changed");
        assertEquals(Collections.emptySet(), shopCart.getDeliveryOptions().get(0).getPaymentOptions(),
                "Delivery payments must not contain a Yandex payment method for an unknown user");
    }

    private void mutate(PushApiPaymentMethodsMutation mutation, Order order, PushApiCartResponse shopCart) {
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

    @Nonnull
    private CartResponse getCartResponseWithPaymentMethods(List<PaymentMethod> paymentMethods) {
        CartResponse shopCart = new CartResponse();
        shopCart.setPaymentMethods(paymentMethods);
        DeliveryResponse deliveryOption = new DeliveryResponse();
        List<DeliveryResponse> deliveryOptions = Arrays.asList(deliveryOption);
        shopCart.setDeliveryOptions(deliveryOptions);
        return shopCart;
    }

    private Order mockOrder(boolean isNoAuth) {
        Order order = mock(Order.class);
        when(order.isFulfilment()).thenReturn(false);
        when(order.isNoAuth()).thenReturn(isNoAuth);
        when(order.isFake()).thenReturn(false);
        return order;
    }

    private ShopService getShopService() {
        ShopService service = mock(MemCachingShopServiceWrapper.class);
        when(service.getMeta(anyLong()))
                .thenReturn(TEST_SHOP_METADATA);
        when(service.getMeta(anyLong(), any(ShopMetaData.class)))
                .thenReturn(TEST_SHOP_METADATA);
        return service;
    }

    private List<PaymentOption> getPaymentOptions(Iterable<PaymentMethod> paymentMethods) {
        ArrayList<PaymentOption> options = new ArrayList<>();
        for (PaymentMethod pm : paymentMethods) {
            options.add(new PaymentOption(pm));
        }
        return options;
    }
}
