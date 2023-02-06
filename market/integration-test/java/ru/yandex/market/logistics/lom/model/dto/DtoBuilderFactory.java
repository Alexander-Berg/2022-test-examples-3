package ru.yandex.market.logistics.lom.model.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;

import ru.yandex.market.logistics.lom.model.enums.CancellationOrderStatus;
import ru.yandex.market.logistics.lom.model.enums.ContactType;
import ru.yandex.market.logistics.lom.model.enums.CourierType;
import ru.yandex.market.logistics.lom.model.enums.DeliveryType;
import ru.yandex.market.logistics.lom.model.enums.EntityType;
import ru.yandex.market.logistics.lom.model.enums.ItemChangeReason;
import ru.yandex.market.logistics.lom.model.enums.LocationType;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;
import ru.yandex.market.logistics.lom.model.enums.PaymentMethod;
import ru.yandex.market.logistics.lom.model.enums.ShipmentApplicationStatus;
import ru.yandex.market.logistics.lom.model.enums.ShipmentOption;
import ru.yandex.market.logistics.lom.model.enums.ShipmentType;
import ru.yandex.market.logistics.lom.model.enums.VatType;
import ru.yandex.market.logistics.lom.model.error.EntityError;
import ru.yandex.market.logistics.lom.model.filter.OrderSearchFilter;
import ru.yandex.market.logistics.lom.model.filter.ShipmentFilter;
import ru.yandex.market.logistics.lom.model.filter.ShipmentSearchFilter;

public final class DtoBuilderFactory {

    private DtoBuilderFactory() {
    }

    @Nonnull
    public static CredentialsDto.CredentialsDtoBuilder credentialsDtoBuilder() {
        return CredentialsDto.builder()
            .name("credentials-name")
            .incorporation("credentials-incorporation")
            .url("credentials-url")
            .legalForm("credentials-legal-form")
            .ogrn("credentials-ogrn")
            .inn("credentials-inn")
            .address("credentials-address")
            .taxation("credentials-taxation")
            .email("credentials-email@test-domain.com");
    }

    @Nonnull
    public static CostDto.CostDtoBuilder costDtoBuilder() {
        return CostDto.builder()
            .paymentMethod(PaymentMethod.CARD)
            .cashServicePercent(BigDecimal.valueOf(5))
            .assessedValue(BigDecimal.valueOf(100))
            .amountPrepaid(BigDecimal.valueOf(0))
            .itemsSum(BigDecimal.valueOf(10000))
            .delivery(BigDecimal.valueOf(1000))
            .deliveryForCustomer(BigDecimal.valueOf(2000))
            .manualDeliveryForCustomer(BigDecimal.valueOf(5000))
            .isFullyPrepaid(false)
            .total(BigDecimal.valueOf(11000))
            .services(List.of(
                OrderServiceDto.builder()
                    .code(ShipmentOption.DELIVERY)
                    .cost(BigDecimal.valueOf(1000))
                    .customerPay(true).build(),
                OrderServiceDto.builder()
                    .code(ShipmentOption.SORT)
                    .cost(BigDecimal.valueOf(2000))
                    .customerPay(true).build()
            ));
    }

    @Nonnull
    public static RecipientDto.RecipientDtoBuilder recipientDtoBuilder() {
        return RecipientDto.builder()
            .lastName("test-last-name")
            .firstName("test-first-name")
            .middleName("test-middle-name")
            .email("test-email@test-domain.com")
            .personalAddressId("personal-address-id")
            .personalGpsId("personal-gps-id")
            .personalEmailId("personal-email-id")
            .personalFullnameId("personal-fullname-id")
            .uid(1234567890L);
    }

    @Nonnull
    public static KorobyteDto.KorobyteDtoBuilder korobyteDtoBuilder() {
        return KorobyteDto.builder()
            .length(1)
            .width(3)
            .height(2)
            .weightGross(BigDecimal.valueOf(4));
    }

    @Nonnull
    public static OrderContactDto.OrderContactDtoBuilder orderContactDtoBuilder() {
        return OrderContactDto.builder()
            .contactType(ContactType.CONTACT)
            .lastName("test-last-name")
            .firstName("test-first-name")
            .middleName("test-middle-name")
            .phone("+7 (495) 999 9999")
            .extension("12345")
            .comment("test-order-contact-comment")
            .personalFullnameId("personal-fullname-id")
            .personalPhoneId("personal-phone-id");
    }

