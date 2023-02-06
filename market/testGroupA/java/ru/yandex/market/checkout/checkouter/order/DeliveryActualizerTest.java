package ru.yandex.market.checkout.checkouter.order;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import ru.yandex.common.util.collections.CollectionFactory;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.common.util.region.RegionService;
import ru.yandex.common.util.region.RegionTree;
import ru.yandex.common.util.region.RegionType;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.ActualDeliveryFetcher;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.DeliveryRouteFetcher;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.PreferableDeliveryOptionFetcher;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.mutations.BuyerDeliveryOutletMutation;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.mutations.DeliveryOptionsMutation;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.mutations.DeliveryOutletsMutation;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.mutations.DigitalDeliveryTypeValidation;
import ru.yandex.market.checkout.checkouter.actualization.flow.ContextualFlowRuntimeSession;
import ru.yandex.market.checkout.checkouter.actualization.flow.context.CartFetchingContext;
import ru.yandex.market.checkout.checkouter.actualization.flow.context.MultiCartFetchingContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ActualizationContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableActualizationContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartParameters;
import ru.yandex.market.checkout.checkouter.actualization.model.MultiCartContext;
import ru.yandex.market.checkout.checkouter.actualization.services.ActualDeliveryParcelCreationService;
import ru.yandex.market.checkout.checkouter.color.ColorConfig;
import ru.yandex.market.checkout.checkouter.color.SingleColorConfig;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryCipherService;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.ShopDeliveryServicesService;
import ru.yandex.market.checkout.checkouter.delivery.YandexMarketDeliveryActualizer;
import ru.yandex.market.checkout.checkouter.delivery.converter.DeliveryPricesConverterImpl;
import ru.yandex.market.checkout.checkouter.delivery.outlet.DeliveryOutletService;
import ru.yandex.market.checkout.checkouter.delivery.outlet.MarketOutletId;
import ru.yandex.market.checkout.checkouter.delivery.outlet.ShopOutlet;
import ru.yandex.market.checkout.checkouter.delivery.outlet.ShopOutletId;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureReader;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.log.Loggers;
import ru.yandex.market.checkout.checkouter.mocks.Mocks;
import ru.yandex.market.checkout.checkouter.order.immutable.ImmutableOrder;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.service.personal.PersonalDataService;
import ru.yandex.market.checkout.checkouter.service.yalavka.YaLavkaDeliveryConfigService;
import ru.yandex.market.checkout.checkouter.shop.ShopService;
import ru.yandex.market.checkout.checkouter.util.OfferItemUtils;
import ru.yandex.market.checkout.helpers.FlowSessionHelper;
import ru.yandex.market.checkout.providers.MultiCartProvider;
import ru.yandex.market.checkout.pushapi.client.entity.CartResponse;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.CurrencyConvertServiceImpl;
import ru.yandex.market.common.report.model.ActualDelivery;
import ru.yandex.market.common.report.model.ActualDeliveryOption;
import ru.yandex.market.common.report.model.ActualDeliveryResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.checkout.checkouter.actualization.utils.PushApiUtils.mapToPushApiCartResponse;
import static ru.yandex.market.checkout.checkouter.order.Color.GREEN;

public class DeliveryActualizerTest {

    private static final Long MARKET_OUTLET_ID = 123L;
    private static final Integer OUTLET_STORAGE_PERIOD = 8;

    private MarketReportInfoFetcher marketReportInfoFetcher;
    private MarketReportSearchService marketReportSearchService;
    private DeliveryOutletService deliveryOutletService;
    private RegionService regionService;
    private ShopDeliveryServicesService shopDeliveryServicesService;
    private Clock clock;
    private ImmutableMultiCartContext immutableMultiCartContext;
    private YaLavkaDeliveryConfigService yaLavkaDeliveryConfigService;
    private DeliveryOptionsMutation deliveryOptionsMutation;
    private DeliveryOutletsMutation deliveryOutletsMutation;
    private BuyerDeliveryOutletMutation buyerDeliveryOutletMutation;
    private ActualDeliveryFetcher actualDeliveryFetcher;
    private DeliveryRouteFetcher deliveryRouteFetcher;
    private PersonalDataService personalDataService;

