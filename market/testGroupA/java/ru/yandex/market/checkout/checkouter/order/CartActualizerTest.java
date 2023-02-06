package ru.yandex.market.checkout.checkouter.order;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.common.collect.Iterables;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.internal.stubbing.defaultanswers.ReturnsSmartNulls;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Profile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.common.util.region.RegionService;
import ru.yandex.common.util.region.RegionTree;
import ru.yandex.inside.passport.tvm2.Tvm2;
import ru.yandex.market.checkout.checkouter.actualization.CartActualizer;
import ru.yandex.market.checkout.checkouter.actualization.CartActualizersInvoker;
import ru.yandex.market.checkout.checkouter.actualization.CartPostprocessorsInvoker;
import ru.yandex.market.checkout.checkouter.actualization.actualizers.Actualizer;
import ru.yandex.market.checkout.checkouter.actualization.actualizers.AddressRegionActualizer;
import ru.yandex.market.checkout.checkouter.actualization.actualizers.BestDeliveryOptionsActualizer;
import ru.yandex.market.checkout.checkouter.actualization.actualizers.BusinessActualizer;
import ru.yandex.market.checkout.checkouter.actualization.actualizers.BuyerDeliveryDatesActualizer;
import ru.yandex.market.checkout.checkouter.actualization.actualizers.CurrencyActualizer;
import ru.yandex.market.checkout.checkouter.actualization.actualizers.DeliveryDatesActualizer;
import ru.yandex.market.checkout.checkouter.actualization.actualizers.DeliveryLeaveAtTheDoorActualizer;
import ru.yandex.market.checkout.checkouter.actualization.actualizers.DeliveryLiftPriceActualizer;
import ru.yandex.market.checkout.checkouter.actualization.actualizers.DeliveryOptionIdActualizer;
import ru.yandex.market.checkout.checkouter.actualization.actualizers.DeliveryTariffStatsActualizer;
import ru.yandex.market.checkout.checkouter.actualization.actualizers.ExternalCertificateItemsActualizer;
import ru.yandex.market.checkout.checkouter.actualization.actualizers.FakeCartDeliveryActualizer;
import ru.yandex.market.checkout.checkouter.actualization.actualizers.ItemServiceActualizer;
import ru.yandex.market.checkout.checkouter.actualization.actualizers.ItemServiceTimeslotsActualizer;
import ru.yandex.market.checkout.checkouter.actualization.actualizers.ItemsCountActualizer;
import ru.yandex.market.checkout.checkouter.actualization.actualizers.ItemsDeliveryActualizer;
import ru.yandex.market.checkout.checkouter.actualization.actualizers.ItemsPriceActualizer;
import ru.yandex.market.checkout.checkouter.actualization.actualizers.LiftOptionsActualizer;
import ru.yandex.market.checkout.checkouter.actualization.actualizers.MissingItemsActualizer;
import ru.yandex.market.checkout.checkouter.actualization.actualizers.PostOutletActualizer;
import ru.yandex.market.checkout.checkouter.actualization.actualizers.ReportDeliveryDatesDiffActualizer;
import ru.yandex.market.checkout.checkouter.actualization.actualizers.ReportDeliveryOptionsDiffActualizer;
import ru.yandex.market.checkout.checkouter.actualization.actualizers.ReportDeliveryPricesDiffActualizer;
import ru.yandex.market.checkout.checkouter.actualization.actualizers.ReportPaymentOptionsDiffActualizer;
import ru.yandex.market.checkout.checkouter.actualization.actualizers.ServicePriceActualizer;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.ActualDeliveryDraftFetcher;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.ActualDeliveryFetcher;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.DeliveryAddressFetcher;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.DeliveryAddressMultiCartFetcher;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.DeliveryFetcher;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.DeliveryRouteFetcher;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.PushApiCartResponseFetcher;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.ReportOffersFetcher;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.mutations.ActualDeliveryFlagsMutation;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.mutations.ActualDeliveryMutation;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.mutations.ItemsPriceAndCountMutation;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.mutations.OrderAcceptMethodMutation;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.mutations.OrderDeliveryAddressMutation;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.mutations.OrderDeliveryPartnerTypeMutation;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.mutations.OrderItemsOfferDataMutation;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.mutations.OrderPropertiesMutation;
import ru.yandex.market.checkout.checkouter.actualization.flow.CartFlowFactory;
import ru.yandex.market.checkout.checkouter.actualization.flow.context.CartFetchingContext;
import ru.yandex.market.checkout.checkouter.actualization.flow.context.MultiCartFetchingContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ActualizationContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartParameters;
import ru.yandex.market.checkout.checkouter.actualization.model.MultiCartContext;
import ru.yandex.market.checkout.checkouter.actualization.processors.postprocessors.AddressRegionPostprocessor;
import ru.yandex.market.checkout.checkouter.actualization.processors.postprocessors.PostOutletPostprocessor;
import ru.yandex.market.checkout.checkouter.actualization.processors.postprocessors.ReportDiscountPromoCartPostprocessor;
import ru.yandex.market.checkout.checkouter.actualization.processors.postprocessors.SetPickupDeliveryTimePostprocessor;
import ru.yandex.market.checkout.checkouter.actualization.processors.postprocessors.SetYandexEmployeeCartPostprocessor;
import ru.yandex.market.checkout.checkouter.actualization.services.PaymentMethodValidator;
import ru.yandex.market.checkout.checkouter.actualization.utils.TotalWeightAndPriceCalculator;
import ru.yandex.market.checkout.checkouter.actualization.validation.ReportInfoAwareOrderValidator;
import ru.yandex.market.checkout.checkouter.balance.service.BalanceTokenProvider;
import ru.yandex.market.checkout.checkouter.cart.ItemChange;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.color.BlueConfig;
import ru.yandex.market.checkout.checkouter.color.ColorConfig;
import ru.yandex.market.checkout.checkouter.color.GreenConfig;
import ru.yandex.market.checkout.checkouter.color.SingleColorConfig;
import ru.yandex.market.checkout.checkouter.color.TurboConfig;
import ru.yandex.market.checkout.checkouter.color.TurboPlusConfig;
import ru.yandex.market.checkout.checkouter.color.WhiteConfig;
import ru.yandex.market.checkout.checkouter.config.ActualizeLoggingSerializationConfig;
import ru.yandex.market.checkout.checkouter.degradation.client.DegradableGeocoderClientDecorator;
import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureReader;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureResolverStub;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureWriter;
import ru.yandex.market.checkout.checkouter.geo.GeoRegionService;
import ru.yandex.market.checkout.checkouter.geo.GeobaseService;
import ru.yandex.market.checkout.checkouter.geo.GeocodeMemCacheStorageService;
import ru.yandex.market.checkout.checkouter.geo.GeocodeProvider;
import ru.yandex.market.checkout.checkouter.order.global.CurrencyConvertService;
import ru.yandex.market.checkout.checkouter.order.immutable.ImmutableOrder;
import ru.yandex.market.checkout.checkouter.order.validation.cancelpolicy.CancelPolicyDataValidationService;
import ru.yandex.market.checkout.checkouter.pay.CurrencyRates;
import ru.yandex.market.checkout.checkouter.promo.blueset.BlueSetPromoFeatureSupportHelper;
import ru.yandex.market.checkout.checkouter.promo.bundles.BundleItemsJoiner;
import ru.yandex.market.checkout.checkouter.promo.bundles.BundlesFeatureSupportHelper;
import ru.yandex.market.checkout.checkouter.promo.flash.FlashPromoFeatureSupportHelper;
import ru.yandex.market.checkout.checkouter.report.Experiments;
import ru.yandex.market.checkout.checkouter.service.personal.PersonalDataRetrieveResult;
import ru.yandex.market.checkout.checkouter.service.personal.PersonalDataService;
import ru.yandex.market.checkout.checkouter.service.personal.model.PersAddress;
import ru.yandex.market.checkout.checkouter.service.yalavka.YaLavkaOrderProperties;
import ru.yandex.market.checkout.checkouter.shop.PaymentClass;
import ru.yandex.market.checkout.checkouter.shop.ShopActualDeliveryRegionalSettings;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.checkout.checkouter.shop.ShopService;
import ru.yandex.market.checkout.checkouter.shop.SupplierMetaDataService;
import ru.yandex.market.checkout.checkouter.shop.pushapi.SettingsService;
import ru.yandex.market.checkout.checkouter.trace.CheckoutContextHolder;
import ru.yandex.market.checkout.checkouter.transliterate.TransliterateService;
import ru.yandex.market.checkout.checkouter.util.CheckouterProperties;
import ru.yandex.market.checkout.checkouter.util.CheckouterPropertiesImpl;
import ru.yandex.market.checkout.common.pay.FinancialValidator;
import ru.yandex.market.checkout.common.rest.TvmTicketProvider;
import ru.yandex.market.checkout.helpers.FlowSessionHelper;
import ru.yandex.market.checkout.providers.FulfilmentProvider;
import ru.yandex.market.checkout.providers.MultiCartProvider;
import ru.yandex.market.checkout.pushapi.client.PushApi;
import ru.yandex.market.checkout.pushapi.client.entity.Cart;
import ru.yandex.market.checkout.pushapi.client.entity.CartResponse;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.test.providers.ItemServiceProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;
import ru.yandex.market.checkout.util.report.OfferServiceBuilder;
import ru.yandex.market.common.report.model.ActualDelivery;
import ru.yandex.market.common.report.model.CurrencyConvertResult;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.common.report.model.FoundOffer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.ASYNC_ACTUAL_DELIVERY_REQUEST;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.ENABLE_UNIFIED_TARIFFS;
import static ru.yandex.market.checkout.checkouter.feature.type.common.CollectionFeatureType.ENABLED_FETCHERS;
import static ru.yandex.market.checkout.checkouter.mocks.Mocks.createMock;
import static ru.yandex.market.checkout.checkouter.order.TestCustomerOrder.FEED_OFFER_ID;
import static ru.yandex.market.checkout.checkouter.report.Experiments.MARKET_UNIFIED_TARIFFS;
import static ru.yandex.market.checkout.checkouter.report.Experiments.MARKET_UNIFIED_TARIFFS_VALUE;
import static ru.yandex.market.checkout.checkouter.trace.CheckoutContextHolder.getExperiments;

