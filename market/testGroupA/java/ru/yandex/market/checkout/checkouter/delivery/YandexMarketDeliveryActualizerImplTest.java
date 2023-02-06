package ru.yandex.market.checkout.checkouter.delivery;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.assertj.core.api.Assertions;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.ActualDeliveryFetcher;
import ru.yandex.market.checkout.checkouter.actualization.flow.context.CartFetchingContext;
import ru.yandex.market.checkout.checkouter.actualization.flow.context.MultiCartFetchingContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ActualDeliveryStageResult;
import ru.yandex.market.checkout.checkouter.actualization.model.ActualizationContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartParameters;
import ru.yandex.market.checkout.checkouter.actualization.model.MultiCartContext;
import ru.yandex.market.checkout.checkouter.balance.service.BalanceTokenProvider;
import ru.yandex.market.checkout.checkouter.color.ColorConfig;
import ru.yandex.market.checkout.checkouter.color.SingleColorConfig;
import ru.yandex.market.checkout.checkouter.color.WhiteConfig;
import ru.yandex.market.checkout.checkouter.delivery.exceptions.InvalidDeliveryOption;
import ru.yandex.market.checkout.checkouter.delivery.exceptions.NoActualDeliveryOptions;
import ru.yandex.market.checkout.checkouter.delivery.outlet.ShopOutlet;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureReader;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureWriter;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.ActualDeliveryUtils;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.immutable.ImmutableOrder;
import ru.yandex.market.checkout.checkouter.service.personal.PersonalDataService;
import ru.yandex.market.checkout.checkouter.service.yalavka.YaLavkaDeliveryConfigService;
import ru.yandex.market.checkout.checkouter.service.yalavka.YaLavkaOrderProperties;
import ru.yandex.market.checkout.checkouter.util.CheckouterProperties;
import ru.yandex.market.checkout.checkouter.util.CheckouterPropertiesImpl;
import ru.yandex.market.checkout.common.util.DeliveryOptionPartnerType;
import ru.yandex.market.checkout.helpers.FlowSessionHelper;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.MultiCartProvider;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.common.report.model.ActualDelivery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryFeature.DBS_WITH_ROUTE;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryFeature.EXPRESS_DELIVERY_FASTEST;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryFeature.EXPRESS_DELIVERY_WIDE;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.ENABLE_UNIFIED_TARIFFS;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;

@ExtendWith(MockitoExtension.class)
public class YandexMarketDeliveryActualizerImplTest {

    private static final List<DeliveryOptionPartnerType> ALL_DELIVERY_PARTNER_TYPES = List.of(
            DeliveryOptionPartnerType.MARKET_DELIVERY,
            DeliveryOptionPartnerType.MARKET_DELIVERY_WHITE,
            DeliveryOptionPartnerType.REGULAR
    );

    private YandexMarketDeliveryActualizerImpl yandexMarketDeliveryActualizer;
    @Mock
    private CheckouterFeatureWriter checkouterFeatureWriter;
    @Mock
    private CheckouterFeatureReader checkouterFeatureReader;
    @Mock
    private ColorConfig colorConfig;
    @Mock
    private ActualDeliveryFetcher actualDeliveryFetcher;
    @Mock
    private PersonalDataService personalDataService;

    @BeforeEach
    public void init() {
        CheckouterProperties checkouterProperties = new CheckouterPropertiesImpl(checkouterFeatureReader,
                checkouterFeatureWriter);
        lenient().when(colorConfig.getFor(any(Order.class))).thenReturn(mock(SingleColorConfig.class));
        yandexMarketDeliveryActualizer = new YandexMarketDeliveryActualizerImpl(
                null,
                Clock.systemDefaultZone(),
                actualDeliveryFetcher,
                new YaLavkaDeliveryConfigService(null),
                new YaLavkaOrderProperties("10:00-14:00", "12:00-16:30", 4),
                checkouterProperties,
                colorConfig,
                checkouterFeatureReader,
                personalDataService);
    }

    @Test
    public void testCreatePickupWithEmptyShipmentDate() {
        Order order = OrderProvider.getBlueOrder();
        order.setDeliveryMethods(new HashMap<>());
        order.setRgb(Color.BLUE);

        ActualizationContext actualizationContext = getActualizationContext(
                order,
                ActualDeliveryProvider.builder()
                        .addPickup(MOCK_DELIVERY_SERVICE_ID, null)
                        .withFreeDelivery()
                        .build());

        assertTrue(yandexMarketDeliveryActualizer.createYandexMarketOptions(
                order,
                ActualDeliveryUtils.getActualDeliveryResult(order, actualizationContext.getActualDelivery())
                        .orElse(null),
                makeShopOutlets(actualizationContext.getActualDelivery()), actualizationContext).isEmpty());
    }


