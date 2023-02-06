package ru.yandex.market.checkout.pushapi.service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.delivery.AddressBuilder;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.LiftType;
import ru.yandex.market.checkout.checkouter.delivery.outlet.ShopOutlet;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelItem;
import ru.yandex.market.checkout.checkouter.order.ItemPrices;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderPrices;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.shop.MigrationMapping;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaDataGetterService;
import ru.yandex.market.checkout.pushapi.client.entity.Cart;
import ru.yandex.market.checkout.pushapi.client.entity.CartItem;
import ru.yandex.market.checkout.pushapi.client.entity.PushApiOrder;
import ru.yandex.market.checkout.pushapi.client.entity.Region;
import ru.yandex.market.checkout.pushapi.client.entity.order.Courier;
import ru.yandex.market.checkout.pushapi.client.entity.order.DeliveryWithRegion;
import ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil;
import ru.yandex.market.checkout.pushapi.settings.DataType;
import ru.yandex.market.checkout.pushapi.shop.ApiSelectorUtil;
import ru.yandex.market.checkout.pushapi.shop.entity.ExternalCart;
import ru.yandex.market.checkout.pushapi.shop.entity.ExternalCartItem;
import ru.yandex.market.checkout.pushapi.shop.entity.ShopOrder;
import ru.yandex.market.checkout.pushapi.shop.entity.ShopOrderItem;
import ru.yandex.market.checkout.pushapi.warehouse.WarehouseMappingCache;
import ru.yandex.market.personal_market.PersonalMarketService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType.SHOP;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryType.DELIVERY;
import static ru.yandex.market.checkout.common.util.ChainCalls.safeNull;

class RequestDetailsServiceTest {

    private static final ApiSelectorUtil.ApiSelection EXTERNAL = new ApiSelectorUtil.ApiSelection(
            "url", "args", DataType.JSON
    );

    private final GeoService geoService = mock(GeoService.class);
    private final PushApiOrderSecurity orderSecurity = mock(PushApiOrderSecurity.class);
    private final WarehouseMappingCache warehouseMappingCache = mock(WarehouseMappingCache.class);
    private final ShopMetaDataGetterService shopMetaDataGetterService = mock(ShopMetaDataGetterService.class);

    private final EnvironmentService environmentService = mock(EnvironmentService.class);
    private final PersonalMarketService personalMarketService = mock(PersonalMarketService.class);
    private final RequestDetailsService requestDetailsService = new RequestDetailsService(geoService, orderSecurity,
            warehouseMappingCache, shopMetaDataGetterService, environmentService, personalMarketService);

    private static void assertItemsEqual(OrderItem orderItem, ShopOrderItem shopOrderItem) {
        assertEquals(orderItem.getFeedId(), shopOrderItem.getFeedId());
        assertEquals(orderItem.getOfferId(), shopOrderItem.getOfferId());
        assertEquals(orderItem.getOfferName(), shopOrderItem.getOfferName());
        assertEquals(orderItem.getCategoryId(), shopOrderItem.getCategoryId());
        assertEquals(orderItem.getFeedCategoryId(), shopOrderItem.getFeedCategoryId());
        assertEquals(orderItem.getPrice(), shopOrderItem.getPrice());
        assertEquals(orderItem.getCount(), shopOrderItem.getCount());
        assertEquals(orderItem.getDelivery(), shopOrderItem.getDelivery());
        assertEquals(orderItem.getKind2ParametersString(), shopOrderItem.getKind2ParametersAsString());
        assertEquals(orderItem.getVat(), shopOrderItem.getVat());
        assertEquals(orderItem.getExternalFeedId(), shopOrderItem.getExternalFeedId());
        assertEquals(orderItem.getId(), shopOrderItem.getId());
        assertEquals(orderItem.getBuyerPrice(), shopOrderItem.getBuyerPrice());
        assertEquals(orderItem.getPrices().getBuyerPriceBeforeDiscount(), shopOrderItem.getBuyerPriceBeforeDiscount());
        assertEquals(orderItem.getFee(), shopOrderItem.getFee());
        assertEquals(orderItem.getSupplierId(), shopOrderItem.getSupplierId());
        assertEquals(orderItem.getSku(), shopOrderItem.getSku());
        assertEquals(orderItem.getShopSku(), shopOrderItem.getShopSku());
        assertEquals(orderItem.getWarehouseId(), shopOrderItem.getWarehouseId());
        assertEquals(orderItem.getPromos(), shopOrderItem.getPromos());
        assertEquals(
                Optional.ofNullable(orderItem.getPrices().getSubsidy()).orElse(BigDecimal.ZERO),
                shopOrderItem.getSubsidy()
        );
    }

