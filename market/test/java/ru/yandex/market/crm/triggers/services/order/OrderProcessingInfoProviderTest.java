package ru.yandex.market.crm.triggers.services.order;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import ru.yandex.market.checkout.checkouter.delivery.AddressImpl;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.LiftType;
import ru.yandex.market.checkout.checkouter.delivery.outlet.ShopOutlet;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.ItemService;
import ru.yandex.market.checkout.checkouter.order.ItemServiceStatus;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.orginfo.model.OrganizationType;
import ru.yandex.market.crm.core.domain.Color;
import ru.yandex.market.crm.core.services.report.ReportService;
import ru.yandex.market.crm.domain.report.Outlet;
import ru.yandex.market.crm.external.blackbox.BlackBoxClient;
import ru.yandex.market.crm.external.loyalty.Coin;
import ru.yandex.market.crm.external.loyalty.MarketLoyaltyClient;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.triggers.utils.OrderUtils;
import ru.yandex.market.loyalty.api.model.coin.CoinStatus;
import ru.yandex.market.loyalty.api.model.coin.CoinType;
import ru.yandex.market.loyalty.api.model.coin.SmartShoppingImageTypes;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerInfoDTO;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerOrgInfoDTO;

import static groovy.json.JsonOutput.toJson;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.common.util.text.Charsets.UTF_8;

@RunWith(MockitoJUnitRunner.class)
public class OrderProcessingInfoProviderTest {
    private static final ThreadLocalRandom RND = ThreadLocalRandom.current();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final LocalDateTime COIN_CREATION_DATE = LocalDateTime.of(2018, 9, 2, 0, 0);
    private static final LocalDateTime COIN_END_DATE = LocalDateTime.of(2018, 10, 2, 0, 0);
    private static final String COIN_IMAGE_URL = "http://example.com/img";
    private static final String EMAIL = "lalala@ololo.ru";
    private static final Uid UID = Uid.asPuid(72365298L);
    private static final String PHONE_NUMBER_FROM_ORDER = "79161234567";
    private static final long REGION_ID = 120999;
    private static final String ORDER_ID = "5856665";
    private static final String SHOP_ORDER_ID = "1397";
    private static final long SHOP_ID = 17;
    private static final String BUYER_ID = "123146778";

    @Mock
    private MbiApiClient mbiApiClient;
    @Mock
    private ReportService reportService;
    @Mock
    private MarketLoyaltyClient marketLoyaltyClient;
    @Mock
    private BlackBoxClient blackBoxClient;

    private OrderProcessingInfoProvider orderProcessingInfoProvider;


    @Before
    public void setUp() {
        OrderInfoProvider orderInfoProvider = new OrderInfoProvider(mbiApiClient, blackBoxClient);
        orderProcessingInfoProvider = new OrderProcessingInfoProvider(
                marketLoyaltyClient, reportService, orderInfoProvider, mbiApiClient
        );
    }

