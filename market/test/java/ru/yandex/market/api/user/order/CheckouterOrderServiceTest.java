package ru.yandex.market.api.user.order;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import ru.yandex.market.api.common.MarketType;
import ru.yandex.market.api.common.client.MarketTypeResolver;
import ru.yandex.market.api.common.client.rules.BlueMobileAndroidReceiptsHackRule;
import ru.yandex.market.api.common.client.rules.BlueMobileApplicationRule;
import ru.yandex.market.api.common.url.params.UrlParamsFactoryImpl;
import ru.yandex.market.api.domain.OfferId;
import ru.yandex.market.api.domain.OfferIdEncodingService;
import ru.yandex.market.api.domain.v2.PerkType;
import ru.yandex.market.api.domain.v2.OfferV2;
import ru.yandex.market.api.domain.v2.PerkStatus;
import ru.yandex.market.api.domain.v2.ShopInfoV2;
import ru.yandex.market.api.domain.v2.Sku;
import ru.yandex.market.api.domain.v2.ThumbnailSize;
import ru.yandex.market.api.domain.v2.WarningInfo;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.common.GenericParams;
import ru.yandex.market.api.internal.common.GenericParamsBuilder;
import ru.yandex.market.api.internal.resizer.ResizerService;
import ru.yandex.market.api.loyallty.PerkService;
import ru.yandex.market.api.shop.ShopInfoService;
import ru.yandex.market.api.supplier.SupplierService;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithMocks;
import ru.yandex.market.api.user.order.builders.DeliveryOptionBuilder;
import ru.yandex.market.api.user.order.builders.MultiCartBuilder;
import ru.yandex.market.api.user.order.builders.MultiCartOrderBuilder;
import ru.yandex.market.api.user.order.builders.MultiCartOrderItemBuilder;
import ru.yandex.market.api.user.order.builders.OrderBuilder;
import ru.yandex.market.api.user.order.builders.OrderItemBuilder;
import ru.yandex.market.api.user.order.builders.OrderOptionsRequestBuilder;
import ru.yandex.market.api.user.order.builders.OrderOptionsRequestDeliveryPointBuilder;
import ru.yandex.market.api.user.order.builders.OrderOptionsRequestOrderItemBuilder;
import ru.yandex.market.api.user.order.builders.OrderOptionsRequestShopOrderBuilder;
import ru.yandex.market.api.user.order.builders.OrderOptionsResponseBuilder;
import ru.yandex.market.api.user.order.builders.OrderOptionsResponseShopOptionsBuilder;
import ru.yandex.market.api.user.order.builders.OutletBuilder;
import ru.yandex.market.api.user.order.builders.ShopOrderItemBuilder;
import ru.yandex.market.api.user.order.checkout.DeliveryPointId;
import ru.yandex.market.api.user.order.checkout.PersistentOrder;
import ru.yandex.market.api.user.order.credit.creditoptionselector.CreditOptionSelector;
import ru.yandex.market.api.user.order.preorder.OrderOptionsRequest;
import ru.yandex.market.api.user.order.preorder.OrderOptionsResponse;
import ru.yandex.market.api.user.order.preorder.WorkScheduleFormat;
import ru.yandex.market.api.util.concurrent.Pipelines;
import ru.yandex.market.api.util.json.JsonSerializer;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.order.ApiSettings;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.checkouter.order.HitRateGroup;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentOptionHiddenReason;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.Receipts;
import ru.yandex.market.checkout.checkouter.request.BasicOrderRequest;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.api.user.order.RequestGeneratorHelper.CHECKOUTED_RESPONSE;
import static ru.yandex.market.api.user.order.RequestGeneratorHelper.CartParametersMatcher;
import static ru.yandex.market.api.user.order.RequestGeneratorHelper.CartParametersMatcherWithYandexEmployeePerk;
import static ru.yandex.market.api.user.order.RequestGeneratorHelper.CartParametersMatcherWithPerks;
import static ru.yandex.market.api.user.order.RequestGeneratorHelper.MultiCartMatcher;
import static ru.yandex.market.api.user.order.RequestGeneratorHelper.MultiOrderMatcher;
import static ru.yandex.market.api.user.order.RequestGeneratorHelper.CartParametersMatcherWithOptionalRules;
import static ru.yandex.market.api.user.order.RequestGeneratorHelper.CartParametersMatcherWithSeparateOrdersCalculation;
import static ru.yandex.market.api.user.order.RequestGeneratorHelper.TEST_MARKET_OFFER_ID;
import static ru.yandex.market.api.user.order.RequestGeneratorHelper.TEST_OFFER_ID;
import static ru.yandex.market.api.user.order.RequestGeneratorHelper.TEST_OFFER_RIGHT_FEE_SHOW;
import static ru.yandex.market.api.user.order.RequestGeneratorHelper.TEST_OFFER_SHOP_ID;
import static ru.yandex.market.api.user.order.RequestGeneratorHelper.TEST_ORDER_ID;
import static ru.yandex.market.api.user.order.RequestGeneratorHelper.TEST_RGBS;
import static ru.yandex.market.api.user.order.RequestGeneratorHelper.TEST_SHOP_FEED_ID;
import static ru.yandex.market.api.user.order.RequestGeneratorHelper.TEST_SHOP_ID;
import static ru.yandex.market.api.user.order.RequestGeneratorHelper.TEST_SHOP_NAME;
import static ru.yandex.market.api.user.order.RequestGeneratorHelper.TEST_SKU_ID;
import static ru.yandex.market.api.user.order.RequestGeneratorHelper.TEST_SUPPLIER_ID;
import static ru.yandex.market.api.user.order.RequestGeneratorHelper.TEST_USER_ID;
import static ru.yandex.market.api.user.order.RequestGeneratorHelper.TEST_USER_REGION_ID;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
@WithMocks
@WithContext
public class CheckouterOrderServiceTest extends BaseTest {
    CheckouterOrderService service;

