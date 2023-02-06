package ru.yandex.market.logistics.nesu.base.order;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.market.logistics.lom.model.dto.AddressDto;
import ru.yandex.market.logistics.lom.model.dto.CostDto;
import ru.yandex.market.logistics.lom.model.dto.DeliveryIntervalDto;
import ru.yandex.market.logistics.lom.model.dto.ItemDto;
import ru.yandex.market.logistics.lom.model.dto.KorobyteDto;
import ru.yandex.market.logistics.lom.model.dto.LocationDto;
import ru.yandex.market.logistics.lom.model.dto.MonetaryDto;
import ru.yandex.market.logistics.lom.model.dto.OrderContactDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.OrderItemBoxDto;
import ru.yandex.market.logistics.lom.model.dto.OrderServiceDto;
import ru.yandex.market.logistics.lom.model.dto.PhoneDto;
import ru.yandex.market.logistics.lom.model.dto.RecipientDto;
import ru.yandex.market.logistics.lom.model.dto.StorageUnitDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillOrderRequestDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto.WaybillSegmentDtoBuilder;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentStatusHistoryDto;
import ru.yandex.market.logistics.lom.model.enums.LocationType;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.lom.model.enums.SegmentStatus;
import ru.yandex.market.logistics.lom.model.enums.SegmentType;
import ru.yandex.market.logistics.lom.model.enums.ShipmentOption;
import ru.yandex.market.logistics.lom.model.enums.StorageUnitType;
import ru.yandex.market.logistics.lom.model.enums.VatType;
import ru.yandex.market.logistics.nesu.api.model.Dimensions;
import ru.yandex.market.logistics.nesu.client.enums.ShipmentType;
import ru.yandex.market.logistics.nesu.dto.Item;
import ru.yandex.market.logistics.nesu.dto.ItemInstance;
import ru.yandex.market.logistics.nesu.dto.MultiplaceItem;
import ru.yandex.market.logistics.nesu.dto.Place;
import ru.yandex.market.logistics.nesu.dto.enums.ContactType;
import ru.yandex.market.logistics.nesu.dto.enums.DeliveryType;
import ru.yandex.market.logistics.nesu.dto.enums.PaymentMethod;
import ru.yandex.market.logistics.nesu.dto.order.OrderContact;
import ru.yandex.market.logistics.nesu.dto.order.OrderCost;
import ru.yandex.market.logistics.nesu.dto.order.OrderDraft;
import ru.yandex.market.logistics.nesu.dto.order.OrderDraftDeliveryOption;
import ru.yandex.market.logistics.nesu.dto.order.OrderDraftDeliveryOptionService;
import ru.yandex.market.logistics.nesu.dto.order.OrderDraftRecipient;
import ru.yandex.market.logistics.nesu.dto.order.OrderDraftShipment;
import ru.yandex.market.logistics.nesu.dto.order.OrderRecipientAddress;
import ru.yandex.market.logistics.nesu.enums.VatRate;
import ru.yandex.market.logistics.nesu.model.entity.ServiceType;
import ru.yandex.market.logistics.nesu.service.lms.PlatformClientId;
import ru.yandex.market.logistics.nesu.utils.CommonsConstants;

import static ru.yandex.market.logistics.lom.model.enums.LocationType.WAREHOUSE;

@ParametersAreNonnullByDefault
public final class OrderDtoFactory {
    public static final String YANDEX_INN = "7736207543";

    private OrderDtoFactory() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    public static Consumer<OrderDraft> defaultOrderDraft() {
        return draft -> {
            draft
                .setExternalId("a\\Bc-12356")
                .setDeliveryType(DeliveryType.COURIER)
                .setComment("test_comment")
                .setCost(createOrderCost())
                .setPlaces(List.of(createPlace(45, 30, 15, 50, List.of(createItem()))))
                .setContacts(List.of(createOrderContact()));

            OrderDraftRecipient recipient = new OrderDraftRecipient();
            draft.setRecipient(recipient);
            recipient
                .setPickupPointId(101L)
                .setFirstName("recipient_first")
                .setMiddleName("recipient_middle")
                .setLastName("recipient_last")
                .setEmail("recipient@email.com")
                .setAddress(createRecipientAddress());

            draft.setShipment(orderDraftShipment());
            draft.setDeliveryOption(defaultDeliveryOption());
        };
    }

