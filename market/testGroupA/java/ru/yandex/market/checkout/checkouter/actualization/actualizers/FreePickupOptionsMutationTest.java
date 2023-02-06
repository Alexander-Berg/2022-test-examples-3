package ru.yandex.market.checkout.checkouter.actualization.actualizers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.mutations.FreePickupOptionsMutation;
import ru.yandex.market.checkout.checkouter.actualization.flow.context.CartFetchingContext;
import ru.yandex.market.checkout.checkouter.actualization.flow.context.MultiCartFetchingContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ActualizationContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartParameters;
import ru.yandex.market.checkout.checkouter.actualization.model.MultiCartContext;
import ru.yandex.market.checkout.checkouter.actualization.model.PushApiCartResponse;
import ru.yandex.market.checkout.checkouter.color.ColorConfig;
import ru.yandex.market.checkout.checkouter.color.SingleColorConfig;
import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.delivery.AddressImpl;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.outlet.DeliveryOutletService;
import ru.yandex.market.checkout.checkouter.delivery.outlet.MarketOutletId;
import ru.yandex.market.checkout.checkouter.delivery.outlet.OutletPurpose;
import ru.yandex.market.checkout.checkouter.delivery.outlet.ShopOutlet;
import ru.yandex.market.checkout.checkouter.delivery.outlet.ShopOutletId;
import ru.yandex.market.checkout.checkouter.delivery.outlet.ShopOutletMeta;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureReader;
import ru.yandex.market.checkout.checkouter.order.ApiSettings;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.immutable.ImmutableOrder;
import ru.yandex.market.checkout.checkouter.util.CheckouterProperties;
import ru.yandex.market.checkout.checkouter.util.CheckouterPropertiesImpl;
import ru.yandex.market.checkout.common.xml.outlets.OutletType;
import ru.yandex.market.checkout.helpers.FlowSessionHelper;
import ru.yandex.market.checkout.providers.MultiCartProvider;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FreePickupOptionsMutationTest {

    private static final Long SHOP_ID = 1L;
    @InjectMocks
    private FreePickupOptionsMutation freePickupOptionsMutation;
    @Mock
    private DeliveryOutletService deliveryOutletService;
    @Spy
    private final CheckouterProperties checkouterProperties = new CheckouterPropertiesImpl();
    @Mock
    private CheckouterFeatureReader checkouterFeatureReader;
    @Mock
    private ColorConfig colorConfig;
    @Mock
    private SingleColorConfig singleColorConfig;

    private ActualizationContext.ActualizationContextBuilder actualizationContextBuilder;

    @BeforeEach
    public void initContext() {
        lenient().when(deliveryOutletService.getOutletMetaByMarket(new MarketOutletId(SHOP_ID, 1L)))
                .thenReturn(new ShopOutletMeta(new ShopOutletId(SHOP_ID, "1"),
                        OutletType.RETAIL, 1L));
        lenient().when(deliveryOutletService.getOutletMetaByMarket(new MarketOutletId(SHOP_ID, 2L)))
                .thenReturn(new ShopOutletMeta(new ShopOutletId(SHOP_ID, "2"),
                        OutletType.DEPOT, 2L));
        lenient().when(deliveryOutletService.getOutletMetaByMarket(new MarketOutletId(SHOP_ID, 3L)))
                .thenReturn(new ShopOutletMeta(new ShopOutletId(SHOP_ID, "3"),
                        OutletType.DEPOT, 3L));

        lenient().when(deliveryOutletService.getMarketByShop(new ShopOutletId(SHOP_ID, "first")))
                .thenReturn(new MarketOutletId(SHOP_ID, 1L));
        lenient().when(deliveryOutletService.getMarketByShop(new ShopOutletId(SHOP_ID, "second")))
                .thenReturn(new MarketOutletId(SHOP_ID, 2L));
        lenient().when(deliveryOutletService.getMarketByShop(new ShopOutletId(SHOP_ID, "third")))
                .thenReturn(new MarketOutletId(SHOP_ID, 3L));
        when(singleColorConfig.selfDeliveryIsClickAndCollect()).thenReturn(false);
        when(checkouterProperties.getEnableFreePickupForDbs()).thenReturn(true);

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
                .withOriginalBuyerCurrency(Currency.RUR);
    }

    @DisplayName("Правильно склонировалась доставка + Должны скинуться цены в 0, а также цена поставщика и скидка")
    @Test
    public void deliveryCorrectlyCopiedAndFreeTest() {
        Address address = new AddressImpl() {{
            setCity("Moscow");
            setBlock("222");
        }};
        ShopOutlet shopOutlet1 = new ShopOutlet();
        shopOutlet1.setId(1L);
        ShopOutlet shopOutlet2 = new ShopOutlet();
        shopOutlet2.setId(2L);
        DeliveryResponse deliveryResponse = new DeliveryResponse() {{
            setPrice(BigDecimal.ONE);
            setSupplierPrice(BigDecimal.ONE);
            setSupplierDiscount(BigDecimal.ONE);
            setDeliveryCurrency(Currency.AFN);
            setShopAddress(address);
            setType(DeliveryType.PICKUP);
            setDeliveryPartnerType(DeliveryPartnerType.SHOP);
            setOutlets(List.of(shopOutlet1, shopOutlet2));
            setOutletCodes(Collections.emptySet());
        }};
        PushApiCartResponse pushApiCartResponse = new PushApiCartResponse();

        List<DeliveryResponse> mutableList = new ArrayList<>();
        mutableList.add(deliveryResponse);

        pushApiCartResponse.setDeliveryOptions(mutableList);

        Order order = new Order() {{
            setShopId(SHOP_ID);
        }};

        assertEquals(1, mutableList.size());

        mutate(order, pushApiCartResponse);

        assertEquals(2, mutableList.size());

        assertSame(deliveryResponse, mutableList.get(0));
        assertNotSame(deliveryResponse, mutableList.get(1));

        DeliveryResponse copiedDeliveryResponse = mutableList.get(1);

        assertEquals(address, copiedDeliveryResponse.getShopAddress());
        assertSame(address, copiedDeliveryResponse.getShopAddress());

        assertEquals("Moscow", copiedDeliveryResponse.getShopAddress().getCity());
        assertEquals(0, BigDecimal.ONE.compareTo(deliveryResponse.getPrice()));
        assertEquals(0, BigDecimal.ZERO.compareTo(copiedDeliveryResponse.getPrice()));

        assertEquals(0, BigDecimal.ONE.compareTo(deliveryResponse.getSupplierPrice()));
        assertEquals(0, BigDecimal.ZERO.compareTo(copiedDeliveryResponse.getSupplierPrice()));

        assertEquals(0, BigDecimal.ONE.compareTo(deliveryResponse.getSupplierDiscount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(copiedDeliveryResponse.getSupplierDiscount()));

        assertEquals(1, deliveryResponse.getOutletIds().size());
        assertEquals(2, deliveryResponse.getOutletIds().get(0));
        assertTrue(deliveryResponse.getOutletCodes().isEmpty());

        assertEquals(1, copiedDeliveryResponse.getOutletIds().size());
        assertEquals(1, copiedDeliveryResponse.getOutletIds().get(0));
        assertTrue(copiedDeliveryResponse.getOutletCodes().isEmpty());
    }

    @DisplayName("Без разбиения не должно появиться доп опции + Должны скинуться цены в 0, а также цена поставщика и " +
            "скидка")
    @Test
    public void oneFreeOutletWithoutCopyButFreeTest() {
        Address address = new AddressImpl() {{
            setCity("Moscow");
            setBlock("222");
        }};
        ShopOutlet shopOutlet = new ShopOutlet();
        shopOutlet.setId(1L);
        DeliveryResponse deliveryResponse = new DeliveryResponse() {{
            setPrice(BigDecimal.ONE);
            setSupplierPrice(BigDecimal.ONE);
            setSupplierDiscount(BigDecimal.ONE);

            setDeliveryCurrency(Currency.AFN);
            setShopAddress(address);
            setType(DeliveryType.PICKUP);
            setDeliveryPartnerType(DeliveryPartnerType.SHOP);
            setOutlets(List.of(shopOutlet));
            setOutletCodes(Collections.emptySet());
        }};
        PushApiCartResponse pushApiCartResponse = new PushApiCartResponse();

        List<DeliveryResponse> mutableList = new ArrayList<>();
        mutableList.add(deliveryResponse);

        pushApiCartResponse.setDeliveryOptions(mutableList);

        Order order = new Order() {{
            setShopId(SHOP_ID);
        }};

        assertEquals(1, mutableList.size());

        mutate(order, pushApiCartResponse);

        assertEquals(1, mutableList.size());

        assertSame(deliveryResponse, mutableList.get(0));

        assertEquals("Moscow", deliveryResponse.getShopAddress().getCity());
        assertEquals(0, BigDecimal.ZERO.compareTo(deliveryResponse.getPrice()));
        assertEquals(0, BigDecimal.ZERO.compareTo(deliveryResponse.getSupplierPrice()));
        assertEquals(0, BigDecimal.ZERO.compareTo(deliveryResponse.getSupplierDiscount()));
    }

    @DisplayName("Разбиение пвз работает по логике + При наличии разбиения должны правильно поменяться аутлеты, коды " +
            "и айдишки")
    @Test
    public void outletChangesInSortedViewTest() {
        Address address = new AddressImpl() {{
            setCity("Moscow");
            setBlock("222");
        }};

        ShopOutlet shopOutletFree = new ShopOutlet() {{
            setId(1L);
            setRank(1);
            setCode("first");
            addPurpose(OutletPurpose.STORE);
        }};
        ShopOutlet shopOutletNotFree1 = new ShopOutlet() {{
            setId(2L);
            setCode("second");
            setRank(3);
            addPurpose(OutletPurpose.PICKUP);
        }};
        ShopOutlet shopOutletNotFree2 = new ShopOutlet() {{
            setId(3L);
            setCode("third");
            setRank(2);
            addPurpose(OutletPurpose.PICKUP);
        }};

        DeliveryResponse deliveryResponse = new DeliveryResponse() {{
            setPrice(BigDecimal.ONE);
            setSupplierPrice(BigDecimal.ONE);
            setSupplierDiscount(BigDecimal.ONE);

            setDeliveryCurrency(Currency.AFN);
            setShopAddress(address);
            setType(DeliveryType.PICKUP);
            setDeliveryPartnerType(DeliveryPartnerType.SHOP);
            setOutlets(Arrays.asList(shopOutletFree, shopOutletNotFree1, shopOutletNotFree2));
            setOutletCodes(Set.of("first", "second", "third"));
        }};
        PushApiCartResponse pushApiCartResponse = new PushApiCartResponse();

        List<DeliveryResponse> mutableList = new ArrayList<>();
        mutableList.add(deliveryResponse);

        pushApiCartResponse.setDeliveryOptions(mutableList);

        Order order = new Order() {{
            setShopId(SHOP_ID);
        }};

        assertEquals(1, mutableList.size());

        mutate(order, pushApiCartResponse);

        assertEquals(2, mutableList.size());

        assertSame(deliveryResponse, mutableList.get(0));
        assertNotSame(deliveryResponse, mutableList.get(1));

        DeliveryResponse copiedDeliveryResponse = mutableList.get(1);

        assertEquals(address, copiedDeliveryResponse.getShopAddress());
        assertSame(address, copiedDeliveryResponse.getShopAddress());

        assertEquals(2, deliveryResponse.getOutlets().size());
        assertEquals(1, copiedDeliveryResponse.getOutlets().size());

        // проверяем отсортированность
        assertEquals(2, deliveryResponse.getOutlets().get(0).getRank());
        assertEquals(3, deliveryResponse.getOutlets().get(1).getRank());


        assertEquals(2, deliveryResponse.getOutletCodes().size());
        assertEquals(1, copiedDeliveryResponse.getOutletCodes().size());

        assertTrue(copiedDeliveryResponse.getOutletCodes().contains("first"));

        assertTrue(deliveryResponse.getOutletCodes().contains("second"));
        assertTrue(deliveryResponse.getOutletCodes().contains("third"));
    }

    private void mutate(Order order, PushApiCartResponse pushApiCartResponse) {
        when(colorConfig.getFor(order)).thenReturn(singleColorConfig);

        var multiCart = MultiCartProvider.single(order);
        var multiCartFetchingContext = MultiCartFetchingContext.of(MultiCartContext.createBy(
                ImmutableMultiCartParameters.builder().build(), multiCart), multiCart);
        var fetchingContext = CartFetchingContext.of(
                multiCartFetchingContext, actualizationContextBuilder, order);

        FlowSessionHelper.patchSession(fetchingContext, CartFetchingContext::makeImmutableContext,
                (c, v) -> c.getActualizationContextBuilder().withPushApiCartStage(v), pushApiCartResponse);

        actualizationContextBuilder
                .withCart(order)
                .withInitialCart(ImmutableOrder.from(order));
        freePickupOptionsMutation.onSuccess(fetchingContext);
    }
}
