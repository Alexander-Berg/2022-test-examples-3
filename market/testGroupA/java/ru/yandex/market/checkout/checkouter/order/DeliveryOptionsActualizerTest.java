package ru.yandex.market.checkout.checkouter.order;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.checkout.checkouter.actualization.fetchers.ActualDeliveryFetcher;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.DeliveryRouteFetcher;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.PreferableDeliveryOptionFetcher;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.mutations.DeliveryOptionsMutation;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.mutations.DigitalDeliveryTypeValidation;
import ru.yandex.market.checkout.checkouter.actualization.flow.ContextualFlowRuntimeSession;
import ru.yandex.market.checkout.checkouter.actualization.flow.context.CartFetchingContext;
import ru.yandex.market.checkout.checkouter.actualization.flow.context.MultiCartFetchingContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ActualizationContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartParameters;
import ru.yandex.market.checkout.checkouter.actualization.model.MultiCartContext;
import ru.yandex.market.checkout.checkouter.actualization.services.ActualDeliveryParcelCreationService;
import ru.yandex.market.checkout.checkouter.color.ColorConfig;
import ru.yandex.market.checkout.checkouter.color.SingleColorConfig;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.ExtraCharge;
import ru.yandex.market.checkout.checkouter.delivery.converter.DeliveryPricesConverterImpl;
import ru.yandex.market.checkout.checkouter.delivery.outlet.ShopOutletId;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureReader;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.mocks.Mocks;
import ru.yandex.market.checkout.checkouter.order.immutable.ImmutableOrder;
import ru.yandex.market.checkout.checkouter.service.personal.PersonalDataService;
import ru.yandex.market.checkout.checkouter.shop.ShopService;
import ru.yandex.market.checkout.checkouter.util.OfferItemUtils;
import ru.yandex.market.checkout.helpers.FlowSessionHelper;
import ru.yandex.market.checkout.providers.MultiCartProvider;
import ru.yandex.market.checkout.pushapi.client.entity.CartResponse;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.CurrencyConvertServiceImpl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.checkout.checkouter.actualization.utils.PushApiUtils.mapToPushApiCartResponse;

public class DeliveryOptionsActualizerTest {

    private DeliveryOptionsMutation deliveryOptionsMutation;
    private Clock clock;
    private ActualDeliveryFetcher actualDeliveryFetcher;
    private DeliveryRouteFetcher deliveryRouteFetcher;
    private ImmutableMultiCartContext immutableMultiCartContext;

    @BeforeEach
    public void init() {
        clock = Clock.systemDefaultZone();
        DeliveryPricesConverterImpl deliveryPricesConverter = new DeliveryPricesConverterImpl();
        deliveryPricesConverter.setCurrencyConvertService(CurrencyConvertServiceImpl.getMock());

        ColorConfig colorConfig = mock(ColorConfig.class);
        when(colorConfig.getFor(any(Order.class))).thenReturn(mock(SingleColorConfig.class));

        CheckouterFeatureReader featureReader = mock(CheckouterFeatureReader.class);
        doReturn(false).when(featureReader)
                .getBoolean(BooleanFeatureType.DISABLE_CREDIT_FOR_B2B);

        actualDeliveryFetcher = Mocks.createMock(ActualDeliveryFetcher.class);
        deliveryRouteFetcher = Mocks.createMock(DeliveryRouteFetcher.class);
        deliveryOptionsMutation = new DeliveryOptionsMutation(
                deliveryPricesConverter,
                new ActualDeliveryParcelCreationService(clock),
                mock(PreferableDeliveryOptionFetcher.class),
                actualDeliveryFetcher,
                deliveryRouteFetcher,
                clock,
                featureReader, new DigitalDeliveryTypeValidation(), mock(ShopService.class),
                mock(PersonalDataService.class));

        MultiCartContext multiCartContext = MultiCartContext.createBy(ImmutableMultiCartParameters.builder()
                .build(), Map.of());
        immutableMultiCartContext = ImmutableMultiCartContext.from(multiCartContext,
                MultiCartProvider.buildMultiCart(List.of()));
    }

    @Test
    public void actualizeMarketBrandedForDbsPickip() {
        String outletCode = "534";
        ShopOutletId shopOutletId = new ShopOutletId(774L, outletCode);
        BigDecimal price = BigDecimal.valueOf(123);
        BigDecimal buyerPrice = BigDecimal.TEN;

        Order order = OrderProvider.getOrderWithPickupDelivery();

        DeliveryResponse deliveryOption = new DeliveryResponse();
        deliveryOption.setBuyerPrice(buyerPrice);
        deliveryOption.setServiceName("Самовывоз");
        deliveryOption.setPrice(price);
        deliveryOption.setType(DeliveryType.PICKUP);
        deliveryOption.setMarketBranded(true);
        deliveryOption.setMarketPartner(true);
        deliveryOption.setMarketPostTerm(true);
        deliveryOption.setDeliveryDates(order.getDelivery().getDeliveryDates());
        deliveryOption.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        CartResponse cartResponse = new CartResponse();
        cartResponse.setItems(
                order.getItems().stream().map(OfferItemUtils::deepCopy).collect(Collectors.toList())
        );
        cartResponse.setDeliveryOptions(Lists.newArrayList(deliveryOption));
        cartResponse.setPaymentMethods(Lists.newArrayList());
        //пришел от магазина список числовых id
        cartResponse.getDeliveryOptions().get(0).setOutletIds(Set.of(534L));

        Assertions.assertFalse(order.getDelivery().isMarketBranded());
        Assertions.assertFalse(order.getDelivery().isMarketPartner());
        Assertions.assertFalse(order.getDelivery().isMarketPostTerm());

        var contextBuilder = ActualizationContext.builder()
                .withImmutableMulticartContext(immutableMultiCartContext)
                .withCart(order)
                .withInitialCart(ImmutableOrder.from(order))
                .withOriginalBuyerCurrency(order.getBuyerCurrency());
        var fetchContext = CartFetchingContext.of(Mockito.mock(MultiCartFetchingContext.class),
                contextBuilder, order);

        fetchContext.setSession(ContextualFlowRuntimeSession.useSession(fetchContext,
                CartFetchingContext::makeImmutableContext));

        mutate(
                order,
                cartResponse,
                fetchContext.getActualizationContextBuilder()
        );
        Assertions.assertTrue(order.getDelivery().isMarketBranded());
        Assertions.assertTrue(order.getDelivery().isMarketPartner());
        Assertions.assertTrue(order.getDelivery().isMarketPostTerm());
    }