    @Nonnull
    public static OrderDto orderResponseDto() {
        return new OrderDto()
            .setExternalId("1001")
            .setPlatformClientId(3L)
            .setSenderId(1L)
            .setRecipient(recipientDtoBuilder().build())
            .setCredentials(credentialsDtoBuilder().build())
            .setCost(costDtoBuilder().build())
            .setComment("test-comment")
            .setContacts(Collections.singletonList(orderContactDtoBuilder().build()));
    }

    @Nonnull
    public static WaybillOrderRequestDto orderRequestDto() {
        return fillFields(new WaybillOrderRequestDto());
    }

    @Nonnull
    public static RouteOrderRequestDto orderWithRouteRequestDto() {
        return fillFields(new RouteOrderRequestDto());
    }

    @Nonnull
    private static <T extends AbstractOrderRequestDto> T fillFields(T orderDto) {
        orderDto
            .setExternalId("1001")
            .setPlatformClientId(3L)
            .setSenderId(1L)
            .setRecipient(recipientDtoBuilder().build())
            .setCredentials(credentialsDtoBuilder().build())
            .setCost(costDtoBuilder().build())
            .setComment("test-comment")
            .setContacts(Collections.singletonList(orderContactDtoBuilder().build()));
        return orderDto;
    }

    @Nonnull
    public static ContactDto.ContactDtoBuilder contactDtoBuilder() {
        return ContactDto.builder()
            .lastName("test-last-name")
            .firstName("test-first-name")
            .middleName("test-middle-name")
            .phone("+7 (495) 999-9999")
            .extension("12345")
            .personalFullnameId("personal-fullname-id")
            .personalPhoneId("personal-phone-id");
    }

    @Nonnull
    public static CarDto.CarDtoBuilder carDtoBuilder() {
        return CarDto.builder()
            .number("C065MK78")
            .brand("toyota");
    }

    @Nonnull
    public static ShipmentApplicationDto.ShipmentApplicationDtoBuilder shipmentApplicationDtoBuilder() {
        return ShipmentApplicationDto.builder()
            .id(1L)
            .shipment(shipmentDtoBuilder().build())
            .requisiteId("test-requisite-id")
            .externalId("test-external-id")
            .interval(timeIntervalDtoBuilder().build())
            .status(ShipmentApplicationStatus.NEW)
            .korobyteDto(korobyteDtoBuilder().build())
            .courier(createCourier())
            .cost(BigDecimal.valueOf(350))
            .comment("test-comment");
    }

    @Nonnull
    private static CourierDto createCourier() {
        return CourierDto.builder()
            .contact(contactDtoBuilder().build())
            .car(carDtoBuilder().build())
            .type(CourierType.CAR)
            .build();
    }

    @Nonnull
    public static TimeIntervalDto.TimeIntervalDtoBuilder timeIntervalDtoBuilder() {
        return TimeIntervalDto.builder()
            .from(LocalTime.of(12, 0))
            .to(LocalTime.of(15, 0));
    }

    @Nonnull
    public static ShipmentDto.ShipmentDtoBuilder shipmentDtoBuilder() {
        return ShipmentDto.builder()
            .id(1L)
            .marketIdFrom(1L)
            .marketIdTo(2L)
            .shipmentType(ShipmentType.IMPORT)
            .shipmentDate(LocalDate.of(2019, Month.JUNE, 11))
            .warehouseFrom(1L)
            .warehouseTo(2L);
    }

    @Nonnull
    public static ShipmentSearchDto.ShipmentSearchDtoBuilder shipmentSearchDtoBuilder() {
        return ShipmentSearchDto.builder()
            .id(1L)
            .created(LocalDate.of(2020, 2, 20).atTime(20, 2, 20).atZone(ZoneOffset.of("+09:00")).toInstant())
            .marketIdFrom(1L)
            .marketIdTo(2L)
            .shipmentType(ShipmentType.WITHDRAW)
            .shipmentDate(LocalDate.of(2019, 6, 10))
            .warehouseFrom(1L)
            .warehouseTo(2L)
            .applicationId(1L)
            .interval(timeIntervalDtoBuilder().build())
            .status(ShipmentApplicationStatus.NEW)
            .korobyteDto(korobyteDtoBuilder().build())
            .cost(BigDecimal.valueOf(100))
            .comment("test_comment")
            .courier(createCourier())
            .externalId("ext_id");
    }

    @Nonnull
    public static ShipmentSearchFilter.ShipmentSearchFilterBuilder shipmentSearchFilterBuilder() {
        return ShipmentSearchFilter.builder()
            .marketIdFrom(1L)
            .marketIdsTo(Set.of(2L))
            .fromDate(LocalDate.of(2019, 5, 1))
            .toDate(LocalDate.of(2019, 6, 20))
            .fromTime(LocalTime.of(8, 0))
            .toTime(LocalTime.of(20, 0))
            .shipmentType(ShipmentType.WITHDRAW)
            .statuses(Set.of(ShipmentApplicationStatus.NEW))
            .warehousesFrom(Set.of(1L))
            .warehouseTo(2L);
    }

