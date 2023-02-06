package ru.yandex.market.delivery.mdbapp.integration.utils;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.SneakyThrows;
import org.mockito.stubbing.Answer;

import ru.yandex.market.checkout.checkouter.delivery.AddressLanguage;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.returns.ReturnDelivery;
import ru.yandex.market.checkout.checkouter.returns.ReturnItem;
import ru.yandex.market.checkout.checkouter.returns.ReturnItemType;
import ru.yandex.market.checkout.checkouter.returns.SenderAddress;
import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.PickupPoint;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.ReturnRequest;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.ReturnRequestItem;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.dto.PickupPointDto;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.dto.ReturnRequestDto;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.dto.ReturnRequestItemDto;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.type.ReturnClientType;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.type.ReturnReasonType;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.type.ReturnRequestState;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.type.ReturnStatus;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.type.ReturnSubreason;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.type.ReturnType;
import ru.yandex.market.delivery.mdbapp.components.storage.dto.ReturnRequestUpdateDto;
import ru.yandex.market.logistics.lom.model.dto.CombinatorRoute;
import ru.yandex.market.logistics.lom.model.dto.LocationDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.lom.model.enums.SegmentType;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerSubtypeResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PickupPointType;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.mdb.lrm.client.model.CourierInterval;
import ru.yandex.market.logistics.mdb.lrm.client.model.CourierReturnAddress;
import ru.yandex.market.logistics.mdb.lrm.client.model.CourierReturnClient;
import ru.yandex.market.logistics.mdb.lrm.client.model.CourierReturnItem;
import ru.yandex.market.logistics.mdb.lrm.client.model.CreateClientCourierReturnRequest;
import ru.yandex.market.logistics.mdb.lrm.client.model.CreateReturnRequest;
import ru.yandex.market.logistics.mdb.lrm.client.model.Dimensions;
import ru.yandex.market.logistics.mdb.lrm.client.model.OrderItemInfo;
import ru.yandex.market.logistics.mdb.lrm.client.model.ReturnBoxRequest;
import ru.yandex.market.logistics.mdb.lrm.client.model.ReturnSource;
import ru.yandex.market.pvz.client.logistics.dto.ReturnDto;
import ru.yandex.market.pvz.client.logistics.dto.ReturnItemCreateDto;
import ru.yandex.market.pvz.client.logistics.dto.ReturnRequestCreateDto;
import ru.yandex.market.sc.internal.model.ContactDto;
import ru.yandex.market.sc.internal.model.CourierDto;
import ru.yandex.market.sc.internal.model.CreateClientReturnDto;
import ru.yandex.market.sc.internal.model.PersonDto;
import ru.yandex.market.sc.internal.model.SenderDto;
import ru.yandex.market.sc.internal.model.WarehouseDto;
import ru.yandex.market.tpl.internal.client.model.clientreturn.ClientReturnCreateDto;