@ActiveProfiles("actualizer-test")
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextHierarchy({
        @ContextConfiguration
})
public class CartActualizerTest {

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private PushApiCartResponseFetcher pushApiCartResponseFetcher;
    @Autowired
    private DeliveryAddressFetcher deliveryAddressFetcher;
    @Autowired
    private ActualDeliveryDraftFetcher actualDeliveryDraftFetcher;
    @Autowired
    private OrderDeliveryAddressMutation orderDeliveryAddressMutation;
    @Autowired
    private CartActualizer cartActualizer;
    @Autowired
    private OrderItemsOfferDataMutation orderItemsOfferDataMutation;
    @Autowired
    private ActualDeliveryFlagsMutation actualDeliveryFlagsMutation;
    @Autowired
    private OrderPropertiesMutation orderPropertiesMutation;
    @Autowired
    private OrderDeliveryPartnerTypeMutation orderDeliveryPartnerTypeMutation;
    @Autowired
    private CheckouterFeatureWriter checkouterFeatureWriter;
    @Autowired
    private ItemsPriceAndCountMutation itemsPriceAndCountMutation;
    @Autowired
    private ActualDeliveryFetcher actualDeliveryFetcher;
    @Autowired
    private ActualDeliveryMutation actualDeliveryMutation;
    @Autowired
    private OrderAcceptMethodMutation orderAcceptMethodMutation;
    private ExecutorService executor;
    private ImmutableMultiCartParameters multiCartParameters;

    static void setOffers(MultiCartContext context, MultiCart multiCart, List<FoundOffer> offers) {
        FlowSessionHelper.patchSession(
                MultiCartFetchingContext.of(context, multiCart),
                MultiCartFetchingContext::makeImmutableContext,
                (c, v) -> c.getMultiCartContext().setOffersStage(v),
                offers
        );
    }

    static void setOffers(MultiCartContext context, MultiCart multiCart) {
        setOffers(context, multiCart, multiCart.getCarts().stream()
                .flatMap(c -> c.getItems().stream()
                        .map(FoundOfferBuilder::createFrom)
                        .map(b -> b.shopId(c.getShopId())
                                .priorityRegionId(c.getPriorityRegionId())
                                .cpa("real"))
                        .map(FoundOfferBuilder::build))
                .collect(Collectors.toUnmodifiableList()));
    }

    @BeforeEach
    public void setUp() throws Exception {
        executor = Executors.newCachedThreadPool();
        multiCartParameters = ImmutableMultiCartParameters.builder()
                .withBuyerRegionId(10L)
                .withIsMultiCart(false)
                .withApiSettings(ApiSettings.PRODUCTION)
                .withActionId("actionId")
                .build();
    }

    @AfterEach
    private void clean() {
        executor.shutdown();
    }

    @Test
    void shouldFillDeliveryPartnerTypeIfNotExist() {
        Order order = new TestCustomerOrder().build();
        order.setFulfilment(false);
        order.setRgb(Color.BLUE);
        order.getItems().forEach(oi -> {
            oi.setShopSku(FulfilmentProvider.TEST_SHOP_SKU);
            oi.setSupplierId(FulfilmentProvider.FF_SHOP_ID);
            oi.setWarehouseId(FulfilmentProvider.TEST_WAREHOUSE_ID);
            oi.setFulfilmentWarehouseId(FulfilmentProvider.TEST_WAREHOUSE_ID.longValue());
            oi.setWeight(10L);
            oi.setWidth(10L);
            oi.setHeight(10L);
            oi.setDepth(10L);
        });
        //пустая доставка
        order.setDelivery(new Delivery());

        final Delivery deliveryOption = createDelivery();
        final List<DeliveryResponse> deliveryOptions = Collections.singletonList(createDeliveryOption(deliveryOption));
        order.setDeliveryOptions(deliveryOptions);

        CartResponse cartResponse = new CartResponse();
        cartResponse.setItems(
                buildOrderItems(order)
        );
        cartResponse.getItems().forEach(i -> i.setDelivery(true));

        cartResponse.setDeliveryCurrency(Currency.RUR);
        cartResponse.setDeliveryOptions(deliveryOptions);

        ArgumentCaptor<Cart> pushApiCart = ArgumentCaptor.forClass(Cart.class);

        configure(PushApi.class, pa -> doReturn(cartResponse)
                .when(pa).cart(anyLong(), anyLong(), pushApiCart.capture(), anyBoolean(), any(Context.class),
                        any(ApiSettings.class), anyString()
                ));

        cartActualizer.actualizeCart(order, prepareContext(order, order.getItems().stream()
                .map(FoundOfferBuilder::createFrom)
                .map(b -> b.deliveryPartnerType(DeliveryPartnerType.SHOP.name())
                        .shopId(order.getShopId())
                        .cpa("real")
                        .isFulfillment(false)
                        .atSupplierWarehouse(true)
                        .build())
                .collect(Collectors.toUnmodifiableList()), true, null).build());
        assertEquals(deliveryOption.getDeliveryPartnerType(),
                pushApiCart.getValue().getDelivery().getDeliveryPartnerType());
    }

    @Test
    public void shouldChangeOrderAcceptMethodForShopAdmin() {
        Order customerOrder = new TestCustomerOrder().build();
        assertEquals(OrderAcceptMethod.DEFAULT, customerOrder.getAcceptMethod(),
                "Новый заказ имеет нестандартный способ обработки");

        CartResponse shopOrder = new TestShopOrder().withItems(customerOrder.getItems()).build();
        shopOrder.setShopAdmin(true);

        configureItemActualizer();
        configureYandexPayment();
        configure(PushApi.class, pa -> doReturn(shopOrder)
                .when(pa)
                .cart(eq(customerOrder.getShopId()), eq(customerOrder.getBuyer().getUid()), any(Cart.class),
                        eq(false), any(Context.class), any(ApiSettings.class), anyString()
                ));
        cartActualizer.actualizeCart(customerOrder, prepareContext(customerOrder, List.of(), true, null).build());
        assertEquals(OrderAcceptMethod.WEB_INTERFACE, customerOrder.getAcceptMethod(),
                "Метод приема заказа должен быть админкой");
    }

