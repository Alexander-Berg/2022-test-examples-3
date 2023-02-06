package ru.yandex.market.logistics.logistics4go.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.ImmutableSortedSet;
import lombok.experimental.UtilityClass;

import ru.yandex.market.logistics.lom.model.dto.AddressDto;
import ru.yandex.market.logistics.lom.model.dto.CancellationOrderRequestDto;
import ru.yandex.market.logistics.lom.model.dto.ChangeOrderRequestDto;
import ru.yandex.market.logistics.lom.model.dto.CostDto;
import ru.yandex.market.logistics.lom.model.dto.DeliveryIntervalDto;
import ru.yandex.market.logistics.lom.model.dto.ItemDto;
import ru.yandex.market.logistics.lom.model.dto.KorobyteDto;
import ru.yandex.market.logistics.lom.model.dto.LocationDto;
import ru.yandex.market.logistics.lom.model.dto.MonetaryDto;
import ru.yandex.market.logistics.lom.model.dto.OrderActionsDto;
import ru.yandex.market.logistics.lom.model.dto.OrderContactDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.OrderItemBoxDto;
import ru.yandex.market.logistics.lom.model.dto.OrderServiceDto;
import ru.yandex.market.logistics.lom.model.dto.OrderStatusHistoryDto;
import ru.yandex.market.logistics.lom.model.dto.PartnerSettingsDto;
import ru.yandex.market.logistics.lom.model.dto.PhoneDto;
import ru.yandex.market.logistics.lom.model.dto.RecipientDto;
import ru.yandex.market.logistics.lom.model.dto.RouteOrderRequestDto;
import ru.yandex.market.logistics.lom.model.dto.StorageUnitDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentStatusHistoryDto;
import ru.yandex.market.logistics.lom.model.enums.CancellationOrderReason;
import ru.yandex.market.logistics.lom.model.enums.CancellationOrderStatus;
import ru.yandex.market.logistics.lom.model.enums.CargoType;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestReason;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestStatus;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestType;
import ru.yandex.market.logistics.lom.model.enums.ContactType;
import ru.yandex.market.logistics.lom.model.enums.DeliveryType;
import ru.yandex.market.logistics.lom.model.enums.LocationType;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;
import ru.yandex.market.logistics.lom.model.enums.PartnerSubtype;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.lom.model.enums.PaymentMethod;
import ru.yandex.market.logistics.lom.model.enums.SegmentStatus;
import ru.yandex.market.logistics.lom.model.enums.SegmentType;
import ru.yandex.market.logistics.lom.model.enums.ShipmentOption;
import ru.yandex.market.logistics.lom.model.enums.StorageUnitType;
import ru.yandex.market.logistics.lom.model.enums.VatType;
import ru.yandex.market.logistics.lom.model.enums.tags.WaybillSegmentTag;

import static ru.yandex.market.logistics.logistics4go.utils.OrderFactory.PICKUP_POINT_ID;
import static ru.yandex.market.logistics.logistics4go.utils.OrderFactory.SENDER_ID;
import static ru.yandex.market.logistics.logistics4go.utils.OrderFactory.SUPPLIER_ID;
import static ru.yandex.market.logistics.logistics4go.utils.OrderFactory.YANDEX_INN;

@UtilityClass
@SuppressWarnings("HideUtilityClassConstructor")
public class LomFactory {