    @Nonnull
    public static OrderDraftShipment orderDraftShipment() {
        OrderDraftShipment shipment = new OrderDraftShipment();
        shipment.setPartnerTo(5L)
            .setWarehouseFrom(3L)
            .setWarehouseTo(4L)
            .setType(ShipmentType.WITHDRAW)
            .setDate(LocalDate.of(2019, 8, 1));
        return shipment;
    }

    @Nonnull
    public static Consumer<OrderDraft> defaultExtendedInvalidOrderDraft() {
        return draft -> {
            defaultOrderDraft().accept(draft);
            draft.setPlaces(List.of(
                new Place().setItems(List.of(createItem().setSupplierInn(null).setTax(null)))
            ));
        };
    }

    @Nonnull
    public static OrderDraftDeliveryOption defaultDeliveryOption() {
        OrderDraftDeliveryOption deliveryOption = new OrderDraftDeliveryOption();
        deliveryOption
            .setDeliveryIntervalId(1L)
            .setPartnerId(5L)
            .setDelivery(BigDecimal.valueOf(25))
            .setDeliveryForCustomer(BigDecimal.valueOf(50))
            .setTariffId(33L)
            .setCalculatedDeliveryDateMin(LocalDate.of(2019, 8, 6))
            .setCalculatedDeliveryDateMax(LocalDate.of(2019, 8, 6))
            .setServices(defaultDeliveryOptionServices("0.75"));
        return deliveryOption;
    }

    @Nonnull
    public static OrderContact createOrderContact() {
        return new OrderContact()
            .setType(ContactType.RECIPIENT)
            .setFirstName("contact_first")
            .setMiddleName("contact_middle")
            .setLastName("contact_last")
            .setPhone("+79998886655")
            .setAdditional("4321");
    }

    @Nonnull
    public static OrderCost createOrderCost() {
        return new OrderCost()
            .setPaymentMethod(PaymentMethod.CASH)
            .setAssessedValue(BigDecimal.valueOf(125))
            .setFullyPrepaid(false);
    }

    @Nonnull
    private static OrderRecipientAddress createRecipientAddress() {
        return OrderRecipientAddress.builder()
            .geoId(42)
            .country("Россия")
            .region("Республика Мордовия")
            .subRegion("Городской округ Саранск")
            .locality("Саранск")
            .postalCode("recipient_zip")
            .street("recipient_street")
            .house("recipient_house")
            .building("recipient_building")
            .housing("recipient_housing")
            .apartment("recipient_room")
            .build();
    }

    @Nonnull
    static MultiplaceItem createItem() {
        return createItem(10, 20, 100);
    }

    @Nonnull
    static MultiplaceItem createItem(int price, int count, int assessedValue) {
        MultiplaceItem multiplaceItem = new MultiplaceItem();
        multiplaceItem
            .setExternalId("ext_item_id")
            .setName("item_name")
            .setCount(count)
            .setPrice(BigDecimal.valueOf(price))
            .setAssessedValue(BigDecimal.valueOf(assessedValue))
            .setTax(VatRate.VAT_0)
            .setDimensions(new Dimensions()
                .setHeight(10)
                .setWidth(23)
                .setLength(15)
                .setWeight(BigDecimal.valueOf(5)))
            .setSupplierInn(YANDEX_INN);
        return multiplaceItem;
    }

    @Nonnull
    static ItemInstance createItemInstance() {
        return new ItemInstance()
            .setCis("item-instance-cis");
    }

    @Nonnull
    static Place createDefaultPlace(Integer length, Integer width, Integer height, String externalId) {
        return createDefaultPlace(createItem().setExternalId(externalId))
            .setExternalId(externalId)
            .setDimensions(new Dimensions()
                .setLength(length)
                .setWidth(width)
                .setHeight(height)
                .setWeight(BigDecimal.TEN));
    }

    static Dimensions createDimensions() {
        return createDimensions(150, 170, 110, 50);
    }

    static Dimensions createDimensions(int length, int width, int height, int weight) {
        return new Dimensions()
            .setLength(length)
            .setWidth(width)
            .setHeight(height)
            .setWeight(new BigDecimal(weight));
    }

    @Nonnull
    static Place createDefaultPlace(Item item) {
        return createPlace(45, 30, 15, 50, List.of(item));
    }

    @Nonnull
    static Place createPlace(
        Integer length,
        Integer width,
        Integer height,
        Integer weight,
        @Nullable List<Item> items
    ) {
        return new Place()
            .setExternalId("ext_place_id")
            .setDimensions(new Dimensions()
                .setHeight(height)
                .setWidth(width)
                .setLength(length)
                .setWeight(new BigDecimal(weight)))
            .setItems(items);
    }