    @BeforeEach
    public void init() {
        marketReportInfoFetcher = Mocks.createMock(MarketReportInfoFetcher.class);
        marketReportSearchService = Mocks.createMock(MarketReportSearchService.class);
        deliveryOutletService = Mocks.createMock(DeliveryOutletService.class);
        personalDataService = Mocks.createMock(PersonalDataService.class);
        clock = Clock.systemDefaultZone();
        CheckouterFeatureReader checkouterFeatureReader = mock(CheckouterFeatureReader.class);
        DeliveryPricesConverterImpl deliveryPricesConverter = new DeliveryPricesConverterImpl();
        deliveryPricesConverter.setCurrencyConvertService(CurrencyConvertServiceImpl.getMock());
        DeliveryCipherService deliveryCipherService = Mocks.createMock(DeliveryCipherService.class);
        Mockito.doNothing()
                .when(deliveryCipherService).cipherDelivery(any(Delivery.class));
        regionService = Mocks.createMock(RegionService.class);
        YandexMarketDeliveryActualizer yandexMarketDeliveryActualizer = Mocks.createMock(
                YandexMarketDeliveryActualizer.class
        );
        Mockito.doReturn(Collections.emptyList())
                .when(yandexMarketDeliveryActualizer)
                .createYandexMarketOptions(any(), any(), any(), any());

        shopDeliveryServicesService = Mocks.createMock(ShopDeliveryServicesService.class);
        buyerDeliveryOutletMutation = new BuyerDeliveryOutletMutation(
                marketReportInfoFetcher,
                deliveryOutletService,
                shopDeliveryServicesService,
                checkouterFeatureReader
        );

        ColorConfig colorConfig = mock(ColorConfig.class);
        when(colorConfig.getFor(any(Order.class))).thenReturn(mock(SingleColorConfig.class));

        doReturn(false).when(checkouterFeatureReader)
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
                checkouterFeatureReader, new DigitalDeliveryTypeValidation(), mock(ShopService.class),
                mock(PersonalDataService.class));

        deliveryOutletsMutation = new DeliveryOutletsMutation(
                marketReportInfoFetcher,
                regionService,
                yandexMarketDeliveryActualizer,
                deliveryOutletService,
                shopDeliveryServicesService,
                checkouterFeatureReader,
                actualDeliveryFetcher, deliveryRouteFetcher, colorConfig
        );
        MultiCartContext multiCartContext = MultiCartContext.createBy(ImmutableMultiCartParameters.builder()
                .build(), Map.of());
        immutableMultiCartContext = ImmutableMultiCartContext.from(multiCartContext,
                MultiCartProvider.buildMultiCart(List.of()));