public class ReturnRequestTestUtils {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static final long ORDER_ID = 167802870L;
    public static final long SENDER_ID = 1789235L;
    public static final long COMMITED_ORDER_ID = 167802872L;
    public static final long LOM_ORDER_ID = 267802870L;
    public static final long RETURN_ID = 7832L;
    public static final long LRM_RETURN_ID = 11111L;
    public static final String RETURN_ID_STR = Long.toString(RETURN_ID);
    public static final long COMMITED_RETURN_ID = 9702;
    public static final String COMMITED_RETURN_ID_STR = Long.toString(COMMITED_RETURN_ID);
    public static final long RETURN_REQUEST_ID = 6511L;
    public static final LocalDateTime RETURN_CREATED_AT = LocalDateTime.of(2021, 2, 13, 15, 37, 54);
    public static final LocalDate EXPIRATION_DATE = LocalDate.of(2021, 2, 17);
    public static final String ITEM_NAME = "Красные лабутены 43го размера";
    public static final String RETURN_ITEM_REASON = "bad quality";
    public static final long ITEM_ID_1 = 4567L;
    public static final double PRICE_1 = 99.99;
    public static final String ITEM_SKU_1 = "item-sku-1";
    public static final Long ITEM_SUPPLIER_1 = 1L;
    public static final String ITEM_UIT_1 = "item-uit-1";
    public static final long FEED_ID_1 = 99990L;
    public static final String OFFER_ID_1 = "offer_id_1";
    public static final long ITEM_ID_2 = 9999L;
    public static final double PRICE_2 = 45.67;
    public static final String ITEM_SKU_2 = "item-sku-2";
    public static final Long ITEM_SUPPLIER_2 = 2L;
    public static final long FEED_ID_2 = 45671L;
    public static final String OFFER_ID_2 = "offer_id_2";
    public static final long RETURN_DS_ID = 2381L;
    public static final long PICKUP_POINT_LMS_ID = 6871L;
    public static final String PICKUP_POINT_EXTERNAL_ID = "68710";
    public static final String DS_OUTLET_CODE = "dsOutletCode";
    public static final long PICKUP_POINT_MBI_ID = 4860L;
    public static final String BUYER_NAME = "Константин Вячеславович Воронцов";
    public static final Long PARTNER_ID = 7235L;
    public static final List<Long> MARKET_PVZ_SUBTYPE_IDS = List.of(3L, 4L);
    public static final Long NON_MARKET_PVZ_SUBTYPE_ID = 999L;
    public static final Long DESTINATION_SC_PARTNER_ID = 123L;
    public static final Long DESTINATION_SC_LOGISTIC_POINT_ID = 123456L;
    //public static final Long DESTINATION_SC_ORDER_ID_SHIFT = 1000000L;
    public static final Long MIDDLE_SC_PARTNER_ID = 124L;
    public static final Long MIDDLE_SC_LOGISTIC_POINT_ID = 12345L;
    //public static final String BARCODE_FF = "VOZVRAT_SF_PVZ_" + RETURN_ID;
    public static final String COMMITED_RETURN_BARCODE_FF = "VOZVRAT_SF_PVZ_" + COMMITED_RETURN_ID_STR;
    //public static final String BARCODE_DROPSHIP = "VOZVRAT_FBS_" + DESTINATION_SC_LOGISTIC_POINT_ID + "_" + RETURN_ID;
    //public static final String BARCODE_DROPSHIP_TAR = "VOZVRAT_TAR_" + RETURN_ID;
    public static final String BARCODE_DROPSHIP_LRM = "VOZ_FBS_" + RETURN_ID;
    public static final String BARCODE_FF_LRM = "VOZ_FF_" + RETURN_ID;
    private static final CourierDto FAKE_COURIER = new CourierDto(404L, "UNKNOWN_COURIER", null);
    private static final String SENDER_ADDRESS_CITY = "sender-address-city";
    private static final String SENDER_ADDRESS_STREET = "sender-address-street";
    private static final String SENDER_ADDRESS_HOUSE = "sender-address-house";
    private static final String SENDER_ADDRESS_ENTRANCE = "sender-address-entrance";
    private static final String SENDER_ADDRESS_APARTMENT = "sender-address-apartment";
    private static final String SENDER_ADDRESS_FLOOR = "sender-address-floor";
    private static final String SENDER_ADDRESS_ENTRY_PHONE = "sender-address-entry-phone";
    private static final String SENDER_ADDRESS_LAT = "55.75";
    private static final String SENDER_ADDRESS_LON = "37.62";
    private static final String SENDER_ADDRESS_SENDER = "sender-address-sender";
    private static final String SENDER_ADDRESS_PHONE = "sender-address-phone";
    private static final String SENDER_ADDRESS_EMAIL = "sender-address-email";
    private static final String SENDER_ADDRESS_PERSONAL_FULLNAME_ID = "sender-address-personal-fullname-id";
    private static final String SENDER_ADDRESS_PERSONAL_PHONE_ID = "sender-address-personal-phone-id";
    private static final String SENDER_ADDRESS_PERSONAL_EMAIL_ID = "sender-address-personal-email-id";
    public static final String SENDER_ADDRESS_PERSONAL_ADDRESS_ID = "sender-address-personal-address-id";
    public static final String LOGISTICS_PERSONAL_ADDRESS_ID = "logistics-personal-address-id";
    private static final String SENDER_ADDRESS_PERSONAL_GPS_COORD_ID = "sender-address-personal-gps-coord-id";
    private static final String SENDER_ADDRESS_COMMENT = "sender-address-comment";
    private static final LocalDate DELIVERY_DATE = LocalDate.of(2022, 3, 4);
    private static final long DELIVERY_SERVICE_ID = 9000;
    private static final String ITEM_TITLE = "item-title";
    private static final String ITEM_CATEGORY_NAME = "item-category-name";
    private static final String ITEM_DESCRIPTION = "item-description";
    private static final String ITEM_PREVIEW_URL = "item-preview-url";
    private static final String ITEM_DETAILS_URL = "item-details-url";
    private static final List<String> ITEM_PHOTO_URLS = List.of(
        "https://example.com/item-photo-1",
        "https://example.com/item-photo-2"
    );

    private ReturnRequestTestUtils() {
    }

    @Nonnull
    public static Answer<Object> setJpaIds() {
        return invocation -> {
            ReturnRequest returnRequest = (ReturnRequest) invocation.getArguments()[0];
            returnRequest.setId(RETURN_REQUEST_ID);
            AtomicLong i = new AtomicLong(1);
            returnRequest.getItems()
                .forEach(item -> item.setId(i.getAndIncrement()));
            return returnRequest;
        };
    }

    @Nonnull
    public static OrderHistoryEvent orderHistoryEvent() {
        final OrderHistoryEvent event = new OrderHistoryEvent();
        event.setOrderAfter(checkouterOrder());
        event.setReturnId(RETURN_ID);
        return event;
    }

    @Nonnull
    public static Order checkouterOrder() {
        Buyer buyer = new Buyer();
        buyer.setLastName("Воронцов");
        buyer.setFirstName("Константин");
        buyer.setMiddleName("Вячеславович");

        Order order = new Order();
        order.setId(ORDER_ID);
        order.setShopId(1L);
        order.setBuyer(buyer);
        order.addItem(orderItem(ITEM_ID_1, PRICE_1, FEED_ID_1, OFFER_ID_1, ITEM_SKU_1, ITEM_SUPPLIER_1, ITEM_UIT_1));
        order.addItem(orderItem(ITEM_ID_2, PRICE_2, FEED_ID_2, OFFER_ID_2, ITEM_SKU_2, ITEM_SUPPLIER_2, null));

        return order;
    }

    @Nonnull
    @SneakyThrows
    public static OrderItem orderItem(
        long itemId, double price, long feedId, String offerId, String sku,
        Long supplierId, String uit
    ) {
        OrderItem orderItem = new OrderItem();
        orderItem.setId(itemId);
        orderItem.setPrice(BigDecimal.valueOf(price));
        orderItem.setFeedId(feedId);
        orderItem.setOfferId(offerId);
        orderItem.setShopSku(sku);
        orderItem.setSupplierId(supplierId);
        if (uit != null) {
            orderItem.setInstances(
                OBJECT_MAPPER.readValue("[{\"UIT\": \"" + uit + "\"}]", ArrayNode.class)
            );
        }
        return orderItem;
    }

    @Nonnull
    public static Return checkouterReturnWithDelivery() {
        Return aReturn = checkouterReturn();
        aReturn.setDelivery(returnDelivery());
        return aReturn;
    }