    @Nonnull
    public static RouteOrderRequestDto lomRequest(boolean isOnlyRequired, boolean isCourier) {
        RouteOrderRequestDto request = new RouteOrderRequestDto();

        request.setExternalId("externalId");
        request.setComment(isOnlyRequired ? null : "comment");
        request.setSenderId(SENDER_ID);
        request.setSenderName("sender-name");
        request.setSenderUrl("sender-site-url");
        request.setSenderLastName("sender-last-name");
        request.setSenderFirstName("sender-first-name");
        request.setSenderMiddleName("sender-middle-name");
        request.setSenderPhone(
            PhoneDto.builder()
                .phoneNumber("sender-phone-number")
                .additionalNumber("sender-additional-number")
                .build()
        );
        request.setSenderEmails(List.of("sender-email"));
        request.setSenderBalanceClientId(110151264L);
        request.setSenderBalanceProductId("daas_10336698");

        request.setMarketIdFrom(2014152L);
        request.setPlatformClientId(6L);
        request.setRecipient(recipient(isOnlyRequired, isCourier));
        request.setDeliveryType(isCourier ? DeliveryType.COURIER : DeliveryType.PICKUP);
        request.setDeliveryInterval(
            DeliveryIntervalDto.builder()
                .deliveryDateMin(LocalDate.of(2022, 3, 15))
                .deliveryDateMax(LocalDate.of(2022, 3, 20))
                .fromTime(isCourier ? LocalTime.of(9, 0, 0) : null)
                .toTime(isCourier ? LocalTime.of(18, 0, 0) : null)
                .build()
        );
        request.setCost(cost());
        request.setUnits(
            isOnlyRequired
                ? List.of(rootUnit())
                : List.of(unit(), rootUnit())
        );
        request.setItems(List.of(item(isOnlyRequired)));
        request.setContacts(List.of(recipientContact(isOnlyRequired)));

        if (!isCourier) {
            request.setPickupPointId(PICKUP_POINT_ID);
        }

        request.setSenderWarehouseId(isOnlyRequired ? null : 10000051904L);

        return request;
    }

    @Nonnull
    public static OrderDto order(boolean isOnlyRequired) {
        return new OrderDto()
            .setId(13L)
            .setPlatformClientId(6L)
            .setBarcode("LOinttest-13")
            .setCost(cost())
            .setDeliveryInterval(isOnlyRequired ? null : deliveryInterval())
            .setItems(List.of(
                item(isOnlyRequired).toBuilder().dimensions(isOnlyRequired ? null : dimensions()).build()
            ))
            .setCancellationOrderRequests(isOnlyRequired ? null : cancellationOrderRequests())
            .setChangeOrderRequests(isOnlyRequired ? null : List.of(changeOrderRequest()))
            .setRecipient(recipient(isOnlyRequired, false))
            .setContacts(List.of(recipientContact(isOnlyRequired)))
            .setStatus(OrderStatus.PROCESSING)
            .setGlobalStatusesHistory(
                List.of(
                    orderStatusHistoryDto(OrderStatus.DRAFT, 10),
                    orderStatusHistoryDto(OrderStatus.VALIDATING, 20),
                    orderStatusHistoryDto(OrderStatus.ENQUEUED, 30),
                    orderStatusHistoryDto(OrderStatus.PROCESSING, 40)
                )
            )
            .setWaybill(isOnlyRequired ? null : waybillSegments())
            .setReturnSortingCenterId(isOnlyRequired ? null : 2001L)
            .setUnits(isOnlyRequired ? null : List.of(unit()))
            .setAvailableActions(
                OrderActionsDto.builder()
                    .updatePlaces(false)
                    .build()
            );
    }

    @Nonnull
    private static OrderStatusHistoryDto orderStatusHistoryDto(
        OrderStatus orderStatus,
        long secondsToAdd
    ) {
        return OrderStatusHistoryDto.builder()
            .status(orderStatus)
            .datetime(Instant.parse("2022-01-01T12:00:00Z").plusSeconds(secondsToAdd))
            .build();
    }

    @Nonnull
    public static OrderDto orderWithAdditionalReturnSegments() {
        return order(false)
            .setWaybill(waybillSegmentsWithReturn());
    }

    @Nonnull
    public static OrderDto orderLastMileNotCancelled() {
        return order(false)
            .setWaybill(waybillSegmentsLastMileNotCancelled());
    }