    @Mock
    CheckouterAPI checkouterAPI;

    @Mock
    JsonSerializer serializer;

    @Mock
    CheckouterOrderConverter converter;

    @Mock
    ShopInfoService shopInfoService;

    @Mock
    MarketTypeResolver marketTypeResolver;

    @Mock
    BlueMobileApplicationRule blueMobileApplicationRule;

    @Mock
    PerkService perkService;

    @Mock
    ResizerService resizerService;

    @Mock
    OfferIdEncodingService offerIdEncodingService;

    @Mock
    SupplierService supplierService;

    @Inject
    UrlParamsFactoryImpl urlParamsFactoryImpl;

    @Mock
    OfferIndexFactory offerIndexFactory;
    @Mock
    OfferIndex offerIndex;

    @Mock
    BlueMobileAndroidReceiptsHackRule blueMobileAndroidReceiptsHackRule;

    @Mock
    CheckouterTvmTicketProvider tvmTicketProvider;

    @Mock
    ColorMapper colorMapper;

    @Inject
    CreditOptionSelector creditOptionSelector;

    GenericParams genericParams = new GenericParamsBuilder()
        .setThumbnailSize(Collections.singleton(ThumbnailSize.W50xH50))
        .build();

    @Before
    public void setUp() throws Exception {
        when(
            checkouterAPI.cart(
                argThat(new MultiCartMatcher()),
                argThat(new CartParametersMatcher())
            )
        ).thenReturn(
            new MultiCartBuilder().random().withOrder(
                new MultiCartOrderBuilder().randomCarted().withItem(
                    new MultiCartOrderItemBuilder().withFeedOfferId(TEST_OFFER_SHOP_ID, TEST_SHOP_FEED_ID).build()
                ).build()
            ).build()
        );
        when(converter.convertToMultiCart(any(OrderOptionsRequest.class), any(GenericParams.class))).thenReturn(
            new MultiCartBuilder()
                .random()
                .withBuyerRegionId(TEST_USER_REGION_ID)
                .withOrder(
                new MultiCartOrderBuilder().randomCarted().withItem(
                    new MultiCartOrderItemBuilder().random()
                        .withFeedOfferId(TEST_OFFER_SHOP_ID, TEST_SHOP_FEED_ID)
                        .withShowInfo(TEST_OFFER_RIGHT_FEE_SHOW)
                        .build()
                ).build()
            ).build()
        );
        ShopOrderItem shopOrderItem = new ShopOrderItemBuilder().random()
            .withPayload(TEST_SHOP_FEED_ID, TEST_OFFER_SHOP_ID, TEST_OFFER_ID, TEST_OFFER_RIGHT_FEE_SHOW)
            .build();
        OrderOptionsResponse.ShopOptions shopOptions = new OrderOptionsResponseShopOptionsBuilder()
            .withShopId(TEST_SHOP_ID)
            .withItems(shopOrderItem)
            .withPaymentMethods(PaymentMethod.CASH_ON_DELIVERY)
            .build();
        OrderOptionsResponse response = new OrderOptionsResponseBuilder().random()
            .withShopOptions(shopOptions)
            .build();
        when(converter.convertToOptionsResponse(
            any(MultiCart.class), any(OrderOptionsRequest.class), any(GenericParams.class),
                anyBoolean(), anyBoolean(), anyBoolean(), anyCollection(),anyBoolean())
        ).thenReturn(response);
        when(
            checkouterAPI.checkout(
                argThat(new MultiOrderMatcher()), eq(TEST_USER_ID), anyBoolean(),
                anyBoolean(), any(Context.class), anyString(),
                any(ApiSettings.class), anyString(), any(HitRateGroup.class),
                any(Color.class)
            )
        ).thenReturn(CHECKOUTED_RESPONSE);
        when(serializer.writeObject(anyObject())).thenReturn("123");
        when(shopInfoService.getShopInfoIndex(any(), any(), any(), any()))
            .thenReturn(Pipelines.startWithValue(Long2ObjectMaps.singleton(TEST_SHOP_ID, new ShopInfoV2(){{
                setId(TEST_SHOP_ID);
                setName(TEST_SHOP_NAME);
            }})));
        when(marketTypeResolver.resolve())
            .thenReturn(MarketType.GREEN);
        when(colorMapper.map(any()))
                .thenReturn(Color.WHITE);
        when(blueMobileApplicationRule.test()).thenReturn(false);

        final PerkStatus yandexEmployerPerk = new PerkStatus();
        yandexEmployerPerk.setType(PerkType.YANDEX_EMPLOYEE.getId());

        final PerkStatus yandexPlusPerk = new PerkStatus();
        yandexPlusPerk.setType(PerkType.YANDEX_PLUS.getId());
        yandexPlusPerk.setPurchased(true);

        when(perkService.getAvailablePerksWithoutFilters(anyInt()))
            .thenReturn(Pipelines.startWithValue(Lists.newArrayList(yandexEmployerPerk, yandexPlusPerk)));
        when(perkService.mapPerkTypes(anyCollection()))
                .thenReturn(Lists.newArrayList(PerkType.YANDEX_EMPLOYEE.getId(), PerkType.YANDEX_PLUS.getId()));
        when(perkService.getDisabledPromoThresholds(anyInt()))
                .thenReturn(Pipelines.startWithValue(Lists.newArrayList()));

        when(resizerService.resizeImages(any(), any()))
            .thenReturn(Pipelines.startWithValue(null));

        when(checkouterAPI.getOrderReceipts(any(RequestClientInfo.class), any(BasicOrderRequest.class)))
            .thenReturn(new Receipts(Collections.singleton(new Receipt())));

        when(offerIdEncodingService.encode(any())).thenReturn(TEST_MARKET_OFFER_ID);

        when(supplierService.getSuppliers(new LongArrayList(new long[] {TEST_SUPPLIER_ID})))
            .thenReturn(Pipelines.startWithValue(
                new Long2ObjectOpenHashMap<>(
                    new long[] {TEST_SUPPLIER_ID},
                    new ShopInfoV2[]{
                        new ShopInfoV2() {{
                            setId(TEST_SUPPLIER_ID);
                        }}
                    })
                )
            );

        service = new CheckouterOrderService(
                checkouterAPI,
                converter,
                null,
                null,
                serializer,
                resizerService,
                shopInfoService,
                offerIdEncodingService,
                marketTypeResolver,
                null,
                null,
                supplierService,
                urlParamsFactoryImpl,
                offerIndexFactory,
                blueMobileAndroidReceiptsHackRule,
                tvmTicketProvider,
                colorMapper,
                perkService,
                creditOptionSelector
        );
    }