    @Test
    public void testCreateDeliveryOptionWithEmptyShipmentDate() {
        Order order = OrderProvider.getBlueOrder();
        order.setDeliveryMethods(new HashMap<>());
        order.setRgb(Color.BLUE);

        ActualizationContext actualizationContext = getActualizationContext(
                order,
                ActualDeliveryProvider.builder()
                        .addDelivery(MOCK_DELIVERY_SERVICE_ID, null)
                        .build());

        try {
            yandexMarketDeliveryActualizer.createYandexMarketOptions(order,
                    ActualDeliveryUtils.getActualDeliveryResult(order, actualizationContext.getActualDelivery())
                            .orElse(null), makeShopOutlets(actualizationContext.getActualDelivery()),
                    actualizationContext);
            fail("Must be InvalidDeliveryOption exception due to empty shipmentDate");
        } catch (InvalidDeliveryOption e) {
            assertTrue(e.getMessage().startsWith("Empty shipment days for"));
        }
    }


    @Test
    public void testCreatePostOptionWithEmptyShipmentDate() {
        Order order = OrderProvider.getBlueOrder();
        order.setDeliveryMethods(new HashMap<>());
        order.setRgb(Color.BLUE);

        ActualizationContext actualizationContext = getActualizationContext(
                order,
                ActualDeliveryProvider.builder()
                        .addPost((Integer) null)
                        .build());

        try {
            yandexMarketDeliveryActualizer.createYandexMarketOptions(order,
                    ActualDeliveryUtils.getActualDeliveryResult(order, actualizationContext.getActualDelivery())
                            .orElse(null), makeShopOutlets(actualizationContext.getActualDelivery()),
                    actualizationContext);
            fail("Must be InvalidDeliveryOption exception due to empty shipmentDate");
        } catch (InvalidDeliveryOption e) {
            assertTrue(e.getMessage().startsWith("Empty shipment days for"));
        }
    }

    @Test
    public void shouldFilterOutAllOptionsWithNoWeightForFulfilmentOrder() {

        Order order = OrderProvider.getBlueOrder();
        order.setDeliveryMethods(new HashMap<>());
        order.setFulfilment(true);


        ActualizationContext actualizationContext = getActualizationContext(
                order,
                actualDeliveryWith(ALL_DELIVERY_PARTNER_TYPES, null));

        Assertions.assertThatThrownBy(() -> yandexMarketDeliveryActualizer.createYandexMarketOptions(
                order, ActualDeliveryUtils.getActualDeliveryResult(order, actualizationContext.getActualDelivery())
                        .orElse(null), makeShopOutlets(actualizationContext.getActualDelivery()),
                actualizationContext
        )).isInstanceOf(NoActualDeliveryOptions.class);
    }

    @Test
    public void shouldFilterOutAllNonFulfilmentOptionsWithYandexMarketDeliveryTypeAndNoWeight() {

        Order order = OrderProvider.getBlueOrder();
        order.setFulfilment(false);


        ActualizationContext actualizationContext = getActualizationContext(
                order,
                actualDeliveryWith(ALL_DELIVERY_PARTNER_TYPES, null));


        List<DeliveryResponse> marketOptions = yandexMarketDeliveryActualizer.createYandexMarketOptions(
                order, ActualDeliveryUtils.getActualDeliveryResult(order, actualizationContext.getActualDelivery())
                        .orElse(null), makeShopOutlets(actualizationContext.getActualDelivery()),
                actualizationContext
        );
        assertThat(marketOptions)
                .extracting(DeliveryResponse::getDeliveryPartnerType)
                .containsExactlyInAnyOrder(
                        DeliveryPartnerType.SHOP, DeliveryPartnerType.SHOP,
                        DeliveryPartnerType.SHOP, DeliveryPartnerType.SHOP
                );
    }