    private void configureItemActualizer() {
        configure(
                ItemActualizer.class,
                ia -> doAnswer(inv -> inv.getArguments()[0])
                        .when(ia).actualizeItem(
                                any(),
                                anyLong(),
                                any(ApiSettings.class),
                                anyBoolean(),
                                any(),
                                any(),
                                anyLong(),
                                anyString(),
                                any()
                        )
        );
    }

    private void configureYandexPayment() {
        configure(ShopService.class, (ss) -> {
            ShopMetaData shopMetaData = Mockito.mock(ShopMetaData.class);
            Mockito.when(shopMetaData.getPaymentClass(true))
                    .thenReturn(PaymentClass.YANDEX);

            doReturn(shopMetaData)
                    .when(ss).getMeta(TestCustomerOrder.DEFAULT_SHOP_ID, ShopMetaData.DEFAULT);

        });
    }

    private void configureRegionTree(int regionId) {
        Map<Integer, List<Integer>> tree = Map.of(
                213, List.of(213, 1, 3, 225, 10001, 10000),
                114620, List.of(114620, 213, 1, 3, 225, 10001, 10000),
                2, List.of(2, 10174, 17, 225, 10001, 10000)
        );

        RegionTree regionTree = mock(RegionTree.class);
        when(regionTree.getPathToRoot(eq(regionId))).thenReturn(tree.getOrDefault(regionId, List.of(regionId)));

        configure(RegionService.class, service ->
                doReturn(regionTree).when(service).getRegionTree()
        );
    }

    @Test
    public void shouldFailIfDifferentDeliveryInReport() {
        Order order = new TestCustomerOrder().build();
        order.getItem(FEED_OFFER_ID).setDelivery(true);

        CartResponse cartResponse = new CartResponse();
        cartResponse.setItems(buildOrderItems(order));

        configure(PushApi.class, pa -> {
            doReturn(cartResponse)
                    .when(pa).cart(anyLong(), anyLong(), any(Cart.class), anyBoolean(), any(Context.class),
                    any(ApiSettings.class), anyString()
            );
        });
        configureYandexPayment();
        configureItemActualizer();

        assertFalse(cartActualizer.actualizeCart(order, prepareContext(order,
                order.getItems().stream()
                        .map(FoundOfferBuilder::createFrom)
                        .map(b -> b.shopId(order.getShopId())
                                .cpa("real")
                                .isFulfillment(false)
                                .atSupplierWarehouse(true)
                                .build())
                        .collect(Collectors.toUnmodifiableList()), true, null).build()));
        OrderItem item = order.getItem(FEED_OFFER_ID);
        assertEquals(1, item.getChanges().size());
        assertEquals(ItemChange.DELIVERY, Iterables.getOnlyElement(item.getChanges()));
    }

    @Test
    public void shouldFillActualDeliveryForBlueMarket() {
        ActualDelivery thisActualDelivery = new ActualDelivery();

        Order order = new TestCustomerOrder().build();
        order.setFulfilment(true);
        order.setRgb(Color.BLUE);
        order.getItems().forEach(oi -> {
            oi.setShopSku(FulfilmentProvider.TEST_SHOP_SKU);
            oi.setSupplierId(FulfilmentProvider.FF_SHOP_ID);
            oi.setWarehouseId(FulfilmentProvider.TEST_WAREHOUSE_ID);
            oi.setFulfilmentWarehouseId(FulfilmentProvider.TEST_WAREHOUSE_ID.longValue());
        });

        CartResponse cartResponse = new CartResponse();
        cartResponse.setItems(
                buildOrderItems(order)
        );
        cartResponse.getItems().forEach(i -> i.setDelivery(true));

        configure(PersonalDataService.class, personalDataService -> {
            PersAddress persAddress = PersAddress.convertToPersonal(order.getDelivery().getBuyerAddress());

            when(personalDataService.retrieve(any()))
                    .thenReturn(new PersonalDataRetrieveResult(null, null, null, persAddress, null));

            when(personalDataService.getPersAddress(any()))
                    .thenReturn(persAddress);
        });

        configure(PushApi.class, pa -> {
            doReturn(cartResponse)
                    .when(pa).cart(anyLong(), anyLong(), any(Cart.class), anyBoolean(), any(Context.class),
                    any(ApiSettings.class), anyString()
            );
        });
        configureYandexPayment();
        configureItemActualizer();

        var actualizationContext = prepareContext(order, List.of(), true, thisActualDelivery).build();

        cartActualizer.actualizeCart(order, actualizationContext);
        assertSame(thisActualDelivery, actualizationContext.getActualDelivery());
    }

    @ParameterizedTest
    @CsvSource({
//            "114620,true,213,true,false", //114620 (Троицк) входит в 213 (Москва)
            "114620,false,213,true,true", //114620 (Троицк) входит в 213 (Москва)
            "114620,true,213,false,false", //114620 (Троицк) входит в 213 (Москва)
            "114620,false,213,false,true", //114620 (Троицк) входит в 213 (Москва)
            "2,true,213,true,false", //2 не входит в 213
            "2,false,213,true,true", //2 не входит в 213
            "2,true,213,false,false", //2 не входит в 213
            "2,false,213,false,true", //2 не входит в 213
    })
    public void shouldFillActualDeliveryForWhiteMarketWhenNoExactMatchInSettings(
            int deliveryRegion, boolean isPushApiActualizationDelivery, int otherRegion,
            boolean isPushApiActualizationOther, boolean isActualDelivery) {
        ActualDelivery thisActualDelivery = new ActualDelivery();

        Order order = new TestCustomerOrder().build();
        order.setFulfilment(false);
        order.setIgnoreStocks(true);
        order.setRgb(Color.WHITE);
        order.setPriorityRegionId((long) otherRegion);
        order.getDelivery().setRegionId((long) deliveryRegion);
        order.getItems().forEach(oi -> {
            oi.setShopSku(FulfilmentProvider.TEST_SHOP_SKU);
            oi.setSupplierId(FulfilmentProvider.FF_SHOP_ID);
            oi.setWarehouseId(FulfilmentProvider.TEST_WAREHOUSE_ID);
            oi.setFulfilmentWarehouseId(FulfilmentProvider.TEST_WAREHOUSE_ID.longValue());
            oi.setWeight(10L);
            oi.setWidth(10L);
            oi.setHeight(10L);
            oi.setDepth(10L);
        });

        CartResponse cartResponse = new CartResponse();
        cartResponse.setItems(
                buildOrderItems(order)
        );
        cartResponse.getItems().forEach(i -> i.setDelivery(true));

        configure(PersonalDataService.class, personalDataService -> {
            PersAddress persAddress = PersAddress.convertToPersonal(order.getDelivery().getBuyerAddress());

            when(personalDataService.retrieve(any()))
                    .thenReturn(new PersonalDataRetrieveResult(null, null, null, persAddress, null));

            when(personalDataService.getPersAddress(any())).thenReturn(persAddress);
        });
        configure(PushApi.class, pa -> {
            doReturn(cartResponse)
                    .when(pa).cart(anyLong(), anyLong(), any(Cart.class), anyBoolean(), any(Context.class),
                    any(ApiSettings.class), anyString()
            );
        });
        configureYandexPayment();
        configureItemActualizer();
        configureRegionTree(deliveryRegion);

        ShopActualDeliveryRegionalSettings[] deliveryRegionalSettings = {
                new ShopActualDeliveryRegionalSettings(otherRegion, isPushApiActualizationOther),
                new ShopActualDeliveryRegionalSettings(deliveryRegion, isPushApiActualizationDelivery)
        };

        configure(ShopService.class, service ->
                doReturn(new ShopMetaData(-1L, -1L, -1L, PaymentClass.OFF, PaymentClass.OFF, null, null, null,
                        null, null, null, null, null, null, false, false, false,
                        false, deliveryRegionalSettings, null, false, false, false))
                        .when(service)
                        .getMeta(anyLong())
        );

        var actualizationContext = prepareContext(order, List.of(), isActualDelivery, thisActualDelivery).build();
        cartActualizer.actualizeCart(order, actualizationContext);
        assertSame(isActualDelivery ? thisActualDelivery : null, actualizationContext.getActualDelivery());
    }

