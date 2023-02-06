package ru.yandex.market.checkout.checkouter.actualization.actualizers;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.ShopMetaDataBuilder;
import ru.yandex.market.checkout.checkouter.actualization.flow.context.MultiCartFetchingContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ActualizationContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartParameters;
import ru.yandex.market.checkout.checkouter.actualization.model.MultiCartContext;
import ru.yandex.market.checkout.checkouter.actualization.model.PushApiCartResponse;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.LiftType;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureResolverStub;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.immutable.ImmutableOrder;
import ru.yandex.market.checkout.checkouter.service.personal.PersonalDataRetrieveResult;
import ru.yandex.market.checkout.checkouter.service.personal.PersonalDataService;
import ru.yandex.market.checkout.checkouter.service.personal.model.PersAddress;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaDataGetterService;
import ru.yandex.market.checkout.helpers.FlowSessionHelper;
import ru.yandex.market.checkout.providers.MultiCartProvider;
import ru.yandex.market.checkout.test.providers.AddressProvider;
import ru.yandex.market.common.report.model.FoundOffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.ENABLE_LIFT_OPTIONS;
import static ru.yandex.market.checkout.checkouter.feature.type.common.ComplexFeatureType.DELIVERY_FLOOR_VALIDATION_STRATEGY;
import static ru.yandex.market.checkout.checkouter.report.Experiments.MARKET_UNIFIED_TARIFFS;
import static ru.yandex.market.checkout.checkouter.report.Experiments.MARKET_UNIFIED_TARIFFS_VALUE;
import static ru.yandex.market.checkout.checkouter.trace.CheckoutContextHolder.getExperiments;

@ExtendWith(MockitoExtension.class)
public class DeliveryLiftPriceActualizerTest {

    @InjectMocks
    private DeliveryLiftPriceActualizer deliveryLiftPriceActualizer;
    private Order order;
    @Spy
    private CheckouterFeatureResolverStub checkouterProperties = new CheckouterFeatureResolverStub();
    @Spy
    private ShopMetaDataGetterService shopService = Mockito.mock(ShopMetaDataGetterService.class);
    @Mock
    private PersonalDataService personalDataService;

    @BeforeEach
    public void setUp() throws Exception {
        when(shopService.getMeta(anyLong())).thenReturn(ShopMetaDataBuilder.createTestDefault().build());
        order = new Order();
        order.setShopId(1L);

        checkouterProperties.writeValue(DELIVERY_FLOOR_VALIDATION_STRATEGY, DeliveryFloorValidationStrategy.FALLBACK);
    }

    @AfterEach
    public void clean() {
        checkouterProperties.writeValue(ENABLE_LIFT_OPTIONS, false);
    }