    @Nonnull
    public static OrderDto orderUpdateItemsInstancesAvailable() {
        return order(false)
            .setWaybill(waybillSegmentsUpdateInstancesAvailable());
    }

    @Nonnull
    public static OrderDto orderUpdateRecipientUnavailable() {
        return orderLastMileNotCancelled()
            .setChangeOrderRequests(List.of(
                ChangeOrderRequestDto.builder()
                    .requestType(ChangeOrderRequestType.RECIPIENT)
                    .status(ChangeOrderRequestStatus.PROCESSING)
                    .build()
            ));
    }

    @Nonnull
    private static RecipientDto recipient(boolean isOnlyRequired, boolean isCourier) {
        return RecipientDto.builder()
            .firstName("recipient.firstName")
            .lastName("recipient.lastName")
            .middleName(isOnlyRequired ? null : "recipient.middleName")
            .email(isOnlyRequired ? null : "recipient@email.com")
            .address(isCourier ? address(isOnlyRequired) : null)
            .build();
    }

    @Nonnull
    private static AddressDto address(boolean isOnlyRequired) {
        return AddressDto.builder()
            .geoId(213)
            .latitude(isOnlyRequired ? null : new BigDecimal("55.733974"))
            .longitude(isOnlyRequired ? null : new BigDecimal("37.587093"))
            .country("Россия")
            .region("Москва")
            .subRegion(isOnlyRequired ? null : "Москва")
            .locality("Москва")
            .street(isOnlyRequired ? null : "Льва Толстого")
            .house("16")
            .housing(isOnlyRequired ? null : "1")
            .building(isOnlyRequired ? null : "1")
            .room(isOnlyRequired ? null : "1")
            .zipCode(isOnlyRequired ? null : "119021")
            .build();
    }

    @Nonnull
    private static CostDto cost() {
        return CostDto.builder()
            .paymentMethod(PaymentMethod.PREPAID)
            .assessedValue(BigDecimal.valueOf(999.90).setScale(2, RoundingMode.HALF_UP))
            .deliveryForCustomer(BigDecimal.valueOf(249).setScale(2, RoundingMode.HALF_UP))
            .isFullyPrepaid(true)
            .delivery(BigDecimal.ZERO)
            .cashServicePercent(BigDecimal.ZERO)
            .services(
                List.of(
                    OrderServiceDto.builder()
                        .code(ShipmentOption.INSURANCE)
                        .cost(BigDecimal.ZERO)
                        .customerPay(false)
                        .build(),
                    OrderServiceDto.builder()
                        .code(ShipmentOption.DELIVERY)
                        .cost(BigDecimal.valueOf(249).setScale(2, RoundingMode.HALF_UP))
                        .customerPay(true)
                        .taxes(ImmutableSortedSet.of(VatType.VAT_20))
                        .build()
                )
            )
            .build();
    }

    @Nonnull
    public static KorobyteDto dimensions() {
        return dimensions(40, 50, 30, 1.234);
    }

    @Nonnull
    public static KorobyteDto dimensions(int length, int width, int height, double weight) {
        return KorobyteDto.builder()
            .height(height)
            .length(length)
            .width(width)
            .weightGross(BigDecimal.valueOf(weight).setScale(3, RoundingMode.HALF_UP))
            .build();
    }