    @ParameterizedTest
    @CsvSource({
            "213,true,false",
            "213,false,true"
    })
    public void shouldFillActualDeliveryForWhiteMarketWhenExactMatchInSettings(
            int regionId, boolean isPushApiActualization, boolean isActualDelivery) {
        ActualDelivery thisActualDelivery = new ActualDelivery();

        Order order = new TestCustomerOrder().build();
        order.setFulfilment(true);
        order.setRgb(Color.WHITE);
        order.setPriorityRegionId((long) regionId);
        order.getDelivery().setRegionId((long) regionId);
        order.getItems().forEach(oi -> {
            oi.setShopSku(FulfilmentProvider.TEST_SHOP_SKU);
            oi.setSupplierId(FulfilmentProvider.FF_SHOP_ID);
            oi.setWarehouseId(FulfilmentProvider.TEST_WAREHOUSE_ID);
            oi.setFulfilmentWarehouseId(FulfilmentProvider.TEST_WAREHOUSE_ID.longValue());
            oi.setWeight(10L);
            oi.setWidth(10L);
            oi.setHeight(10L);
            oi.setDepth(10L);
        });

        CartResponse cartResponse = new CartResponse();
        cartResponse.setItems(
                buildOrderItems(order)
        );
        cartResponse.getItems().forEach(i -> i.setDelivery(true));

        configure(PersonalDataService.class, personalDataService -> {
            PersAddress persAddress = PersAddress.convertToPersonal(order.getDelivery().getBuyerAddress());

            when(personalDataService.retrieve(any()))
                    .thenReturn(new PersonalDataRetrieveResult(null, null, null, persAddress, null));

            when(personalDataService.getPersAddress(any())).thenReturn(persAddress);
        });

        configure(PushApi.class, pa -> {
            doReturn(cartResponse)
                    .when(pa).cart(anyLong(), anyLong(), any(Cart.class), anyBoolean(), any(Context.class),
                    any(ApiSettings.class), anyString()
            );
        });
        configureYandexPayment();
        configureItemActualizer();
        configureRegionTree(regionId);

        ShopActualDeliveryRegionalSettings[] deliveryRegionalSettings = {
                new ShopActualDeliveryRegionalSettings(regionId, isPushApiActualization)
        };

        configure(ShopService.class, service ->
                doReturn(new ShopMetaData(-1L, -1L, -1L, PaymentClass.OFF, PaymentClass.OFF, null, null, null,
                        null, null, null, null, null, null, false, false,
                        false, false, deliveryRegionalSettings, null, false, false, false))
                        .when(service)
                        .getMeta(anyLong())
        );

        var actualizationContext = prepareContext(order, List.of(), isActualDelivery,
                thisActualDelivery).build();
        cartActualizer.actualizeCart(order, actualizationContext);
        assertSame(isActualDelivery ? thisActualDelivery : null, actualizationContext.getActualDelivery());
    }

    @Test
    public void shouldSetDeliveryForBlueMarketWithoutFulfilment() {
        ActualDelivery thisActualDelivery = new ActualDelivery();

        Delivery delivery = createDelivery();

        Order order = new TestCustomerOrder().build();
        order.setDelivery(delivery);
        order.setRgb(Color.BLUE);
        order.setFulfilment(false);
        order.getItems().forEach(oi -> {
            oi.setShopSku(FulfilmentProvider.TEST_SHOP_SKU);
            oi.setSupplierId(FulfilmentProvider.FF_SHOP_ID);
            oi.setWarehouseId(FulfilmentProvider.TEST_WAREHOUSE_ID);
        });

        CartResponse cartResponse = new CartResponse();
        cartResponse.setItems(
                buildOrderItems(order)
        );
        cartResponse.getItems().forEach(i -> i.setDelivery(true));

        cartResponse.setDeliveryCurrency(Currency.RUR);
        cartResponse.setDeliveryOptions(Collections.singletonList(createDeliveryOption(delivery)));

        configure(PushApi.class, pa -> {
            doReturn(cartResponse)
                    .when(pa).cart(anyLong(), anyLong(), any(Cart.class), anyBoolean(), any(Context.class),
                    any(ApiSettings.class), anyString()
            );
        });
        configureYandexPayment();
        configureItemActualizer();

        var multiCart = MultiCartProvider.single(order);
        var multiCartContext = MultiCartContext.createBy(
                multiCartParameters,
                multiCart
        );
        var actualizationContextBuilder = ActualizationContext.builder()
                .withOriginalBuyerCurrency(Currency.RUR)
                .withImmutableMulticartContext(ImmutableMultiCartContext.from(multiCartContext, multiCart))
                .withCart(order)
                .withInitialCart(ImmutableOrder.from(order));
        applyActualizationMutations(CartFetchingContext.of(
                MultiCartFetchingContext.of(multiCartContext, multiCart),
                actualizationContextBuilder, order));

        var actualizationContext = prepareContext(order, List.of(), true, thisActualDelivery).build();

        assertTrue(cartActualizer.actualizeCart(order, actualizationContext));
        assertSame(thisActualDelivery, actualizationContext.getActualDelivery());

        OrderItem item = order.getItem(FEED_OFFER_ID);
        assertNull(item.getChanges());

    }

    private DeliveryResponse createDeliveryOption(Delivery delivery) {
        DeliveryResponse deliveryResponse = new DeliveryResponse();
        deliveryResponse.setType(DeliveryType.PICKUP);
        deliveryResponse.setDeliveryServiceId(delivery.getDeliveryServiceId());
        deliveryResponse.setDeliveryPartnerType(delivery.getDeliveryPartnerType());
        deliveryResponse.setPrice(new BigDecimal("1.11"));
        deliveryResponse.setServiceName(delivery.getServiceName());
        deliveryResponse.setOutletIds(Set.of(111L));
        deliveryResponse.setOutletCodes(Collections.singleton("111"));
        deliveryResponse.setDeliveryDates(delivery.getDeliveryDates());
        return deliveryResponse;
    }

    private Delivery createDelivery() {
        Delivery delivery = new Delivery();
        delivery.setType(DeliveryType.PICKUP);
        delivery.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        delivery.setServiceName("Foreign Service");
        delivery.setPrice(BigDecimal.valueOf(1.11));
        delivery.setBuyerPrice(BigDecimal.valueOf(1.11));
        delivery.setDeliveryServiceId(99L);
        return delivery;
    }

    private List<OrderItem> buildOrderItems(Order order) {
        return order.getItems().stream().map(oi -> {
                    OrderItem orderItem = new OrderItem();
                    orderItem.setCount(oi.getCount());
                    orderItem.setDelivery(false);
                    orderItem.setFeedOfferId(oi.getFeedOfferId());
                    return orderItem;
                }
        ).collect(Collectors.toList());
    }

    private <T> void configure(Class<T> clazz, Consumer<T> configure) {
        T bean = applicationContext.getBean(clazz);
        configure.accept(bean);
    }

    @Test
    public void shouldFetchActualDeliveryForWhiteMarketWhenUnifiedTariffsEnabled() {
        checkouterFeatureWriter.writeValue(ASYNC_ACTUAL_DELIVERY_REQUEST, true);
        Order order = new TestCustomerOrder().build();
        //только для того чтоб скипнуть запрос в пуш апи
        order.setFulfilment(true);
        order.setRgb(Color.WHITE);
        order.setPriorityRegionId((long) 213);
        order.getDelivery().setRegionId((long) 213);
        order.getDelivery().setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        configureRegionTree(213);
        ShopActualDeliveryRegionalSettings[] deliveryRegionalSettings = {
                new ShopActualDeliveryRegionalSettings(213, true)
        };
        configure(ShopService.class, service ->
                doReturn(new ShopMetaData(-1L, -1L, -1L, PaymentClass.OFF, PaymentClass.OFF, null, null, null,
                        null, null, null, null, null, null, false,
                        false, false, false, deliveryRegionalSettings, null, false, false, false))
                        .when(service)
                        .getMeta(anyLong())
        );

        var actualizationContext = prepareContext(order, List.of(), true, null).build();
        cartActualizer.actualizeCart(order, actualizationContext);
    }