    @Test
    public void testOrdersUniteInBlocks() {
        var orderId1 = 123L;
        var orderId2 = 456L;
        var orderId3 = 789L;
        var orders = List.of(
                createOrder(orderId1, order ->
                        order.getDelivery().setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET)
                ),
                createOrder(orderId2, order ->
                        order.getDelivery().setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET)
                ),
                createOrder(orderId3, order -> order.setPaymentMethod(PaymentMethod.APPLE_PAY), PaymentType.POSTPAID));

        var processingInfo = orderProcessingInfoProvider.getOrderProcessingInfo(orders, UID);
        var ordersBlocks = processingInfo.getOrdersBlocks();

        var sizes = ordersBlocks.stream().map(x -> x.getOrdersData().size()).collect(Collectors.toSet());
        assertFalse(processingInfo.isPrepaid());
        assertEquals(2, ordersBlocks.size());
        assertEquals(Set.of(1, 2), sizes);
        assertEquals(String.format("%d, %d, %d", orderId1, orderId2, orderId3), processingInfo.getOrders());
    }

    @Test
    public void testPostOrdersUniteInBlocks() {
        var orderId1 = 123L;
        var orderId2 = 456L;
        var orderId3 = 789L;

        var outlet = new ShopOutlet();
        outlet.setName("Name");
        outlet.setBuilding("Building");
        outlet.setCity("City");
        outlet.setStreet("Street");
        outlet.setKm("Km");
        outlet.setEstate("Estate");
        outlet.setBlock("Block");
        outlet.setNotes("Notes");

        var orders = List.of(
                createOrder(orderId1, order -> {
                    order.getDelivery().setOutlet(outlet);
                    order.getDelivery().setBuyerAddress(null);
                    order.getDelivery().setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
                }, PaymentType.PREPAID),
                createOrder(orderId2, order -> {
                    order.getDelivery().setOutlet(outlet);
                    order.getDelivery().setBuyerAddress(null);
                    order.getDelivery().setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
                }, PaymentType.PREPAID),
                createOrder(orderId3));

        var info = orderProcessingInfoProvider.getOrderProcessingInfo(orders, UID);
        var ordersBlocks = Collections.unmodifiableList(info.getOrdersBlocks());

        var sizes = ordersBlocks.stream().map(x -> x.getOrdersData().size()).collect(Collectors.toSet());
        assertTrue(info.isPrepaid());
        assertEquals(2, ordersBlocks.size());
        assertEquals(Set.of(1, 2), sizes);
        assertEquals(String.format("%d, %d, %d", orderId1, orderId2, orderId3), info.getOrders());
    }

    @Test
    public void testOrderBlock() {
        var orderId1 = 123L;
        var orderId2 = 456L;
        var orderId3 = 789L;
        var address = new AddressImpl();
        address.setCity("Москва");
        address.setStreet("Льва Толстого");
        address.setHouse("16");

        var orderItem = generateOrderItem(BigDecimal.ONE, 1, 0L);

        var outlet = new ShopOutlet();
        outlet.setName("Name");
        outlet.setBuilding("Building");
        outlet.setCity("City");
        outlet.setStreet("Street");
        outlet.setKm("Km");
        outlet.setEstate("Estate");
        outlet.setBlock("Block");
        outlet.setNotes("Notes");
        Consumer<Order> enricher = order -> {
            order.getDelivery().setBuyerAddress(address);
            order.getDelivery().setOutlet(outlet);
            order.getDelivery().setDeliveryPartnerType(DeliveryPartnerType.SHOP);
            order.setShopOrderId(SHOP_ORDER_ID);
            order.setShopId(SHOP_ID);
            order.setItems(Collections.singletonList(orderItem));
        };

        var orders = List.of(
                createOrder(orderId1, enricher.andThen(order -> {
                    order.setBuyerTotal(BigDecimal.ONE);
                    order.getDelivery().setBuyerPrice(BigDecimal.TEN);
                    order.getPromoPrices().setBuyerItemsTotalBeforeDiscount(BigDecimal.valueOf(4));
                    order.getPromoPrices().setBuyerItemsTotalDiscount(BigDecimal.ZERO);
                }), PaymentType.PREPAID),
                createOrder(orderId2, enricher.andThen(order -> {
                    order.setBuyerTotal(BigDecimal.ONE);
                    order.getDelivery().setBuyerPrice(BigDecimal.ONE);
                    order.getPromoPrices().setBuyerItemsTotalBeforeDiscount(BigDecimal.ONE);
                    order.getPromoPrices().setBuyerItemsTotalDiscount(BigDecimal.valueOf(-7));
                }), PaymentType.PREPAID),
                createOrder(orderId3, enricher.andThen(order -> {
                    order.getDelivery().setLiftType(LiftType.MANUAL);
                    order.getDelivery().setLiftPrice(BigDecimal.ONE);
                    order.setBuyerTotal(BigDecimal.TEN);
                    order.getDelivery().setBuyerPrice(BigDecimal.ONE);
                    order.getPromoPrices().setBuyerItemsTotalBeforeDiscount(BigDecimal.valueOf(8));
                    order.getPromoPrices().setBuyerItemsTotalDiscount(BigDecimal.valueOf(-7));
                }), PaymentType.PREPAID)
        );

        var info = orderProcessingInfoProvider.getOrderProcessingInfo(orders, UID);
        List<OrderProcessingInfoProvider.OrderGroup> orders_blocks = Collections.unmodifiableList(
                info.getOrdersBlocks());

        assertEquals(2, orders_blocks.size());
        var block = orders_blocks.stream()
                .filter(orderGroup -> orderGroup.getOrdersData().size() == 2)
                .findAny().orElseThrow();

        assertTrue(block.getPrepaid());
        assertEquals("2", block.getOrderPrice());
        assertEquals("11", block.getDeliveryPrice());
        assertEquals("5", block.getItemsTotalPrice());
        assertEquals("7", block.getDiscountValue());
        assertEquals(String.format("%d/%s и %d/%s",
                orderId1, SHOP_ORDER_ID, orderId2, SHOP_ORDER_ID), block.getOrdersString());
        assertEquals(2, block.getTotalItems().intValue());
        assertEquals("YANDEX", block.getPaymentMethod());

        var resultAddress = block.getAddress();
        assertEquals(address.getCity(), resultAddress.getCity());
        assertEquals(address.getStreet(), resultAddress.getStreet());
        assertEquals(address.getHouse(), resultAddress.getHouse());

        assertEquals(OrderUtils.getOutletInfo(outlet), block.getOutlet());

        block = orders_blocks.stream()
                .filter(orderGroup -> orderGroup.getOrdersData().size() == 1)
                .findAny().orElseThrow();
        assertEquals("10", block.getOrderPrice());
        assertEquals("1", block.getDeliveryPrice());
        assertEquals("8", block.getItemsTotalPrice());
        assertEquals("7", block.getDiscountValue());
        assertEquals("1", block.getLiftPrice());
        assertEquals("MANUAL", block.getLiftType());

        assertEquals(String.format("%d/%s, %d/%s, %d/%s",
                orderId1, SHOP_ORDER_ID, orderId2, SHOP_ORDER_ID, orderId3, SHOP_ORDER_ID), info.getOrders());
    }

    @Test
    public void testCoinsExists() throws Exception {
        var orders = Collections.singletonList(createOrder(Long.valueOf(ORDER_ID)));
        setupLoyaltyGetCoinsForOrder(Long.parseLong(ORDER_ID), OrderStatus.PROCESSING, marketLoyaltyClient);
        var info = orderProcessingInfoProvider.getOrderProcessingInfo(orders, UID);

        checkCoinsModel(info.getCoins());
    }

    @Test
    public void testGeneralTemplateParams() {
        var orders = Collections.singletonList(createOrder(Long.valueOf(ORDER_ID)));

        var info = orderProcessingInfoProvider.getOrderProcessingInfo(orders, UID);

        assertTrue(info.isPrepaid());
        assertEquals(EMAIL, info.getClientEmail());
        assertFalse(info.isCreditExist());
        var orderListItems = info.getOrderList();
        assertEquals(1, orderListItems.size());
        Map<String, Object> orderListItem = orderListItems.get(0);
        assertEquals(Color.BLUE.name(), orderListItem.get("color"));
        assertEquals(Long.valueOf(ORDER_ID), orderListItem.get("market_order_number"));
        assertEquals(ORDER_ID, orderListItem.get("order_number"));
    }

    @Test
    public void testShopParams() throws Exception {
        var orderId1 = 123L;
        var orderId2 = 456L;
        var orders = List.of(
                createOrder(orderId1, order -> {
                    order.setFulfilment(false);
                    order.getDelivery().setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
                    order.setItems(Collections.singletonList(generateOrderItem(BigDecimal.ONE, 1, 0L)));
                    order.getDelivery().setOutletId(1234L);
                }, PaymentType.PREPAID),
                createOrder(orderId2, order -> {
                    order.getDelivery().setDeliveryPartnerType(DeliveryPartnerType.SHOP);
                    order.setShopOrderId(SHOP_ORDER_ID);
                    order.setShopId(SHOP_ID);
                }, PaymentType.PREPAID)
        );

        setupReportService("1234", "nuuuuuumber");
        PartnerInfoDTO partner1 = createPartnerInfo(null, "Supplier1", "+74951111111", "ogrn1",
                "Fact address 1", "Jur address 1");
        PartnerInfoDTO partner2 = createPartnerInfo(OrganizationType.IP, "Supplier2", "+74952222222", "ogrn2",
                "Fact address 2", "Jur address 2");
        when(mbiApiClient.getPartnerInfo(eq(0L))).thenReturn(partner1);
        when(mbiApiClient.getPartnerInfo(eq(1L))).thenReturn(partner2);

        var info = orderProcessingInfoProvider.getOrderProcessingInfo(orders, UID);

        assertEquals(String.format("%d, %d/%s", orderId1, orderId2, SHOP_ORDER_ID), info.getOrders());

        JSONAssert.assertEquals(loadShopsAsString(), toJson(info.getShops()), JSONCompareMode.LENIENT);
    }

    @Test
    public void testItemServiceTemplateParams() {
        var orders = Collections.singletonList(createOrder(Long.valueOf(ORDER_ID), order -> {
            var firstItem = order.getItems().iterator().next();
            firstItem.addService(buildItemService());
            firstItem.addService(buildItemService());
        }));

        var info = orderProcessingInfoProvider.getOrderProcessingInfo(orders, UID);

        assertTrue(info.isPrepaid());
        assertEquals(EMAIL, info.getClientEmail());
        assertFalse(info.isCreditExist());

        var orderListItems = info.getOrderList();
        assertEquals(1, orderListItems.size());
        Map<String, Object> orderListItem = orderListItems.get(0);
        assertEquals(Color.BLUE.name(), orderListItem.get("color"));
        assertEquals(Long.valueOf(ORDER_ID), orderListItem.get("market_order_number"));
        assertEquals(ORDER_ID, orderListItem.get("order_number"));
        // только уникальные названия
        assertEquals(Collections.singletonList("test title"), orderListItem.get("item_service_titles"));

        var orderBlocks = info.getOrdersBlocks();
        OrderProcessingInfoProvider.OrderGroup orderBlock = orderBlocks.get(0);
        //common info
        assertEquals(2, orderBlock.getItemServicesCount());
        assertEquals("20", orderBlock.getItemServicesTotalPrice());
        //item service info
        var ordersData = orderBlock.getOrdersData();
        var firstOrder = ordersData.get(0);
        var itemServices = firstOrder.getServices();
        var itemService = itemServices.get(0);
        assertEquals("test title", itemService.getTitle());
        assertEquals("test description", itemService.getDescription());
        assertEquals("10", itemService.getPrice());
        assertEquals("10 июня", itemService.getDate());
        assertEquals("10:00", itemService.getFromTime());
        assertEquals("12:00", itemService.getToTime());
        assertEquals("10 июня с 10:00 до 12:00", itemService.getDateIntervalText());
    }

    private ItemService buildItemService() {
        var itemService = new ItemService();
        itemService.setId(RND.nextLong());
        itemService.setServiceId(RND.nextLong());
        itemService.setPrice(BigDecimal.TEN);
        itemService.setTitle("test title");
        itemService.setDescription("test description");
        itemService.setStatus(ItemServiceStatus.NEW);
        var date = Date.from(LocalDate.of(2020, Month.JUNE, 10)
                .atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        itemService.setDate(date);
        itemService.setFromTime(LocalTime.of(10, 0));
        itemService.setToTime(LocalTime.of(12, 0));
        return itemService;
    }

    private void setupReportService(String outlet, String number) {
        Outlet.LegalInfo legalInfo = new Outlet.LegalInfo();
        Outlet.LicenceInfo licenceInfo = new Outlet.LicenceInfo();
        licenceInfo.setNumber(number);
        legalInfo.setLicence(licenceInfo);

        Outlet outl = new Outlet();
        outl.setLegalInfo(legalInfo);

        when(reportService.getOutletInfo(outlet, Color.BLUE)).thenReturn(outl);
    }

    private static PartnerInfoDTO createPartnerInfo(OrganizationType type, String name, String phone, String ogrn, String factAddress,
                                                    String juridicalAddress) {
        return new PartnerInfoDTO(0, 0L, CampaignType.SUPPLIER, name, "", phone, "",
                new PartnerOrgInfoDTO(type, "Supplier Official Name", ogrn, factAddress, juridicalAddress,
                        null, null, null), false, null);
    }

    private void checkCoinsModel(Object coinsJson) throws JSONException, JsonProcessingException {
        assertNotNull(coinsJson);
        JSONAssert.assertEquals("""
                [{
                "coin_type":"FIXED",
                "title":"title",
                "sub_title":"subtitle",
                "nominal":"10",
                "image_url":"http://example.com/img160x160",
                "end_date":"до 02.10.2018"
                }]
                """, OBJECT_MAPPER.writeValueAsString(coinsJson), false);
    }

    private Order createOrder(Long orderId) {
        return createOrder(orderId, order -> {});
    }

    private Order createOrder(Long orderId, Consumer<Order> orderEnricher) {
        return createOrder(orderId, orderEnricher, PaymentType.PREPAID);
    }

    private Order createOrder(Long orderId, Consumer<Order> orderEnricher, PaymentType paymentType) {
        Order order = new Order();
        order.setRgb(ru.yandex.market.checkout.checkouter.order.Color.BLUE);
        order.setPaymentType(paymentType);
        order.setStatus(OrderStatus.PROCESSING);
        order.setId(orderId);
        order.setPaymentMethod(PaymentMethod.YANDEX);

        Buyer buyer = new Buyer();
        buyer.setId(BUYER_ID);
        buyer.setEmail(EMAIL);
        buyer.setUid(Long.parseLong(UID.getValue()));
        buyer.setNormalizedPhone(PHONE_NUMBER_FROM_ORDER);
        order.setBuyer(buyer);

        order.setDelivery(generateOrderDelivery());

        List<OrderItem> items = new ArrayList<>();
        int count = 2;
        var total = BigDecimal.ZERO;
        for (var i = 0; i < count; i++) {
            var itemCount = RND.nextInt(10);
            var price = new BigDecimal(RND.nextInt(10_000));
            items.add(generateOrderItem(price, itemCount, (long) i));
            total = total.add(price.multiply(BigDecimal.valueOf(itemCount)));
        }
        order.setItems(items);
        order.setBuyerItemsTotal(total);

        orderEnricher.accept(order);
        return order;
    }

    private Delivery generateOrderDelivery() {
        Delivery delivery = new Delivery();
        Date date = Date.from(LocalDate.now().plusDays(2).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        delivery.setDeliveryDates(new DeliveryDates(date, date));
        final AddressImpl address = new AddressImpl();
        address.setCountry("Россия");
        address.setCity("Москва");
        address.setStreet("Льва Толстого");
        address.setHouse("16");
        delivery.setRegionId(REGION_ID);
        delivery.setBuyerAddress(address);

        return delivery;
    }

    private OrderItem generateOrderItem(BigDecimal price, int count, Long ffShopId) {
        OrderItem item = new OrderItem(
                new FeedOfferId(UUID.randomUUID().toString(), RND.nextLong()),
                price, count
        );
        item.setModelId(Math.abs(RND.nextLong()));
        item.setOfferName(UUID.randomUUID().toString());
        item.setSupplierId(ffShopId);
        return item;
    }

    private void setupLoyaltyGetCoinsForOrder(long orderId,
                                              OrderStatus orderStatus,
                                              MarketLoyaltyClient marketLoyaltyClient) {
        when(marketLoyaltyClient.getCoinsForOrder(orderId, orderStatus.name())).thenReturn(Collections.singletonList(
                new Coin(1L, 123L, "title", "subtitle", CoinType.FIXED.name(), BigDecimal.TEN,
                        "description", "inactiveDescription",
                        COIN_CREATION_DATE, COIN_CREATION_DATE, COIN_END_DATE, COIN_IMAGE_URL,
                        Collections.singletonMap(SmartShoppingImageTypes.STANDARD, COIN_IMAGE_URL),
                        "#000000", CoinStatus.ACTIVE.name(), false, "123",
                        "reason", "reasonParam", null, null)
                )
        );
    }

    private String loadShopsAsString() {
        try {
            return IOUtils.toString(getClass().getResourceAsStream("shops.json"), UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