    @Nonnull
    public static ItemDto item(boolean isOnlyRequired) {
        return ItemDto.builder()
            .article("item[0].externalId")
            .name("item[0].name")
            .vendorId(SUPPLIER_ID)
            .supplierInn(YANDEX_INN)
            .supplierName("sender-name")
            .supplierPhone("sender-phone-number ext. sender-additional-number")
            .count(1)
            .vatType(VatType.VAT_20)
            .price(price(999.90))
            .assessedValue(
                isOnlyRequired
                    ? null
                    : MonetaryDto.builder()
                        .currency("RUB")
                        .value(BigDecimal.valueOf(99.90).setScale(2, RoundingMode.HALF_UP))
                        .exchangeRate(BigDecimal.ONE)
                        .build()
            )
            .boxes(List.of(
                OrderItemBoxDto.builder()
                    .dimensions(dimensions())
                    .storageUnitExternalIds(isOnlyRequired ? null : Set.of("place[0].externalId"))
                    .build()
            ))
            .cargoTypes(
                isOnlyRequired
                    ? null
                    : Set.of(
                        CargoType.BULKY_CARGO,
                        CargoType.BULKY_CARGO_MORE_THAN_PALLET,
                        CargoType.BULKY_CARGO_20_KG
                    )
            )
            .instances(isOnlyRequired ? null : List.of(Map.of(
                "CIS_FULL", OrderFactory.CIS_FULL,
                "CIS", OrderFactory.CIS
            )))
            .dimensions(new KorobyteDto(40, 50, 30, BigDecimal.valueOf(1.234)))
            .build();
    }

    @Nonnull
    public static MonetaryDto price(double price) {
        return MonetaryDto.builder()
            .currency("RUB")
            .value(BigDecimal.valueOf(price).setScale(2, RoundingMode.HALF_UP))
            .exchangeRate(BigDecimal.ONE)
            .build();
    }

    @Nullable
    private static DeliveryIntervalDto deliveryInterval() {
        return DeliveryIntervalDto.builder()
            .fromTime(LocalTime.of(12, 13, 14))
            .toTime(LocalTime.of(15, 16, 17))
            .deliveryDateMin(LocalDate.of(2022, 2, 3))
            .deliveryDateMax(LocalDate.of(2022, 2, 4))
            .build();
    }

    @Nonnull
    private static List<CancellationOrderRequestDto> cancellationOrderRequests() {
        return List.of(
            CancellationOrderRequestDto.builder()
                .id(100L)
                .status(CancellationOrderStatus.CREATED)
                .cancellationOrderReason(CancellationOrderReason.SHOP_CANCELLED)
                .build(),
            CancellationOrderRequestDto.builder()
                .id(101L)
                .status(CancellationOrderStatus.MANUALLY_CONFIRMED)
                .cancellationOrderReason(CancellationOrderReason.DELIVERY_SERVICE_LOST)
                .build()
        );
    }

    @Nonnull
    private static ChangeOrderRequestDto changeOrderRequest() {
        return ChangeOrderRequestDto.builder()
            .id(200L)
            .status(ChangeOrderRequestStatus.ORDER_CANCELLED)
            .reason(ChangeOrderRequestReason.DELIVERY_DATE_UPDATED_BY_DELIVERY)
            .requestType(ChangeOrderRequestType.DELIVERY_DATE)
            .build();
    }

    @Nonnull
    private static WaybillSegmentStatusHistoryDto waybillSegmentStatusHistory() {
        return WaybillSegmentStatusHistoryDto.builder()
            .id(10000L)
            .created(Instant.parse("2022-02-01T01:11:03Z"))
            .date(Instant.parse("2022-02-01T01:10:03Z"))
            .status(SegmentStatus.IN)
            .trackerStatus("wssh-tracker-status")
            .build();
    }

    @Nonnull
    public static StorageUnitDto unit() {
        return StorageUnitDto.builder()
            .externalId("place[0].externalId")
            .type(StorageUnitType.PLACE)
            .dimensions(dimensions())
            .parentExternalId("l4g-generated-0")
            .build();
    }

    @Nonnull
    public static StorageUnitDto rootUnit() {
        return StorageUnitDto.builder()
            .externalId("l4g-generated-0")
            .type(StorageUnitType.ROOT)
            .dimensions(dimensions())
            .build();
    }

    @Nonnull
    public static OrderContactDto recipientContact(boolean isOnlyRequired) {
        return OrderContactDto.builder()
            .contactType(ContactType.RECIPIENT)
            .firstName("recipient.firstName")
            .lastName("recipient.lastName")
            .middleName(isOnlyRequired ? null : "recipient.middleName")
            .phone("+7 999 888 7766")
            .extension(isOnlyRequired ? null : "12345")
            .build();
    }