    @ParameterizedTest
    @CsvSource({
            "true,true"
    })
    public void shouldNotFetchActualDeliveryForWhiteMarketWhenUnifiedTariffsDisabled(
            boolean enableUnifiedTariffs,
            boolean isEda
    ) {
        checkouterFeatureWriter.writeValue(ASYNC_ACTUAL_DELIVERY_REQUEST, true);
        checkouterFeatureWriter.writeValue(ENABLED_FETCHERS, Set.of());
        Order order = new TestCustomerOrder().build();
        order.setFulfilment(true);
        order.setRgb(Color.WHITE);
        order.setPriorityRegionId((long) 213);
        order.getDelivery().setRegionId((long) 213);
        order.getDelivery().setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        OrderItem orderItem = OrderItemProvider.buildOrderItem(new FeedOfferId("1", 1L));
        order.setItems(Collections.singletonList(orderItem));
        FoundOffer foundOffer = new FoundOffer();
        foundOffer.setShopOfferId("1");
        foundOffer.setFeedId(1L);
        foundOffer.setIsEda(isEda);
        foundOffer.setDeliveryPartnerTypes(List.of("SHOP"));
        configureRegionTree(213);
        ShopActualDeliveryRegionalSettings[] deliveryRegionalSettings = {
                new ShopActualDeliveryRegionalSettings(213, true)
        };
        configure(PersonalDataService.class, personalDataService -> {
            PersAddress persAddress = PersAddress.convertToPersonal(order.getDelivery().getBuyerAddress());

            when(personalDataService.retrieve(any()))
                    .thenReturn(new PersonalDataRetrieveResult(null, null, null, persAddress, null));

            when(personalDataService.getPersAddress(any())).thenReturn(persAddress);
        });
        configure(ShopService.class, service ->
                doReturn(new ShopMetaData(-1L, -1L, -1L, PaymentClass.OFF, PaymentClass.OFF, null, null, null,
                        null, null, null, null, null, null, false,
                        false, false, false, deliveryRegionalSettings, null, false, false, false))
                        .when(service)
                        .getMeta(anyLong())
        );

        var actualizationContext = prepareContext(order, List.of(foundOffer), false, null).build();
        cartActualizer.actualizeCart(order, actualizationContext);

        assertTrue(actualizationContext.isSkipActualDelivery());
    }

    @ParameterizedTest
    @CsvSource({
            "true,false,true",
            "false,false,false",
            "false,true,false",
            "true,true,false",
    })
    public void shouldPassOptionForWhiteMarketWhenUnifiedTariffsEnabledAndIsNotEda(
            boolean enableUnifiedTariffs,
            boolean isEda,
            boolean expectedForceWhiteOfferOptions) {
        Order order = new TestCustomerOrder().build();
        order.setFulfilment(true);
        order.setRgb(Color.WHITE);
        order.setPriorityRegionId((long) 213);
        order.getDelivery().setRegionId((long) 213);
        order.getDelivery().setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        OrderItem orderItem = OrderItemProvider.buildOrderItem(new FeedOfferId("1", 1L));
        order.setItems(Collections.singletonList(orderItem));
        FoundOffer foundOffer = new FoundOffer();
        foundOffer.setShopOfferId("1");
        foundOffer.setFeedId(1L);
        foundOffer.setIsEda(isEda);
        configureRegionTree(213);
        ShopActualDeliveryRegionalSettings[] deliveryRegionalSettings = {
                new ShopActualDeliveryRegionalSettings(213, true)
        };
        configure(PersonalDataService.class, personalDataService -> {
            PersAddress persAddress = PersAddress.convertToPersonal(order.getDelivery().getBuyerAddress());

            when(personalDataService.retrieve(any()))
                    .thenReturn(new PersonalDataRetrieveResult(null, null, null, persAddress, null));

            when(personalDataService.getPersAddress(any())).thenReturn(persAddress);
        });

        configure(ShopService.class, service ->
                doReturn(new ShopMetaData(-1L, -1L, -1L, PaymentClass.OFF, PaymentClass.OFF, null, null, null,
                        null, null, null, null, null, null, false,
                        false, false, false, deliveryRegionalSettings, null, false, false, false))
                        .when(service)
                        .getMeta(anyLong())
        );

        var actualizationContext = prepareContext(order, List.of(foundOffer), enableUnifiedTariffs, null).build();
        cartActualizer.actualizeCart(order, actualizationContext);

        assertEquals(expectedForceWhiteOfferOptions, actualizationContext.isForceWhiteOfferOptions());
    }

    @ParameterizedTest
    @CsvSource({"true", "false"})
    public void shouldNotFetchActualDeliveryForTurboPlus(boolean enableUnifiedTariffs) {
        checkouterFeatureWriter.writeValue(ASYNC_ACTUAL_DELIVERY_REQUEST, true);
        Order order = new TestCustomerOrder().build();
        order.setFulfilment(true);
        order.setRgb(Color.TURBO_PLUS);
        order.setPriorityRegionId((long) 213);
        order.getDelivery().setRegionId((long) 213);
        order.getDelivery().setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        OrderItem orderItem = OrderItemProvider.buildOrderItem(new FeedOfferId("1", 1L));
        order.setItems(Collections.singletonList(orderItem));
        FoundOffer foundOffer = new FoundOffer();
        foundOffer.setShopOfferId("1");
        foundOffer.setFeedId(1L);
        configureRegionTree(213);
        ShopActualDeliveryRegionalSettings[] deliveryRegionalSettings = {
                new ShopActualDeliveryRegionalSettings(213, true)
        };
        configure(ShopService.class, service ->
                doReturn(new ShopMetaData(-1L, -1L, -1L, PaymentClass.OFF, PaymentClass.OFF, null, null, null,
                        null, null, null, null, null, null, false,
                        false, false, false, deliveryRegionalSettings, null, false, false, false))
                        .when(service)
                        .getMeta(anyLong())
        );

        var actualizationContext = prepareContext(order, List.of(foundOffer), enableUnifiedTariffs, null).build();
        cartActualizer.actualizeCart(order, actualizationContext);
    }

    @ParameterizedTest
    @CsvSource({"true", "false"})
    public void shouldNotFetchActualDeliveryForTurbo(boolean enableUnifiedTariffs) {
        checkouterFeatureWriter.writeValue(ASYNC_ACTUAL_DELIVERY_REQUEST, true);
        Order order = new TestCustomerOrder().build();
        order.setFulfilment(true);
        order.setRgb(Color.TURBO);
        order.setPriorityRegionId((long) 213);
        order.getDelivery().setRegionId((long) 213);
        order.getDelivery().setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        OrderItem orderItem = OrderItemProvider.buildOrderItem(new FeedOfferId("1", 1L));
        order.setItems(Collections.singletonList(orderItem));
        FoundOffer foundOffer = new FoundOffer();
        foundOffer.setShopOfferId("1");
        foundOffer.setFeedId(1L);
        configureRegionTree(213);
        ShopActualDeliveryRegionalSettings[] deliveryRegionalSettings = {
                new ShopActualDeliveryRegionalSettings(213, true)
        };
        configure(ShopService.class, service ->
                doReturn(new ShopMetaData(-1L, -1L, -1L, PaymentClass.OFF, PaymentClass.OFF, null, null, null,
                        null, null, null, null, null, null, false,
                        false, false, false, deliveryRegionalSettings, null, false, false, false))
                        .when(service)
                        .getMeta(anyLong())
        );

        var actualizationContext = prepareContext(order, List.of(foundOffer), enableUnifiedTariffs, null).build();
        cartActualizer.actualizeCart(order, actualizationContext);
    }