    @Nonnull
    public static OrderDto createLomOrder() {
        return createLomOrder(null);
    }

    @Nonnull
    public static OrderDto createLomOrder(@Nullable BigDecimal totalCost) {
        return new OrderDto()
            .setStatus(OrderStatus.DRAFT)
            .setSenderId(1L)
            .setSenderBalanceClientId(200L)
            .setSenderBalanceProductId("product-200")
            .setBalanceContractId(1L)
            .setBalancePersonId(101L)
            .setExternalId("a\\Bc-12356")
            .setComment("test_comment")
            .setPlatformClientId(PlatformClientId.YANDEX_DELIVERY.getId())
            .setPickupPointId(101L)
            .setDeliveryType(ru.yandex.market.logistics.lom.model.enums.DeliveryType.COURIER)
            .setRecipient(createRecipientBuilder().build())
            .setCost(createLomOrderCost().total(totalCost).build())
            .setContacts(List.of(createLomContact().build()))
            .setItems(List.of(createLomItemBuilder().build()))
            .setUnits(List.of(
                createPlaceUnitBuilder().build(),
                createRootUnit()
            ))
            .setWaybill(List.of(createWithdrawBuilder().partnerType(PartnerType.DELIVERY).build()))
            .setDeliveryInterval(createDeliveryInterval().build())
            .setSenderName("test-sender-name")
            .setSenderPhone(PhoneDto.builder().phoneNumber("9999999999").build())
            .setSenderUrl("www.test-sender-name.com")
            .setSenderFirstName("test-first-name")
            .setSenderLastName("test-last-name")
            .setSenderEmails(List.of("second-email@second-sender-name.com", "test-email@test-sender-name.com"))
            .setMarketIdFrom(201L)
            .setReturnSortingCenterId(6L)
            .setFake(false);
    }

    @Nonnull
    public static WaybillOrderRequestDto createMultiplaceLomOrderRequest() {
        return createLomOrderRequest(null);
    }

    @Nonnull
    public static WaybillOrderRequestDto createLomOrderRequest() {
        return createLomOrderRequest(List.of(0));
    }

    @Nonnull
    public static WaybillOrderRequestDto createLomOrderRequest(@Nullable List<Integer> storageUnitIndexes) {
        WaybillOrderRequestDto orderRequestDto = new WaybillOrderRequestDto();
        orderRequestDto
            .setWaybill(List.of(createWithdrawBuilder(false).build()))
            .setSenderId(1L)
            .setSenderBalanceClientId(200L)
            .setSenderBalanceProductId("product-200")
            .setBalanceContractId(1L)
            .setBalancePersonId(101L)
            .setExternalId("a\\Bc-12356")
            .setComment("test_comment")
            .setPlatformClientId(PlatformClientId.YANDEX_DELIVERY.getId())
            .setPickupPointId(101L)
            .setDeliveryType(ru.yandex.market.logistics.lom.model.enums.DeliveryType.COURIER)
            .setRecipient(createRecipientBuilder().build())
            .setCost(createLomOrderCost().build())
            .setContacts(List.of(createLomContact().build()))
            .setItems(List.of(createLomItemBuilder(storageUnitIndexes).build()))
            .setUnits(List.of(
                createPlaceUnitBuilder().build(),
                createRootUnit()
            ))
            .setDeliveryInterval(createDeliveryInterval().build())
            .setSenderName("test-sender-name")
            .setSenderPhone(PhoneDto.builder().phoneNumber("9999999999").build())
            .setSenderUrl("www.test-sender-name.com")
            .setSenderFirstName("test-first-name")
            .setSenderLastName("test-last-name")
            .setSenderEmails(List.of("second-email@second-sender-name.com", "test-email@test-sender-name.com"))
            .setMarketIdFrom(201L)
            .setReturnSortingCenterId(6L)
            .setFake(false);
        return orderRequestDto;
    }

    @Nonnull
    static DeliveryIntervalDto.DeliveryIntervalDtoBuilder createDeliveryInterval() {
        return DeliveryIntervalDto.builder()
            .deliveryIntervalId(1L)
            .deliveryDateMin(LocalDate.of(2019, 8, 6))
            .deliveryDateMax(LocalDate.of(2019, 8, 6))
            .fromTime(LocalTime.of(10, 0))
            .toTime(LocalTime.of(18, 0));
    }