    @Nonnull
    public static LocationDto.LocationDtoBuilder waybillLocationDtoFromBuilder() {
        return LocationDto.builder()
            .type(LocationType.WAREHOUSE)
            .warehouseId(1L);
    }

    @Nonnull
    public static LocationDto.LocationDtoBuilder waybillLocationDtoToBuilder() {
        return LocationDto.builder()
            .type(LocationType.WAREHOUSE)
            .warehouseId(2L);
    }

    @Nonnull
    public static WaybillSegmentDto.ShipmentDto.ShipmentDtoBuilder waybillShipmentDtoBuilder() {
        return WaybillSegmentDto.ShipmentDto.builder()
            .type(ShipmentType.IMPORT)
            .date(LocalDate.parse("2019-06-11"))
            .locationFrom(waybillLocationDtoFromBuilder().build())
            .locationTo(waybillLocationDtoToBuilder().build());
    }

    @Nonnull
    public static WaybillSegmentDto.WaybillSegmentDtoBuilder waybillSegmentDtoBuilder() {
        return WaybillSegmentDto.builder()
            .options(List.of(ShipmentOption.CHECK))
            .partnerId(2L)
            .externalId("test-external-id")
            .shipment(waybillShipmentDtoBuilder().build());
    }

    @Nonnull
    public static OrderSearchFilter.OrderSearchFilterBuilder orderSearchFilterBuilder() {
        return OrderSearchFilter.builder()
            .senderIds(Set.of(2L))
            .marketIdFrom(10L)
            .orderIds(Set.of(2L))
            .externalIds(Set.of("1002"))
            .partnerIds(ImmutableSet.of(2L, 3L))
            .lastCancellationOrderStatuses(ImmutableSet.of(
                CancellationOrderStatus.PROCESSING, CancellationOrderStatus.MANUALLY_CONFIRMED)
            )
            .statuses(ImmutableSet.of(OrderStatus.DRAFT, OrderStatus.VALIDATING))
            .fromDate(Instant.parse("2019-05-20T17:00:00Z"))
            .toDate(Instant.parse("2019-06-20T17:00:00Z"))
            .shipmentFromDate(LocalDate.parse("2019-06-01"))
            .shipmentToDate(LocalDate.parse("2019-06-20"))
            .shipment(
                ShipmentFilter.builder()
                    .date(LocalDate.of(2019, 8, 2))
                    .type(ShipmentType.IMPORT)
                    .warehousesFrom(Set.of(1L))
                    .warehouseTo(4L)
                    .marketIdTo(2L)
                    .build()
            );
    }

    @Nonnull
    public static OrderDto orderSearchDto() {
        return new OrderDto()
            .setId(2L)
            .setStatus(OrderStatus.PROCESSING)
            .setExternalId("1002")
            .setPlatformClientId(3L)
            .setDeliveryType(DeliveryType.COURIER)
            .setPickupPointId(1L)
            .setDeliveryInterval(
                DeliveryIntervalDto.builder()
                    .deliveryDateMin(LocalDate.of(2019, 6, 6))
                    .deliveryDateMax(LocalDate.of(2019, 6, 6))
                    .deliveryIntervalId(1L)
                    .fromTime(LocalTime.of(10, 0))
                    .toTime(LocalTime.of(15, 0))
                    .build()
            )
            .setSenderId(2L)
            .setMarketIdFrom(10L)
            .setRecipient(recipientDtoBuilder().build())
            .setCredentials(credentialsDtoBuilder().build())
            .setCost(costDtoBuilder().build())
            .setComment("test-comment")
            .setContacts(Collections.singletonList(orderContactDtoBuilder().build()))
            .setWaybill(Collections.singletonList(waybillSegmentDtoBuilder().build()))
            .setCreated(Instant.parse("2019-06-01T12:00:00Z"))
            .setCancellationOrderRequests(List.of(
                CancellationOrderRequestDto.builder()
                    .id(1L)
                    .status(CancellationOrderStatus.CREATED)
                    .build(),
                CancellationOrderRequestDto.builder()
                    .id(2L)
                    .status(CancellationOrderStatus.PROCESSING)
                    .build()
                )
            )
            .setAvailableActions(OrderActionsDto.builder()
                .untieFromShipment(true)
                .generateLabel(true)
                .build());
    }