    @ParameterizedTest
    @CsvSource({"true", "false"})
    public void shouldAlwaysFetchActualDeliveryForBlue(boolean enableUnifiedTariffs) {
        checkouterFeatureWriter.writeValue(ASYNC_ACTUAL_DELIVERY_REQUEST, true);
        Order order = new TestCustomerOrder().build();
        order.setFulfilment(true);
        order.setRgb(Color.BLUE);
        order.setPriorityRegionId((long) 213);
        order.getDelivery().setRegionId((long) 213);
        order.getDelivery().setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        OrderItem orderItem = OrderItemProvider.buildOrderItem(new FeedOfferId("1", 1L));
        order.setItems(Collections.singletonList(orderItem));
        FoundOffer foundOffer = new FoundOffer();
        foundOffer.setShopOfferId("1");
        foundOffer.setFeedId(1L);
        configureRegionTree(213);
        ShopActualDeliveryRegionalSettings[] deliveryRegionalSettings = {
                new ShopActualDeliveryRegionalSettings(213, true)
        };
        configure(ShopService.class, service ->
                doReturn(new ShopMetaData(-1L, -1L, -1L, PaymentClass.OFF, PaymentClass.OFF, null, null, null,
                        null, null, null, null, null, null, false,
                        false, false, false, deliveryRegionalSettings, null, false, false, false))
                        .when(service)
                        .getMeta(anyLong())
        );

        var actualizationContext = prepareContext(order, List.of(foundOffer), enableUnifiedTariffs, null).build();
        cartActualizer.actualizeCart(order, actualizationContext);
    }

    private Experiments enableUnifiedTariffs(boolean enable) {
        checkouterFeatureWriter.writeValue(ENABLE_UNIFIED_TARIFFS, enable);
        final Experiments experiments;
        if (enable) {
            experiments = getExperiments().with(Map.of(MARKET_UNIFIED_TARIFFS, MARKET_UNIFIED_TARIFFS_VALUE));
        } else {
            experiments = getExperiments();
        }
        CheckoutContextHolder.setExperiments(experiments);
        return experiments;
    }

    @Test
    public void shouldFailIfDifferentServicePriceInReport() throws Throwable {
        Order order = new TestCustomerOrder().build();
        OrderItem orderItem = order.firstItemFor(FEED_OFFER_ID);
        ItemService itemService = ItemServiceProvider.defaultItemService();
        orderItem.setServices(Set.of(itemService));

        FoundOffer foundOffer = FoundOfferBuilder.createFrom(orderItem)
                .shopId(order.getShopId())
                .cpa("yes")
                .deliveryPartnerType("SHOP")
                .services(List.of(
                        OfferServiceBuilder.createFrom(itemService)
                                .price(BigDecimal.valueOf(100)) // changed price
                                .build()
                ))
                .build();

        CartResponse shopOrder = new TestShopOrder().withItems(order.getItems()).build();
        configure(PushApi.class, pa -> {
            doReturn(shopOrder)
                    .when(pa)
                    .cart(eq(order.getShopId()), eq(order.getBuyer().getUid()), any(Cart.class),
                            eq(false), any(Context.class), any(ApiSettings.class), anyString()
                    );
        });
        configure(PersonalDataService.class, personalDataService -> {
            PersAddress persAddress = PersAddress.convertToPersonal(order.getDelivery().getBuyerAddress());

            when(personalDataService.retrieve(any()))
                    .thenReturn(new PersonalDataRetrieveResult(null, null, null,
                            persAddress, null));

            when(personalDataService.getPersAddress(any())).thenReturn(persAddress);
        });


        var actualizationContext = prepareContext(order, List.of(foundOffer), true, null).build();
        assertFalse(cartActualizer.actualizeCart(order, actualizationContext));
        assertThat(order.firstItemFor(FEED_OFFER_ID).getChanges(), contains(ItemChange.SERVICE_PRICE));
    }

    @Test
    public void shouldPassIfSameServicePriceInReport() {
        Order order = new TestCustomerOrder().build();
        OrderItem orderItem = order.firstItemFor(FEED_OFFER_ID);
        ItemService itemService = ItemServiceProvider.defaultItemService();
        orderItem.setServices(Set.of(itemService));

        FoundOffer foundOffer = FoundOfferBuilder.createFrom(orderItem)
                .shopId(order.getShopId())
                .cpa("yes")
                .deliveryPartnerType("SHOP")
                .services(List.of(OfferServiceBuilder.createFrom(itemService).build()))
                .build();

        CartResponse shopOrder = new TestShopOrder().withItems(order.getItems()).build();
        configure(PushApi.class, pa -> {
            doReturn(shopOrder)
                    .when(pa)
                    .cart(eq(order.getShopId()), eq(order.getBuyer().getUid()), any(Cart.class),
                            eq(false), any(Context.class), any(ApiSettings.class), anyString()
                    );
        });

        assertTrue(cartActualizer.actualizeCart(order, prepareContext(order, List.of(foundOffer), true, null).build()));
    }

    private void applyActualizationMutations(CartFetchingContext cartFetchingContext) {
        cartFetchingContext.getActualizationContextBuilder()
                .withPushApiCartStage(pushApiCartResponseFetcher);
        CartFlowFactory.allCompleteOf(CartFlowFactory.fetch(deliveryAddressFetcher)
                .mutate(orderDeliveryAddressMutation)
                .mutate(orderPropertiesMutation)
                .mutate(actualDeliveryFlagsMutation)
                .mutate(orderDeliveryPartnerTypeMutation)
                //идем в апи магазина
                .whenSuccess(CartFlowFactory.fetch(actualDeliveryDraftFetcher))
                .whenSuccess(CartFlowFactory.fetch(pushApiCartResponseFetcher)
                        .mutate(orderAcceptMethodMutation))
        )
                .mutate(itemsPriceAndCountMutation)
                .whenSuccess(CartFlowFactory.fetch(actualDeliveryFetcher)
                        .mutate(actualDeliveryMutation))
                .apply(cartFetchingContext).awaitChildrenSilently();
    }

    ActualizationContext.ActualizationContextBuilder prepareContext(Order order,
                                                                    List<FoundOffer> offers,
                                                                    boolean enableUnifiedTariffs,
                                                                    ActualDelivery actualDelivery) {
        if (actualDelivery != null) {
            configure(MarketReportSearchService.class, searchService -> {
                doReturn(actualDelivery)
                        .when(searchService)
                        .searchActualDelivery(
                                any(ActualDeliveryRequestBuilder.class));
            });
        }

        var multiCart = MultiCartProvider.single(order);
        var multiCartContext = MultiCartContext.createBy(
                multiCartParameters,
                multiCart
        );
        if (CollectionUtils.isEmpty(offers)) {
            setOffers(multiCartContext, multiCart);
        } else {
            setOffers(multiCartContext, multiCart, offers);
        }
        var multiCartFetchingContext = MultiCartFetchingContext.of(multiCartContext, multiCart);
        FlowSessionHelper.patchSession(
                multiCartFetchingContext,
                MultiCartFetchingContext::makeImmutableContext,
                (c, v) -> c.getMultiCartContext().setDeliveryAddressRegionPreciseStage(v),
                Map.<String, Address>of()
        );
        FlowSessionHelper.patchSession(
                multiCartFetchingContext,
                MultiCartFetchingContext::makeImmutableContext,
                (c, v) -> c.getMultiCartContext().setPresetsGeocodingStage(v),
                Map.<String, Long>of()
        );

        orderItemsOfferDataMutation.onSuccess(
                multiCartFetchingContext.makeImmutableContext().getMultiCartOffers(), multiCartFetchingContext);

        var actualizationContextBuilder = ActualizationContext.builder()
                .withOriginalBuyerCurrency(Currency.RUR)
                .withImmutableMulticartContext(ImmutableMultiCartContext.from(multiCartContext, multiCart))
                .withCart(order)
                .withInitialCart(ImmutableOrder.from(order))
                .withExperiments(enableUnifiedTariffs(enableUnifiedTariffs))
                .withPushApiCartStage(pushApiCartResponseFetcher)
                .withActualDeliveryStage(actualDeliveryFetcher);
        var cartFetchingContext = CartFetchingContext.of(multiCartFetchingContext,
                actualizationContextBuilder, order);

        applyActualizationMutations(cartFetchingContext);
        return actualizationContextBuilder;
    }