    @Nonnull
    public static Return checkouterReturn() {
        Return aReturn = new Return();
        aReturn.setOrderId(ORDER_ID);
        aReturn.setId(RETURN_ID);
        aReturn.setCreatedAt(RETURN_CREATED_AT.atZone(ZoneId.systemDefault()).toInstant());
        aReturn.setItems(List.of(
            returnItem(ITEM_ID_1),
            returnItem(ITEM_ID_2),
            deliveryServiceReturnItem()
        ));
        return aReturn;
    }

    @Nonnull
    public static Return checkouterCourierReturn() {
        Return result = new Return();
        result.setId(RETURN_ID);
        result.setOrderId(ORDER_ID);

        ReturnDelivery delivery = new ReturnDelivery();
        result.setDelivery(delivery);
        delivery.setType(DeliveryType.DELIVERY);
        delivery.setDeliveryServiceId(DELIVERY_SERVICE_ID);

        SenderAddress senderAddress = new SenderAddress(
            "sender-address-country",
            SENDER_ADDRESS_CITY,
            SENDER_ADDRESS_SENDER,
            SENDER_ADDRESS_PERSONAL_ADDRESS_ID,
            SENDER_ADDRESS_PERSONAL_FULLNAME_ID,
            SENDER_ADDRESS_PHONE,
            SENDER_ADDRESS_PERSONAL_PHONE_ID,
            AddressLanguage.RUS,
            SENDER_ADDRESS_LON + "," + SENDER_ADDRESS_LAT,
            SENDER_ADDRESS_PERSONAL_GPS_COORD_ID,
            SENDER_ADDRESS_EMAIL,
            SENDER_ADDRESS_PERSONAL_EMAIL_ID
        );
        senderAddress.setStreet(SENDER_ADDRESS_STREET);
        senderAddress.setHouse(SENDER_ADDRESS_HOUSE);
        senderAddress.setEntrance(SENDER_ADDRESS_ENTRANCE);
        senderAddress.setApartment(SENDER_ADDRESS_APARTMENT);
        senderAddress.setFloor(SENDER_ADDRESS_FLOOR);
        senderAddress.setEntryPhone(SENDER_ADDRESS_ENTRY_PHONE);
        senderAddress.setNotes(SENDER_ADDRESS_COMMENT);
        delivery.setSenderAddress(senderAddress);

        DeliveryDates dates = new DeliveryDates();
        delivery.setDates(dates);
        Date deliveryDate = Date.from(DELIVERY_DATE.atStartOfDay(DateTimeUtils.MOSCOW_ZONE).toInstant());
        dates.setFromDate(deliveryDate);
        dates.setFromTime(LocalTime.of(10, 11));
        dates.setToDate(deliveryDate);
        dates.setToTime(LocalTime.of(12, 13));

        result.setItems(List.of(
            courierReturnItem(ITEM_ID_1),
            emptyCourierReturnItem(ITEM_ID_2),
            nonOrderReturnItem()
        ));

        return result;
    }

    @Nonnull
    public static OrderDto lomOrder(PartnerType partnerType) {
        OrderDto lomOrder = new OrderDto();
        lomOrder.setId(LOM_ORDER_ID);
        lomOrder.setSenderId(SENDER_ID);
        lomOrder.setExternalId(String.valueOf(ORDER_ID));
        lomOrder.setBarcode(String.valueOf(ORDER_ID));
        lomOrder.setWaybill(List.of(
            WaybillSegmentDto.builder()
                .id(1L)
                .partnerId(101L)
                .partnerType(partnerType)
                .segmentType(SegmentType.FULFILLMENT)
                .warehouseLocation(LocationDto.builder().warehouseId(DESTINATION_SC_LOGISTIC_POINT_ID - 1).build())
                .build(),
            WaybillSegmentDto.builder()
                .id(2L)
                .segmentType(SegmentType.SORTING_CENTER)
                .partnerType(PartnerType.DELIVERY)
                .partnerId(DESTINATION_SC_PARTNER_ID)
                .warehouseLocation(LocationDto.builder().warehouseId(DESTINATION_SC_LOGISTIC_POINT_ID).build())
                .shipment(
                    WaybillSegmentDto.ShipmentDto.builder()
                        .locationFrom(LocationDto.builder().warehouseId(DESTINATION_SC_LOGISTIC_POINT_ID - 1).build())
                        .build()
                )
                .returnPartnerId(101L)
                .build(),
            WaybillSegmentDto.builder()
                .id(3L)
                .segmentType(SegmentType.SORTING_CENTER)
                .partnerType(PartnerType.SORTING_CENTER)
                .partnerId(MIDDLE_SC_PARTNER_ID)
                .warehouseLocation(LocationDto.builder().warehouseId(MIDDLE_SC_LOGISTIC_POINT_ID).build())
                .shipment(
                    WaybillSegmentDto.ShipmentDto.builder()
                        .locationFrom(LocationDto.builder().warehouseId(DESTINATION_SC_LOGISTIC_POINT_ID).build())
                        .build()
                )
                .returnPartnerId(DESTINATION_SC_PARTNER_ID)
                .build(),
            WaybillSegmentDto.builder()
                .id(4L)
                .partnerId(401L)
                .segmentType(SegmentType.COURIER)
                .returnPartnerId(MIDDLE_SC_PARTNER_ID)
                .build()
        ));
        return lomOrder;
    }