    @Test
    public void shouldNotFilterOutFulfilmentDeliveryOptionsWithWeightSpecified() {
        Order order = OrderProvider.getBlueOrder();
        order.setFulfilment(true);


        ActualizationContext actualizationContext = getActualizationContext(
                order,
                actualDeliveryWith(ALL_DELIVERY_PARTNER_TYPES, BigDecimal.TEN));

        List<DeliveryResponse> marketOptions = yandexMarketDeliveryActualizer.createYandexMarketOptions(
                order, ActualDeliveryUtils.getActualDeliveryResult(order, actualizationContext.getActualDelivery())
                        .orElse(null), makeShopOutlets(actualizationContext.getActualDelivery()),
                actualizationContext
        );
        assertThat(marketOptions)
                .extracting(DeliveryResponse::getDeliveryPartnerType)
                .containsExactlyInAnyOrder(
                        DeliveryPartnerType.YANDEX_MARKET, DeliveryPartnerType.SHOP, DeliveryPartnerType.SHOP,
                        DeliveryPartnerType.YANDEX_MARKET, DeliveryPartnerType.SHOP, DeliveryPartnerType.SHOP
                );
    }

    @Test
    public void shouldFilterOutAllPostOptionsWithNoPostcodes() {

        Order order = OrderProvider.getBlueOrder();
        order.setDeliveryMethods(new HashMap<>());
        order.setRgb(Color.BLUE);

        ActualizationContext actualizationContext = getActualizationContext(
                order,
                ActualDeliveryProvider.builder()
                        .addPost(1)
                        .build());
        actualizationContext.getActualDelivery()
                .getResults()
                .get(0)
                .getPost()
                .get(0)
                .setPostCodes(Collections.emptyList());

        List<DeliveryResponse> marketOptions = yandexMarketDeliveryActualizer.createYandexMarketOptions(
                order, ActualDeliveryUtils.getActualDeliveryResult(order, actualizationContext.getActualDelivery())
                        .orElse(null), makeShopOutlets(actualizationContext.getActualDelivery()),
                actualizationContext);
        assertThat(marketOptions)
                .isEmpty();
    }

    @Test
    public void shouldNotFilterNonFulfilmentDeliveryOptionsWithWeightSpecified() {

        Order order = OrderProvider.getBlueOrder();
        order.setFulfilment(false);


        ActualizationContext actualizationContext = getActualizationContext(
                order,
                actualDeliveryWith(ALL_DELIVERY_PARTNER_TYPES, BigDecimal.TEN));


        List<DeliveryResponse> marketOptions = yandexMarketDeliveryActualizer.createYandexMarketOptions(
                order, ActualDeliveryUtils.getActualDeliveryResult(order, actualizationContext.getActualDelivery())
                        .orElse(null), makeShopOutlets(actualizationContext.getActualDelivery()),
                actualizationContext
        );
        assertThat(marketOptions)
                .extracting(DeliveryResponse::getDeliveryPartnerType)
                .containsExactlyInAnyOrder(
                        DeliveryPartnerType.YANDEX_MARKET, DeliveryPartnerType.SHOP, DeliveryPartnerType.SHOP,
                        DeliveryPartnerType.YANDEX_MARKET, DeliveryPartnerType.SHOP, DeliveryPartnerType.SHOP
                );
    }

    @Test
    public void shouldAddDbsWithRouteFeature() {
        Mockito.doReturn(true).when(checkouterFeatureReader)
                .getBoolean(Mockito.eq((BooleanFeatureType.ENABLE_DBS_WITH_ROUTE_DELIVERY_FEATURE)));
        Mockito.doReturn(true).when(checkouterFeatureReader)
                .getBoolean(Mockito.eq(ENABLE_UNIFIED_TARIFFS));
        Mockito.doReturn(false).when(checkouterFeatureReader)
                .getBoolean(Mockito.eq(BooleanFeatureType.ENABLE_ASYNC_DELIVERY_OPTIONS_PROCESSING));

        Order order = OrderProvider.getWhiteOrder(o -> {
            o.setDelivery(DeliveryProvider
                    .shopSelfPickupDeliveryByMarketOutletId()
                    .build());
        });
        ActualizationContext actualizationContext = getActualizationContext(
                order,
                ActualDeliveryProvider.builder()
                        .addPickup(DeliveryProvider.createFrom(order.getDelivery())
                                .buildPickupOption())
                        .withIsExternalLogistics(false)
                        .build());

        when(colorConfig.getFor(any(Order.class))).thenReturn(new WhiteConfig(
                false,
                true,
                "url",
                "fallbackUrl",
                Map.of(),
                mock(BalanceTokenProvider.class),
                true));

        List<DeliveryResponse> marketOptions = yandexMarketDeliveryActualizer.createYandexMarketOptions(
                order, ActualDeliveryUtils.getActualDeliveryResult(order, actualizationContext.getActualDelivery())
                        .orElse(null), makeShopOutlets(actualizationContext.getActualDelivery()),
                actualizationContext
        );

        assertNotNull(marketOptions);
        assertEquals(1, marketOptions.size());
        assertNotNull(marketOptions.get(0).getFeatures());
        MatcherAssert.assertThat(marketOptions.get(0).getFeatures(), hasSize(1));
        MatcherAssert.assertThat(marketOptions.get(0).getFeatures(), contains(DBS_WITH_ROUTE));
    }