    @Profile("actualizer-test")
    @Import(ActualizeLoggingSerializationConfig.class)
    @ImportResource("classpath:WEB-INF/checkouter-serialization.xml")
    @Configuration
    public static class CartActualizerTestConfiguration {

        public <T extends Actualizer> T createActualizerMock(Class<T> actualizerClass) {
            T actualizerMock = createMock(actualizerClass);
            doReturn(true)
                    .when(actualizerMock).canApply(any(), any(), any());
            doReturn(true)
                    .when(actualizerMock).doActualize(any(), any(), any());
            return actualizerMock;
        }

        @Bean
        public OrderItemInflater orderItemInflater(ColorConfig colorConfig) {
            return new ReportOrderItemInflater(
                    mock(HsCodeToMaterialMapping.class),
                    colorConfig,
                    mock(CancelPolicyDataValidationService.class)
            );
        }

        @Bean
        public OrderItemsOfferDataMutation offerDataPrepareSubscriber(OrderItemInflater orderItemInflater,
                                                                      CheckouterFeatureReader checkouterFeatureReader,
                                                                      ColorConfig colorConfig) {
            return new OrderItemsOfferDataMutation(
                    orderItemInflater,
                    new FinancialValidator(),
                    checkouterFeatureReader,
                    colorConfig
            );
        }

        @Bean
        public ActualDeliveryFlagsMutation actualDeliveryFlagsMutation(CheckouterFeatureReader checkouterFeatureReader,
                                                                       ColorConfig colorConfig,
                                                                       ShopService shopService,
                                                                       RegionService regionService) {
            return new ActualDeliveryFlagsMutation(colorConfig, checkouterFeatureReader, shopService, regionService);
        }

        @Bean
        public MarketReportInfoFetcher getMarketReportInfoFetcher() {
            return createMock(MarketReportInfoFetcher.class);
        }

        @Bean
        public MarketReportSearchService getMarketReportSearchService() {
            MarketReportSearchService mock = createMock(MarketReportSearchService.class);
            doReturn(new ActualDelivery())
                    .when(mock).searchActualDelivery(any(ActualDeliveryRequestBuilder.class));
            return mock;
        }

        @Bean
        public PaymentMethodValidator paymentMethodValidator() {
            return createMock(PaymentMethodValidator.class);
        }

        @Bean
        public ItemActualizer itemActualizer() {
            return createMock(ItemActualizer.class);
        }

        @Bean
        public PushApi pushApi() {
            PushApi mock = createMock(PushApi.class);
            doNothing().when(mock).setTvmTicketProvider(any(TvmTicketProvider.class));
            return mock;
        }

        @Bean
        public ShopService shopService() {
            return createMock(ShopService.class);
        }

        @Bean
        public ReportInfoAwareOrderValidator reportInfoAwareOrderValidator() {
            return new ReportInfoAwareOrderValidator(() -> Boolean.FALSE);
        }

        @Bean
        public CheckouterProperties checkouterProperties(
                CheckouterFeatureReader checkouterFeatureReader,
                CheckouterFeatureWriter checkouterFeatureWriter
        ) {
            var result = new CheckouterPropertiesImpl(checkouterFeatureReader, checkouterFeatureWriter);
            result.setSetOrderColorUsingReportOfferColor(true);
            result.setForcePostpaid(CheckouterProperties.ForcePostpaid.DISABLE);
            return result;
        }

        @Bean
        public CheckouterFeatureReader checkouterFeatureReader() {
            return new CheckouterFeatureResolverStub();
        }

        @Bean
        public CheckouterFeatureWriter checkouterFeatureWriter(CheckouterFeatureReader checkouterFeatureReader) {
            return (CheckouterFeatureWriter) checkouterFeatureReader;
        }

        @Bean
        public FlashPromoFeatureSupportHelper flashPromoFeatureSupportHelper(
        ) {
            return new FlashPromoFeatureSupportHelper();
        }

        @Bean
        public BlueSetPromoFeatureSupportHelper blueSetPromoFeatureSupportHelper() {
            return new BlueSetPromoFeatureSupportHelper();
        }

        @Bean
        public ReportOffersFetcher reportOffersFetcher(MarketReportSearchService searchService,
                                                       ColorConfig colorConfig,
                                                       ExecutorService requestExecutor,
                                                       CheckouterProperties checkouterProperties) {
            return new ReportOffersFetcher(searchService, colorConfig, requestExecutor, 1000, checkouterProperties);
        }

        @Bean
        public ItemsCountActualizer itemsActualizer(
                CheckouterFeatureReader checkouterFeatureReader
        ) {
            ColorConfig colorConfig = mock(ColorConfig.class);
            SingleColorConfig singleColorConfig = mock(SingleColorConfig.class);
            when(colorConfig.getFor(any(Order.class))).thenReturn(singleColorConfig);
            doCallRealMethod().when(singleColorConfig).getShopSku(any(OrderItem.class));
            return spy(new ItemsCountActualizer(
                    checkouterFeatureReader,
                    colorConfig));
        }

        @Bean
        public ItemsPriceActualizer itemPriceActualizer(
                FlashPromoFeatureSupportHelper flashPromoFeatureSupportHelper,
                BlueSetPromoFeatureSupportHelper blueSetPromoFeatureSupportHelper,
                CheckouterFeatureReader checkouterFeatureReader
        ) {
            SingleColorConfig singleColorConfig = mock(SingleColorConfig.class);
            doCallRealMethod().when(singleColorConfig).getShopSku(any(OrderItem.class));
            return spy(new ItemsPriceActualizer(
                    createMock(CurrencyRates.class),
                    flashPromoFeatureSupportHelper,
                    blueSetPromoFeatureSupportHelper,
                    checkouterFeatureReader));
        }

        @Bean
        public MissingItemsActualizer missingItemsActualizer() {
            return spy(new MissingItemsActualizer());
        }

        @Bean
        public Tvm2 tvm2() {
            return mock(Tvm2.class);
        }

        @Bean
        public PushApiCartResponseFetcher pushApiCartResponseFetcher(
                Tvm2 tvm2,
                PushApi pushApi,
                BundleItemsJoiner itemsJoiner
        ) {
            return new PushApiCartResponseFetcher(tvm2, pushApi, itemsJoiner);
        }

        @Bean
        public TransliterateService transliterateService() {
            return mock(TransliterateService.class, new ReturnsSmartNulls());
        }

        @Bean
        public GeocodeProvider geocodeProvider(DegradableGeocoderClientDecorator geocoderClient,
                                               GeocodeMemCacheStorageService geocodeMemCacheStorageService,
                                               CheckouterProperties checkouterProperties,
                                               GeoRegionService geoRegionService
        ) {
            return new GeocodeProvider(geocoderClient,
                    geocodeMemCacheStorageService,
                    geoRegionService,
                    checkouterProperties);
        }

        @Bean
        public PersonalDataService personalDataService() {
            return Mockito.mock(PersonalDataService.class);
        }

        @Bean
        public DeliveryAddressFetcher deliveryAddressFetcher(PersonalDataService personalDataService) {
            return new DeliveryAddressFetcher(personalDataService);
        }

        @Bean
        public DeliveryAddressMultiCartFetcher deliveryAddressMultiCartFetcher(
                GeocodeProvider geocodeProvider,
                GeobaseService geobaseService,
                GeoRegionService geoRegionService,
                CheckouterFeatureReader checkouterFeatureReader,
                PersonalDataService personalDataService
        ) {
            return new DeliveryAddressMultiCartFetcher(geocodeProvider, geobaseService, geoRegionService,
                    checkouterFeatureReader, Executors.newFixedThreadPool(3), personalDataService);
        }

        @Bean
        public ActualDeliveryDraftFetcher actualDeliveryDraftFetcher(
                DeliveryFetcher deliveryFetcher
        ) {
            return new ActualDeliveryDraftFetcher(deliveryFetcher);
        }

        @Bean
        public DeliveryRouteFetcher deliveryRouteFetcher(
                DeliveryFetcher deliveryFetcher,
                CheckouterFeatureReader checkouterFeatureReader
        ) {
            return new DeliveryRouteFetcher(deliveryFetcher, checkouterFeatureReader);
        }