    @Nonnull
    public static OrderDto lomOrderDropshipWithoutSc() {
        OrderDto lomOrder = new OrderDto();
        lomOrder.setId(LOM_ORDER_ID);
        lomOrder.setExternalId(String.valueOf(ORDER_ID));
        lomOrder.setBarcode(String.valueOf(ORDER_ID));
        lomOrder.setWaybill(List.of(
            WaybillSegmentDto.builder()
                .id(1L)
                .partnerId(101L)
                .partnerType(PartnerType.DROPSHIP)
                .segmentType(SegmentType.FULFILLMENT)
                .warehouseLocation(LocationDto.builder().warehouseId(DESTINATION_SC_LOGISTIC_POINT_ID - 1).build())
                .build(),
            WaybillSegmentDto.builder()
                .id(4L)
                .partnerId(401L)
                .segmentType(SegmentType.COURIER)
                .returnPartnerId(MIDDLE_SC_PARTNER_ID)
                .build()
        ));
        return lomOrder;
    }

    @Nonnull
    public static OrderDto lomOrderWithDropoff() {
        OrderDto lomOrder = new OrderDto();
        lomOrder.setId(LOM_ORDER_ID);
        lomOrder.setExternalId(String.valueOf(ORDER_ID));
        lomOrder.setBarcode(String.valueOf(ORDER_ID));
        lomOrder.setWaybill(List.of(
            WaybillSegmentDto.builder()
                .id(1L)
                .partnerId(101L)
                .partnerType(PartnerType.DROPSHIP)
                .segmentType(SegmentType.FULFILLMENT)
                .warehouseLocation(LocationDto.builder().warehouseId(DESTINATION_SC_LOGISTIC_POINT_ID - 2).build())
                .build(),
            WaybillSegmentDto.builder()
                .id(2L)
                .partnerType(PartnerType.SORTING_CENTER)
                .segmentType(SegmentType.SORTING_CENTER)
                .partnerId(DESTINATION_SC_PARTNER_ID - 1)
                .warehouseLocation(LocationDto.builder().warehouseId(DESTINATION_SC_LOGISTIC_POINT_ID - 1).build())
                .shipment(
                    WaybillSegmentDto.ShipmentDto.builder()
                        .locationFrom(LocationDto.builder().warehouseId(DESTINATION_SC_LOGISTIC_POINT_ID - 2).build())
                        .build()
                )
                .returnPartnerId(101L)
                .build(),
            WaybillSegmentDto.builder()
                .id(3L)
                .partnerType(PartnerType.SORTING_CENTER)
                .segmentType(SegmentType.SORTING_CENTER)
                .partnerId(DESTINATION_SC_PARTNER_ID)
                .warehouseLocation(LocationDto.builder().warehouseId(DESTINATION_SC_LOGISTIC_POINT_ID).build())
                .shipment(
                    WaybillSegmentDto.ShipmentDto.builder()
                        .locationFrom(LocationDto.builder().warehouseId(DESTINATION_SC_LOGISTIC_POINT_ID - 1).build())
                        .build()
                )
                .returnPartnerId(DESTINATION_SC_PARTNER_ID - 1)
                .build(),
            WaybillSegmentDto.builder()
                .id(4L)
                .partnerType(PartnerType.SORTING_CENTER)
                .segmentType(SegmentType.SORTING_CENTER)
                .partnerId(MIDDLE_SC_PARTNER_ID)
                .warehouseLocation(LocationDto.builder().warehouseId(MIDDLE_SC_LOGISTIC_POINT_ID).build())
                .shipment(
                    WaybillSegmentDto.ShipmentDto.builder()
                        .locationFrom(LocationDto.builder().warehouseId(DESTINATION_SC_LOGISTIC_POINT_ID).build())
                        .build()
                )
                .returnPartnerId(DESTINATION_SC_PARTNER_ID)
                .build(),
            WaybillSegmentDto.builder()
                .id(5L)
                .partnerId(401L)
                .partnerType(PartnerType.DELIVERY)
                .segmentType(SegmentType.COURIER)
                .returnPartnerId(MIDDLE_SC_PARTNER_ID)
                .build()
        ));
        return lomOrder;
    }

    @Nonnull
    public static CombinatorRoute lomOrderRoute() {
        CombinatorRoute lomOrderRoute = new CombinatorRoute();
        CombinatorRoute.DeliveryRoute deliveryRoute = new CombinatorRoute.DeliveryRoute();
        CombinatorRoute.Point point = new CombinatorRoute.Point();
        point.setIds(new CombinatorRoute.PointIds().setPartnerId(DESTINATION_SC_PARTNER_ID));
        point.setSegmentType(ru.yandex.market.logistics.lom.model.enums.PointType.WAREHOUSE);
        point.setServices(List.of(
            new CombinatorRoute.DeliveryService()
                .setServiceMeta(List.of(
                    new CombinatorRoute.ServiceMeta().setKey("RETURN_SORTING_CENTER_ID").setValue("1")
                ))
        ));
        CombinatorRoute.Point pointPrev = new CombinatorRoute.Point();
        pointPrev.setIds(new CombinatorRoute.PointIds().setPartnerId(DESTINATION_SC_PARTNER_ID - 1));
        pointPrev.setSegmentType(ru.yandex.market.logistics.lom.model.enums.PointType.WAREHOUSE);
        pointPrev.setServices(List.of(
            new CombinatorRoute.DeliveryService()
                .setServiceMeta(null)
        ));
        deliveryRoute.setPoints(List.of(point, pointPrev));
        lomOrderRoute.setRoute(deliveryRoute);
        return lomOrderRoute;
    }

    @Nonnull
    public static ReturnDelivery returnDelivery() {
        ReturnDelivery delivery = new ReturnDelivery();
        delivery.setType(DeliveryType.PICKUP);
        delivery.setDeliveryServiceId(RETURN_DS_ID);
        delivery.setOutletId(PICKUP_POINT_MBI_ID);
        return delivery;
    }