    @Test
    public void shouldFilterDeliveryOptionsById() {
        final String RIGHT_DELIVERY_ID = "DEAD_BEEF";
        final String WRONG_DELIVERY_ID = "ALIVE_PORK";

        when(converter.convertToOptionsResponse(
            any(MultiCart.class), any(OrderOptionsRequest.class), any(GenericParams.class),
                anyBoolean(), anyBoolean(), anyBoolean(), anyCollection(),anyBoolean())
        ).thenReturn(
            new OrderOptionsResponseBuilder()
                .random()
                .withShopOptions(new OrderOptionsResponseShopOptionsBuilder()
                    .random()
                    .withItems(new ShopOrderItemBuilder()
                        .random()
                        .build()
                    )
                    .withDelivery(
                        new DeliveryOptionBuilder(ServiceDeliveryOption.class)
                            .random()
                            .withId(new DeliveryPointId(RIGHT_DELIVERY_ID))
                            .build(),
                        new DeliveryOptionBuilder(ServiceDeliveryOption.class)
                            .random()
                            .withId(new DeliveryPointId(WRONG_DELIVERY_ID))
                            .build()
                    )
                    .build()
                )
                .build()
        );

        OrderOptionsRequest request = new OrderOptionsRequestBuilder()
            .random()
            .withRegionId(TEST_USER_REGION_ID)
            .withShopOrder(new OrderOptionsRequestShopOrderBuilder()
                .random()
                .withDelivery(new OrderOptionsRequestDeliveryPointBuilder(OrderOptionsRequest.AddressDeliveryPoint.class)
                    .random()
                    .withId(new DeliveryPointId(RIGHT_DELIVERY_ID))
                    .build()
                )
                .build()
            )
            .build();

        OrderOptionsResponse response = service.getOrderOptions(request, TEST_USER_ID, true, false,false,
                genericParams, TEST_RGBS, false,false, Collections.emptyList(), true);

        assertEquals(1, response.getShops().get(0).getDeliveryOptions().size());
        assertEquals(new DeliveryPointId(RIGHT_DELIVERY_ID), response.getShops().get(0).getDeliveryOptions().get(0).getId());
    }

    @Test
    public void shouldSkipDeliveryOptionsByFlag() {
        final String RIGHT_DELIVERY_ID1 = "DEAD_BEEF";
        final String RIGHT_DELIVERY_ID2 = "ALIVE_PORK";

        when(converter.convertToOptionsResponse(
                any(MultiCart.class), any(OrderOptionsRequest.class), any(GenericParams.class),
                anyBoolean(), anyBoolean(), anyBoolean(), anyCollection(), anyBoolean())
        ).thenReturn(
                new OrderOptionsResponseBuilder()
                        .random()
                        .withShopOptions(new OrderOptionsResponseShopOptionsBuilder()
                                .random()
                                .withItems(new ShopOrderItemBuilder()
                                        .random()
                                        .build()
                                )
                                .withDelivery(
                                        new DeliveryOptionBuilder(ServiceDeliveryOption.class)
                                                .random()
                                                .withId(new DeliveryPointId(RIGHT_DELIVERY_ID1))
                                                .build(),
                                        new DeliveryOptionBuilder(ServiceDeliveryOption.class)
                                                .random()
                                                .withId(new DeliveryPointId(RIGHT_DELIVERY_ID2))
                                                .build()
                                )
                                .build()
                        )
                        .build()
        );

        OrderOptionsRequest request = new OrderOptionsRequestBuilder()
                .random()
                .withRegionId(TEST_USER_REGION_ID)
                .withShopOrder(new OrderOptionsRequestShopOrderBuilder()
                        .random()
                        .withDelivery(new OrderOptionsRequestDeliveryPointBuilder(OrderOptionsRequest.AddressDeliveryPoint.class)
                                .random()
                                .withId(new DeliveryPointId(RIGHT_DELIVERY_ID1))
                                .build()
                        )
                        .build()
                )
                .build();

        OrderOptionsResponse response = service.getOrderOptions(request, TEST_USER_ID, true, false,true,
                genericParams, TEST_RGBS, false,false, Collections.emptyList(), true);

        assertEquals(2, response.getShops().get(0).getDeliveryOptions().size());
        assertEquals(new DeliveryPointId(RIGHT_DELIVERY_ID1), response.getShops().get(0).getDeliveryOptions().get(0).getId());
        assertEquals(new DeliveryPointId(RIGHT_DELIVERY_ID2), response.getShops().get(0).getDeliveryOptions().get(1).getId());
    }

