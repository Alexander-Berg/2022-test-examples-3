package ru.yandex.market.checkout.checkouter.order;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.Iterables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.common.util.collections.CollectionFactory;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.common.util.region.Region;
import ru.yandex.common.util.region.RegionService;
import ru.yandex.common.util.region.RegionTree;
import ru.yandex.common.util.region.RegionType;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.ActualDeliveryFetcher;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.DeliveryRouteFetcher;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.mutations.DeliveryOutletsMutation;
import ru.yandex.market.checkout.checkouter.actualization.flow.context.CartFetchingContext;
import ru.yandex.market.checkout.checkouter.actualization.flow.context.MultiCartFetchingContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ActualizationContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartParameters;
import ru.yandex.market.checkout.checkouter.actualization.model.MultiCartContext;
import ru.yandex.market.checkout.checkouter.actualization.model.PushApiCartResponse;
import ru.yandex.market.checkout.checkouter.color.ColorConfig;
import ru.yandex.market.checkout.checkouter.color.SingleColorConfig;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.ShopDeliveryServicesService;
import ru.yandex.market.checkout.checkouter.delivery.YandexMarketDeliveryActualizer;
import ru.yandex.market.checkout.checkouter.delivery.outlet.DeliveryOutletService;
import ru.yandex.market.checkout.checkouter.delivery.outlet.MarketOutletId;
import ru.yandex.market.checkout.checkouter.delivery.outlet.ShopOutlet;
import ru.yandex.market.checkout.checkouter.delivery.outlet.ShopOutletId;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureResolverStub;
import ru.yandex.market.checkout.checkouter.order.immutable.ImmutableOrder;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.trace.CheckoutContextHolder;
import ru.yandex.market.checkout.common.util.StreamUtils;
import ru.yandex.market.checkout.helpers.FlowSessionHelper;
import ru.yandex.market.checkout.providers.DeliveryResponseProvider;
import ru.yandex.market.checkout.providers.MultiCartProvider;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.pushapi.client.entity.CartResponse;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.pushapi.client.entity.OutletResponse;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.checkout.checkouter.actualization.utils.PushApiUtils.mapToPushApiCartResponse;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.ENABLE_PARSE_OUTLETS_FROM_PUSH_API;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.ENABLE_UNIFIED_TARIFFS;
import static ru.yandex.market.checkout.checkouter.report.Experiments.MARKET_UNIFIED_TARIFFS;
import static ru.yandex.market.checkout.checkouter.report.Experiments.MARKET_UNIFIED_TARIFFS_VALUE;
import static ru.yandex.market.checkout.checkouter.trace.CheckoutContextHolder.getExperiments;

/**
 * @author zagidullinri
 * @date 01.06.2021
 */
@ExtendWith(MockitoExtension.class)
public class DeliveryOutletsMutationTest {

    private static final Long MARKET_OUTLET_ID = 20695L;
    private static final BigDecimal PUSH_API_DELIVERY_PRICE = BigDecimal.valueOf(1);
    private static final BigDecimal PUSH_API_DELIVERY_BUYER_PRICE = BigDecimal.valueOf(2);
    private static final BigDecimal MAR_DO_DELIVERY_PRICE = BigDecimal.valueOf(3);
    private static final BigDecimal MAR_DO_DELIVERY_BUYER_PRICE = BigDecimal.valueOf(4);
    CheckouterFeatureResolverStub checkouterFeatureReader = new CheckouterFeatureResolverStub();
    @Mock
    SingleColorConfig singleColorConfig;
    private DeliveryOutletsMutation deliveryOutletsMutation;
    private ActualizationContext.ActualizationContextBuilder actualizationContextBuilder;
    private PushApiCartResponse pushApiCartResponse;
    private Order order;
    private DeliveryResponse pushApiDeliveryResponse;
    private DeliveryResponse marDoDeliveryResponse;
    @Mock
    private MarketReportInfoService marketReportInfoFetcher;
    @Mock
    private RegionService regionService;
    @Mock
    private YandexMarketDeliveryActualizer yandexMarketDeliveryActualizer;
    @Mock
    private DeliveryOutletService deliveryOutletService;
    @Mock
    private ShopDeliveryServicesService shopDeliveryServicesService;
    @Mock
    private ColorConfig colorConfig;
    @Mock
    private ActualDeliveryFetcher actualDeliveryFetcher;
    @Mock
    private DeliveryRouteFetcher deliveryRouteFetcher;