    @Nonnull
    public static ReturnItem returnItem(long itemId) {
        ReturnItem returnItem = new ReturnItem();
        returnItem.setItemId(itemId);
        returnItem.setItemTitle(null); // в данный момент checkouter всегда возвращает null
        returnItem.setReasonType(ru.yandex.market.checkout.checkouter.returns.ReturnReasonType.BAD_QUALITY);
        returnItem.setReturnReason(RETURN_ITEM_REASON);
        returnItem.setSubreasonType(ru.yandex.market.checkout.checkouter.returns.ReturnSubreason.BAD_PACKAGE);
        returnItem.setCount(1);
        return returnItem;
    }

    @Nonnull
    public static ReturnItem deliveryServiceReturnItem() {
        ReturnItem returnItem = new ReturnItem();
        returnItem.setItemId(null);
        returnItem.setDeliveryServiceId(321011598L);
        returnItem.setDeliveryService(true);
        returnItem.setCount(1);
        return returnItem;

    }

    @Nonnull
    public static ReturnItem courierReturnItem(long itemId) {
        ReturnItem result = returnItem(itemId);
        result.setType(ReturnItemType.ORDER_ITEM);
        result.setReturnReason(RETURN_ITEM_REASON);
        result.setSubreasonType(ru.yandex.market.checkout.checkouter.returns.ReturnSubreason.NOT_WORKING);
        result.setReasonType(ru.yandex.market.checkout.checkouter.returns.ReturnReasonType.BAD_QUALITY);
        result.setItemTitle(ITEM_TITLE);
        result.setCategoryFullName(ITEM_CATEGORY_NAME);
        result.setDescription(ITEM_DESCRIPTION);
        result.setPreviewUrl(ITEM_PREVIEW_URL);
        result.setShopUrl(ITEM_DETAILS_URL);
        result.setPicturesUrls(
            ITEM_PHOTO_URLS.stream().map(ReturnRequestTestUtils::toUrl).collect(Collectors.toList())
        );
        result.setWeight(1L);
        result.setDepth(2L);
        result.setWidth(3L);
        result.setHeight(4L);
        result.setBuyerPrice(new BigDecimal("12.34"));
        result.setQuantity(BigDecimal.valueOf(2));
        return result;
    }

    @Nonnull
    @SneakyThrows
    private static URL toUrl(String url) {
        return new URL(url);
    }

    @Nonnull
    public static ReturnItem emptyCourierReturnItem(long itemId) {
        ReturnItem result = new ReturnItem();
        result.setType(ReturnItemType.ORDER_ITEM);
        result.setItemId(itemId);
        result.setWeight(1L);
        result.setDepth(2L);
        result.setWidth(3L);
        result.setHeight(4L);
        result.setQuantity(BigDecimal.ONE);
        return result;
    }

    @Nonnull
    public static ReturnItem nonOrderReturnItem() {
        ReturnItem result = new ReturnItem();
        result.setType(ReturnItemType.ORDER_DELIVERY);
        return result;
    }

    @Nonnull
    public static ReturnRequest returnRequest() {
        return returnRequest(RETURN_REQUEST_ID);
    }

    @Nonnull
    public static ReturnRequest returnRequest(Long id) {
        return returnRequest(id, ReturnRequestState.AWAITING_FOR_DATA, null);
    }

    @Nonnull
    public static ReturnRequest returnRequestWithItems(boolean isDropship) {
        return returnRequest(
            RETURN_REQUEST_ID,
            ReturnRequestState.AWAITING_FOR_DATA,
            Set.of(
                item(1L, PRICE_1, ITEM_SKU_1, ITEM_SUPPLIER_1),
                item(2L, PRICE_2, ITEM_SKU_2, ITEM_SUPPLIER_2)
            ),
            isDropship
        );
    }

    @Nonnull
    public static ReturnRequest returnRequest(Long id, Collection<ReturnRequestItem> items) {
        return returnRequest(id, ReturnRequestState.AWAITING_FOR_DATA, items);
    }

    @Nonnull
    public static ReturnRequest returnRequest(Long id, ReturnRequestState state, Collection<ReturnRequestItem> items) {
        return returnRequest(id, state, items, false);
    }

    @Nonnull
    public static ReturnRequest returnRequest(
        Long id,
        ReturnRequestState state,
        Collection<ReturnRequestItem> items,
        boolean isDropship
    ) {
        ReturnRequest returnRequest = new ReturnRequest()
            .setId(id)
            .setReturnId(RETURN_ID_STR)
            .setBarcode(
                isDropship
                    ? BARCODE_DROPSHIP_LRM
                    : BARCODE_FF_LRM
            )
            .setExternalOrderId(ORDER_ID)
            .setBuyerName(BUYER_NAME)
            .setClientType(ReturnClientType.CLIENT)
            .setRequestDate(RETURN_CREATED_AT.toLocalDate())
            .setState(state);
        Optional.ofNullable(items)
            .stream()
            .flatMap(Collection::stream)
            .forEach(returnRequest::addReturnRequestItem);
        return returnRequest;
    }

    @Nonnull
    public static ReturnRequestItem item(long id, double price, String sku, Long supplierId) {
        return new ReturnRequestItem()
            .setId(id)
            .setName(null)
            .setShopSku(sku)
            .setSupplierId(supplierId)
            .setReturnType(ReturnType.WITH_DISADVANTAGES)
            .setReturnReason(RETURN_ITEM_REASON)
            .setReturnSubreason(ReturnSubreason.BAD_PACKAGE)
            .setReturnReasonType(ReturnReasonType.BAD_QUALITY)
            .setPrice(BigDecimal.valueOf(price))
            .setCount(1);
    }