        @Bean
        public OrderDeliveryAddressMutation orderDeliveryAddressMutation(TransliterateService transliterateService,
                                                                         PersonalDataService personalDataService) {
            return new OrderDeliveryAddressMutation(transliterateService, personalDataService);
        }

        @Bean
        public OrderPropertiesMutation orderPropertiesMutation() {
            return new OrderPropertiesMutation();
        }

        @Bean
        public OrderDeliveryPartnerTypeMutation orderDeliveryPartnerTypeMutation() {
            return new OrderDeliveryPartnerTypeMutation();
        }

        @Bean
        public OrderAcceptMethodMutation orderAcceptMethodMutation() {
            return new OrderAcceptMethodMutation();
        }

        @Bean
        public BusinessActualizer businessActualizer() {
            SupplierMetaDataService metaDataService = mock(SupplierMetaDataService.class);
            ShopMetaData shopMetaData = Mockito.mock(ShopMetaData.class);
            when(metaDataService.getSupplierMetadata(anyLong()))
                    .thenReturn(shopMetaData);
            when(shopMetaData.getBusinessId()).thenReturn(999L);
            return new BusinessActualizer(metaDataService);
        }

        @Bean
        public ColorConfig colorConfig() {
            List<SingleColorConfig> colorConfigs = new ArrayList<>();
            colorConfigs.add(new WhiteConfig(
                    false,
                    true,
                    "url",
                    "fallbackUrl",
                    Map.of(),
                    mock(BalanceTokenProvider.class),
                    true));
            colorConfigs.add(new BlueConfig(
                    "url",
                    "fallbackUrl",
                    123L,
                    Map.of(),
                    mock(BalanceTokenProvider.class)));
            colorConfigs.add(new TurboPlusConfig(
                    false,
                    "url",
                    "fallbackUrl",
                    Map.of(),
                    mock(BalanceTokenProvider.class)));
            colorConfigs.add(new TurboConfig(
                    false,
                    "url",
                    "fallbackUrl",
                    Map.of(),
                    mock(BalanceTokenProvider.class)));
            colorConfigs.add(new GreenConfig());
            return new ColorConfig(colorConfigs);
        }

        @Bean
        public DeliveryFetcher deliveryFetcher(
                ColorConfig colorConfig,
                CheckouterFeatureReader checkouterFeatureReader,
                PersonalDataService personalDataService
        ) {
            MarketReportSearchService reportSearchService = getMarketReportSearchService();
            TotalWeightAndPriceCalculator totalWeightAndPriceCalculator =
                    new TotalWeightAndPriceCalculator(colorConfig, checkouterFeatureReader);
            return new DeliveryFetcher(
                    new ActualDeliveryAndYaLavkaFacade(reportSearchService, null,
                            checkouterFeatureReader, null, null, personalDataService),
                    new YaLavkaOrderProperties("10:00-14:00", "12:00-16:30", 4),
                    reportSearchService,
                    totalWeightAndPriceCalculator,
                    colorConfig,
                    checkouterFeatureReader,
                    personalDataService
            );
        }

        @Bean
        public ItemsDeliveryActualizer itemsDeliveryActualizer() {
            return new ItemsDeliveryActualizer(mock(ActualItemMemCacheStorageService.class));
        }

        @Bean
        public ServicePriceActualizer servicePriceActualizer() {
            return new ServicePriceActualizer();
        }

        @Bean
        public CurrencyConvertService convertService() {
            CurrencyConvertService mock = mock(CurrencyConvertService.class);
            CurrencyConvertResult currencyConvertResult = new CurrencyConvertResult();
            currencyConvertResult.setValue(BigDecimal.valueOf(100L));
            currencyConvertResult.setCurrencyTo(Currency.RUR);
            when(mock.convert(any(), any(), any())).thenReturn(currencyConvertResult);
            return mock(CurrencyConvertService.class);
        }

        @Bean
        public ExecutorService asyncJsonLoggingExecutor() {
            return mock(ExecutorService.class);
        }

        @Bean
        public ActualDeliveryFetcher actualDeliveryFetcher(ActualDeliveryDraftFetcher actualDeliveryFetcher,
                                                           DeliveryRouteFetcher deliveryRouteFetcher,
                                                           CheckouterFeatureReader checkouterFeatureReader,
                                                           PersonalDataService personalDataService) {
            return new ActualDeliveryFetcher(
                    convertService(),
                    checkouterFeatureReader,
                    actualDeliveryFetcher,
                    deliveryRouteFetcher,
                    personalDataService);
        }

        @Bean
        public ActualDeliveryMutation actualDeliveryMutation() {
            return new ActualDeliveryMutation();
        }

        @Bean
        @SuppressWarnings("checkstyle:ParameterNumber")
        public CartActualizer cartActualizer(
                CheckouterProperties checkouterProperties,
                CheckouterFeatureReader checkouterFeatureReader
        ) {
            return new CartActualizer(
                    new CartActualizersInvoker(
                            businessActualizer(),
                            createActualizerMock(ItemServiceActualizer.class),
                            createActualizerMock(ItemServiceTimeslotsActualizer.class),
                            createActualizerMock(BuyerDeliveryDatesActualizer.class),
                            createActualizerMock(PostOutletActualizer.class),
                            createActualizerMock(DeliveryTariffStatsActualizer.class),
                            itemsDeliveryActualizer(),
                            createActualizerMock(CurrencyActualizer.class),
                            createActualizerMock(ExternalCertificateItemsActualizer.class),
                            createActualizerMock(DeliveryDatesActualizer.class),
                            createActualizerMock(DeliveryOptionIdActualizer.class),
                            createActualizerMock(AddressRegionActualizer.class),
                            createActualizerMock(LiftOptionsActualizer.class),
                            createActualizerMock(DeliveryLiftPriceActualizer.class),
                            createActualizerMock(ReportDeliveryDatesDiffActualizer.class),
                            createActualizerMock(ReportDeliveryOptionsDiffActualizer.class),
                            createActualizerMock(ReportDeliveryPricesDiffActualizer.class),
                            createActualizerMock(ReportPaymentOptionsDiffActualizer.class),
                            createActualizerMock(DeliveryLeaveAtTheDoorActualizer.class),
                            servicePriceActualizer(),
                            checkouterProperties,
                            checkouterFeatureReader,
                            createActualizerMock(BestDeliveryOptionsActualizer.class),
                            createActualizerMock(FakeCartDeliveryActualizer.class)),
                    new CartPostprocessorsInvoker(
                            mock(SetYandexEmployeeCartPostprocessor.class),
                            mock(ReportDiscountPromoCartPostprocessor.class),
                            mock(PostOutletPostprocessor.class),
                            mock(AddressRegionPostprocessor.class),
                            mock(SetPickupDeliveryTimePostprocessor.class),
                            checkouterFeatureReader)
            );
        }

        @Bean
        public GeobaseService geobaseService() {
            return mock(GeobaseService.class);
        }

        @Bean
        public DegradableGeocoderClientDecorator geoClient() {
            return mock(DegradableGeocoderClientDecorator.class);
        }

        @Bean
        public RegionService regionService() {
            return mock(RegionService.class);
        }

        @Bean
        public GeoRegionService geoRegionService() {
            return mock(GeoRegionService.class);
        }

        @Bean
        public BundleItemsJoiner bundleItemsJoiner() {
            return new BundleItemsJoiner();
        }

        @Bean
        public BundlesFeatureSupportHelper bundlesFeatureSupportHelper() {
            return new BundlesFeatureSupportHelper();
        }

        @Bean
        public GeocodeMemCacheStorageService geocodeMemCacheStorageService() {
            return mock(GeocodeMemCacheStorageService.class);
        }

        @Bean
        public SettingsService settingsService() {
            return mock(SettingsService.class);
        }

        @Bean
        public ItemsPriceAndCountMutation itemPriceAndCountMutation(ItemsCountActualizer itemsCountActualizer,
                                                                    MissingItemsActualizer missingItemsActualizer,
                                                                    ItemsPriceActualizer itemPriceActualizer) {
            return spy(new ItemsPriceAndCountMutation(itemsCountActualizer, missingItemsActualizer,
                    itemPriceActualizer));
        }
    }
}