    @Test
    void testCreateExternalCartWithAddress() throws ExecutionException, InterruptedException {
        final Address address = mock(Address.class);
        final Region region = mock(Region.class);
        final Items items = createItems();

        final Delivery delivery = new Delivery() {{
            setShopAddress(address);
            setRegionId(213L);
        }};

        final Cart cart = new Cart() {{
            setCurrency(Currency.AED);
            setDelivery(delivery);
            setItems(items.offerItems);
        }};

        when(geoService.getRegion(213L)).thenReturn(region);

        final ExternalCart actual =
                requestDetailsService.createExternalCartWithPersonalData(1234L, cart, EXTERNAL).get();

        AddressBuilder addressBuilder = new AddressBuilder();
        assertEquals(cart.getCurrency(), actual.getCurrency());
        assertEquals(items.externalCartItems, actual.getItems());
        assertEquals(
                addressBuilder.fromAddress(address).build(),
                addressBuilder.fromAddress(actual.getDeliveryWithRegion().getShopAddress()).build()
        );
        assertEquals(region, actual.getDeliveryWithRegion().getRegion());
        assertNull(actual.getDeliveryWithRegion().getRegionId());
    }

    private static void fillItems(
            CartItem cartItem, OrderItem orderItem, ExternalCartItem externalCartItem,
            final Long feedId, final String offerId, final String offerName, final String feedCategoryId,
            final BigDecimal price, final int count, final long orderItemId, final BigDecimal priceBeforeDiscount
    ) {
        cartItem.setFeedId(feedId);
        cartItem.setOfferId(offerId);
        cartItem.setOfferName(offerName);
        cartItem.setFeedCategoryId(feedCategoryId);
        cartItem.setPrice(price);
        cartItem.setCount(count);
        cartItem.setSupplierId(1L);

        orderItem.setId(orderItemId);
        orderItem.setFeedId(feedId);
        orderItem.setOfferId(offerId);
        orderItem.setOfferName(offerName);
        orderItem.setFeedCategoryId(feedCategoryId);
        orderItem.setPrice(price);
        orderItem.setCount(count);
        orderItem.setSupplierId(1L);
        ItemPrices prices = orderItem.getPrices();
        prices.setBuyerPriceBeforeDiscount(priceBeforeDiscount);

        externalCartItem.setFeedId(feedId);
        externalCartItem.setOfferId(offerId);
        externalCartItem.setOfferName(offerName);
        externalCartItem.setFeedCategoryId(feedCategoryId);
        externalCartItem.setPrice(price);
        externalCartItem.setCount(count);
    }