        YaLavkaDeliveryConfigService service = Mockito.mock(YaLavkaDeliveryConfigService.class);
        when(service.isLavkaDeliveryService(Mockito.any())).thenReturn(RandomUtils.nextBoolean());
        yaLavkaDeliveryConfigService = service;
    }

    @Test
    public void shouldNotActualizeCartIfDeliveryIsNull() {
        Order order = OrderProvider.getBlueOrder();

        mockNoDeliveryServices();

        Mockito.doReturn(Collections.emptyList())
                .when(marketReportInfoFetcher)
                .fetchShopOutlets(
                        any(), anyLong(), any(), any(), anyBoolean()
                );

        //
        Delivery delivery = DeliveryProvider.getEmptyDelivery();
        delivery.setType(DeliveryType.DELIVERY);
        //
        order.setDelivery(delivery);
        //
        CartResponse shopCart = new CartResponse();
        shopCart.setItems(
                order.getItems().stream().map(OfferItemUtils::deepCopy).collect(Collectors.toList())
        );
        shopCart.setDeliveryOptions(null);
        shopCart.setPaymentMethods(ImmutableList.of());
        boolean actualizeDelivery = actualizeDelivery(
                order,
                shopCart,
                ActualizationContext.builder()
                        .withImmutableMulticartContext(immutableMultiCartContext)
                        .withCart(order)
                        .withInitialCart(ImmutableOrder.from(order))
                        .withOriginalBuyerCurrency(order.getBuyerCurrency())
        );
        Assertions.assertFalse(actualizeDelivery, "Should not actualize delivery if delivery options is null");
    }

    @Test
    public void shouldActualizeCartIfDeliveryIsNotNullAndHasMatchingOption() {
        mockNoOutlets();
        mockNoDeliveryServices();

        DeliveryType deliveryType = DeliveryType.DELIVERY;
        String serviceName = "Asdasd";
        BigDecimal price = BigDecimal.valueOf(123);
        BigDecimal buyerPrice = BigDecimal.valueOf(123);

        Delivery delivery = new Delivery();
        delivery.setRegionId(213L);
        delivery.setBuyerPrice(buyerPrice);
        delivery.setServiceName(serviceName);
        delivery.setPrice(price);
        delivery.setOutletIds(null);
        delivery.setType(deliveryType);
        delivery.setDeliveryDates(DeliveryDates.deliveryDates(clock, 1, 2));

        Order order = new Order();
        order.setShopId(774L);
        order.setDelivery(delivery);
        order.setExchangeRate(BigDecimal.ONE);
        order.addItem(OrderItemProvider.getOrderItem());

        DeliveryResponse deliveryOption = new DeliveryResponse();
        deliveryOption.setBuyerPrice(buyerPrice);
        deliveryOption.setServiceName(serviceName);
        deliveryOption.setPrice(price);
        deliveryOption.setOutletIds(null);
        deliveryOption.setType(deliveryType);
        deliveryOption.setDeliveryDates(DeliveryDates.deliveryDates(clock, 1, 2));
        String shopDeliveryId = "asadfa3849hf39h";
        deliveryOption.setShopDeliveryId(shopDeliveryId);

        CartResponse cartResponse = new CartResponse();
        cartResponse.setDeliveryOptions(Lists.newArrayList(deliveryOption));
        cartResponse.setPaymentMethods(Lists.newArrayList());
        cartResponse.setItems(
                order.getItems().stream().map(OfferItemUtils::deepCopy).collect(Collectors.toList())
        );

        Mockito.doReturn(new ActualDelivery())
                .when(marketReportSearchService)
                .searchActualDelivery(any(ActualDeliveryRequestBuilder.class));

        var contextBuilder = ActualizationContext.builder()
                .withImmutableMulticartContext(immutableMultiCartContext)
                .withCart(order)
                .withInitialCart(ImmutableOrder.from(order))
                .withOriginalBuyerCurrency(order.getBuyerCurrency());
        var fetchContext = CartFetchingContext.of(Mockito.mock(MultiCartFetchingContext.class),
                contextBuilder, order);

        ContextualFlowRuntimeSession.useSession(fetchContext,
                CartFetchingContext::makeImmutableContext);

        Assertions.assertTrue(actualizeDelivery(
                order,
                cartResponse,
                fetchContext.getActualizationContextBuilder()
        ), "Should actualize delivery if delivery is not null and has matching option");

        Assertions.assertEquals(shopDeliveryId, order.getDelivery().getShopDeliveryId());
    }


    @Test
    public void shouldActualizeCartIfDeliveryIsNotNullAndHasntMatchingOption() {
        mockNoOutlets();
        mockNoDeliveryServices();

        DeliveryType deliveryType = DeliveryType.DELIVERY;
        String serviceName = "Asdasd";
        BigDecimal price = BigDecimal.valueOf(123);
        BigDecimal buyerPrice = BigDecimal.valueOf(123);

        Delivery delivery = new Delivery();
        delivery.setRegionId(213L);
        delivery.setBuyerPrice(buyerPrice);
        delivery.setPrice(price);
        delivery.setOutletIds(null);
        delivery.setType(deliveryType);

        Order order = new Order();
        order.setShopId(774L);
        order.setDelivery(delivery);
        order.setExchangeRate(BigDecimal.ONE);
        order.addItem(OrderItemProvider.getOrderItem());

        DeliveryResponse deliveryOption = new DeliveryResponse();
        deliveryOption.setBuyerPrice(buyerPrice);
        deliveryOption.setServiceName(serviceName);
        deliveryOption.setPrice(price);
        deliveryOption.setOutletIds(null);
        deliveryOption.setType(deliveryType);

        CartResponse cartResponse = new CartResponse();
        cartResponse.setDeliveryOptions(Lists.newArrayList(deliveryOption));
        cartResponse.setPaymentMethods(Lists.newArrayList());
        cartResponse.setItems(
                order.getItems().stream().map(OfferItemUtils::deepCopy).collect(Collectors.toList())
        );

        Assertions.assertFalse(actualizeDelivery(
                order,
                cartResponse,
                ActualizationContext.builder()
                        .withImmutableMulticartContext(immutableMultiCartContext)
                        .withCart(order)
                        .withInitialCart(ImmutableOrder.from(order))
                        .withOriginalBuyerCurrency(order.getBuyerCurrency())
        ), "Shouldn't actualize delivery if delivery is not null and hasn't matching option");
    }

    @Test
    public void shouldNotConvertDeliveryCurrencyIfShopSpecifiedIt() {
        mockNoOutlets();
        mockNoDeliveryServices();

        Order order = new Order();

        Currency shopInfoDeliveryCurrency = Currency.EUR;
        Currency responseDeliveryCurrency = Currency.EUR;
        Currency shopCurrency = Currency.RUR;
        Currency buyerCurrency = Currency.RUR;

        order.setShopId(774L);
        order.setBuyerCurrency(buyerCurrency);
        order.setCurrency(shopCurrency);
        order.setDeliveryCurrency(shopInfoDeliveryCurrency);
        order.setDelivery(new Delivery(213L));
        order.setExchangeRate(BigDecimal.ONE);
        order.addItem(OrderItemProvider.getOrderItem());

        DeliveryResponse deliveryResponse = new DeliveryResponse();
        deliveryResponse.setPrice(BigDecimal.valueOf(100));
        deliveryResponse.setServiceName("shouldNotConvertDeliveryCurrencyIfShopSpecifiedItDeliveryOption");
        deliveryResponse.setType(DeliveryType.DELIVERY);
        deliveryResponse.setPaymentOptions(ImmutableSet.of(PaymentMethod.YANDEX));

        CartResponse cartResponse = new CartResponse();
        cartResponse.setDeliveryCurrency(responseDeliveryCurrency);
        cartResponse.setDeliveryOptions(Lists.newArrayList(deliveryResponse));
        cartResponse.setPaymentMethods(ImmutableList.of());
        cartResponse.setItems(
                order.getItems().stream().map(OfferItemUtils::deepCopy).collect(Collectors.toList())
        );

        Assertions.assertTrue(
                actualizeDelivery(
                        order,
                        cartResponse,
                        ActualizationContext.builder()
                                .withImmutableMulticartContext(immutableMultiCartContext)
                                .withCart(order)
                                .withInitialCart(ImmutableOrder.from(order))
                                .withOriginalBuyerCurrency(order.getBuyerCurrency())
                )
        );

        List<? extends Delivery> deliveryOptions = order.getDeliveryOptions();
        Delivery option = deliveryOptions.get(0);
        Assertions.assertEquals(BigDecimal.valueOf(100L * 70), option.getPrice(), "Price check");
        Assertions.assertEquals(BigDecimal.valueOf(100L * 70), option.getBuyerPrice(), "BuyerPrice check");
    }


    @Test
    public void shouldConvertDeliveryCurrencyIfShopSpecifiedIt() {
        mockNoOutlets();
        mockNoDeliveryServices();

        Order order = new Order();

        Currency shopInfoDeliveryCurrency = Currency.EUR;
        Currency responseDeliveryCurrency = Currency.EUR;
        Currency shopCurrency = Currency.USD;
        Currency buyerCurrency = Currency.RUR;

        order.setShopId(774L);
        order.setBuyerCurrency(buyerCurrency);
        order.setCurrency(shopCurrency);
        order.setDeliveryCurrency(shopInfoDeliveryCurrency);
        order.setDelivery(new Delivery(213L));
        order.setExchangeRate(BigDecimal.valueOf(60));
        order.addItem(OrderItemProvider.getOrderItem());

        DeliveryResponse deliveryResponse = new DeliveryResponse();
        deliveryResponse.setPrice(BigDecimal.valueOf(100));
        deliveryResponse.setServiceName("shouldConvertDeliveryCurrencyIfShopSpecifiedItDeliveryOption");
        deliveryResponse.setType(DeliveryType.DELIVERY);
        deliveryResponse.setPaymentOptions(ImmutableSet.of(PaymentMethod.YANDEX));

        CartResponse cartResponse = new CartResponse();
        cartResponse.setDeliveryCurrency(responseDeliveryCurrency);
        cartResponse.setDeliveryOptions(Lists.newArrayList(deliveryResponse));
        cartResponse.setPaymentMethods(ImmutableList.of());
        cartResponse.setItems(
                order.getItems().stream().map(OfferItemUtils::deepCopy).collect(Collectors.toList())
        );

        Assertions.assertTrue(
                actualizeDelivery(
                        order,
                        cartResponse,
                        ActualizationContext.builder()
                                .withImmutableMulticartContext(immutableMultiCartContext)
                                .withCart(order)
                                .withInitialCart(ImmutableOrder.from(order))
                                .withOriginalBuyerCurrency(order.getBuyerCurrency())
                )
        );

        List<? extends Delivery> deliveryOptions = order.getDeliveryOptions();
        Delivery option = deliveryOptions.get(0);
        Assertions.assertEquals(BigDecimal.valueOf(100L).multiply(new BigDecimal("70.00")
                .divide(BigDecimal.valueOf(60L),
                        BigDecimal.ROUND_HALF_UP)), option.getPrice(), "Price check");
        Assertions.assertEquals(BigDecimal.valueOf(100 * 70L), option.getBuyerPrice(), "BuyerPrice check");
    }

    @Test
    public void actualizePICKUPWithOutletCode() {
        String shopOutletCode = "123Lstr";
        ShopOutletId shopOutletId = new ShopOutletId(774L, shopOutletCode);
        Pair<Order, CartResponse> orderCartResponse = prepareActualizePICKUP(shopOutletId);
        Order order = orderCartResponse.getFirst();
        CartResponse cartResponse = orderCartResponse.getSecond();
        //пришел от магазина список кодов
        cartResponse.getDeliveryOptions().get(0).setOutletCodes(Set.of(shopOutletCode));
        //не должно упасть
        actualizeDelivery(
                order,
                cartResponse,
                ActualizationContext.builder()
                        .withImmutableMulticartContext(immutableMultiCartContext)
                        .withCart(order)
                        .withInitialCart(ImmutableOrder.from(order))
                        .withOriginalBuyerCurrency(order.getBuyerCurrency())
        );
        Assertions.assertEquals(shopOutletCode, order.getDelivery().getOutletCode());
        Assertions.assertEquals(MARKET_OUTLET_ID, order.getDelivery().getOutletId());
    }


    @Test
    public void actualizePICKUPWithOutletId() {
        String outletCode = "534";
        ShopOutletId shopOutletId = new ShopOutletId(774L, outletCode);
        Pair<Order, CartResponse> orderCartResponse = prepareActualizePICKUP(shopOutletId);
        Order order = orderCartResponse.getFirst();
        CartResponse cartResponse = orderCartResponse.getSecond();
        //пришел от магазина список числовых id
        cartResponse.getDeliveryOptions().get(0).setOutletIds(Set.of(534L));
        //не должно упасть
        actualizeDelivery(
                order,
                cartResponse,
                ActualizationContext.builder()
                        .withImmutableMulticartContext(immutableMultiCartContext)
                        .withCart(order)
                        .withInitialCart(ImmutableOrder.from(order))
                        .withOriginalBuyerCurrency(order.getBuyerCurrency())
        );
        Assertions.assertEquals(MARKET_OUTLET_ID, order.getDelivery().getOutletId());
        Assertions.assertEquals(outletCode, order.getDelivery().getOutletCode());
    }

    @Test
    public void actualizePICKUPWithOutletStoragePeriod() {
        String outletCode = "534";
        ShopOutletId shopOutletId = new ShopOutletId(774L, outletCode);
        Pair<Order, CartResponse> orderCartResponse = prepareActualizePICKUP(shopOutletId);
        Order order = orderCartResponse.getFirst();
        CartResponse cartResponse = orderCartResponse.getSecond();
        //пришел от магазина список числовых id
        cartResponse.getDeliveryOptions().get(0).setOutletIds(Set.of(534L));
        //не должно упасть
        actualizeDelivery(
                order,
                cartResponse,
                ActualizationContext.builder()
                        .withImmutableMulticartContext(immutableMultiCartContext)
                        .withCart(order)
                        .withInitialCart(ImmutableOrder.from(order))
                        .withOriginalBuyerCurrency(order.getBuyerCurrency())
        );
        Assertions.assertEquals(OUTLET_STORAGE_PERIOD, order.getDelivery().getOutletStoragePeriod());
    }

    @Test
    public void checkLoggingForLavkaDeliveryDates() {
        Logger logger = (Logger) LoggerFactory.getLogger(Loggers.KEY_VALUE_LOG);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        Level oldLevel = logger.getLevel();
        logger.setLevel(Level.INFO);
        try {
            ActualDeliveryAndYaLavkaFacade facade = new ActualDeliveryAndYaLavkaFacade(null,
                    yaLavkaDeliveryConfigService,
                    null, Clock.systemDefaultZone(), null, personalDataService);

            ActualDeliveryOption classicDelivery1 = new ActualDeliveryOption();
            classicDelivery1.setDayFrom(4);
            classicDelivery1.setDayTo(4);

            ActualDeliveryOption classicDelivery2 = new ActualDeliveryOption();
            classicDelivery2.setDayFrom(6);
            classicDelivery2.setDayTo(7);

            ActualDeliveryResult classic = new ActualDeliveryResult();
            classic.setDelivery(Stream.of(classicDelivery1, classicDelivery2).collect(Collectors.toList()));

            ActualDeliveryOption lavkaDelivery1 = new ActualDeliveryOption();
            lavkaDelivery1.setDayFrom(5);
            lavkaDelivery1.setDayTo(7);

            ActualDeliveryOption lavkaDelivery2 = new ActualDeliveryOption();
            lavkaDelivery2.setDayFrom(6);
            lavkaDelivery2.setDayTo(9);

            List<ActualDeliveryOption> options = Stream.of(lavkaDelivery1, lavkaDelivery2).collect(Collectors.toList());

            Order order = new Order();
            Buyer buyer = new Buyer();
            buyer.setUid(12345L);
            order.setBuyer(buyer);
            ActualizationContext context = ActualizationContext.builder()
                    .withImmutableMulticartContext(
                            ImmutableMultiCartContext.from(
                                    MultiCartContext.createBy(
                                            ImmutableMultiCartParameters.builder().build(), Map.of()),
                                    MultiCartProvider.single(order)))
                    .withCart(order)
                    .withInitialCart(ImmutableOrder.from(order))
                    .build();

            facade.logDeliveryDates(classic, options, ImmutableActualizationContext.of(context));
            List<ILoggingEvent> logsList = listAppender.list;
            Assertions.assertEquals(1, logsList.size());
            Assertions.assertTrue(logsList.get(0).toString()
                    .contains(String.valueOf(12345L)));
        } finally {
            logger.detachAppender(listAppender);
            logger.setLevel(oldLevel);
        }
    }

    private boolean actualizeDelivery(Order order,
                                      CartResponse cartResponse,
                                      ActualizationContext.ActualizationContextBuilder builder) {
        var pushApiCartResponse = mapToPushApiCartResponse(cartResponse);
        var multiCart = MultiCartProvider.single(order);
        var multiCartFetchingContext = MultiCartFetchingContext.of(MultiCartContext.createBy(
                ImmutableMultiCartParameters.builder().build(), multiCart), multiCart);
        var fetchingContext = CartFetchingContext.of(
                multiCartFetchingContext, builder, order);

        FlowSessionHelper.patchSession(fetchingContext, CartFetchingContext::makeImmutableContext,
                (c, v) -> c.getActualizationContextBuilder().withPushApiCartStage(v), pushApiCartResponse);
        deliveryOutletsMutation.onSuccess(fetchingContext);
        deliveryOptionsMutation.onSuccess(fetchingContext);
        buyerDeliveryOutletMutation.onSuccess(fetchingContext);
        return (order.getChanges() == null || order.getChanges().isEmpty());
    }

    private Pair<Order, CartResponse> prepareActualizePICKUP(ShopOutletId shopOutletId) {
        ShopOutlet shopOutlet = new ShopOutlet();
        shopOutlet.setId(MARKET_OUTLET_ID);
        shopOutlet.setRegionId(213L);
        shopOutlet.setStoragePeriod(OUTLET_STORAGE_PERIOD);

        MarketOutletId marketOutletId = new MarketOutletId(774L, MARKET_OUTLET_ID);

        Mockito.doReturn(CollectionFactory.list(shopOutlet))
                .when(marketReportInfoFetcher)
                .fetchShopOutlets(
                        any(),
                        any(),
                        anyCollection(),
                        anyCollection(),
                        anyBoolean()
                );
        Mockito.doReturn(shopOutlet)
                .when(marketReportInfoFetcher)
                .fetchShopOutlet(
                        eq(GREEN), anyLong(), any()
                );

        mockNoDeliveryServices();
        Mockito.doReturn(marketOutletId)
                .when(deliveryOutletService).getMarketByShop(shopOutletId);

        Mockito.doReturn(shopOutletId)
                .when(deliveryOutletService).getShopByMarket(marketOutletId);

        final ru.yandex.common.util.region.Region region3 = new ru.yandex.common.util.region.Region(
                213, "region3", RegionType.CITY,
                new ru.yandex.common.util.region.Region(
                        2, "region2", RegionType.CITY,
                        new ru.yandex.common.util.region.Region(
                                1, "region1", RegionType.CITY, null
                        )
                )
        );

        final RegionTree regionTree = mock(RegionTree.class);
        Mockito.doReturn(regionTree)
                .when(regionService).getRegionTree();
        when(regionTree.getRegion(213)).thenReturn(region3);
        when(regionTree.getPathToRoot(213)).thenReturn(CollectionFactory.list(213));

        String serviceName = "Asdasd";
        BigDecimal price = BigDecimal.valueOf(123);
        BigDecimal buyerPrice = BigDecimal.TEN;


        Delivery delivery = new Delivery();
        delivery.setRegionId(213L);
        delivery.setBuyerPrice(buyerPrice);
        delivery.setServiceName(serviceName);
        delivery.setPrice(price);
        delivery.setDeliveryDates(new DeliveryDates(new Date(), new Date()));
        delivery.setOutletId(MARKET_OUTLET_ID);
        delivery.setType(DeliveryType.PICKUP);
        delivery.setMarketBranded(false);
        delivery.setDeliveryPartnerType(DeliveryPartnerType.SHOP);

        Order order = new Order();
        order.setShopId(774L);
        order.setDelivery(delivery);
        order.setExchangeRate(BigDecimal.ONE);
        order.addItem(OrderItemProvider.getOrderItem());
        order.setRgb(Color.GREEN);

        DeliveryResponse deliveryOption = new DeliveryResponse();
        deliveryOption.setBuyerPrice(buyerPrice);
        deliveryOption.setServiceName(serviceName);
        deliveryOption.setPrice(price);
        deliveryOption.setType(DeliveryType.PICKUP);


        CartResponse cartResponse = new CartResponse();
        cartResponse.setItems(
                order.getItems().stream().map(OfferItemUtils::deepCopy).collect(Collectors.toList())
        );
        cartResponse.setDeliveryOptions(Lists.newArrayList(deliveryOption));
        cartResponse.setPaymentMethods(Lists.newArrayList());
        return Pair.of(order, cartResponse);
    }


    private void mockNoDeliveryServices() {
        Mockito.doReturn(new HashSet<>())
                .when(shopDeliveryServicesService).getShopDeliveryServices(anyLong(), any(), any());
    }

    private void mockNoOutlets() {
        Mockito.doReturn(new ArrayList())
                .when(marketReportInfoFetcher).fetchShopOutlets(any(),
                any(),
                anyCollection(),
                anyCollection(),
                anyBoolean());
    }

}