    @Test
    public void shouldFilterDeliveryOptionsByOutletId() {
        final long RIGHT_OUTLET_ID = 42;
        final long WRONG_OUTLET_ID = 100500;

        when(converter.convertToOptionsResponse(
            any(MultiCart.class), any(OrderOptionsRequest.class), any(GenericParams.class),
                anyBoolean(), anyBoolean(), anyBoolean(), anyCollection(),anyBoolean())
        ).thenReturn(
            new OrderOptionsResponseBuilder()
                .random()
                .withShopOptions(new OrderOptionsResponseShopOptionsBuilder()
                    .random()
                    .withItems(new ShopOrderItemBuilder()
                        .random()
                        .build()
                    )
                    .withDelivery(
                        new DeliveryOptionBuilder(ServiceDeliveryOption.class)
                            .random()
                            .build(),
                        new DeliveryOptionBuilder(OutletDeliveryOption.class)
                            .random()
                            .withOutlets(new OutletBuilder()
                                .random()
                                .withId(WRONG_OUTLET_ID)
                                .build()
                            )
                            .build(),
                        new DeliveryOptionBuilder(OutletDeliveryOption.class)
                            .random()
                            .withOutlets(new OutletBuilder()
                                .random()
                                .withId(RIGHT_OUTLET_ID)
                                .build()
                            )
                            .build()
                    )
                    .build()
                )
                .build()
        );

        OrderOptionsRequest request = new OrderOptionsRequestBuilder()
            .random()
            .withRegionId(TEST_USER_REGION_ID)
            .withShopOrder(new OrderOptionsRequestShopOrderBuilder()
                .random()
                .withDelivery(new OrderOptionsRequestDeliveryPointBuilder(OrderOptionsRequest.OutletDeliveryPoint.class)
                    .random()
                    .withOutletId(RIGHT_OUTLET_ID)
                    .build()
                )
                .build()
            )
            .build();

        OrderOptionsResponse response = service.getOrderOptions(request, TEST_USER_ID, true,
                false, false, genericParams, TEST_RGBS,
                false, false, Collections.emptyList(),
                true);

        List<DeliveryOption> deliveryOptions = response.getShops().get(0).getDeliveryOptions();
        assertEquals(1, deliveryOptions.size());
        assertThat(deliveryOptions,
            contains(hasProperty("outlets",
                contains(hasProperty("id", is(RIGHT_OUTLET_ID))))
            )
        );
    }

    @Test
    public void shouldMarkOrderWithoutAllowedPaymentMethodAsUndeliverable() {
        when(converter.convertToOptionsResponse(
            any(MultiCart.class), any(OrderOptionsRequest.class), any(GenericParams.class),
                anyBoolean(), anyBoolean(), anyBoolean(), anyCollection(), anyBoolean())
        ).thenReturn(
            new OrderOptionsResponseBuilder()
                .random()
                .withPaymentMethods(PaymentMethod.CASH_ON_DELIVERY)
                .withShopOptions(new OrderOptionsResponseShopOptionsBuilder()
                    .random()
                    .withItems(new ShopOrderItemBuilder()
                        .random()
                        .build()
                    )
                    .withPaymentMethods(PaymentMethod.CASH_ON_DELIVERY)
                    .withDelivery(new DeliveryOptionBuilder(ServiceDeliveryOption.class)
                        .random()
                        .withPaymentOptions(PaymentMethod.CARD_ON_DELIVERY)
                        .build()
                    )
                    .build()
                )
                .build()
        );

        OrderOptionsRequest.OrderItem orderItem = new OrderOptionsRequestOrderItemBuilder()
            .random()
            .build();
        OrderOptionsRequest request = new OrderOptionsRequestBuilder()
            .random()
            .withRegionId(TEST_USER_REGION_ID)
            .withShopOrder(new OrderOptionsRequestShopOrderBuilder()
                .random()
                .addItem(orderItem)
                .build()
            )
            .withPaymentOptions(PaymentMethod.CASH_ON_DELIVERY)
            .build();

        OrderOptionsResponse response = service.getOrderOptions(request, TEST_USER_ID, true,
                false, false, genericParams, TEST_RGBS,
                false,false, Collections.emptyList(), true);
        for (ShopOrderItem item : response.getShops().get(0).getItems()) {
            assertEquals(1, item.getErrors().size());
            assertEquals(ShopOrderItem.Error.UNDELIVERABLE, item.getErrors().iterator().next());
        }
    }

    @Test
    public void shouldProcessOrderOptionsRequestWithRightFeeShow() {
        OrderOptionsRequest.OrderItem orderItem = new OrderOptionsRequestOrderItemBuilder().random()
            .withOfferId(new OfferId(TEST_OFFER_ID, null))
            .build();
        OrderOptionsRequest request = new OrderOptionsRequestBuilder().random()
            .withRegionId(TEST_USER_REGION_ID)
            .withShopOrder(new OrderOptionsRequestShopOrderBuilder()
                .random()
                .withShopId(TEST_SHOP_ID)
                .addItem(orderItem)
                .build()
            )
            .withPaymentOptions(PaymentMethod.CASH_ON_DELIVERY)
            .build();

        OrderOptionsResponse response = service.getOrderOptions(request, TEST_USER_ID, true,
                false, false, genericParams, TEST_RGBS,
                false,false, Collections.emptyList(), true);

        assertTrue(response.getErrors().isEmpty());
        assertEquals(1, response.getShops().size());

        OrderOptionsResponse.ShopOptions shop = response.getShops().get(0);
        assertTrue(shop.getErrors().isEmpty());
        assertEquals(TEST_SHOP_ID, shop.getShopId());
        ShopInfoV2 shopInfo = (ShopInfoV2) shop.getShop();
        assertEquals(TEST_SHOP_NAME, shopInfo.getName());
        assertEquals(TEST_SHOP_ID, shopInfo.getId());
        assertEquals(1, shop.getItems().size());

        ShopOrderItem item = shop.getItems().get(0);

        Payload payload = item.getPayload();
        assertEquals(TEST_OFFER_RIGHT_FEE_SHOW, payload.getFee());
        assertEquals(TEST_SHOP_FEED_ID, payload.getFeedId());
        assertEquals(TEST_OFFER_SHOP_ID, payload.getShopOfferId());
        assertEquals(TEST_OFFER_ID, payload.getMarketOfferId());
    }