    @BeforeEach
    public void setUp() {
        mockRegionService();
        mockYandexMarketDeliveryActualizer();
        mockDeliveryOutletService();
        mockColorConfig();
        setUpOrder();
        setUpActualizationContext();
        setUpCartResponse();
        setUpDeliveryOutletsActualizer();
    }

    private void mockMarketReportInfoFetcher() {
        ShopOutlet shopOutlet = new ShopOutlet();
        shopOutlet.setId(20695L);
        shopOutlet.setRegionId(213L);
        lenient().doReturn(CollectionFactory.list(shopOutlet))
                .when(marketReportInfoFetcher)
                .fetchShopOutlets(
                        any(),
                        any(),
                        anyCollection(),
                        anyCollection(),
                        anyBoolean()
                );
    }

    private void mockRegionService() {
        final Region region3 = new Region(
                213, "region3", RegionType.CITY,
                new Region(
                        2, "region2", RegionType.CITY,
                        new Region(
                                1, "region1", RegionType.CITY, null
                        )
                )
        );
        final RegionTree regionTree = mock(RegionTree.class);
        lenient().doReturn(regionTree)
                .when(regionService).getRegionTree();
        lenient().when(regionTree.getRegion(213)).thenReturn(region3);
        lenient().when(regionTree.getPathToRoot(213)).thenReturn(CollectionFactory.list(213));
    }

    private void mockYandexMarketDeliveryActualizer() {
        marDoDeliveryResponse = DeliveryResponseProvider.buildDeliveryResponseWithIntervals();
        marDoDeliveryResponse.setPrice(MAR_DO_DELIVERY_PRICE);
        marDoDeliveryResponse.setBuyerPrice(MAR_DO_DELIVERY_BUYER_PRICE);
        marDoDeliveryResponse.setEstimated(true);
        lenient().doReturn(List.of(marDoDeliveryResponse))
                .when(yandexMarketDeliveryActualizer)
                .createYandexMarketOptions(any(), any(), any(), any());
    }

    private void mockDeliveryOutletService() {
        ShopOutletId shopOutletId = new ShopOutletId(21L, "20696");
        MarketOutletId marketOutletId = new MarketOutletId(774L, MARKET_OUTLET_ID);
        lenient().doReturn(marketOutletId)
                .when(deliveryOutletService).getMarketByShop(shopOutletId);
    }

    private void mockShopDeliveryServicesService() {
        lenient().doReturn(new HashSet<>())
                .when(shopDeliveryServicesService).getShopDeliveryServices(anyLong(), any(), any());
    }

    private void mockColorConfig() {
        lenient().when(colorConfig.getFor(any(Order.class))).thenReturn(singleColorConfig);
        lenient().when(singleColorConfig.useOnlyActualDeliveryOptions()).thenReturn(true);
    }

    private void setUpActualizationContext() {
        MultiCartContext multiCartContext = MultiCartContext.createBy(ImmutableMultiCartParameters.builder()
                .withBuyerRegionId(10L)
                .withIsMultiCart(false)
                .withApiSettings(ApiSettings.PRODUCTION)
                .withActionId("actionId")
                .build(), Map.of());
        ImmutableMultiCartContext immutableMultiCartContext = ImmutableMultiCartContext.from(multiCartContext,
                MultiCartProvider.buildMultiCart(List.of()));
        actualizationContextBuilder = ActualizationContext.builder()
                .withImmutableMulticartContext(immutableMultiCartContext)
                .withCart(order)
                .withInitialCart(ImmutableOrder.from(order))
                .withOriginalBuyerCurrency(Currency.RUR);
    }

    private void setUpOrder() {
        order = WhiteParametersProvider.shopDeliveryOrder()
                .itemBuilder(WhiteParametersProvider.dsbsOrderItem())
                .delivery(DeliveryProvider.shopSelfDelivery()
                        .regionId((long) 213)
                        .build())
                .build();
    }

