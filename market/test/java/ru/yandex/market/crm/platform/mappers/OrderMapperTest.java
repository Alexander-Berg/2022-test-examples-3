package ru.yandex.market.crm.platform.mappers;

import java.util.List;

import com.google.common.collect.Iterables;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.crm.external.personal.PersonalService;
import ru.yandex.market.crm.platform.commons.RGBType;
import ru.yandex.market.crm.platform.commons.Uid;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.models.Address;
import ru.yandex.market.crm.platform.models.ChangeRequest;
import ru.yandex.market.crm.platform.models.DeliveryDates;
import ru.yandex.market.crm.platform.models.Order;
import ru.yandex.market.crm.platform.models.OrderDelivery;
import ru.yandex.market.crm.platform.models.OrderDeliveryUpdate;
import ru.yandex.market.crm.platform.models.OrderItem;
import ru.yandex.market.crm.platform.models.OrderProperty;
import ru.yandex.market.crm.platform.models.RecipientPerson;
import ru.yandex.market.crm.util.ResourceHelpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class OrderMapperTest {
    private List<Order> orders;
    private Order order;
    private OrdersEnricher ordersEnricher;

    @Before
    public void setUp() {
        byte[] resource = ResourceHelpers.getResource("order.json");
        var personalService = Mockito.mock(PersonalService.class);
        ordersEnricher = new OrdersEnricher(personalService);
        orders = new OrderMapper(ordersEnricher).apply(resource);
        order = Iterables.getFirst(orders, null);
    }

    @Test
    public void checkCreationDate() {
        assertEquals(1524068485586L, order.getCreationDate());
    }

    @Test
    public void checkId() {
        assertEquals(2233585, order.getId());
    }

    @Test
    public void checkMultiOrderId() {
        assertEquals("3e5fed2a-921c-4fe2-baa6-f9f440cd88d3", order.getMultiOrderId());
    }

    @Test
    public void checkPaymentType() {
        assertEquals("POSTPAID", order.getPaymentType());
    }

    @Test
    public void checkUpdateDate() {
        assertEquals(1524673290000L, order.getUpdateDate());
    }

    @Test
    public void checkDeliveryType() {
        assertEquals("PICKUP", order.getDeliveryAfter().getType());
        assertEquals("PICKUP", order.getDelivery().getType());
    }

    @Test
    public void checkStatus() {
        assertEquals("CANCELLED", order.getStatus());
    }

    @Test
    public void checkRgb() {
        assertEquals(RGBType.BLUE, order.getRgb());
    }

    @Test
    public void checkYandexuid() {
        assertEquals("", order.getUserIds().getYandexuid());
    }

    @Test
    public void checkEmail() {
        assertId("vcty@fthg.ru", UidType.EMAIL);
    }

    @Test
    public void checkPuid() {
        boolean absent = order.getUidList().stream()
                .anyMatch(uid -> uid.getType() == UidType.PUID);

        Assert.assertFalse(absent);
    }

    @Test
    public void checkMuid() {
        long actual = order.getUidList().stream()
                .filter(uid -> uid.getType() == UidType.MUID)
                .map(Uid::getIntValue)
                .findFirst().orElse(0L);

        assertEquals(1152921504654666362L, actual);
    }

    @Test
    public void checkUuid() {
        assertId("8c6e29a136fa5a5d5c0cd5216278c71b", UidType.UUID);
    }

    @Test
    public void checkPhone() {
        assertId("79152823625", UidType.PHONE);
    }

    @Test
    public void checkUid() {
        assertEquals(UidType.MUID, order.getKeyUid().getType());
        assertEquals(1152921504654666362L, order.getKeyUid().getIntValue());
    }

    @Test
    public void checkItemId() {
        OrderItem item = order.getItems(0);
        assertEquals(3364582, item.getId());
    }

    @Test
    public void checkItemModelId() {
        OrderItem item = order.getItems(0);
        assertEquals(1759344275, item.getModelId());
    }

    @Test
    public void checkItemWareMD5() {
        OrderItem item = order.getItems(0);
        assertEquals("aOxf0dyzVNlJX5y32ZUT0w", item.getWareMd5());
    }

    @Test
    public void checkItemOfferName() {
        OrderItem item = order.getItems(0);
        assertEquals("Смартфон OnePlus 5 128GB темно-серый", item.getOfferName());
    }

    @Test
    public void checkItemSku() {
        OrderItem item = order.getItems(0);
        assertEquals("100131946151", item.getSku());
    }

    @Test
    public void checkItemHid() {
        OrderItem item = order.getItems(0);
        assertEquals(91491, item.getHid());
    }

    @Test
    public void checkItemCount() {
        OrderItem item = order.getItems(0);
        assertEquals(1, item.getCount());
    }

    @Test
    public void checkItemPrice() {
        OrderItem item = order.getItems(0);
        assertEquals(1699900, item.getPrice());
    }

    @Test
    public void checkItemPicture() {
        OrderItem item = order.getItems(0);
        assertEquals("//avatars.mds.yandex.net/get-marketpictesting/1363003/market_ZFwbxQqvQ9VBA0wN_O4p1Q/",
                item.getPicture());
    }

    @Test
    public void checkItemSupplierId() {
        OrderItem item = order.getItems(0);
        assertEquals(10264169, item.getSupplierId());
    }

    @Test
    public void checkDelivery() {
        OrderDelivery delivery = order.getDelivery();
        assertEquals("PICKUP", delivery.getType());
        assertEquals("20-04-2018", delivery.getFromDate());
        assertEquals("20-04-2018", delivery.getToDate());
        assertEquals(213, delivery.getRegionId());
        assertEquals(107, delivery.getDeliveryServiceId());
        assertEquals("YANDEX_MARKET", delivery.getDeliveryPartnerType());
        assertTrue(order.getFulfilment());
    }

    @Test
    public void checkBuyerAddress() {
        Address buyerAddress = order.getDelivery().getBuyerAddress();
        assertEquals("apartment1", buyerAddress.getApartment());
        assertEquals("block1", buyerAddress.getBlock());
        assertEquals("building1", buyerAddress.getBuilding());
        assertEquals("Москва", buyerAddress.getCity());
        assertEquals("Россия", buyerAddress.getCountry());
        assertEquals("1", buyerAddress.getEntrance());
        assertEquals("123", buyerAddress.getEntryPhone());
        assertEquals("estate1", buyerAddress.getEstate());
        assertEquals("4", buyerAddress.getFloor());
        assertEquals("30.70079620061024,61.71145050985435", buyerAddress.getGps());
        assertEquals("16", buyerAddress.getHouse());
        assertEquals("139", buyerAddress.getKm());
        assertEquals(Address.AddressLanguage.RUS, buyerAddress.getLanguage());
        assertEquals("Заметка", buyerAddress.getNotes());
        assertEquals("outletName1", buyerAddress.getOutletName());
        assertEquals(2, buyerAddress.getOutletPhonesCount());
        assertEquals("+7 999 123456", buyerAddress.getPhone());
        assertEquals("123000", buyerAddress.getPostcode());
        assertEquals(213L, buyerAddress.getPreciseRegionId());
        assertEquals("", buyerAddress.getRecipient());
        assertEquals(RecipientPerson.newBuilder().setFirstName("Иван").setLastName("Иванов").build(),
                buyerAddress.getRecipientPerson());
        assertEquals("scheduleString1", buyerAddress.getScheduleString());
        assertEquals("Льва Толстого", buyerAddress.getStreet());
        assertEquals("subway1", buyerAddress.getSubway());
        assertEquals(Address.AddressType.BUYER, buyerAddress.getType());
    }

    @Test
    public void checkNoLastDeliveryUpdateIfNoChanges() {
        Assert.assertTrue(order.hasDeliveryAfter());
        Assert.assertTrue(order.hasDelivery());
        Assert.assertFalse(order.hasLastOrderDeliveryUpdate());
    }

    @Test
    public void checkLastDeliveryUpdate() {
        List<Order> orders = new OrderMapper(ordersEnricher).apply(ResourceHelpers.getResource("order_delivery_update.json"));
        Order order = orders.get(0);

        OrderDelivery delivery = order.getDelivery();
        assertEquals("PICKUP", delivery.getType());
        assertEquals("23-06-2018", delivery.getFromDate());
        assertEquals("26-06-2018", delivery.getToDate());
        assertEquals(213, delivery.getRegionId());
        assertEquals(106, delivery.getDeliveryServiceId());
        assertEquals("YANDEX_MARKET", delivery.getDeliveryPartnerType());

        Assert.assertTrue(order.hasLastOrderDeliveryUpdate());
        OrderDeliveryUpdate lastOrderDeliveryUpdate = order.getLastOrderDeliveryUpdate();
        assertEquals("22-06-2018", lastOrderDeliveryUpdate.getBefore().getFromDate());
        assertEquals("25-06-2018", lastOrderDeliveryUpdate.getBefore().getToDate());

        assertEquals("23-06-2018", lastOrderDeliveryUpdate.getAfter().getFromDate());
        assertEquals("26-06-2018", lastOrderDeliveryUpdate.getAfter().getToDate());
    }

    @Test
    public void checkPostOutlet() {
        List<Order> orders = new OrderMapper(ordersEnricher).apply(ResourceHelpers.getResource("post_order_delivery_update.json"));
        Order order = orders.get(0);

        OrderDelivery delivery = order.getDelivery();
        Address outletAddress = delivery.getOutletAddress();
        assertEquals("Москва", outletAddress.getCity());
        assertEquals("Дмитровское", outletAddress.getStreet());
        assertEquals("165Е", outletAddress.getHouse());
        assertEquals("<WorkingTime></WorkingTime>", delivery.getOutletAddress().getScheduleString());

        OrderDelivery deliveryBefore = order.getDeliveryBefore();
        Address beforeOutletAddress = deliveryBefore.getOutletAddress();
        assertEquals("Москва", beforeOutletAddress.getCity());
        assertEquals("Дмитровское", beforeOutletAddress.getStreet());
        assertEquals("165Е", beforeOutletAddress.getHouse());
        assertEquals("<WorkingTime></WorkingTime>", deliveryBefore.getOutletAddress().getScheduleString());
    }

    @Test
    public void checkLastDeliveryAddressUpdate() {
        List<Order> orders = new OrderMapper(ordersEnricher).apply(ResourceHelpers.getResource("order_delivery_address_update.json"));
        Order order = orders.get(0);

        OrderDelivery delivery = order.getDelivery();
        assertEquals("PICKUP", delivery.getType());
        assertEquals("22-06-2018", delivery.getFromDate());
        assertEquals("25-06-2018", delivery.getToDate());
        assertEquals(213, delivery.getRegionId());
        assertEquals(106, delivery.getDeliveryServiceId());
        assertEquals("YANDEX_MARKET", delivery.getDeliveryPartnerType());

        Address buyerAddress = order.getDelivery().getBuyerAddress();
        assertEquals("Москва", buyerAddress.getCity());
        assertEquals("Россия", buyerAddress.getCountry());
        assertEquals("1", buyerAddress.getEntrance());
        assertEquals("123", buyerAddress.getEntryPhone());
        assertEquals("4", buyerAddress.getFloor());
        assertEquals("30.70079620061024,61.71145050985435", buyerAddress.getGps());
        assertEquals("18", buyerAddress.getHouse());
        assertEquals("139", buyerAddress.getKm());
        assertEquals(Address.AddressLanguage.RUS, buyerAddress.getLanguage());
        assertEquals("Заметка", buyerAddress.getNotes());
        assertEquals("+7 999 123456", buyerAddress.getPhone());
        assertEquals("123000", buyerAddress.getPostcode());
        assertEquals(213L, buyerAddress.getPreciseRegionId());
        assertEquals(RecipientPerson.newBuilder().setFirstName("Иван").setLastName("Иванов").build(),
                buyerAddress.getRecipientPerson());
        assertEquals("Льва Толстого", buyerAddress.getStreet());

        Assert.assertTrue(order.hasLastOrderDeliveryUpdate());
        OrderDeliveryUpdate lastOrderDeliveryUpdate = order.getLastOrderDeliveryUpdate();
        assertEquals("16", lastOrderDeliveryUpdate.getBefore().getBuyerAddress().getHouse());

        assertEquals("18", lastOrderDeliveryUpdate.getAfter().getBuyerAddress().getHouse());
    }

    @Test
    public void reasonTest() {
        Order order = new OrderMapper(ordersEnricher).apply(ResourceHelpers.getResource("order_delivery_update.json")).get(0);
        assertEquals("USER_MOVED_DELIVERY_DATES", order.getReason());

        order = new OrderMapper(ordersEnricher).apply(ResourceHelpers.getResource("new_order.json")).get(0);
        assertTrue(order.getReason().isEmpty());
    }

    @Test
    public void outletTest() {
        Order order = new OrderMapper(ordersEnricher).apply(ResourceHelpers.getResource("order_delivery_update.json")).get(0);

        OrderDelivery delivery = order.getDelivery();

        assertEquals("YANDEX_MARKET", delivery.getDeliveryPartnerType());
        assertEquals("365378", delivery.getOutletCode());
        assertEquals(11691648L, delivery.getOutletId());
        assertEquals("Москва", delivery.getOutletAddress().getCity());
        assertEquals("Дмитровское", delivery.getOutletAddress().getStreet());
        assertEquals("165Е", delivery.getOutletAddress().getHouse());
        assertEquals("2020-05-01", delivery.getOutletStorageLimitDate());
        assertEquals(14L, delivery.getOutletStoragePeriod());
        assertEquals("<WorkingTime></WorkingTime>", delivery.getOutletAddress().getScheduleString());
    }

    @Test
    public void checkNoDeliveryUpdateInNewOrder() {
        List<Order> orders = new OrderMapper(ordersEnricher).apply(ResourceHelpers.getResource("new_order.json"));
        Order order = orders.get(0);

        Assert.assertFalse(order.hasLastOrderDeliveryUpdate());
        Assert.assertTrue(order.hasDelivery());

        OrderDelivery delivery = order.getDelivery();
        assertEquals("PICKUP", delivery.getType());
        assertEquals("25-07-2018", delivery.getFromDate());
        assertEquals("26-07-2018", delivery.getToDate());
        assertEquals(213, delivery.getRegionId());
        assertEquals(106, delivery.getDeliveryServiceId());
        assertEquals("YANDEX_MARKET", delivery.getDeliveryPartnerType());
    }

    @Test
    public void checkChangeRequests() {
        byte[] resource = ResourceHelpers.getResource("order_change_request_status_updated.json");
        orders = new OrderMapper(ordersEnricher).apply(resource);
        order = Iterables.getFirst(orders, null);

        assertNotNull(order);

        List<ChangeRequest> changeRequestList = order.getChangeRequestList();
        assertEquals(1, changeRequestList.size());

        ChangeRequest deliveryDatesChangeRequest = changeRequestList.get(0);
        assertEquals(ChangeRequest.ChangeRequestStatus.APPLIED, deliveryDatesChangeRequest.getChangeRequestStatus());
        assertEquals("DELIVERY_DATES", deliveryDatesChangeRequest.getChangeRequestType());
        assertEquals("USER_MOVED_DELIVERY_DATES", deliveryDatesChangeRequest.getRequestReason());


        DeliveryDates deliveryDates = deliveryDatesChangeRequest.getDeliveryDates();
        assertEquals("12-09-2019", deliveryDates.getFromDate());
        assertEquals("13-09-2019", deliveryDates.getToDate());

        assertEquals("10:00:00", deliveryDates.getFromTime());
        assertEquals("18:00:00", deliveryDates.getToTime());
    }

    @Test
    public void checkDeadlineStatus() {
        byte[] resource = ResourceHelpers.getResource("order_parcel_delivery_deadline_status_updated.json");
        orders = new OrderMapper(ordersEnricher).apply(resource);
        order = Iterables.getFirst(orders, null);

        assertNotNull(order);
        assertEquals("READY_TO_SHIP", order.getSubstatus());
        assertEquals("DELIVERY_DATES_DEADLINE", order.getDelivery().getDeliveryDeadlineStatus());
    }


    @Test
    public void checkProperties() {
        List<OrderProperty> properties = order.getOrderPropertiesList();
        assertTrue(properties.contains(OrderProperty.newBuilder().setKey("deviceId").setTextValue("{\"androidBuildModel\":\"SM-A307FN\",\"androidDeviceId\":\"146afc8e217c7cb5\",\"googleServiceId\":\"4a100ed2-cc16-447d-844d-481aa6bd3a5e\",\"androidBuildManufacturer\":\"samsung\",\"androidHardwareSerial\":\"unknown\"}").build()));
        assertTrue(properties.contains(OrderProperty.newBuilder().setKey("experiments").setTextValue("market_dj_exp_for_blue_deals=blue_discount_any_region;secret_sale_enabled=1;market_cms_catalog_new=1;market_promo_blue_generic_bundle=1;market_promo_by_user_cart_hids=1;promo_generic_bundle_deny_spread=1").build()));
        assertTrue(properties.contains(OrderProperty.newBuilder().setKey("mrid").setTextValue("1577015933749/d05e03ea0acf96babe4789a2499a0500/7").build()));
        assertTrue(properties.contains(OrderProperty.newBuilder().setKey("multiOrderId").setTextValue("3e5fed2a-921c-4fe2-baa6-f9f440cd88d3").build()));
        assertTrue(properties.contains(OrderProperty.newBuilder().setKey("platform").setTextValue("ANDROID").build()));
    }

    private void assertId(String expected, UidType uidType) {
        String actual = order.getUidList().stream()
                .filter(uid -> uid.getType() == uidType)
                .map(Uid::getStringValue)
                .findFirst().orElse(null);

        assertEquals(expected, actual);
    }
}