    @Test
    public void shouldProcessOrderOptionsParamsWithYandexEmployeeFlag() {

        OrderOptionsRequest.OrderItem orderItem = new OrderOptionsRequestOrderItemBuilder().random()
            .withOfferId(new OfferId(TEST_OFFER_ID, null))
            .build();
        OrderOptionsRequest request = new OrderOptionsRequestBuilder().random()
            .withRegionId(TEST_USER_REGION_ID)
            .withShopOrder(new OrderOptionsRequestShopOrderBuilder()
                .random()
                .withShopId(TEST_SHOP_ID)
                .addItem(orderItem)
                .build()
            )
            .withPaymentOptions(PaymentMethod.CASH_ON_DELIVERY)
            .build();

        service.getOrderOptions(request, TEST_USER_ID, true, false,
                false, genericParams, TEST_RGBS, false,false, Collections.emptyList(), true);

        Mockito.verify(checkouterAPI)
            .cart(
                argThat(new MultiCartMatcher()),
                argThat(new CartParametersMatcherWithYandexEmployeePerk(true))
            );
    }

    @Test
    public void shouldMapAllAllowedPerksInCheckouterCart() {

        OrderOptionsRequest.OrderItem orderItem = new OrderOptionsRequestOrderItemBuilder().random()
                .withOfferId(new OfferId(TEST_OFFER_ID, null))
                .build();
        OrderOptionsRequest request = new OrderOptionsRequestBuilder().random()
                .withRegionId(TEST_USER_REGION_ID)
                .withShopOrder(new OrderOptionsRequestShopOrderBuilder()
                        .random()
                        .withShopId(TEST_SHOP_ID)
                        .addItem(orderItem)
                        .build()
                )
                .withPaymentOptions(PaymentMethod.CASH_ON_DELIVERY)
                .build();

        service.getOrderOptions(request, TEST_USER_ID, true, false,
                false, genericParams, TEST_RGBS, false,false, Collections.emptyList(), true);

        Mockito.verify(checkouterAPI)
                .cart(
                        argThat(new MultiCartMatcher()),
                        argThat(new CartParametersMatcherWithPerks("yandex_employee,yandex_plus"))
                );
    }

    @Test
    public void shouldFilterHiddenDeliveryOptionByHiddenReason() {
        when(converter.convertToOptionsResponse(
            any(MultiCart.class), any(OrderOptionsRequest.class), any(GenericParams.class),
                anyBoolean(), anyBoolean(), anyBoolean(), anyCollection(), anyBoolean())
        ).thenReturn(
            new OrderOptionsResponseBuilder()
                .random()
                .withShopOptions(new OrderOptionsResponseShopOptionsBuilder()
                    .random()
                    .withShopId(TEST_SHOP_ID)
                    .withItems(new ShopOrderItemBuilder()
                        .random()
                        .build()
                    )
                    .withDelivery(
                        new DeliveryOptionBuilder(ServiceDeliveryOption.class)
                            .random()
                            .withPaymentOptions() // empty
                            .withHiddenPaymentOptions(
                                new DeliveryOption.HiddenPaymentOption(PaymentMethod.YANDEX, PaymentOptionHiddenReason.MUID),
                                new DeliveryOption.HiddenPaymentOption(PaymentMethod.YANDEX, PaymentOptionHiddenReason.POST)
                            )
                            .build(),
                        new DeliveryOptionBuilder(ServiceDeliveryOption.class)
                            .random()
                            .withPaymentOptions() // empty
                            .withHiddenPaymentOptions(
                                new DeliveryOption.HiddenPaymentOption(PaymentMethod.YANDEX, PaymentOptionHiddenReason.POST)
                            )
                            .build()
                    )
                    .build()
                )
                .build()
        );

        OrderOptionsRequest request = new OrderOptionsRequestBuilder()
            .random()
            .withRegionId(TEST_USER_REGION_ID)
            .withShopOrder(new OrderOptionsRequestShopOrderBuilder()
                .random()
                .withShopId(TEST_SHOP_ID)
                .build()
            )
            .withPaymentOptionsHiddenReasons(PaymentOptionHiddenReason.MUID)
            .build();

        OrderOptionsResponse response = service.getOrderOptions(request, TEST_USER_ID, true,
                false, false, genericParams, TEST_RGBS, false,false, Collections.emptyList(), true);

        List<DeliveryOption> deliveryOptions = response.getShops().get(0).getDeliveryOptions();

        assertThat(deliveryOptions, notNullValue());
        assertEquals(1, deliveryOptions.size());

        DeliveryOption deliveryOption = deliveryOptions.get(0);
        assertEquals(1, deliveryOption.getHiddenPaymentMethods().size());
        assertEquals(
            PaymentOptionHiddenReason.MUID,
            deliveryOption.getHiddenPaymentMethods().iterator().next().getHiddenReason()
        );
    }

    @Test
    public void shouldGetSupplierFromOffer() {
        when(
                checkouterAPI.getOrders(
                        argThat(new RequestGeneratorHelper.RequestClientInfoMatcher()),
                        argThat(new RequestGeneratorHelper.OrderSearchRequestMatcher())
                )
        )
            .thenReturn(new PagedOrders() {{
                setItems(Collections.singletonList(
                    new OrderBuilder()
                        .random()
                        .withItems(
                            new OrderItemBuilder()
                                .random()
                                .withOfferId(TEST_OFFER_ID)
                                .build()
                        )
                        .build()
                ));
            }});

        ShopOrderItem shopOrderItem = new ShopOrderItemBuilder()
            .random()
            .withMarketOfferId(TEST_MARKET_OFFER_ID)
            .build();

        when(converter.convertToPersistentOrder(any(), any(), any()))
            .thenReturn(new PersistentOrder() {{
                setItems(Collections.singletonList(
                    shopOrderItem));
            }});

        when(offerIndex.create(any(), any(), any()))
            .thenReturn(Pipelines.startWithValue(offerIndex));

        when(offerIndex.getOffer(shopOrderItem))
            .thenReturn(new OfferV2() {{
                setSupplier(new ShopInfoV2() {{
                    setId(TEST_SUPPLIER_ID);
                }});
            }});

        when(offerIndexFactory.get())
            .thenReturn(offerIndex);

        PersistentOrder order = service.getOrder(TEST_ORDER_ID, TEST_USER_ID, WorkScheduleFormat.V2, Collections.emptyList(),
                genericParams, false, false, TEST_RGBS);

        assertEquals(TEST_SUPPLIER_ID, order.getItems().get(0).getSupplier().getId());
    }