    @Nonnull
    static OrderContactDto.OrderContactDtoBuilder createLomContact() {
        return OrderContactDto.builder()
            .contactType(ru.yandex.market.logistics.lom.model.enums.ContactType.RECIPIENT)
            .firstName("contact_first")
            .middleName("contact_middle")
            .lastName("contact_last")
            .phone("+79998886655")
            .extension("4321");
    }

    @Nonnull
    static RecipientDto.RecipientDtoBuilder createRecipientBuilder() {
        return RecipientDto.builder()
            .firstName("recipient_first")
            .middleName("recipient_middle")
            .lastName("recipient_last")
            .email("recipient@email.com")
            .address(createAddressBuilder().build());
    }

    @Nonnull
    public static AddressDto.AddressDtoBuilder createAddressBuilder() {
        return AddressDto.builder()
            .geoId(42)
            .country("Россия")
            .region("Республика Мордовия")
            .subRegion("Городской округ Саранск")
            .locality("Саранск")
            .zipCode("recipient_zip")
            .street("recipient_street")
            .house("recipient_house")
            .building("recipient_building")
            .housing("recipient_housing")
            .room("recipient_room")
            .latitude(new BigDecimal("55.7513100141919"))
            .longitude(new BigDecimal("37.5846221554295"));
    }

    @Nonnull
    public static StorageUnitDto createRootUnit() {
        return createRootUnit(createPlaceKorobyte());
    }

    @Nonnull
    public static StorageUnitDto createRootUnit(KorobyteDto dto) {
        return StorageUnitDto.builder()
            .type(StorageUnitType.ROOT)
            .externalId("generated-0")
            .dimensions(dto)
            .build();
    }

    @Nonnull
    public static StorageUnitDto.StorageUnitDtoBuilder createPlaceUnitBuilder() {
        return StorageUnitDto.builder()
            .parentExternalId("generated-0")
            .externalId("ext_place_id")
            .dimensions(createPlaceKorobyte())
            .type(StorageUnitType.PLACE);
    }

    @Nonnull
    public static StorageUnitDto createDefaultLomPlace(
        Integer length,
        Integer width,
        Integer height,
        String externalId
    ) {
        return createPlaceUnitBuilder()
            .dimensions(createKorobyte(height, width, length, 10))
            .externalId(externalId)
            .build();
    }

    @Nonnull
    public static KorobyteDto createPlaceKorobyte() {
        return createKorobyte(15, 30, 45, 50);
    }

    @Nonnull
    public static KorobyteDto createItemKorobyte() {
        return createKorobyte(10, 23, 15, 5);
    }

    @Nonnull
    public static ItemDto.ItemDtoBuilder createLomItemBuilder() {
        return createLomItemBuilder(List.of(0));
    }

    @Nonnull
    public static ItemDto.ItemDtoBuilder createLomItemBuilder(@Nullable List<Integer> storageUnitIndexes) {
        return ItemDto.builder()
            .article("ext_item_id")
            .name("item_name")
            .vendorId(1L)
            .count(20)
            .price(createMonetary(BigDecimal.TEN))
            .assessedValue(createMonetary(BigDecimal.valueOf(100)))
            .vatType(VatType.VAT_0)
            .dimensions(createItemKorobyte())
            .boxes(List.of(createItemBoxBuilder(storageUnitIndexes).build()))
            .supplierInn(YANDEX_INN);
    }

    @Nonnull
    public static ItemDto createDefaultLomItem(
        String article,
        @Nullable Set<String> externalId,
        @Nullable List<Integer> unitIndex
    ) {
        return createLomItemBuilder()
            .dimensions(createItemKorobyte())
            .article(article)
            .boxes(List.of(
                OrderDtoFactory.createItemBoxBuilder()
                    .storageUnitExternalIds(externalId)
                    .storageUnitIndexes(unitIndex)
                    .build()
            ))
            .build();
    }

    @Nonnull
    public static OrderItemBoxDto.OrderItemBoxDtoBuilder createItemBoxBuilder() {
        return createItemBoxBuilder(List.of(0));
    }

    @Nonnull
    public static OrderItemBoxDto.OrderItemBoxDtoBuilder createItemBoxBuilder(
        @Nullable List<Integer> storageUnitIndexes
    ) {
        return OrderItemBoxDto.builder()
            .dimensions(createItemKorobyte())
            .storageUnitExternalIds(Set.of("ext_place_id"))
            .storageUnitIndexes(storageUnitIndexes);
    }