    @Nonnull
    public static PickupPoint newPickupPoint() {
        return new PickupPoint()
            .setPvzMarketId(PICKUP_POINT_EXTERNAL_ID)
            .setLogisticPointId(PICKUP_POINT_LMS_ID);
    }

    @Nonnull
    public static PickupPoint pickupPoint() {
        return newPickupPoint().setId(1L);
    }

    @Nonnull
    public static PickupPointDto pickupPointDto() {
        return newPickupPointDtoBuilder()
            .id(1L)
            .build();
    }

    @Nonnull
    public static PickupPointDto.PickupPointDtoBuilder newPickupPointDtoBuilder() {
        return PickupPointDto.builder()
            .pvzMarketId(PICKUP_POINT_EXTERNAL_ID)
            .logisticPointId(PICKUP_POINT_LMS_ID);
    }

    @Nonnull
    public static ReturnRequestCreateDto returnRequestCreateDtoWithItems() {
        final ReturnRequestCreateDto returnRequestCreateDto = returnRequestCreateDto();
        returnRequestCreateDto.setItems(List.of(
            returnItemCreateDto(PRICE_1),
            returnItemCreateDto(PRICE_2)
        ));
        return returnRequestCreateDto;
    }

    @Nonnull
    public static ReturnRequestCreateDto returnRequestCreateDto() {
        return ReturnRequestCreateDto.builder()
            .returnId(RETURN_ID_STR)
            .orderId(Long.toString(ORDER_ID))
            .buyerName(BUYER_NAME)
            .barcode(BARCODE_FF_LRM)
            .clientType(ru.yandex.market.pvz.client.logistics.model.ReturnClientType.CLIENT)
            .requestDate(RETURN_CREATED_AT.toLocalDate())
            .pickupPointId(Long.valueOf(PICKUP_POINT_EXTERNAL_ID))
            .items(Collections.emptyList())
            .build();
    }

    @Nonnull
    public static ReturnItemCreateDto returnItemCreateDto(double price) {
        return ReturnItemCreateDto.builder()
            .name(ITEM_NAME)
            .returnType(ru.yandex.market.pvz.client.logistics.model.ReturnType.WITH_DISADVANTAGES)
            .returnReason(RETURN_ITEM_REASON)
            .price(price)
            .count(1L)
            .build();
    }

    @Nonnull
    public static ReturnRequestDto returnRequestDtoWithItems() {
        return returnRequestDtoWithItems(false);
    }

    @Nonnull
    public static ReturnRequestDto returnRequestDtoWithItems(boolean isDropship) {
        return returnRequestDtoBuilder(isDropship)
            .items(Set.of(
                returnRequestItemDto(ITEM_ID_1, PRICE_1, ITEM_SKU_1, ITEM_SUPPLIER_1),
                returnRequestItemDto(ITEM_ID_2, PRICE_2, ITEM_SKU_2, ITEM_SUPPLIER_2)
            ))
            .build();
    }

    @Nonnull
    public static ReturnRequestDto returnRequestDtoWithItems(boolean isDropship, @Nullable String barcode) {
        return returnRequestDtoBuilder(isDropship)
            .barcode(barcode)
            .items(Set.of(
                returnRequestItemDto(ITEM_ID_1, PRICE_1, ITEM_SKU_1, ITEM_SUPPLIER_1),
                returnRequestItemDto(ITEM_ID_2, PRICE_2, ITEM_SKU_2, ITEM_SUPPLIER_2)
            ))
            .build();
    }

    @Nonnull
    public static ReturnRequestDto returnRequestDtoWithItemsLrmFlow(boolean isDropship) {
        return returnRequestDtoWithItems(
            isDropship,
            isDropship ? BARCODE_DROPSHIP_LRM : BARCODE_FF_LRM
        );
    }

    @Nonnull
    public static ReturnRequestDto returnRequestDto() {
        return returnRequestDtoBuilder(false).build();
    }

    @Nonnull
    public static ReturnRequestDto.ReturnRequestDtoBuilder returnRequestDtoBuilder(boolean isDropship) {
        return ReturnRequestDto.builder()
            .returnId(RETURN_ID_STR)
            .externalOrderId(ORDER_ID)
            .buyerName(BUYER_NAME)
            .barcode(isDropship ? BARCODE_DROPSHIP_LRM : BARCODE_FF_LRM)
            .clientType(ReturnClientType.CLIENT)
            .requestDate(RETURN_CREATED_AT.toLocalDate())
            .pickupPoint(pickupPointDto())
            .items(Collections.emptySet())
            .destinationScPartner(isDropship ? DESTINATION_SC_PARTNER_ID : null)
            .destinationScLogisticPoint(isDropship ? DESTINATION_SC_LOGISTIC_POINT_ID : null);
    }

    @Nonnull
    public static ReturnRequestItemDto returnRequestItemDto(long id, double price, String sku, Long supplierId) {
        return ReturnRequestItemDto.builder()
            .id(id)
            .name(ITEM_NAME)
            .shopSku(sku)
            .supplierId(supplierId)
            .returnType(ReturnType.WITH_DISADVANTAGES)
            .returnReason(RETURN_ITEM_REASON)
            .returnSubreason(ReturnSubreason.BAD_PACKAGE)
            .returnReasonType(ReturnReasonType.BAD_QUALITY)
            .price(BigDecimal.valueOf(price))
            .count(1)
            .build();
    }

    @Nonnull
    public static ru.yandex.market.logistics.mdb.lrm.client.model.ReturnItem lrmReturnItem(
        String vendorCode,
        Long supplierId,
        String barcode
    ) {
        return new ru.yandex.market.logistics.mdb.lrm.client.model.ReturnItem()
            .vendorCode(vendorCode)
            .supplierId(supplierId)
            .returnReason(RETURN_ITEM_REASON)
            .returnSubreason(ru.yandex.market.logistics.mdb.lrm.client.model.ReturnSubreason.BAD_PACKAGE)
            .returnReasonType(ru.yandex.market.logistics.mdb.lrm.client.model.ReturnReasonType.BAD_QUALITY)
            .boxExternalId(barcode);
    }