    @Test
    public void shouldGetSupplierFromCheckouter() {
        when(
                checkouterAPI.getOrders(
                        argThat(new RequestGeneratorHelper.RequestClientInfoMatcher()),
                        argThat(new RequestGeneratorHelper.OrderSearchRequestMatcher())
                )
        )
            .thenReturn(new PagedOrders() {{
                setItems(Collections.singletonList(
                    new OrderBuilder()
                        .random()
                        .withItems(
                            new OrderItemBuilder()
                                .random()
                                .withOfferId(TEST_OFFER_ID)
                                .withSupplierId(TEST_SUPPLIER_ID)
                                .build()
                        )
                        .build()
                ));
            }});

        when(converter.convertToPersistentOrder(any(), any(), any()))
            .thenReturn(new PersistentOrder() {{
                setItems(Collections.singletonList(
                    new ShopOrderItemBuilder()
                        .random()
                        .withMarketOfferId(TEST_MARKET_OFFER_ID)
                        .withSupplierId(TEST_SUPPLIER_ID)
                        .build()));
            }});

        when(offerIndex.create(any(), any(), any()))
            .thenReturn(Pipelines.startWithValue(offerIndex));

        when(offerIndex.getOffer(any()))
            .thenReturn(null);

        when(offerIndexFactory.get())
            .thenReturn(offerIndex);


        PersistentOrder order = service.getOrder(TEST_ORDER_ID, TEST_USER_ID, WorkScheduleFormat.V2, Collections.emptyList(),
                genericParams, false, false, TEST_RGBS);

        assertEquals(TEST_SUPPLIER_ID, order.getItems().get(0).getSupplier().getId());
    }

    @Test
    public void shouldNotReturnSupplierIfNoOfferAndNoSupplierId() {
        when(
                checkouterAPI.getOrders(
                        argThat(new RequestGeneratorHelper.RequestClientInfoMatcher()),
                        argThat(new RequestGeneratorHelper.OrderSearchRequestMatcher())
                )
        )
            .thenReturn(new PagedOrders() {{
                setItems(Collections.singletonList(
                    new OrderBuilder()
                        .random()
                        .withItems(
                            new OrderItemBuilder()
                                .random()
                                .withOfferId(TEST_OFFER_ID)
                                .build()
                        )
                        .build()
                ));
            }});

        when(converter.convertToPersistentOrder(any(), any(), any()))
            .thenReturn(new PersistentOrder() {{
                setItems(Collections.singletonList(
                    new ShopOrderItemBuilder()
                        .random()
                        .withMarketOfferId(TEST_MARKET_OFFER_ID)
                        .build()));
            }});

        when(offerIndex.create(any(), any(), any()))
            .thenReturn(Pipelines.startWithValue(offerIndex));

        when(offerIndex.getOffer(any()))
            .thenReturn(null);

        when(offerIndexFactory.get())
            .thenReturn(offerIndex);

        PersistentOrder order = service.getOrder(TEST_ORDER_ID, TEST_USER_ID, WorkScheduleFormat.V2, Collections.emptyList(),
                genericParams, false, false, TEST_RGBS);

        assertNull(order.getItems().get(0).getSupplier());
    }

    @Test
    public void shouldOrderEnrichSkuLink() {
        PagedOrders orders = new PagedOrders();
        orders.setItems(
            Collections.singleton(
                new OrderBuilder()
                    .random()
                    .withItems(
                        new OrderItemBuilder()
                            .random()
                            .withOfferId("45")
                            .build()
                    )
                    .build()
            )
        );


        when(
                checkouterAPI.getOrders(
                        argThat(new RequestGeneratorHelper.RequestClientInfoMatcher()),
                        argThat(new RequestGeneratorHelper.OrderSearchRequestMatcher())
                )
        )
            .thenReturn(orders);

        PersistentOrder convertedOrder = new PersistentOrder();
        ShopOrderItem shopOrderItem = new ShopOrderItemBuilder()
            .random()
            .withMarketOfferId(TEST_MARKET_OFFER_ID)
            .build();
        convertedOrder.setItems(Collections.singletonList(
            shopOrderItem));
        when(converter.convertToPersistentOrder(any(), any(), any()))
            .thenReturn(convertedOrder);

        OfferV2 offer = new OfferV2();
        Sku sku = new Sku();
        sku.setId(TEST_SKU_ID);

        when(offerIndex.create(any(), any(), any()))
            .thenReturn(Pipelines.startWithValue(offerIndex));

        when(offerIndex.getOffer(shopOrderItem))
            .thenReturn(offer);

        when(offerIndex.getSku(shopOrderItem))
            .thenReturn(sku);

        when(offerIndexFactory.get())
            .thenReturn(offerIndex);


        when(supplierService.getSuppliers(any(LongList.class)))
            .thenReturn(Pipelines.startWithValue(Long2ObjectMaps.emptyMap()));

        PersistentOrder result = service.getOrder(
            TEST_ORDER_ID,
            TEST_USER_ID,
            WorkScheduleFormat.V2,
            Collections.emptyList(),
            genericParams,
            false,
            false,
            TEST_RGBS
        );

        Assert.assertThat(
            result.getItems().get(0).getSkuLink(),
            Matchers.allOf(
                    Matchers.startsWith("http://m.pokupki.market.yandex.ru/product/"),
                    Matchers.containsString(TEST_SKU_ID)
            )
        );
    }