    @Nonnull
    public static ShipmentConfirmationDto.ShipmentConfirmationDtoBuilder shipmentConfirmationDtoBuilder() {
        return ShipmentConfirmationDto.builder();
    }

    @Nonnull
    public static EntityError.EntityErrorBuilder entityErrorBuilder() {
        return EntityError.builder()
            .entityType(EntityType.SHIPMENT_APPLICATION)
            .errorCode(EntityError.ErrorCode.CUTOFF_REACHED)
            .id(1L);
    }

    @Nonnull
    public static UpdateOrderItemsRequest.UpdateOrderItemsRequestBuilder validUpdateOrderItemsRequestBuilder() {
        return validUpdateOrderItemsRequestBuilder(validItemBuilder());
    }

    private static UpdateOrderItemsRequest.UpdateOrderItemsRequestBuilder validUpdateOrderItemsRequestBuilder(
        ItemDto.ItemDtoBuilder itemDtoBuilder
    ) {
        return UpdateOrderItemsRequest.builder()
            .barcode("LOinttest-1")
            .cost(
                CostDto.builder()
                    .paymentMethod(PaymentMethod.CARD)
                    .cashServicePercent(BigDecimal.valueOf(5))
                    .assessedValue(BigDecimal.valueOf(100))
                    .amountPrepaid(BigDecimal.valueOf(0))
                    .itemsSum(BigDecimal.valueOf(2000))
                    .delivery(BigDecimal.valueOf(1000))
                    .deliveryForCustomer(BigDecimal.valueOf(2000))
                    .manualDeliveryForCustomer(BigDecimal.valueOf(5000))
                    .isFullyPrepaid(false)
                    .total(BigDecimal.valueOf(4000))
                    .tariffId(1L)
                    .services(List.of(
                        OrderServiceDto.builder()
                            .code(ShipmentOption.INSURANCE)
                            .cost(BigDecimal.valueOf(40.5))
                            .customerPay(true).build()
                    ))
                    .build()
            )
            .items(List.of(itemDtoBuilder.build()));
    }

    private static ItemDto.ItemDtoBuilder validItemBuilder() {
        return ItemDto.builder()
            .name("test-item-name")
            .vendorId(1L)
            .article("test-item-article")
            .count(10)
            .price(
                MonetaryDto.builder()
                    .currency("RUB")
                    .exchangeRate(BigDecimal.ONE)
                    .value(BigDecimal.valueOf(2))
                    .build()
            )
            .assessedValue(MonetaryDto.builder()
                .currency("RUB")
                .exchangeRate(BigDecimal.valueOf(3))
                .value(BigDecimal.valueOf(4))
                .build()
            )
            .vatType(VatType.NO_VAT)
            .dimensions(KorobyteDto.builder()
                .weightGross(BigDecimal.ONE)
                .length(1)
                .width(2)
                .height(3)
                .build()
            );
    }

    @Nonnull
    public static CreateOrderItemIsNotSuppliedRequestsDto validCreateOrderItemIsNotSuppliedRequests() {
        return CreateOrderItemIsNotSuppliedRequestsDto.builder()
            .createItemIsNotSuppliedRequests(
                List.of(
                    validCreateOrderItemIsNotSuppliedRequest(1L),
                    validCreateOrderItemIsNotSuppliedRequest(2L)
                )
            )
            .build();
    }

    @Nonnull
    private static CreateOrderItemIsNotSuppliedRequestDto validCreateOrderItemIsNotSuppliedRequest(long id) {
        return CreateOrderItemIsNotSuppliedRequestDto.builder()
            .barcode("LO-" + id)
            .items(List.of(
                ChangedItemDto.builder()
                    .article("article " + id)
                    .reason(ItemChangeReason.ITEM_IS_NOT_SUPPLIED)
                    .count(id)
                    .vendorId(id)
                    .build()
            ))
            .build();
    }

    @Nonnull
    public static AddressDto.AddressDtoBuilder addressDtoBuilber() {
        return AddressDto.builder()
            .country("Country")
            .federalDistrict("Federal District")
            .region("Region")
            .locality("Locality")
            .subRegion("Sub-region")
            .settlement("Settlement")
            .district("District")
            .street("Street")
            .house("1")
            .building("2")
            .housing("3")
            .room("10")
            .zipCode("123321")
            .porch("4")
            .floor(5)
            .metro("Metro")
            .latitude(new BigDecimal("55.8352"))
            .longitude(new BigDecimal("37.5258"))
            .geoId(213)
            .intercom("20");
    }
}