    @Test
    public void shouldAddExpressDeliveryFeatures() {
        Order order = OrderProvider.getWhiteOrder(o -> {
            o.setDelivery(DeliveryProvider
                    .yandexDelivery()
                    .build());
        });
        ActualizationContext actualizationContext = getActualizationContext(order, ActualDeliveryProvider.builder()
                .addDelivery(MOCK_DELIVERY_SERVICE_ID)
                .withIsExternalLogistics(false)
                .build());

        List<DeliveryResponse> marketOptions = yandexMarketDeliveryActualizer.createYandexMarketOptions(
                order, ActualDeliveryUtils.getActualDeliveryResult(order, actualizationContext.getActualDelivery())
                        .orElse(null), makeShopOutlets(actualizationContext.getActualDelivery()),
                actualizationContext
        );

        assertNotNull(marketOptions);
        assertEquals(1, marketOptions.size());
        assertNotNull(marketOptions.get(0).getFeatures());
        MatcherAssert.assertThat(marketOptions.get(0).getFeatures(), hasSize(2));
        MatcherAssert.assertThat(
                marketOptions.get(0).getFeatures(),
                hasItems(EXPRESS_DELIVERY_WIDE, EXPRESS_DELIVERY_FASTEST)
        );
    }


    private ActualizationContext getActualizationContext(Order order, ActualDelivery actualDelivery) {
        var multiCart = MultiCartProvider.single(order);
        MultiCartContext multiCartContext = MultiCartContext.createBy(
                ImmutableMultiCartParameters.builder().withMinifyOutlets(true).build(), multiCart);
        ImmutableMultiCartContext immutableMultiCartContext = ImmutableMultiCartContext.from(multiCartContext,
                MultiCartProvider.buildMultiCart(List.of()));
        var multiCartFetchingContext = MultiCartFetchingContext.of(multiCartContext, multiCart);
        var actualizationContextBuilder = ActualizationContext.builder()
                .withImmutableMulticartContext(immutableMultiCartContext)
                .withCart(order)
                .withInitialCart(ImmutableOrder.from(order))
                .withOriginalBuyerCurrency(Currency.RUR);
        var fetchingContext = CartFetchingContext.of(
                multiCartFetchingContext, actualizationContextBuilder, order);

        FlowSessionHelper.patchSession(fetchingContext, CartFetchingContext::makeImmutableContext,
                (c, v) -> c.getActualizationContextBuilder().withActualDeliveryStage(v),
                new ActualDeliveryStageResult(actualDelivery));

        return actualizationContextBuilder.build();
    }

    private ActualDelivery actualDeliveryWith(List<DeliveryOptionPartnerType> partnerTypes, BigDecimal weight) {
        ActualDeliveryProvider.ActualDeliveryBuilder builder = ActualDeliveryProvider.builder();
        partnerTypes.forEach(partnerType -> {
            builder.addDeliveryWithPartnerType(BlueParametersProvider.DELIVERY_SERVICE_ID, partnerType);
            builder.addPickupWithPartnerType(BlueParametersProvider.DELIVERY_SERVICE_ID, partnerType);
        });
        ActualDelivery actualDelivery = builder.build();
        actualDelivery.getResults().forEach(actualDeliveryResult -> actualDeliveryResult.setWeight(weight));

        return actualDelivery;
    }

    @Nonnull
    private Map<Long, ShopOutlet> makeShopOutlets(@Nullable ActualDelivery actualDelivery) {
        return Optional.ofNullable(actualDelivery)
                .map(ActualDelivery::getResults).stream()
                .flatMap(Collection::stream)
                .flatMap(actualDeliveryResult -> actualDeliveryResult.getPickup().stream())
                .flatMap(pickupOption -> pickupOption.getOutletIds().stream())
                .collect(Collectors.toUnmodifiableMap(Function.identity(), id -> {
                    var outlet = new ShopOutlet();
                    outlet.setId(id);
                    return outlet;
                }, (e1, e2) -> e1));
    }

}