    @Nonnull
    public static CostDto.CostDtoBuilder createLomOrderCost() {
        return createLomOrderCost(new BigDecimal("0.017"));
    }

    @Nonnull
    static CostDto.CostDtoBuilder createLomOrderCost(@Nullable BigDecimal cashServicePercent) {
        return CostDto.builder()
            .paymentMethod(ru.yandex.market.logistics.lom.model.enums.PaymentMethod.CASH)
            .assessedValue(BigDecimal.valueOf(125))
            .delivery(BigDecimal.valueOf(25))
            .deliveryForCustomer(BigDecimal.valueOf(50))
            .isFullyPrepaid(false)
            .cashServicePercent(cashServicePercent)
            .tariffId(33L)
            .services(defaultLomDeliveryServices("0.75"));
    }

    @Nonnull
    static List<OrderDraftDeliveryOptionService> defaultDeliveryOptionServices(@Nullable String insuranceCost) {
        return defaultDeliveryOptionServices("3.400", insuranceCost);
    }

    @Nonnull
    public static List<OrderDraftDeliveryOptionService> defaultDeliveryOptionServices(
        String cashServiceCost,
        @Nullable String insuranceCost
    ) {
        OrderDraftDeliveryOptionService cashService = deliveryServiceBuilder()
            .setCost(new BigDecimal(cashServiceCost))
            .setCode(ServiceType.CASH_SERVICE)
            .setCustomerPay(false);
        BigDecimal sortCost = new BigDecimal("20.000");
        OrderDraftDeliveryOptionService returnService = deliveryServiceBuilder()
            .setCost(BigDecimal.valueOf(0.75))
            .setCode(ServiceType.RETURN)
            .setCustomerPay(false);
        OrderDraftDeliveryOptionService returnSort = deliveryServiceBuilder()
            .setCost(sortCost)
            .setCode(ServiceType.RETURN_SORT)
            .setCustomerPay(false);

        if (insuranceCost == null) {
            return List.of(cashService, returnService, returnSort);
        } else {
            OrderDraftDeliveryOptionService insurance = deliveryServiceBuilder()
                .setCost(new BigDecimal(insuranceCost))
                .setCode(ServiceType.INSURANCE)
                .setCustomerPay(false);
            return List.of(cashService, insurance, returnService, returnSort);
        }
    }

    @Nonnull
    static List<OrderServiceDto> defaultLomDeliveryServices(@Nullable String insuranceCost) {
        return defaultLomDeliveryServices("3.400", insuranceCost);
    }

    @Nonnull
    public static List<OrderServiceDto> defaultLomDeliveryServices(
        String cashServiceCost,
        @Nullable String insuranceCost
    ) {
        OrderServiceDto cashService = defaultLomOrderService()
            .code(ShipmentOption.CASH_SERVICE)
            .customerPay(false)
            .cost(new BigDecimal(cashServiceCost).stripTrailingZeros())
            .build();
        OrderServiceDto returnService = defaultLomOrderService()
            .code(ShipmentOption.RETURN)
            .customerPay(false)
            .cost(new BigDecimal("0.75"))
            .build();
        OrderServiceDto returnSort = defaultLomOrderService()
            .code(ShipmentOption.RETURN_SORT)
            .customerPay(false)
            .cost(new BigDecimal("20"))
            .build();
        if (insuranceCost == null) {
            return List.of(cashService, returnService, returnSort);
        } else {
            OrderServiceDto insurance = defaultLomOrderService()
                .code(ShipmentOption.INSURANCE)
                .customerPay(false)
                .cost(new BigDecimal(insuranceCost).stripTrailingZeros())
                .build();
            return List.of(cashService, insurance, returnService, returnSort);
        }
    }

    @Nonnull
    public static WaybillSegmentDto.WaybillSegmentDtoBuilder createWithdrawBuilder() {
        return createWithdrawBuilder(true);
    }

    @Nonnull
    public static WaybillSegmentDto.WaybillSegmentDtoBuilder createWithdrawBuilder(boolean includeDestination) {
        WaybillSegmentDto.ShipmentDto.ShipmentDtoBuilder build = defaultShipmentDtoBuilder();
        if (!includeDestination) {
            build.locationTo(null);
        }
        return createWaybillSegmentBuilder(build.build(), 5L);
    }

    @Nonnull
    public static WaybillSegmentDto.WaybillSegmentDtoBuilder createWithdrawBuilder(
        WaybillSegmentDto.ShipmentDto shipmentDto
    ) {
        return createWaybillSegmentBuilder(shipmentDto, 5L);
    }