    public static Stream<Arguments> parameterizedTestData() {
        return Stream.of(
                new Object[]{
                        DeliveryType.PICKUP, DeliveryPartnerType.SHOP, null, "1",
                        null, null
                },
                new Object[]{
                        DeliveryType.DELIVERY, DeliveryPartnerType.YANDEX_MARKET, LiftType.ELEVATOR, "1",
                        LiftType.FREE, BigDecimal.valueOf(0)
                },
                new Object[]{
                        DeliveryType.DELIVERY, DeliveryPartnerType.SHOP, null, "1",
                        LiftType.NOT_NEEDED, null
                },
                new Object[]{
                        DeliveryType.DELIVERY, DeliveryPartnerType.SHOP, LiftType.NOT_NEEDED, "1",
                        LiftType.NOT_NEEDED, null
                },
                new Object[]{
                        DeliveryType.DELIVERY, DeliveryPartnerType.SHOP, LiftType.ELEVATOR, "1",
                        LiftType.ELEVATOR, BigDecimal.valueOf(150L)
                },
                new Object[]{
                        DeliveryType.DELIVERY, DeliveryPartnerType.SHOP, LiftType.CARGO_ELEVATOR, "1",
                        LiftType.CARGO_ELEVATOR, BigDecimal.valueOf(150L)
                },
                new Object[]{
                        DeliveryType.DELIVERY, DeliveryPartnerType.SHOP, LiftType.FREE, "1",
                        LiftType.NOT_NEEDED, null
                },
                new Object[]{
                        DeliveryType.DELIVERY, DeliveryPartnerType.SHOP, LiftType.UNKNOWN, "1",
                        LiftType.NOT_NEEDED, null
                },
                new Object[]{
                        DeliveryType.DELIVERY, DeliveryPartnerType.SHOP, LiftType.MANUAL, "0",
                        LiftType.NOT_NEEDED, null
                },
                new Object[]{
                        DeliveryType.DELIVERY, DeliveryPartnerType.SHOP, LiftType.MANUAL, "string",
                        LiftType.NOT_NEEDED, null
                },
                new Object[]{
                        DeliveryType.DELIVERY, DeliveryPartnerType.SHOP, LiftType.MANUAL, "2.5",
                        LiftType.NOT_NEEDED, null
                },
                new Object[]{
                        DeliveryType.DELIVERY, DeliveryPartnerType.SHOP, LiftType.MANUAL, "2",
                        LiftType.MANUAL, BigDecimal.valueOf(300L)
                },
                new Object[]{
                        DeliveryType.DELIVERY, DeliveryPartnerType.SHOP, LiftType.MANUAL, "-2",
                        LiftType.MANUAL, BigDecimal.valueOf(300L)
                }
        ).map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void testDeliveryLiftPriceActualizerWithLargeSizeAndFallbackStrategy(
            DeliveryType deliveryType,
            DeliveryPartnerType deliveryPartnerType,
            LiftType liftType,
            String floor,
            LiftType expectedLiftType,
            BigDecimal expectedLiftPrice
    ) throws Throwable {
        PersAddress persAddress = new PersAddress();
        persAddress.setFloor(floor);

        Mockito.lenient().when(personalDataService.retrieve(any()))
                .thenReturn(new PersonalDataRetrieveResult(null, null, null, persAddress, null));

        Mockito.lenient().when(personalDataService.getPersAddress(any())).thenReturn(persAddress);

        Delivery delivery = buildDelivery(deliveryType, deliveryPartnerType, liftType);
        delivery.setBuyerAddress(AddressProvider.getAddress(address -> address.setFloor(floor)));
        order.setDelivery(delivery);
        order.setDeliveryOptions(Arrays.asList(
                buildDelivery(deliveryType, deliveryPartnerType, liftType),
                buildDelivery(deliveryType, deliveryPartnerType, liftType)
        ));

        ActualizationContext context = buildContext(order, true, true);

        assertTrue(deliveryLiftPriceActualizer.canApply(order, mock(PushApiCartResponse.class), context));
        assertTrue(deliveryLiftPriceActualizer.actualize(order, mock(PushApiCartResponse.class), context));

        checkOrder(order, expectedLiftType, expectedLiftPrice);
    }

    @Test
    public void testDeliveryLiftPriceActualizerIsDisabled() throws Throwable {
        Delivery delivery = buildDelivery(DeliveryType.DELIVERY, DeliveryPartnerType.SHOP, LiftType.ELEVATOR);
        delivery.setBuyerAddress(AddressProvider.getAddress(address -> address.setFloor("1")));
        order.setDelivery(delivery);
        order.setDeliveryOptions(Arrays.asList(
                buildDelivery(DeliveryType.DELIVERY, DeliveryPartnerType.SHOP, LiftType.ELEVATOR),
                buildDelivery(DeliveryType.DELIVERY, DeliveryPartnerType.SHOP, LiftType.ELEVATOR)
        ));

        ActualizationContext context = buildContext(order, true, false);

        assertTrue(deliveryLiftPriceActualizer.canApply(order, mock(PushApiCartResponse.class), context));
        assertTrue(deliveryLiftPriceActualizer.actualize(order, mock(PushApiCartResponse.class), context));

        checkOrder(order, null, null);
    }

    @Test
    public void testOrderHasNoLargeSizeOffers() throws Throwable {
        Delivery delivery = buildDelivery(DeliveryType.DELIVERY, DeliveryPartnerType.SHOP, LiftType.ELEVATOR);
        delivery.setBuyerAddress(AddressProvider.getAddress(address -> address.setFloor("1")));
        order.setDelivery(delivery);
        order.setDeliveryOptions(Arrays.asList(
                buildDelivery(DeliveryType.DELIVERY, DeliveryPartnerType.SHOP, LiftType.ELEVATOR),
                buildDelivery(DeliveryType.DELIVERY, DeliveryPartnerType.SHOP, LiftType.ELEVATOR)
        ));

        ActualizationContext context = buildContext(order, false, true);

        assertTrue(deliveryLiftPriceActualizer.canApply(order, mock(PushApiCartResponse.class), context));
        assertTrue(deliveryLiftPriceActualizer.actualize(order, mock(PushApiCartResponse.class), context));

        checkOrder(order, null, null);
    }

    private void checkOrder(Order order, LiftType expectedLiftType, BigDecimal expectedLiftPrice) {
        assertEquals(expectedLiftType, order.getDelivery().getLiftType());
        assertEquals(expectedLiftPrice, order.getDelivery().getLiftPrice());
        order.getDeliveryOptions().forEach(it -> {
            assertEquals(expectedLiftType, it.getLiftType());
            assertEquals(expectedLiftPrice, it.getLiftPrice());
        });
    }

    private Delivery buildDelivery(DeliveryType deliveryType, DeliveryPartnerType deliveryPartnerType,
                                   LiftType liftType) {
        Delivery delivery = new Delivery();
        delivery.setType(deliveryType);
        delivery.setDeliveryPartnerType(deliveryPartnerType);
        delivery.setLiftType(liftType);
        return delivery;
    }

    private ActualizationContext buildContext(Order order,
                                              boolean hasLargeSizeOffers,
                                              boolean enableLeftOptions) throws Throwable {
        MultiCartContext multiCartContext =
                MultiCartContext.createBy(ImmutableMultiCartParameters.builder().build(), Map.of());
        MultiCartFetchingContext fetchingContext = MultiCartFetchingContext.of(multiCartContext,
                MultiCartProvider.single(order));
        FoundOffer foundOffer1 = new FoundOffer();
        foundOffer1.setLargeSize(false);
        FoundOffer foundOffer2 = new FoundOffer();
        foundOffer2.setLargeSize(hasLargeSizeOffers);
        FlowSessionHelper.patchSession(fetchingContext,
                MultiCartFetchingContext::makeImmutableContext,
                (c, v) -> c.getMultiCartContext().setOffersStage(v), Arrays.asList(foundOffer1, foundOffer2));

        checkouterProperties.writeValue(ENABLE_LIFT_OPTIONS, enableLeftOptions);

        return ActualizationContext.builder()
                .withImmutableMulticartContext(ImmutableMultiCartContext.from(multiCartContext,
                        MultiCartProvider.empty()))
                .withCart(order)
                .withInitialCart(ImmutableOrder.from(order))
                .withOriginalBuyerCurrency(Currency.RUR)
                .withExperiments(enableLeftOptions ?
                        getExperiments().with(Map.of(MARKET_UNIFIED_TARIFFS, MARKET_UNIFIED_TARIFFS_VALUE)) :
                        getExperiments())
                .build();
    }
}