    @Test
    public void shouldActualizeIsTryingAvailable() {
        BigDecimal price = BigDecimal.valueOf(123);
        BigDecimal buyerPrice = BigDecimal.TEN;

        Order order = OrderProvider.getOrderWithYandexMarketDelivery();
        order.setDelivery(DeliveryProvider.getYandexMarketDelivery(false));

        DeliveryResponse deliveryOption = new DeliveryResponse();
        deliveryOption.setBuyerPrice(buyerPrice);
        deliveryOption.setServiceName("Доставка");
        deliveryOption.setPrice(price);
        deliveryOption.setType(DeliveryType.DELIVERY);
        deliveryOption.setDeliveryDates(order.getDelivery().getDeliveryDates());
        deliveryOption.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        deliveryOption.setTryingAvailable(true);
        CartResponse cartResponse = new CartResponse();
        cartResponse.setItems(
                order.getItems().stream().map(OfferItemUtils::deepCopy).collect(Collectors.toList())
        );
        cartResponse.setDeliveryOptions(Lists.newArrayList(deliveryOption));
        cartResponse.setPaymentMethods(Lists.newArrayList());

        Assertions.assertNotEquals(Boolean.TRUE, order.getDelivery().getTryingAvailable());

        var contextBuilder = ActualizationContext.builder()
                .withImmutableMulticartContext(immutableMultiCartContext)
                .withCart(order)
                .withInitialCart(ImmutableOrder.from(order))
                .withOriginalBuyerCurrency(order.getBuyerCurrency());
        var fetchContext = CartFetchingContext.of(Mockito.mock(MultiCartFetchingContext.class),
                contextBuilder, order);

        fetchContext.setSession(ContextualFlowRuntimeSession.useSession(fetchContext,
                CartFetchingContext::makeImmutableContext));

        mutate(
                order,
                cartResponse,
                fetchContext.getActualizationContextBuilder()
        );

        Assertions.assertTrue(Boolean.TRUE.equals(order.getDelivery().getTryingAvailable()));
    }

    @Test
    public void shouldActualizeExtraCharge() {

        ExtraCharge extraCharge = new ExtraCharge(new BigDecimal("123"),
                new BigDecimal("222"), List.of("REASON"));

        BigDecimal price = BigDecimal.valueOf(123);
        BigDecimal buyerPrice = BigDecimal.TEN;

        Order order = OrderProvider.getOrderWithYandexMarketDelivery();
        order.setDelivery(DeliveryProvider.getYandexMarketDelivery(false));

        DeliveryResponse deliveryOption = new DeliveryResponse();
        deliveryOption.setBuyerPrice(buyerPrice);
        deliveryOption.setServiceName("Доставка");
        deliveryOption.setPrice(price);
        deliveryOption.setType(DeliveryType.DELIVERY);
        deliveryOption.setDeliveryDates(order.getDelivery().getDeliveryDates());
        deliveryOption.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        deliveryOption.setExtraCharge(extraCharge);
        CartResponse cartResponse = new CartResponse();
        cartResponse.setItems(
                order.getItems().stream().map(OfferItemUtils::deepCopy).collect(Collectors.toList())
        );
        cartResponse.setDeliveryOptions(Lists.newArrayList(deliveryOption));
        cartResponse.setPaymentMethods(Lists.newArrayList());

        var contextBuilder = ActualizationContext.builder()
                .withImmutableMulticartContext(immutableMultiCartContext)
                .withCart(order)
                .withInitialCart(ImmutableOrder.from(order))
                .withOriginalBuyerCurrency(order.getBuyerCurrency());
        var fetchContext = CartFetchingContext.of(Mockito.mock(MultiCartFetchingContext.class),
                contextBuilder, order);

        fetchContext.setSession(ContextualFlowRuntimeSession.useSession(fetchContext,
                CartFetchingContext::makeImmutableContext));

        mutate(
                order,
                cartResponse,
                fetchContext.getActualizationContextBuilder()
        );

        Assertions.assertEquals(extraCharge, order.getDelivery().getExtraCharge());
    }

    private void mutate(Order order,
                        CartResponse cartResponse,
                        ActualizationContext.ActualizationContextBuilder builder) {
        var multiCart = MultiCartProvider.single(order);
        var multiCartFetchingContext = MultiCartFetchingContext.of(MultiCartContext.createBy(
                ImmutableMultiCartParameters.builder().build(), multiCart), multiCart);
        var fetchingContext = CartFetchingContext.of(
                multiCartFetchingContext, builder, order);
        FlowSessionHelper.patchSession(fetchingContext, CartFetchingContext::makeImmutableContext,
                (c, v) -> c.getActualizationContextBuilder().withPushApiCartStage(v),
                mapToPushApiCartResponse(cartResponse));
        deliveryOptionsMutation.onSuccess(CartFetchingContext.of(multiCartFetchingContext, builder, order));
    }
}