    @Nonnull
    public static ClientReturnCreateDto clientReturnCreateDto() {
        final var dto = new ClientReturnCreateDto();
        dto.setPickupPointId(Long.valueOf(PICKUP_POINT_EXTERNAL_ID));
        dto.setLogisticPointId(PICKUP_POINT_LMS_ID);
        dto.setReturnId(RETURN_ID_STR);
        dto.setBarcode(BARCODE_FF_LRM);
        dto.setRequestDate(RETURN_CREATED_AT.toLocalDate());
        return dto;
    }

    @Nonnull
    public static LogisticsPointResponse terminalLogisticsPointResponse() {
        return LogisticsPointResponse.newBuilder()
            .id(PICKUP_POINT_LMS_ID)
            .partnerId(PARTNER_ID)
            .externalId(PICKUP_POINT_EXTERNAL_ID)
            .type(PointType.PICKUP_POINT)
            .pickupPointType(PickupPointType.TERMINAL)
            .build();
    }

    @Nonnull
    public static LogisticsPointResponse pvzLogisticsPointResponse() {
        return LogisticsPointResponse.newBuilder()
            .id(PICKUP_POINT_LMS_ID)
            .partnerId(PARTNER_ID)
            .externalId(PICKUP_POINT_EXTERNAL_ID)
            .type(PointType.PICKUP_POINT)
            .pickupPointType(PickupPointType.PICKUP_POINT)
            .build();
    }

    @Nonnull
    public static PartnerResponse marketPvzPartnerResponse() {
        return PartnerResponse.newBuilder()
            .id(PARTNER_ID)
            .subtype(PartnerSubtypeResponse.newBuilder().id(MARKET_PVZ_SUBTYPE_IDS.get(0)).build())
            .build();
    }

    @Nonnull
    public static PartnerResponse nonMarketPvzPartnerResponse() {
        return PartnerResponse.newBuilder()
            .id(PARTNER_ID)
            .subtype(PartnerSubtypeResponse.newBuilder().id(NON_MARKET_PVZ_SUBTYPE_ID).build())
            .build();
    }

    @Nonnull
    public static ReturnDto returnDto() {
        return ReturnDto.builder()
            .returnId(RETURN_ID_STR)
            .orderId(String.valueOf(ORDER_ID))
            .status(ru.yandex.market.pvz.client.logistics.model.ReturnStatus.NEW)
            .expirationDate(EXPIRATION_DATE)
            .build();
    }

    @Nonnull
    public static ReturnRequestUpdateDto returnRequestUpdateDto() {
        return ReturnRequestUpdateDto.builder()
            .returnId(RETURN_ID_STR)
            .status(ReturnStatus.NEW)
            .expirationDate(EXPIRATION_DATE)
            .build();
    }

    @Nonnull
    public static CreateClientReturnDto createClientReturnDto(
        Long sortingCenterId,
        Long sortingCenterPointId,
        String sortingCenterToken,
        Long warehouseId,
        Long senderId,
        String street,
        Long locationId
    ) {
        return createClientReturnDto(
            sortingCenterId,
            sortingCenterPointId,
            sortingCenterToken,
            warehouseId,
            senderId,
            street,
            locationId,
            BARCODE_DROPSHIP_LRM
        );
    }

    @Nonnull
    @SuppressWarnings("checkstyle:ParameterNumber")
    public static CreateClientReturnDto createClientReturnDto(
        Long sortingCenterId,
        Long sortingCenterPointId,
        String sortingCenterToken,
        Long warehouseId,
        Long senderId,
        String street,
        Long locationId,
        String barcode
    ) {
        return CreateClientReturnDto.builder()
            .barcode(barcode)
            .returnDate(RETURN_CREATED_AT.toLocalDate())
            .sortingCenterId(sortingCenterId)
            .token(sortingCenterToken)
            .logisticPointId(String.valueOf(sortingCenterPointId))
            .warehouse(
                warehouseId != null
                    ? WarehouseDto.builder()
                    .yandexId(String.valueOf(warehouseId))
                    .location(
                        ru.yandex.market.sc.internal.model.LocationDto.builder()
                            .country("country")
                            .street(street)
                            .locationId(locationId)
                            .build()
                    )
                    .contact(ContactDto.builder().name("name").surname("surname").patronymic("").build())
                    .phones(List.of())
                    .build()
                    : null
            )
            .sender(
                senderId != null
                    ? SenderDto.builder()
                    .yandexId(String.valueOf(senderId))
                    .location(
                        ru.yandex.market.sc.internal.model.LocationDto.builder()
                            .country("country")
                            .street(street)
                            .locationId(locationId)
                            .build()
                    )
                    .contact(new PersonDto("name", "surname", ""))
                    .phones(List.of())
                    .build()
                    : null
            )
            .courier(FAKE_COURIER)
            .build();
    }