    @Test
    void testCreateExternalCartWithoutAddress() throws ExecutionException, InterruptedException {
        final Items items = createItems();
        final Region region = mock(Region.class);

        final Delivery delivery = new Delivery() {{
            setRegionId(213L);
        }};

        final Cart cart = new Cart() {{
            setCurrency(Currency.AFN);
            setDelivery(delivery);

            setItems(items.offerItems);
        }};

        when(geoService.getRegion(213L)).thenReturn(region);

        final ExternalCart actual =
                requestDetailsService.createExternalCartWithPersonalData(1234L, cart, EXTERNAL).get();

        assertEquals(cart.getCurrency(), actual.getCurrency());
        assertEquals(items.externalCartItems, actual.getItems());
        assertEquals(region, actual.getDeliveryWithRegion().getRegion());
        assertNull(actual.getDeliveryWithRegion().getRegionId());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getArgumentsForCreateOrderTest")
    void testCreateShopOrder(String caseName, PushApiOrder order, LocalDate shipmentDate) throws ExecutionException,
            InterruptedException {
        final Region region = mock(Region.class);
        when(geoService.getRegion(213)).thenReturn(region);

        final ShopOrder shopOrder =
                requestDetailsService.createShopOrderEnrichedWithPersonalData(111L, order).get();

        checkCreateShopOrderResult(order, shopOrder, region, shipmentDate);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testFakeShopOrderDeadline(boolean flag) throws Exception {
        var order = getTestOrder();
        order.setFake(flag);
        if (flag) {
            var creationDate = new SimpleDateFormat("dd-MM-yyyy hh:mm").parse("15-11-2021 16:10");
            order.setCreationDate(creationDate);
        }
        final ShopOrder shopOrder = requestDetailsService
                .createShopOrderEnrichedWithPersonalData(111L, order).get();
        assertEquals(Objects.nonNull(shopOrder.getDeliveryWithRegion().getParcels().get(0).getShipmentTime()), flag);
    }

    @Test
    void testShopOrderCourier() throws Exception {
        var order = getTestOrder();
        order.getDelivery().setCourier(new Courier() {{
            setFullName("Иванов Иван Иванович");
            setPhone("+7 495 999-00-11");
            setPhoneExtension("1234");
            setVehicleNumber("а123бв 790 RUS");
            setVehicleDescription("Бежевая Lada Priora");
        }});
        final ShopOrder shopOrder = requestDetailsService
                .createShopOrderEnrichedWithPersonalData(111L, order).get();
        assertEquals(shopOrder.getDeliveryWithRegion().getCourier().getFullName(), "Иванов Иван Иванович");
        assertEquals(shopOrder.getDeliveryWithRegion().getCourier().getPhone(), "+7 495 999-00-11");
        assertEquals(shopOrder.getDeliveryWithRegion().getCourier().getPhoneExtension(), "1234");
        assertEquals(shopOrder.getDeliveryWithRegion().getCourier().getVehicleNumber(), "а123бв 790 RUS");
        assertEquals(shopOrder.getDeliveryWithRegion().getCourier().getVehicleDescription(), "Бежевая Lada Priora");
    }

    static Stream<Arguments> getArgumentsForCreateOrderTest() throws Exception {

        // Это старая логика, когда shipmentDate для магазина берется из parcel.shipmentDate
        Order orderWithShipmentDate = getTestOrder();

        // Это новая логика, когда shipmentDate для магазина берется из parcel.item.shipmentDateTimeBySupplier
        Order orderWithShipmentDateTimeBySupplier = getTestOrder();
        Parcel parcel = CollectionUtils.extractSingleton(
                orderWithShipmentDateTimeBySupplier.getDelivery().getParcels());
        List<ParcelItem> parcelItems = orderWithShipmentDateTimeBySupplier.getItems().stream()
                .map(oi -> new ParcelItem())
                .collect(Collectors.toList());
        parcel.setParcelItems(parcelItems);

        Iterator<OrderItem> orderItemsIter = orderWithShipmentDateTimeBySupplier.getItems().iterator();

        OrderItem orderItem = orderItemsIter.next();
        orderItem.setAtSupplierWarehouse(true);
        parcelItems.get(0).setShipmentDateTimeBySupplier(LocalDateTime.parse("2020-01-01T12:00:00"));
        parcelItems.get(0).setItemId(orderItem.getId());

        orderItem = orderItemsIter.next();
        orderItem.setAtSupplierWarehouse(false);
        parcelItems.get(1).setShipmentDateTimeBySupplier(LocalDateTime.parse("2020-01-02T12:00:00"));
        parcelItems.get(1).setItemId(orderItem.getId());

        return Stream.of(
                Arguments.of(
                        "Создаем заказ в магазине с датой отгрузки из поля shipmentDate",
                        orderWithShipmentDate,
                        orderWithShipmentDate.getDelivery().getParcels().get(0).getShipmentDate()
                ),
                Arguments.of(
                        "Создаем заказ в магазине с датой отгрузки из поля shipmentDateTimeBySupplier",
                        orderWithShipmentDateTimeBySupplier,
                        LocalDate.parse("2020-01-01")
                )
        );
    }

    static PushApiOrder getTestOrder() throws Exception {
        final Address address = mock(Address.class);
        final Items items = createItems();

        return new PushApiOrder() {{
            setId(1234L);
            setCurrency(Currency.AOA);
            setPaymentType(PaymentType.PREPAID);
            setItems(items.orderItems);
            setDelivery(new DeliveryWithRegion() {{
                setType(DELIVERY);
                setServiceName("2345");
                setPrice(new BigDecimal(3456));
                setDeliveryDates(new DeliveryDates(
                        XmlTestUtil.date("2013-05-20"), XmlTestUtil.date("2013-05-23")
                ));
                setRegionId(213L);
                setShopAddress(address);
                setOutletId(4567L);
                Parcel parcel = new Parcel();
                parcel.setShipmentDate(LocalDate.now());
                setParcels(List.of(parcel));
            }});
        }};
    }

    @Test
    void testCreateShopOrderStatus() throws Exception {
        final Address address = mock(Address.class);
        final Items items = createItems();
        final LocalDate shipmentDate = LocalDate.now();

        final Order order = new Order() {{
            setId(1234L);
            setCurrency(Currency.AOA);
            setPaymentType(PaymentType.PREPAID);
            setItems(items.orderItems);
            setDelivery(new Delivery() {{
                setType(DELIVERY);
                setServiceName("2345");
                setPrice(new BigDecimal(3456));
                setDeliveryDates(new DeliveryDates(
                        XmlTestUtil.date("2013-05-20"), XmlTestUtil.date("2013-05-23")
                ));
                setRegionId(213L);
                setShopAddress(address);
                setOutletId(4567L);
                Parcel parcel = new Parcel();
                parcel.setShipmentDate(shipmentDate);
                setParcels(List.of(parcel));
            }});
            setStatus(OrderStatus.DELIVERY);
            setSubstatus(OrderSubstatus.PACKAGING);
            setCreationDate(new Date());
            setItemsTotal(new BigDecimal(543));
            setTotal(new BigDecimal(1000));
            getPromoPrices().setSubsidyTotal(new BigDecimal(324));
        }};

        final Region region = mock(Region.class);
        when(geoService.getRegion(213)).thenReturn(region);
        PushApiOrder pushApiOrder = new PushApiOrder(order);
        final ShopOrder shopOrder = requestDetailsService.createShopOrderStatus(111L, pushApiOrder).get();

        assertEquals(order.getStatus(), shopOrder.getStatus());
        assertEquals(order.getSubstatus(), shopOrder.getSubstatus());
        assertEquals(order.getCreationDate(), shopOrder.getCreationDate());
        assertEquals(order.getItemsTotal(), shopOrder.getItemsTotal());
        assertEquals(order.getPromoPrices().getSubsidyTotal(), shopOrder.getSubsidy());

        checkCreateShopOrderResult(order, shopOrder, region);
    }

    @Test
    void testCreateShopOrderWithOutletCode() throws Exception {
        final Address address = mock(Address.class);
        final Items items = createItems();

        final PushApiOrder order = new PushApiOrder() {{
            setId(1234L);
            setCurrency(Currency.AOA);
            setPaymentType(PaymentType.PREPAID);
            setItems(items.orderItems);
            setDelivery(new Delivery() {{
                setType(DELIVERY);
                setServiceName("2345");
                setPrice(new BigDecimal(3456));
                setDeliveryDates(new DeliveryDates(
                        XmlTestUtil.date("2013-05-20"), XmlTestUtil.date("2013-05-23")
                ));
                setRegionId(213L);
                setShopAddress(address);
                setOutletId(4567L);
            }});
        }};

        final Region region = mock(Region.class);
        when(geoService.getRegion(213)).thenReturn(region);

        final ShopOrder shopOrder = requestDetailsService
                .createShopOrderEnrichedWithPersonalData(111L, order).get();

        checkCreateShopOrderResult(order, shopOrder, region);
    }

    @Test
    void testCreateShopOrderStatusWithTotalWithSubsidy() throws Exception {
        final Address address = mock(Address.class);
        final Items items = createItems();
        final LocalDate shipmentDate = LocalDate.now();

        final Order order = getOrderWithTotalWithSubsidy(address, items, shipmentDate);
        final Region region = mock(Region.class);
        when(geoService.getRegion(213)).thenReturn(region);
        PushApiOrder pushApiOrder = new PushApiOrder(order);
        final ShopOrder shopOrder = requestDetailsService.createShopOrderStatus(111L, pushApiOrder).get();

        assertEquals(order.getStatus(), shopOrder.getStatus());
        assertEquals(order.getSubstatus(), shopOrder.getSubstatus());
        assertEquals(order.getCreationDate(), shopOrder.getCreationDate());
        assertEquals(order.getItemsTotal(), shopOrder.getItemsTotal());
        assertEquals(order.getPromoPrices().getSubsidyTotal(), shopOrder.getSubsidy());
        assertEquals(order.getTotalWithSubsidy(), shopOrder.getTotalWithSubsidy());

        checkCreateShopOrderResult(order, shopOrder, region);
    }

    @Test
    void testCreateShopOrderWithZeroTotalWithSubsidy() throws Exception {
        final Address address = mock(Address.class);
        final Items items = createItems();
        final LocalDate shipmentDate = LocalDate.now();

        final Order order = getOrderWithEmptySubsidy(address, items, shipmentDate);
        final Region region = mock(Region.class);
        when(geoService.getRegion(213)).thenReturn(region);
        PushApiOrder pushApiOrder = new PushApiOrder(order);
        final ShopOrder shopOrder = requestDetailsService
                .createShopOrderStatus(111L, pushApiOrder).get();
        // тк скидок нет, то totalWithSubsidy = total, поле показываем
        Assertions.assertThat(shopOrder.getTotalWithSubsidy()).isNotNull().isEqualTo(order.getTotal());

        checkCreateShopOrderResult(order, shopOrder, region);
    }

    @Test
    void testCreateShopOrderAcceptWithPricesInfo() throws Exception {
        when(environmentService.getBooleanValueOrDefault(
                eq(RequestDetailsService.PRICES_INFO_AVAILABLE), eq(false))).thenReturn(true);
        final Address address = mock(Address.class);
        final Items items = createItems();
        final LocalDate shipmentDate = LocalDate.now();

        final Order order = getOrderWithTotalWithSubsidy(address, items, shipmentDate);
        final Region region = mock(Region.class);
        when(geoService.getRegion(213)).thenReturn(region);
        PushApiOrder pushApiOrder = new PushApiOrder(order);
        final ShopOrder shopOrder = requestDetailsService
                .createShopOrderEnrichedWithPersonalData(111L, pushApiOrder).get();

        Assertions.assertThat(shopOrder.getTotal()).isNotNull();
        Assertions.assertThat(shopOrder.getItemsTotal()).isNotNull().isEqualTo(order.getItemsTotal());
        Assertions.assertThat(shopOrder.getSubsidy()).isNotNull().isEqualTo(order.getPromoPrices().getSubsidyTotal());
        Assertions.assertThat(shopOrder.getTotalWithSubsidy()).isNotNull().isEqualTo(order.getTotalWithSubsidy());
        assertEquals(order.getItemsTotal(), shopOrder.getItemsTotal());

        checkCreateShopOrderResult(order, shopOrder, region);
    }

    @Test
    void testCreateShopOrderAcceptWithBuyerItemsTotal() throws Exception {
        when(environmentService.getBooleanValueOrDefault(
                eq(RequestDetailsService.PRICES_INFO_AVAILABLE), eq(false))).thenReturn(true);
        final Address address = mock(Address.class);
        final Items items = createItems();
        final LocalDate shipmentDate = LocalDate.now();

        final Order order = getOrderWithBuyerItemsTotal(address, items, shipmentDate);
        final Region region = mock(Region.class);
        when(geoService.getRegion(213)).thenReturn(region);
        PushApiOrder pushApiOrder = new PushApiOrder(order);
        final ShopOrder shopOrder = requestDetailsService
                .createShopOrderEnrichedWithPersonalData(111L, pushApiOrder).get();

        Assertions.assertThat(shopOrder.getTotal()).isNotNull();
        Assertions.assertThat(shopOrder.getItemsTotal()).isNotNull().isEqualTo(order.getItemsTotal());
        Assertions.assertThat(shopOrder.getBuyerItemsTotal()).isNotNull().isEqualTo(order.getBuyerItemsTotal());
        Assertions.assertThat(shopOrder.getBuyerTotal()).isNotNull().isEqualTo(order.getBuyerTotal());
        Assertions.assertThat(shopOrder.getBuyerItemsTotalBeforeDiscount()).isNotNull()
                .isEqualTo(order.getPromoPrices().getBuyerItemsTotalBeforeDiscount());
        Assertions.assertThat(shopOrder.getBuyerTotalBeforeDiscount()).isNotNull()
                .isEqualTo(order.getPromoPrices().getBuyerTotalBeforeDiscount());

        checkCreateShopOrderResult(order, shopOrder, region);
    }

    @Test
    void testCreateShopShipmentDates() throws Exception {
        var address = mock(Address.class);
        var items = createItems();
        var shipmentDate = LocalDate.of(2022, 7, 5);
        var shipmentDateBySupplier = LocalDateTime.of(2022, 7, 6, 0, 0, 0);
        final Order order = getOrderWithEmptySubsidy(address, items, shipmentDate);
        order.getDelivery().getParcels()
                .forEach(parcel -> parcel.setShipmentDateTimeBySupplier(shipmentDateBySupplier));

        final Region region = mock(Region.class);
        when(geoService.getRegion(213)).thenReturn(region);
        PushApiOrder pushApiOrder = new PushApiOrder(order);
        final ShopOrder shopOrder = requestDetailsService
                .createShopOrderEnrichedWithPersonalData(111L, pushApiOrder).get();

        Assertions.assertThat(shopOrder.getDeliveryWithRegion().getParcels()).allMatch(
                parcel -> parcel.getShipmentDate().equals(shipmentDateBySupplier.toLocalDate())
        );
    }

    @Test
    void testCreateShopOrderWithLiftingOptions() throws Exception {
        final Address address = mock(Address.class);
        final Items items = createItems();

        final PushApiOrder order = new PushApiOrder() {{
            setId(1234L);
            setCurrency(Currency.AOA);
            setPaymentType(PaymentType.PREPAID);
            setItems(items.orderItems);
            setDelivery(new Delivery() {{
                setType(DELIVERY);
                setServiceName("2345");
                setPrice(new BigDecimal(3456));
                setDeliveryDates(new DeliveryDates(
                        XmlTestUtil.date("2013-05-20"), XmlTestUtil.date("2013-05-23")
                ));
                setRegionId(213L);
                setShopAddress(address);
                setOutletId(4567L);
                setLiftType(LiftType.ELEVATOR);
                setLiftPrice(BigDecimal.valueOf(150L));
            }});
        }};

        final Region region = mock(Region.class);
        when(geoService.getRegion(213)).thenReturn(region);

        final ShopOrder shopOrder = requestDetailsService
                .createShopOrderEnrichedWithPersonalData(111L, order).get();

        checkCreateShopOrderResult(order, shopOrder, region);
        final DeliveryWithRegion deliveryWithRegion = shopOrder.getDeliveryWithRegion();
        assertNotEquals(address, deliveryWithRegion.getShopAddress());
        assertEquals(LiftType.ELEVATOR, deliveryWithRegion.getLiftType());
        assertEquals(BigDecimal.valueOf(150L), deliveryWithRegion.getLiftPrice());
    }

    @Test
    void testShouldReplaceWarehouseId() throws Exception {
        final long expectedSupplierId = 123L;
        final long expectedWarehouseId = 1234L;
        final MigrationMapping migrationMapping = new MigrationMapping(expectedSupplierId, expectedWarehouseId);
        final ShopMetaData mockMeta = mock(ShopMetaData.class);
        when(shopMetaDataGetterService.getMeta(anyLong())).thenReturn(mockMeta);
        when(mockMeta.getMigrationMapping()).thenReturn(migrationMapping);
        final Address address = mock(Address.class);
        final Items items = createItems();
        final Delivery delivery = new Delivery() {{
            setShopAddress(address);
            setRegionId(213L);
        }};
        final PushApiOrder order = new PushApiOrder() {{
            setId(1234L);
            setCurrency(Currency.AOA);
            setPaymentType(PaymentType.PREPAID);
            setItems(items.orderItems);
            setDelivery(new Delivery() {{
                setType(DELIVERY);
                setServiceName("2345");
                setPrice(new BigDecimal(3456));
                setDeliveryDates(new DeliveryDates(
                        XmlTestUtil.date("2013-05-20"), XmlTestUtil.date("2013-05-23")
                ));
                setRegionId(213L);
                setShopAddress(address);
                setOutletId(4567L);
                setLiftType(LiftType.ELEVATOR);
                setLiftPrice(BigDecimal.valueOf(150L));
            }});
        }};
        final Cart cart = new Cart() {{
            setCurrency(Currency.AED);
            setDelivery(delivery);
            setItems(items.offerItems);
        }};
        final ShopOrder shopOrder = requestDetailsService
                .createShopOrderEnrichedWithPersonalData(111L, order).get();
        final ExternalCart externalCart = requestDetailsService.createExternalCartWithPersonalData(111L, cart,
                EXTERNAL).get();
        for (ShopOrderItem item : shopOrder.getItems()) {
            assertEquals(expectedSupplierId, item.getSupplierId());
            assertEquals(expectedWarehouseId, item.getWarehouseId().longValue());
        }
        for (ExternalCartItem item : externalCart.getItems()) {
            assertEquals(expectedSupplierId, item.getSupplierId());
            assertEquals(expectedWarehouseId, item.getWarehouseId().longValue());
        }
    }

    private void checkCreateShopOrderResult(Order order, ShopOrder shopOrder, Region region) {
        checkCreateShopOrderResult(order, shopOrder, region,
                // Старая логика вычисления shipmentDate
                safeNull(order.getDelivery().getParcels(), parcels -> parcels.get(0).getShipmentDate()));
    }

    private void checkCreateShopOrderResult(Order order,
                                            ShopOrder shopOrder,
                                            Region region,
                                            LocalDate expectedShipmentDate) {
        AddressBuilder addressBuilder = new AddressBuilder();
        assertEquals(order.getId(), shopOrder.getId());
        assertEquals(order.getCurrency(), shopOrder.getCurrency());
        assertEquals(order.getPaymentType(), shopOrder.getPaymentType());
        assertEquals(order.getItems().size(), shopOrder.getItems().size());
        Iterator<OrderItem> orderItemIterator = order.getItems().iterator();
        Iterator<ShopOrderItem> shopOrderItemIterator = shopOrder.getItems().iterator();
        while (orderItemIterator.hasNext()) {
            assertItemsEqual(orderItemIterator.next(), shopOrderItemIterator.next());
        }
        assertEquals(order.getDelivery().getType(), shopOrder.getDeliveryWithRegion().getType());
        assertEquals(order.getDelivery().getServiceName(), shopOrder.getDeliveryWithRegion().getServiceName());
        assertEquals(order.getDelivery().getPrice(), shopOrder.getDeliveryWithRegion().getPrice());
        assertEquals(order.getDelivery().getDeliveryDates(), shopOrder.getDeliveryWithRegion().getDeliveryDates());
        assertEquals(
                addressBuilder.fromAddress(order.getDelivery().getShopAddress()).build(),
                addressBuilder.fromAddress(shopOrder.getDeliveryWithRegion().getShopAddress()).build()
        );
        assertEquals(order.getDelivery().getOutletId(), shopOrder.getDeliveryWithRegion().getOutletId());
        assertThat(shopOrder.getDeliveryWithRegion().getParcels())
                .usingElementComparatorIgnoringFields("items", "shipmentDate")
                .isEqualTo(order.getDelivery().getParcels());
        assertNull(
                safeNull(shopOrder.getDeliveryWithRegion().getParcels(), parcels -> parcels.get(0).getParcelItems()));
        assertEquals(expectedShipmentDate,
                safeNull(shopOrder.getDeliveryWithRegion().getParcels(), parcels -> parcels.get(0).getShipmentDate()));
        assertEquals(region, shopOrder.getDeliveryWithRegion().getRegion());
        assertNull(shopOrder.getDeliveryWithRegion().getRegionId());
    }

    private static Items createItems() {
        final CartItem offerItem1 = new CartItem();
        final OrderItem orderItem1 = new OrderItem();
        final ExternalCartItem externalCartItem1 = new ExternalCartItem();
        fillItems(
                offerItem1, orderItem1, externalCartItem1,
                1234L, "2345", "iphone", "3456", new BigDecimal(4567), 2, 1,
                new BigDecimal(4568)
        );
        final CartItem offerItem2 = new CartItem();
        final OrderItem orderItem2 = new OrderItem();
        final ExternalCartItem externalCartItem2 = new ExternalCartItem();
        fillItems(
                offerItem2, orderItem2, externalCartItem2,
                5687L, "6789", "htc one", "7890", new BigDecimal(8901), 3, 2,
                new BigDecimal(8902)
        );
        final Items items = new Items();
        items.offerItems = Arrays.asList(offerItem1, offerItem2);
        items.orderItems = Arrays.asList(orderItem1, orderItem2);
        items.externalCartItems = Arrays.asList(externalCartItem1, externalCartItem2);
        return items;
    }

    private static Order getOrderWithTotalWithSubsidy(Address address, Items items, LocalDate shipmentDate)
            throws Exception {
        Order order = getOrderWithEmptySubsidy(address, items, shipmentDate);
        order.getPromoPrices().setSubsidyTotal(new BigDecimal(324));
        return order;
    }

    private static Order getOrderWithBuyerItemsTotal(Address address, Items items, LocalDate shipmentDate)
            throws Exception {
        Order order = getOrderWithEmptySubsidy(address, items, shipmentDate);
        OrderPrices promoPrices = order.getPromoPrices();
        promoPrices.setBuyerItemsTotalBeforeDiscount(BigDecimal.valueOf(500));
        promoPrices.setBuyerTotalBeforeDiscount(BigDecimal.valueOf(550));
        order.setBuyerItemsTotal(BigDecimal.valueOf(470));
        order.setBuyerTotal(BigDecimal.valueOf(520));
        return order;
    }

    private static Order getOrderWithEmptySubsidy(Address address, Items items, LocalDate shipmentDate)
            throws Exception {
        return new Order() {{
            setId(1234L);
            setCurrency(Currency.AOA);
            setPaymentType(PaymentType.PREPAID);
            setItems(items.orderItems);
            setDelivery(new Delivery() {{
                setType(DELIVERY);
                setServiceName("2345");
                setPrice(new BigDecimal(3456));
                setDeliveryDates(new DeliveryDates(
                        XmlTestUtil.date("2013-05-20"), XmlTestUtil.date("2013-05-23")
                ));
                setRegionId(213L);
                setShopAddress(address);
                setOutletId(4567L);
                Parcel parcel = new Parcel();
                parcel.setShipmentDate(shipmentDate);
                setParcels(List.of(parcel));
            }});
            setStatus(OrderStatus.DELIVERY);
            setSubstatus(OrderSubstatus.PACKAGING);
            setCreationDate(new Date());
            setItemsTotal(new BigDecimal(543));
            setTotal(new BigDecimal(1000));
        }};
    }

    private static class Items {

        private List<ExternalCartItem> externalCartItems;
        private List<CartItem> offerItems;
        private List<OrderItem> orderItems;
    }

    @Test
    void testCreateDbsToMarketBrandedOrder() throws Exception {
        final Address address = mock(Address.class);
        final Items items = createItems();
        String cityName = "Москва";
        String streetName = "ул. Льва Толстого";
        String houseName = "16";

        final PushApiOrder order = new PushApiOrder() {{
            setId(1234L);
            setCurrency(Currency.AOA);
            setPaymentType(PaymentType.PREPAID);
            setItems(items.orderItems);
            setDelivery(new Delivery() {{
                setType(DeliveryType.PICKUP);
                setDeliveryPartnerType(SHOP);
                setServiceName("2345");
                setOutletId(1L);
                setOutletCode("ABC");
                setPrice(new BigDecimal(3456));
                setDeliveryDates(new DeliveryDates(
                        XmlTestUtil.date("2013-05-20"), XmlTestUtil.date("2013-05-23")
                ));
                setRegionId(213L);
                setShopAddress(address);
                setOutletId(4567L);
                setMarketBranded(true);
                setOutlet(new ShopOutlet() {{
                    setCity(cityName);
                    setStreet(streetName);
                    setHouse(houseName);
                    setScheduleString(getScheduleAsString());
                }});
            }});
        }};

        final Region region = mock(Region.class);
        when(geoService.getRegion(213)).thenReturn(region);

        final ShopOrder shopOrder = requestDetailsService
                .createShopOrderEnrichedWithPersonalData(111L, order).get();

        Address shopAddress = shopOrder.getDeliveryWithRegion().getShopAddress();
        assertNotNull(shopAddress);
        assertEquals(cityName, shopAddress.getCity());
        assertEquals(streetName, shopAddress.getStreet());
        assertEquals(houseName, shopAddress.getHouse());
        assertNull(shopOrder.getDeliveryWithRegion().getOutletId());
        assertNull(shopOrder.getDeliveryWithRegion().getOutletCode());
        assertEquals(LocalTime.of(9, 0),
                shopOrder.getDeliveryWithRegion().getDeliveryDates().getFromTime());
    }

    private String getScheduleAsString() {
        return "<WorkingTime>\n" +
                "    <WorkingDaysFrom>1</WorkingDaysFrom>\n" +
                "    <WorkingDaysTill>1</WorkingDaysTill>\n" +
                "    <WorkingHoursFrom>09:00</WorkingHoursFrom>\n" +
                "    <WorkingHoursTill>20:00</WorkingHoursTill>\n" +
                "</WorkingTime><WorkingTime>\n" +
                "<WorkingDaysFrom>2</WorkingDaysFrom>\n" +
                "<WorkingDaysTill>2</WorkingDaysTill>\n" +
                "<WorkingHoursFrom>09:00</WorkingHoursFrom>\n" +
                "<WorkingHoursTill>20:00</WorkingHoursTill>\n" +
                "</WorkingTime><WorkingTime>\n" +
                "<WorkingDaysFrom>3</WorkingDaysFrom>\n" +
                "<WorkingDaysTill>3</WorkingDaysTill>\n" +
                "<WorkingHoursFrom>09:00</WorkingHoursFrom>\n" +
                "<WorkingHoursTill>20:00</WorkingHoursTill>\n" +
                "</WorkingTime><WorkingTime>\n" +
                "<WorkingDaysFrom>4</WorkingDaysFrom>\n" +
                "<WorkingDaysTill>4</WorkingDaysTill>\n" +
                "<WorkingHoursFrom>09:00</WorkingHoursFrom>\n" +
                "<WorkingHoursTill>20:00</WorkingHoursTill>\n" +
                "</WorkingTime><WorkingTime>\n" +
                "<WorkingDaysFrom>5</WorkingDaysFrom>\n" +
                "<WorkingDaysTill>5</WorkingDaysTill>\n" +
                "<WorkingHoursFrom>09:00</WorkingHoursFrom>\n" +
                "<WorkingHoursTill>20:00</WorkingHoursTill>\n" +
                "</WorkingTime><WorkingTime>\n" +
                "<WorkingDaysFrom>6</WorkingDaysFrom>\n" +
                "<WorkingDaysTill>6</WorkingDaysTill>\n" +
                "<WorkingHoursFrom>09:00</WorkingHoursFrom>\n" +
                "<WorkingHoursTill>19:00</WorkingHoursTill>\n" +
                "</WorkingTime><WorkingTime>\n" +
                "<WorkingDaysFrom>7</WorkingDaysFrom>\n" +
                "<WorkingDaysTill>7</WorkingDaysTill>\n" +
                "<WorkingHoursFrom>09:00</WorkingHoursFrom>\n" +
                "<WorkingHoursTill>19:00</WorkingHoursTill>\n" +
                "</WorkingTime>";
    }
}