    @Test
    public void shouldOrderNoEnrichSkuLinkIfNoSkuId() {
        PagedOrders orders = new PagedOrders();
        orders.setItems(
            Collections.singleton(
                new OrderBuilder()
                    .random()
                    .withItems(
                        new OrderItemBuilder()
                            .random()
                            .withOfferId("45")
                            .build()
                    )
                    .build()
            )
        );


        when(
                checkouterAPI.getOrders(
                        argThat(new RequestGeneratorHelper.RequestClientInfoMatcher()),
                        argThat(new RequestGeneratorHelper.OrderSearchRequestMatcher())
                )
        )
            .thenReturn(orders);

        PersistentOrder convertedOrder = new PersistentOrder();
        ShopOrderItem shopOrderItem = new ShopOrderItemBuilder()
            .random()
            .withMarketOfferId(TEST_MARKET_OFFER_ID)
            .build();
        convertedOrder.setItems(Collections.singletonList(
            shopOrderItem));
        when(converter.convertToPersistentOrder(any(), any(), any()))
            .thenReturn(convertedOrder);

        OfferV2 offer = new OfferV2();

        when(offerIndex.create(any(), any(), any()))
            .thenReturn(Pipelines.startWithValue(offerIndex));

        when(offerIndex.getOffer(shopOrderItem))
            .thenReturn(offer);

        when(offerIndexFactory.get())
            .thenReturn(offerIndex);


        when(supplierService.getSuppliers(any(LongList.class)))
            .thenReturn(Pipelines.startWithValue(Long2ObjectMaps.emptyMap()));

        PersistentOrder result = service.getOrder(
            TEST_ORDER_ID,
            TEST_USER_ID,
            WorkScheduleFormat.V2,
            Collections.emptyList(),
            genericParams,
            false,
            false,
            TEST_RGBS
        );

        Assert.assertThat(
            result.getItems().get(0).getSkuLink(),
            Matchers.nullValue()
        );
    }

    @Test
    public void shouldOrderEnrichCpaUrlWithOffer() {
        PagedOrders orders = new PagedOrders();
        orders.setItems(
            Collections.singleton(
                new OrderBuilder()
                    .random()
                    .withItems(
                        new OrderItemBuilder()
                            .random()
                            .withOfferId("45")
                            .build()
                    )
                    .build()
            )
        );


        when(
                checkouterAPI.getOrders(
                        argThat(new RequestGeneratorHelper.RequestClientInfoMatcher()),
                        argThat(new RequestGeneratorHelper.OrderSearchRequestMatcher())
                )
        )
            .thenReturn(orders);

        PersistentOrder convertedOrder = new PersistentOrder();
        ShopOrderItem shopOrderItem = new ShopOrderItemBuilder()
            .random()
            .withMarketOfferId(TEST_MARKET_OFFER_ID)
            .build();
        convertedOrder.setItems(Collections.singletonList(
            shopOrderItem));
        when(converter.convertToPersistentOrder(any(), any(), any()))
            .thenReturn(convertedOrder);

        OfferV2 offer = new OfferV2();
        offer.setCpaUrl("test-cpa-url");

        when(offerIndex.create(any(), any(), any()))
            .thenReturn(Pipelines.startWithValue(offerIndex));

        when(offerIndex.getOffer(shopOrderItem))
            .thenReturn(offer);

        when(offerIndexFactory.get())
            .thenReturn(offerIndex);

        when(supplierService.getSuppliers(any(LongList.class)))
            .thenReturn(Pipelines.startWithValue(Long2ObjectMaps.emptyMap()));

        PersistentOrder result = service.getOrder(
            TEST_ORDER_ID,
            TEST_USER_ID,
            WorkScheduleFormat.V2,
            Collections.emptyList(),
            genericParams,
            false,
            false,
            TEST_RGBS
        );

        Assert.assertNotNull(result.getItems().get(0).getMarketOfferId());
        Assert.assertEquals("test-cpa-url", result.getItems().get(0).getCpaUrl());
    }

    @Test
    public void shouldOrderNoEnrichOfferIdAndCpaUrlIfNoOffer() {
        PagedOrders orders = new PagedOrders();
        orders.setItems(
            Collections.singleton(
                new OrderBuilder()
                    .random()
                    .withItems(
                        new OrderItemBuilder()
                            .random()
                            .withOfferId("45")
                            .build()
                    )
                    .build()
            )
        );

        when(
                checkouterAPI.getOrders(
                        argThat(new RequestGeneratorHelper.RequestClientInfoMatcher()),
                        argThat(new RequestGeneratorHelper.OrderSearchRequestMatcher())
                )
        )
            .thenReturn(orders);

        PersistentOrder convertedOrder = new PersistentOrder();
        convertedOrder.setItems(Collections.singletonList(
            new ShopOrderItemBuilder()
                .random()
                .withMarketOfferId(TEST_MARKET_OFFER_ID)
                .build()));
        when(converter.convertToPersistentOrder(any(), any(), any()))
            .thenReturn(convertedOrder);

        OfferV2 offer = new OfferV2();
        offer.setCpaUrl("test-cpa-url");

        when(supplierService.getSuppliers(any(LongList.class)))
            .thenReturn(Pipelines.startWithValue(Long2ObjectMaps.emptyMap()));

        when(offerIndex.create(any(), any(), any()))
            .thenReturn(Pipelines.startWithValue(offerIndex));

        when(offerIndex.getOffer(any()))
            .thenReturn(null);

        when(offerIndexFactory.get())
            .thenReturn(offerIndex);

        PersistentOrder result = service.getOrder(
            TEST_ORDER_ID,
            TEST_USER_ID,
            WorkScheduleFormat.V2,
            Collections.emptyList(),
            genericParams,
            false,
            false,
            TEST_RGBS
        );

        Assert.assertNull(result.getItems().get(0).getMarketOfferId());
        Assert.assertNull(result.getItems().get(0).getCpaUrl());
    }