    @Nonnull
    public static WaybillSegmentDto.ShipmentDto.ShipmentDtoBuilder defaultShipmentDtoBuilder() {
        return WaybillSegmentDto.ShipmentDto.builder()
            .date(LocalDate.of(2019, 8, 1))
            .locationFrom(createLocation(3L))
            .locationTo(createLocation(4L))
            .type(ru.yandex.market.logistics.lom.model.enums.ShipmentType.WITHDRAW);
    }

    @Nonnull
    static WaybillSegmentDto.WaybillSegmentDtoBuilder createWaybillSegmentBuilder(
        @Nullable LocalDate date,
        @Nullable LocationDto locationFrom,
        @Nullable LocationDto locationTo,
        @Nullable Long partnerId,
        @Nullable ru.yandex.market.logistics.lom.model.enums.ShipmentType shipmentType
    ) {
        return WaybillSegmentDto.builder()
            .requisiteId(200L)
            .partnerId(partnerId)
            .shipment(WaybillSegmentDto.ShipmentDto.builder()
                .type(shipmentType)
                .locationFrom(locationFrom)
                .locationTo(locationTo)
                .date(date)
                .build())
            .externalId(null)
            .options(null)
            .segmentType(SegmentType.COURIER)
            .rootStorageUnitExternalId("generated-0");
    }

    @Nonnull
    static WaybillSegmentDto.WaybillSegmentDtoBuilder createWaybillSegmentBuilder(
        WaybillSegmentDto.ShipmentDto shipmentDto,
        @Nullable Long partnerId
    ) {
        return WaybillSegmentDto.builder()
            .requisiteId(200L)
            .partnerId(partnerId)
            .shipment(shipmentDto)
            .externalId(null)
            .options(null)
            .segmentType(SegmentType.COURIER)
            .rootStorageUnitExternalId("generated-0");
    }

    @Nonnull
    public static LocationDto createLocation(Long id) {
        return createLocation(id, WAREHOUSE);
    }

    @Nonnull
    static LocationDto createLocation(Long id, LocationType locationType) {
        return LocationDto.builder()
            .warehouseId(id)
            .type(locationType)
            .build();
    }

    @Nonnull
    static MonetaryDto createMonetary(@Nullable BigDecimal value) {
        return MonetaryDto.builder()
            .currency("RUB")
            .exchangeRate(BigDecimal.ONE)
            .value(value)
            .build();
    }

    @Nonnull
    public static KorobyteDto createKorobyte(int height, int width, int length, int weight) {
        return KorobyteDto.builder()
            .height(height)
            .width(width)
            .length(length)
            .weightGross(BigDecimal.valueOf(weight))
            .build();
    }

    @Nonnull
    static OrderDraftDeliveryOptionService deliveryServiceBuilder(
        @Nullable ServiceType code,
        @Nullable BigDecimal cost,
        @Nullable Boolean customerPay
    ) {
        return new OrderDraftDeliveryOptionService()
            .setCode(code)
            .setCost(cost)
            .setCustomerPay(customerPay);
    }

    @Nonnull
    static OrderDraftDeliveryOptionService deliveryServiceBuilder() {
        return deliveryServiceBuilder(null, BigDecimal.TEN, true);
    }

    @Nonnull
    static OrderServiceDto.OrderServiceDtoBuilder defaultLomOrderService() {
        return OrderServiceDto.builder()
            .cost(BigDecimal.TEN)
            .customerPay(true);
    }

    @Nonnull
    static WaybillSegmentDtoBuilder waybillSegmentWithStatusDto(int id, SegmentStatus segmentStatus) {
        return WaybillSegmentDto.builder()
            .partnerType(PartnerType.DELIVERY)
            .segmentStatus(segmentStatus)
            .waybillSegmentStatusHistory(List.of(new WaybillSegmentStatusHistoryDto(
                (long) id,
                segmentStatus,
                LocalDate.of(2019, 10, id)
                    .atStartOfDay(CommonsConstants.MSK_TIME_ZONE)
                    .toInstant(),
                LocalDate.of(2019, 10, id)
                    .atStartOfDay(CommonsConstants.MSK_TIME_ZONE)
                    .toInstant(),
                null,
                null
            )));
    }

    @Nonnull
    static WaybillSegmentDtoBuilder waybillSegmentWithStatusDto(int id) {
        return waybillSegmentWithStatusDto(id, SegmentStatus.IN);
    }
}