    @Nonnull
    public static OrderContactDto senderContact(boolean isOnlyRequired) {
        return OrderContactDto.builder()
            .contactType(ContactType.PHYSICAL_PERSON_SENDER)
            .firstName("sender.firstName")
            .lastName("sender.lastName")
            .middleName(isOnlyRequired ? null : "sender.middleName")
            .phone("+7 987 456 7890")
            .extension(isOnlyRequired ? null : "54321")
            .build();
    }

    @Nonnull
    public List<WaybillSegmentDto> waybillSegments() {
        return List.of(
            yandexGoShopSegment(),
            dropoffSegment(),
            scSegment(),
            movementSegment(),
            pickupSegment()
        );
    }

    @Nonnull
    public List<WaybillSegmentDto> waybillSegmentsWithReturn() {
        List<WaybillSegmentDto> segments = new ArrayList<>(waybillSegments());
        segments.add(
            WaybillSegmentDto.builder()
                .id(1005L)
                .partnerId(2005L)
                .partnerName("return-sc-partner-name")
                .partnerLegalName("return-sc-partner-legal-name")
                .partnerAddress("return-sc-partner-address")
                .partnerType(PartnerType.SORTING_CENTER)
                .segmentType(SegmentType.SORTING_CENTER)
                .warehouseLocation(
                    LocationDto.builder()
                        .type(LocationType.WAREHOUSE)
                        .warehouseId(3005L)
                        .build()
                )
                .waybillSegmentTags(List.of(WaybillSegmentTag.RETURN))
                .waybillSegmentStatusHistory(List.of())
                .build()
        );
        segments.add(
            WaybillSegmentDto.builder()
                .id(1006L)
                .partnerId(2001L)
                .partnerName("return-dropoff-partner-name")
                .partnerLegalName("return-dropoff-partner-legal-name")
                .partnerAddress("return-dropoff-partner-address")
                .partnerType(PartnerType.DELIVERY)
                .segmentType(SegmentType.SORTING_CENTER)
                .warehouseLocation(
                    LocationDto.builder()
                        .type(LocationType.WAREHOUSE)
                        .warehouseId(3006L)
                        .build()
                )
                .waybillSegmentTags(List.of(WaybillSegmentTag.RETURN))
                .waybillSegmentStatusHistory(List.of())
                .build()
        );
        return segments;
    }

    @Nonnull
    public List<WaybillSegmentDto> waybillSegmentsLastMileNotCancelled() {
        return List.of(
            yandexGoShopSegment(),
            dropoffSegment(),
            scSegment(),
            movementSegment(),
            pickupSegment().toBuilder()
                .waybillSegmentStatusHistory(List.of(waybillSegmentStatusHistory()))
                .build()
        );
    }

    @Nonnull
    public List<WaybillSegmentDto> waybillSegmentsUpdateInstancesAvailable() {
        return List.of(
            yandexGoShopSegment(),
            dropoffSegment().toBuilder()
                .waybillSegmentStatusHistory(List.of())
                .build(),
            scSegment(),
            movementSegment(),
            pickupSegment().toBuilder()
                .waybillSegmentStatusHistory(List.of())
                .partnerSettings(PartnerSettingsDto.builder().updateInstancesEnabled(true).build())
                .build()
        );
    }

    @Nonnull
    public List<WaybillSegmentDto> waybillSegmentsWithoutStatusHistory() {
        return List.of(
            yandexGoShopSegment(),
            dropoffSegment().toBuilder()
                .waybillSegmentStatusHistory(List.of())
                .build(),
            scSegment(),
            movementSegment(),
            pickupSegment().toBuilder()
                .waybillSegmentStatusHistory(List.of())
                .build()
        );
    }