    @Nonnull
    public static CreateReturnRequest lrmCreateReturnRequest(boolean isDropship) {
        String barcode = isDropship ? BARCODE_DROPSHIP_LRM : BARCODE_FF_LRM;
        return new CreateReturnRequest()
            .externalId(String.valueOf(RETURN_ID))
            .orderExternalId(String.valueOf(ORDER_ID))
            .source(ReturnSource.CLIENT)
            .full(null)
            .logisticPointFromId(PICKUP_POINT_LMS_ID)
            .items(List.of(
                lrmReturnItem(ITEM_SKU_1, ITEM_SUPPLIER_1, barcode),
                lrmReturnItem(ITEM_SKU_2, ITEM_SUPPLIER_2, barcode),
                lrmReturnItem(ITEM_SKU_2, ITEM_SUPPLIER_2, barcode)
            ))
            .boxes(List.of(
                new ReturnBoxRequest().externalId(barcode)
            ))
            .orderItemsInfo(List.of(
                new OrderItemInfo()
                    .supplierId(ITEM_SUPPLIER_1)
                    .vendorCode(ITEM_SKU_1)
                    .instances(List.of(Map.of("UIT", ITEM_UIT_1))),
                new OrderItemInfo()
                    .supplierId(ITEM_SUPPLIER_2)
                    .vendorCode(ITEM_SKU_2)
                    .instances(List.of())
            ));
    }

    @Nonnull
    public static CreateClientCourierReturnRequest lrmCreateClientCourierReturnRequest() {
        return new CreateClientCourierReturnRequest()
            .externalId(RETURN_ID_STR)
            .orderExternalId(String.valueOf(ORDER_ID))
            .partnerFromId(DELIVERY_SERVICE_ID)
            .items(List.of(
                new CourierReturnItem()
                    .supplierId(ITEM_SUPPLIER_1)
                    .vendorCode(ITEM_SKU_1)
                    .returnReason(RETURN_ITEM_REASON)
                    .returnSubreason(ru.yandex.market.logistics.mdb.lrm.client.model.ReturnSubreason.NOT_WORKING)
                    .returnReasonType(ru.yandex.market.logistics.mdb.lrm.client.model.ReturnReasonType.BAD_QUALITY)
                    .name(ITEM_TITLE)
                    .categoryName(ITEM_CATEGORY_NAME)
                    .description(ITEM_DESCRIPTION)
                    .previewPhotoUrl(ITEM_PREVIEW_URL)
                    .itemDetailsUrl(ITEM_DETAILS_URL)
                    .clientPhotoUrls(ITEM_PHOTO_URLS)
                    .dimensions(
                        new Dimensions()
                            .weight(1)
                            .length(2)
                            .width(3)
                            .height(4)
                    )
                    .buyerPrice(new BigDecimal("12.34")),
                new CourierReturnItem()
                    .supplierId(ITEM_SUPPLIER_1)
                    .vendorCode(ITEM_SKU_1)
                    .returnReason(RETURN_ITEM_REASON)
                    .returnSubreason(ru.yandex.market.logistics.mdb.lrm.client.model.ReturnSubreason.NOT_WORKING)
                    .returnReasonType(ru.yandex.market.logistics.mdb.lrm.client.model.ReturnReasonType.BAD_QUALITY)
                    .name(ITEM_TITLE)
                    .categoryName(ITEM_CATEGORY_NAME)
                    .description(ITEM_DESCRIPTION)
                    .previewPhotoUrl(ITEM_PREVIEW_URL)
                    .itemDetailsUrl(ITEM_DETAILS_URL)
                    .clientPhotoUrls(ITEM_PHOTO_URLS)
                    .dimensions(
                        new Dimensions()
                            .weight(1)
                            .length(2)
                            .width(3)
                            .height(4)
                    )
                    .buyerPrice(new BigDecimal("12.34")),
                new CourierReturnItem()
                    .supplierId(ITEM_SUPPLIER_2)
                    .vendorCode(ITEM_SKU_2)
                    .dimensions(
                        new Dimensions()
                            .weight(1)
                            .length(2)
                            .width(3)
                            .height(4)
                    )
            ))
            .orderItemsInfo(List.of(
                new OrderItemInfo()
                    .supplierId(ITEM_SUPPLIER_1)
                    .vendorCode(ITEM_SKU_1)
                    .instances(List.of(Map.of("UIT", ITEM_UIT_1))),
                new OrderItemInfo()
                    .supplierId(ITEM_SUPPLIER_2)
                    .vendorCode(ITEM_SKU_2)
                    .instances(List.of())
            ))
            .client(
                new CourierReturnClient()
                    .fullName(SENDER_ADDRESS_SENDER)
                    .phone(SENDER_ADDRESS_PHONE)
                    .email(SENDER_ADDRESS_EMAIL)
                    .personalFullNameId(SENDER_ADDRESS_PERSONAL_FULLNAME_ID)
                    .personalPhoneId(SENDER_ADDRESS_PERSONAL_PHONE_ID)
                    .personalEmailId(SENDER_ADDRESS_PERSONAL_EMAIL_ID)
            )
            .address(
                new CourierReturnAddress()
                    .city(SENDER_ADDRESS_CITY)
                    .street(SENDER_ADDRESS_STREET)
                    .house(SENDER_ADDRESS_HOUSE)
                    .entrance(SENDER_ADDRESS_ENTRANCE)
                    .apartment(SENDER_ADDRESS_APARTMENT)
                    .floor(SENDER_ADDRESS_FLOOR)
                    .entryPhone(SENDER_ADDRESS_ENTRY_PHONE)
                    .lat(new BigDecimal(SENDER_ADDRESS_LAT))
                    .lon(new BigDecimal(SENDER_ADDRESS_LON))
                    .comment(SENDER_ADDRESS_COMMENT)
                    .personalAddressId(LOGISTICS_PERSONAL_ADDRESS_ID)
                    .personalGpsCoordId(SENDER_ADDRESS_PERSONAL_GPS_COORD_ID)
            )
            .interval(
                new CourierInterval()
                    .dateFrom(LocalDate.of(2022, 3, 4))
                    .timeFrom(LocalTime.of(10, 11))
                    .dateTo(LocalDate.of(2022, 3, 4))
                    .timeTo(LocalTime.of(12, 13))
            );
    }
}