    private void setUpCartResponse() {
        var orderItems = new ArrayList<>(order.getItems());
        pushApiDeliveryResponse = DeliveryResponseProvider.buildDeliveryResponseWithIntervals();
        pushApiDeliveryResponse.setPrice(PUSH_API_DELIVERY_PRICE);
        pushApiDeliveryResponse.setBuyerPrice(PUSH_API_DELIVERY_BUYER_PRICE);

        var deliveryResponses = new ArrayList<>(List.of(pushApiDeliveryResponse));
        List<PaymentMethod> paymentMethods = List.of(PaymentMethod.CASH_ON_DELIVERY);
        CartResponse cartResponse = new CartResponse(orderItems, deliveryResponses, paymentMethods);
        pushApiCartResponse = mapToPushApiCartResponse(cartResponse);
        pushApiCartResponse.getDeliveryOptions()
                .forEach(d -> d.setPaymentOptions(StreamUtils.stream(pushApiCartResponse.getPaymentMethods())
                        .collect(Collectors.toUnmodifiableSet())));
        pushApiCartResponse.getDeliveryOptions().forEach(d -> d.setOutletResponses(
                List.of(new OutletResponse(MARKET_OUTLET_ID, "20696", null, null)
                )));
    }

    private void setUpDeliveryOutletsActualizer() {
        deliveryOutletsMutation = new DeliveryOutletsMutation(
                marketReportInfoFetcher,
                regionService,
                yandexMarketDeliveryActualizer,
                deliveryOutletService,
                shopDeliveryServicesService,
                checkouterFeatureReader,
                actualDeliveryFetcher, deliveryRouteFetcher, colorConfig);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void shouldUsePushApiDeliveryOptionsWhenOrderIsEda(Boolean useNewLogic) {
        checkouterFeatureReader.writeValue(ENABLE_PARSE_OUTLETS_FROM_PUSH_API, useNewLogic);
        mockMarketReportInfoFetcher();
        mockShopDeliveryServicesService();
        order.setProperty(OrderPropertyType.IS_EDA, true);

        mutate(order, pushApiCartResponse);
        DeliveryResponse deliveryResponse = Iterables.getOnlyElement(pushApiCartResponse.getDeliveryOptions());

        assertThat(deliveryResponse, equalTo(pushApiDeliveryResponse));
        assertThat(deliveryResponse, not(equalTo(marDoDeliveryResponse)));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void shouldUseReportDeliveryOptionsWhenPushApiDeliveryOptionsIsEmpty(Boolean useNewLogic) {
        checkouterFeatureReader.writeValue(ENABLE_PARSE_OUTLETS_FROM_PUSH_API, useNewLogic);
        pushApiCartResponse.setDeliveryOptions(Collections.emptyList());

        mutate(order, pushApiCartResponse);
        DeliveryResponse deliveryResponse = Iterables.getOnlyElement(pushApiCartResponse.getDeliveryOptions());

        assertThat(deliveryResponse, equalTo(marDoDeliveryResponse));
        assertThat(deliveryResponse, not(equalTo(pushApiDeliveryResponse)));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void shouldUseReportDeliveryOptionsWhenPropertyEnabledAndAcceptMethodIsNotPushApi(Boolean useNewLogic) {
        checkouterFeatureReader.writeValue(ENABLE_PARSE_OUTLETS_FROM_PUSH_API, useNewLogic);
        enableUnifiedTariffs();
        mockMarketReportInfoFetcher();
        mockShopDeliveryServicesService();
        order.setAcceptMethod(OrderAcceptMethod.WEB_INTERFACE);

        mutate(order, pushApiCartResponse);
        DeliveryResponse deliveryResponse = Iterables.getOnlyElement(pushApiCartResponse.getDeliveryOptions());

        assertThat(deliveryResponse, equalTo(marDoDeliveryResponse));
        assertThat(deliveryResponse, not(equalTo(pushApiDeliveryResponse)));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void shouldUsePushApiDeliveryOptionsButReportPricesWhenPropertyEnabledAndAcceptMethodIsPushApi(
            Boolean useNewLogic
    ) {
        checkouterFeatureReader.writeValue(ENABLE_PARSE_OUTLETS_FROM_PUSH_API, useNewLogic);
        enableUnifiedTariffs();
        mockMarketReportInfoFetcher();
        mockShopDeliveryServicesService();
        pushApiDeliveryResponse.setDeliveryServiceId(marDoDeliveryResponse.getDeliveryServiceId());

        mutate(order, pushApiCartResponse);
        DeliveryResponse deliveryResponse = Iterables.getOnlyElement(pushApiCartResponse.getDeliveryOptions());

        assertThat(deliveryResponse, equalTo(pushApiDeliveryResponse));
        assertThat(deliveryResponse.getPrice(), not(equalTo(PUSH_API_DELIVERY_PRICE)));
        assertThat(deliveryResponse.getBuyerPrice(), not(equalTo(PUSH_API_DELIVERY_BUYER_PRICE)));

        assertThat(deliveryResponse, not(equalTo(marDoDeliveryResponse)));
        assertThat(deliveryResponse.getPrice(), equalTo(MAR_DO_DELIVERY_PRICE));
        assertThat(deliveryResponse.getBuyerPrice(), equalTo(MAR_DO_DELIVERY_BUYER_PRICE));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void estimatedDeliverySwappedToShopDeliveryTest(Boolean useNewLogic) {
        checkouterFeatureReader.writeValue(ENABLE_PARSE_OUTLETS_FROM_PUSH_API, useNewLogic);
        enableUnifiedTariffs();
        mockMarketReportInfoFetcher();
        mockShopDeliveryServicesService();
        pushApiDeliveryResponse.setDeliveryServiceId(marDoDeliveryResponse.getDeliveryServiceId());

        mutate(order, pushApiCartResponse);
        DeliveryResponse deliveryResponse = Iterables.getOnlyElement(pushApiCartResponse.getDeliveryOptions());

        assertThat(deliveryResponse, equalTo(pushApiDeliveryResponse));
        assertThat(deliveryResponse, not(equalTo(marDoDeliveryResponse)));

        assertTrue(deliveryResponse.getEstimated());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void shouldUseAllDeliveryOptionsButReportPricesWhenMethodIsPushApi(
            Boolean useNewLogic
    ) {
        checkouterFeatureReader.writeValue(ENABLE_PARSE_OUTLETS_FROM_PUSH_API, useNewLogic);
        enableUnifiedTariffs();
        mockMarketReportInfoFetcher();
        mockShopDeliveryServicesService();
        marDoDeliveryResponse.setMarketBranded(true);
        marDoDeliveryResponse.setType(DeliveryType.PICKUP);
        pushApiDeliveryResponse.setDeliveryServiceId(marDoDeliveryResponse.getDeliveryServiceId());
        pushApiDeliveryResponse.setType(DeliveryType.DELIVERY);
        var deliveryId = "someDeliveryShopId";
        pushApiDeliveryResponse.setShopDeliveryId(deliveryId);

        order.getDelivery().setType(DeliveryType.PICKUP);
        order.getDelivery().setMarketBranded(true);
        mutate(order, pushApiCartResponse);
        List<DeliveryResponse> deliveryResponseList = pushApiCartResponse.getDeliveryOptions();

        assertEquals(deliveryId, deliveryResponseList.stream().filter(Delivery::isMarketBranded)
                .findFirst().orElseThrow().getShopDeliveryId());
        assertEquals(pushApiDeliveryResponse.getDeliveryDates(),
                deliveryResponseList.stream().filter(Delivery::isMarketBranded)
                        .findFirst().orElseThrow().getDeliveryDates());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void shouldNotRemoveDeliveryOptionsWhenDifferentDeliveryServices(Boolean useNewLogic) {
        checkouterFeatureReader.writeValue(ENABLE_PARSE_OUTLETS_FROM_PUSH_API, useNewLogic);
        enableUnifiedTariffs();
        mockMarketReportInfoFetcher();
        mockShopDeliveryServicesService();
        marDoDeliveryResponse.setDeliveryServiceId(pushApiDeliveryResponse.getDeliveryServiceId() + 1);

        mutate(order, pushApiCartResponse);

        assertThat(pushApiCartResponse.getDeliveryOptions(), hasSize(1));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void shouldRemoveDeliveryOptionsWhenDifferentDeliveryTypes(Boolean useNewLogic) {
        checkouterFeatureReader.writeValue(ENABLE_PARSE_OUTLETS_FROM_PUSH_API, useNewLogic);
        enableUnifiedTariffs();
        mockMarketReportInfoFetcher();
        mockShopDeliveryServicesService();
        marDoDeliveryResponse.setType(DeliveryType.PICKUP);

        mutate(order, pushApiCartResponse);

        assertThat(pushApiCartResponse.getDeliveryOptions(), empty());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void shouldUseReportDeliveryOptionsWhenPropertyDisabledAndReportDeliveryOptionsIsNotEmpty(
            Boolean useNewLogic
    ) {
        checkouterFeatureReader.writeValue(ENABLE_PARSE_OUTLETS_FROM_PUSH_API, useNewLogic);
        mockMarketReportInfoFetcher();
        mockShopDeliveryServicesService();
        mutate(order, pushApiCartResponse);
        DeliveryResponse deliveryResponse = Iterables.getOnlyElement(pushApiCartResponse.getDeliveryOptions());

        assertThat(deliveryResponse, equalTo(marDoDeliveryResponse));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void shouldUsePushApiDeliveryOptionsWhenPropertyDisabledAndReportDeliveryOptionsIsEmpty(
            Boolean useNewLogic
    ) {
        checkouterFeatureReader.writeValue(ENABLE_PARSE_OUTLETS_FROM_PUSH_API, useNewLogic);
        mockMarketReportInfoFetcher();
        mockShopDeliveryServicesService();
        doReturn(Collections.emptyList())
                .when(yandexMarketDeliveryActualizer)
                .createYandexMarketOptions(any(), any(), any(), any());
        mutate(order, pushApiCartResponse);
        DeliveryResponse deliveryResponse = Iterables.getOnlyElement(pushApiCartResponse.getDeliveryOptions());

        assertThat(deliveryResponse, equalTo(pushApiDeliveryResponse));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void shouldUseAllDeliveryOptionsWhenUseOnlyActualDeliveryOptionsIsNotTrue(Boolean useNewLogic) {
        checkouterFeatureReader.writeValue(ENABLE_PARSE_OUTLETS_FROM_PUSH_API, useNewLogic);
        mockMarketReportInfoFetcher();
        mockShopDeliveryServicesService();
        lenient().when(singleColorConfig.useOnlyActualDeliveryOptions()).thenReturn(false);

        mutate(order, pushApiCartResponse);

        assertThat(pushApiCartResponse.getDeliveryOptions(), hasSize(2));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void shouldReturnEmptyOptionsWhenNoPushApiOptionsAndPropertyEnabled(Boolean useNewLogic) {
        checkouterFeatureReader.writeValue(ENABLE_PARSE_OUTLETS_FROM_PUSH_API, useNewLogic);
        enableUnifiedTariffs();
        pushApiCartResponse.setDeliveryOptions(Collections.emptyList());

        mutate(order, pushApiCartResponse);
        assertThat(pushApiCartResponse.getDeliveryOptions(), hasSize(0));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void shouldReturnNullWhenNoPushApiOptionsAndPropertyEnabled(Boolean useNewLogic) {
        checkouterFeatureReader.writeValue(ENABLE_PARSE_OUTLETS_FROM_PUSH_API, useNewLogic);
        enableUnifiedTariffs();
        pushApiCartResponse.setDeliveryOptions(null);

        mutate(order, pushApiCartResponse);
        assertThat(pushApiCartResponse.getDeliveryOptions(), nullValue());
    }

    private void mutate(Order order, PushApiCartResponse pushApiCartResponse) {
        var multiCart = MultiCartProvider.single(order);
        var multiCartFetchingContext = MultiCartFetchingContext.of(MultiCartContext.createBy(
                ImmutableMultiCartParameters.builder().build(), multiCart), multiCart);
        var fetchingContext = CartFetchingContext.of(
                multiCartFetchingContext, actualizationContextBuilder, order);

        FlowSessionHelper.patchSession(fetchingContext, CartFetchingContext::makeImmutableContext,
                (c, v) -> c.getActualizationContextBuilder().withPushApiCartStage(v), pushApiCartResponse);
        deliveryOutletsMutation.onSuccess(fetchingContext);
    }

    private void enableUnifiedTariffs() {
        checkouterFeatureReader.writeValue(ENABLE_UNIFIED_TARIFFS, true);
        var exps = getExperiments()
                .with(Map.of(MARKET_UNIFIED_TARIFFS, MARKET_UNIFIED_TARIFFS_VALUE));
        actualizationContextBuilder.withExperiments(exps);
        CheckoutContextHolder.setExperiments(exps);
    }
}