    @Nonnull
    private WaybillSegmentDto yandexGoShopSegment() {
        return WaybillSegmentDto.builder()
            .id(1000L)
            .partnerId(2000L)
            .partnerName("go-shop-partner-name")
            .partnerLegalName("go-shop-partner-legal-name")
            .partnerAddress("go-shop-partner-address")
            .partnerType(PartnerType.YANDEX_GO_SHOP)
            .segmentType(SegmentType.NO_OPERATION)
            .waybillSegmentStatusHistory(List.of())
            .waybillSegmentTags(List.of())
            .build();
    }

    @Nonnull
    private WaybillSegmentDto dropoffSegment() {
        return WaybillSegmentDto.builder()
            .id(1001L)
            .partnerId(2001L)
            .partnerName("dropoff-partner-name")
            .partnerLegalName("dropoff-partner-legal-name")
            .partnerAddress("dropoff-partner-address")
            .partnerType(PartnerType.DELIVERY)
            .segmentType(SegmentType.SORTING_CENTER)
            .warehouseLocation(
                LocationDto.builder()
                    .type(LocationType.WAREHOUSE)
                    .warehouseId(3001L)
                    .build()
            )
            .waybillSegmentTags(List.of(WaybillSegmentTag.DIRECT, WaybillSegmentTag.RETURN))
            .segmentStatus(SegmentStatus.OUT)
            .waybillSegmentStatusHistory(List.of(waybillSegmentStatusHistory()))
            .build();
    }

    @Nonnull
    private WaybillSegmentDto scSegment() {
        return WaybillSegmentDto.builder()
            .id(1002L)
            .partnerId(2002L)
            .partnerName("sc-partner-name")
            .partnerLegalName("sc-partner-legal-name")
            .partnerAddress("sc-partner-address")
            .partnerType(PartnerType.SORTING_CENTER)
            .segmentType(SegmentType.SORTING_CENTER)
            .warehouseLocation(
                LocationDto.builder()
                    .type(LocationType.WAREHOUSE)
                    .warehouseId(3002L)
                    .build()
            )
            .waybillSegmentTags(List.of(WaybillSegmentTag.DIRECT, WaybillSegmentTag.RETURN))
            .waybillSegmentStatusHistory(List.of())
            .build();
    }

    @Nonnull
    private WaybillSegmentDto movementSegment() {
        return WaybillSegmentDto.builder()
            .id(1003L)
            .partnerId(2003L)
            .partnerName("market-courier-partner-name")
            .partnerLegalName("market-courier-partner-legal-name")
            .partnerAddress("market-courier-partner-address")
            .partnerType(PartnerType.DELIVERY)
            .partnerSubtype(PartnerSubtype.MARKET_COURIER)
            .segmentType(SegmentType.MOVEMENT)
            .waybillSegmentStatusHistory(List.of())
            .waybillSegmentTags(List.of(WaybillSegmentTag.DIRECT))
            .build();
    }

    @Nonnull
    private WaybillSegmentDto pickupSegment() {
        return WaybillSegmentDto.builder()
            .id(1004L)
            .partnerId(2001L)
            .partnerName("dropoff-partner-name")
            .partnerLegalName("dropoff-partner-legal-name")
            .partnerAddress("dropoff-partner-address")
            .partnerType(PartnerType.DELIVERY)
            .partnerSubtype(PartnerSubtype.PARTNER_PICKUP_POINT_IP)
            .segmentType(SegmentType.PICKUP)
            .waybillSegmentStatusHistory(
                List.of(
                    waybillSegmentStatusHistory(),
                    waybillSegmentStatusHistory().toBuilder()
                        .id(10001L)
                        .date(Instant.parse("2022-03-01T01:10:03Z"))
                        .status(SegmentStatus.CANCELLED)
                        .build()
                )
            )
            .waybillSegmentTags(List.of(WaybillSegmentTag.DIRECT))
            .build();
    }
}