    @Test
    public void testWarningsFromSku() {
        when(
                checkouterAPI.getOrders(
                        argThat(new RequestGeneratorHelper.RequestClientInfoMatcher()),
                        argThat(new RequestGeneratorHelper.OrderSearchRequestMatcher())
                )
        )
                .thenReturn(new PagedOrders() {{
                    setItems(Collections.singletonList(
                            new OrderBuilder()
                                    .random()
                                    .withItems(
                                            new OrderItemBuilder()
                                                    .random()
                                                    .withOfferId(TEST_OFFER_ID)
                                                    .build()
                                    )
                                    .build()
                    ));
                }});

        ShopOrderItem shopOrderItem = new ShopOrderItemBuilder()
                .random()
                .withMarketOfferId(TEST_MARKET_OFFER_ID)
                .build();

        when(converter.convertToPersistentOrder(any(), any(), any()))
                .thenReturn(new PersistentOrder() {{
                    setItems(Collections.singletonList(
                            shopOrderItem));
                }});

        when(offerIndex.create(any(), any(), any()))
                .thenReturn(Pipelines.startWithValue(offerIndex));

        when(offerIndex.getSku(shopOrderItem))
                .thenReturn(new Sku() {{
                    setWarnings(Arrays.asList(new WarningInfo() {{
                        setCode("warning_sku");
                    }}));
                }});

        when(offerIndex.getOffer(shopOrderItem))
                .thenReturn(new OfferV2() {{
                    setWarnings(Arrays.asList(new WarningInfo() {{
                        setCode("warning_offer");
                    }}));
                }});

        when(offerIndexFactory.get())
                .thenReturn(offerIndex);

        PersistentOrder order = service.getOrder(TEST_ORDER_ID, TEST_USER_ID, WorkScheduleFormat.V2, Collections.emptyList(),
                genericParams, false, false, TEST_RGBS);

        assertEquals(1, order.getItems().get(0).getWarnings().size());
        assertEquals("warning_sku", order.getItems().get(0).getWarnings().get(0).getCode());
    }

    @Test
    public void testWarningsFromOffer() {
        when(
                checkouterAPI.getOrders(
                        argThat(new RequestGeneratorHelper.RequestClientInfoMatcher()),
                        argThat(new RequestGeneratorHelper.OrderSearchRequestMatcher())
                )
        )
                .thenReturn(new PagedOrders() {{
                    setItems(Collections.singletonList(
                            new OrderBuilder()
                                    .random()
                                    .withItems(
                                            new OrderItemBuilder()
                                                    .random()
                                                    .withOfferId(TEST_OFFER_ID)
                                                    .build()
                                    )
                                    .build()
                    ));
                }});

        ShopOrderItem shopOrderItem = new ShopOrderItemBuilder()
                .random()
                .withMarketOfferId(TEST_MARKET_OFFER_ID)
                .build();

        when(converter.convertToPersistentOrder(any(), any(), any()))
                .thenReturn(new PersistentOrder() {{
                    setItems(Collections.singletonList(
                            shopOrderItem));
                }});

        when(offerIndex.create(any(), any(), any()))
                .thenReturn(Pipelines.startWithValue(offerIndex));

        when(offerIndex.getSku(shopOrderItem))
                .thenReturn(new Sku() {{
                }});

        when(offerIndex.getOffer(shopOrderItem))
                .thenReturn(new OfferV2() {{
                    setWarnings(Arrays.asList(new WarningInfo() {{
                        setCode("warning_offer");
                    }}));
                }});

        when(offerIndexFactory.get())
                .thenReturn(offerIndex);

        PersistentOrder order = service.getOrder(TEST_ORDER_ID, TEST_USER_ID, WorkScheduleFormat.V2, Collections.emptyList(),
                genericParams, false, false, TEST_RGBS);

        assertEquals(1, order.getItems().get(0).getWarnings().size());
        assertEquals("warning_offer", order.getItems().get(0).getWarnings().get(0).getCode());
    }

    @Test
    public void shouldAddIsOptionalRulesEnabled() {
        OrderOptionsRequest.OrderItem orderItem = new OrderOptionsRequestOrderItemBuilder().random()
                .withOfferId(new OfferId(TEST_OFFER_ID, null))
                .build();
        OrderOptionsRequest request = new OrderOptionsRequestBuilder().random()
                .withRegionId(TEST_USER_REGION_ID)
                .withShopOrder(new OrderOptionsRequestShopOrderBuilder()
                        .random()
                        .withShopId(TEST_SHOP_ID)
                        .addItem(orderItem)
                        .build()
                )
                .withPaymentOptions(PaymentMethod.CASH_ON_DELIVERY)
                .withOptionalRules(true)
                .build();

        service.getOrderOptions(request, TEST_USER_ID, true, false,
                false, genericParams, TEST_RGBS, false,false, Collections.emptyList(), true);

        Mockito.verify(checkouterAPI)
                .cart(
                        argThat(new MultiCartMatcher()),
                        argThat(new CartParametersMatcherWithOptionalRules(true))
                );
    }

    @Test
    public void shouldAddCalculateOrdersSeparately() {
        OrderOptionsRequest.OrderItem orderItem = new OrderOptionsRequestOrderItemBuilder().random()
                .withOfferId(new OfferId(TEST_OFFER_ID, null))
                .build();
        OrderOptionsRequest request = new OrderOptionsRequestBuilder().random()
                .withRegionId(TEST_USER_REGION_ID)
                .withShopOrder(new OrderOptionsRequestShopOrderBuilder()
                        .random()
                        .withShopId(TEST_SHOP_ID)
                        .addItem(orderItem)
                        .build()
                )
                .withPaymentOptions(PaymentMethod.CASH_ON_DELIVERY)
                .withSeparateCalculationForOrders(true)
                .build();

        service.getOrderOptions(request, TEST_USER_ID, true, false,
                false, genericParams, TEST_RGBS, false,false, Collections.emptyList(), true);

        Mockito.verify(checkouterAPI)
                .cart(
                        argThat(new MultiCartMatcher()),
                        argThat(new CartParametersMatcherWithSeparateOrdersCalculation(true))
                );
    }
}
