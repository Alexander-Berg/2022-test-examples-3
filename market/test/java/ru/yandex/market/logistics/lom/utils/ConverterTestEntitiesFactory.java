package ru.yandex.market.logistics.lom.utils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableSortedSet;

import ru.yandex.market.logistics.lom.entity.Contact;
import ru.yandex.market.logistics.lom.entity.Location;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.OrderContact;
import ru.yandex.market.logistics.lom.entity.Phone;
import ru.yandex.market.logistics.lom.entity.Registry;
import ru.yandex.market.logistics.lom.entity.Shipment;
import ru.yandex.market.logistics.lom.entity.ShipmentApplication;
import ru.yandex.market.logistics.lom.entity.WarehouseWorkTime;
import ru.yandex.market.logistics.lom.entity.WaybillSegment;
import ru.yandex.market.logistics.lom.entity.embedded.Address;
import ru.yandex.market.logistics.lom.entity.embedded.Cost;
import ru.yandex.market.logistics.lom.entity.embedded.Credentials;
import ru.yandex.market.logistics.lom.entity.embedded.DeliveryInterval;
import ru.yandex.market.logistics.lom.entity.embedded.Fio;
import ru.yandex.market.logistics.lom.entity.embedded.Korobyte;
import ru.yandex.market.logistics.lom.entity.embedded.Monetary;
import ru.yandex.market.logistics.lom.entity.embedded.PickupPoint;
import ru.yandex.market.logistics.lom.entity.embedded.Recipient;
import ru.yandex.market.logistics.lom.entity.embedded.Sender;
import ru.yandex.market.logistics.lom.entity.embedded.TimeInterval;
import ru.yandex.market.logistics.lom.entity.enums.ContactType;
import ru.yandex.market.logistics.lom.entity.enums.DeliveryType;
import ru.yandex.market.logistics.lom.entity.enums.LocationType;
import ru.yandex.market.logistics.lom.entity.enums.PartnerSubtype;
import ru.yandex.market.logistics.lom.entity.enums.PartnerType;
import ru.yandex.market.logistics.lom.entity.enums.PlatformClient;
import ru.yandex.market.logistics.lom.entity.enums.RegistryStatus;
import ru.yandex.market.logistics.lom.entity.enums.SegmentType;
import ru.yandex.market.logistics.lom.entity.enums.ShipmentOption;
import ru.yandex.market.logistics.lom.entity.enums.ShipmentType;
import ru.yandex.market.logistics.lom.entity.enums.StorageUnitType;
import ru.yandex.market.logistics.lom.entity.enums.TaxSystem;
import ru.yandex.market.logistics.lom.entity.enums.VatType;
import ru.yandex.market.logistics.lom.entity.enums.tags.WaybillSegmentTag;
import ru.yandex.market.logistics.lom.entity.items.OrderItem;
import ru.yandex.market.logistics.lom.entity.items.OrderItemBox;
import ru.yandex.market.logistics.lom.entity.items.OrderItemBoxStorageUnit;
import ru.yandex.market.logistics.lom.entity.items.StorageUnit;
import ru.yandex.market.logistics.lom.model.enums.CargoType;
import ru.yandex.market.logistics.lom.model.enums.PaymentMethod;

@ParametersAreNonnullByDefault
public class ConverterTestEntitiesFactory {

    private ConverterTestEntitiesFactory() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    public static Order createOrder(String barcode, StorageUnit place) {
        return new Order()
            .setBarcode(barcode)
            .setCredentials(createCredentials())
            .setSender(createSender())
            .setCost(createCost())
            .setDeliveryType(DeliveryType.PICKUP)
            .setDeliveryInterval(new DeliveryInterval()
                .setDateMin(LocalDate.parse("2019-06-06"))
                .setDateMax(LocalDate.parse("2019-06-07"))
                .setStartTime(LocalTime.parse("14:00"))
                .setEndTime(LocalTime.parse("17:00"))
            )
            .setRecipient(createRecipient())
            .setItems(List.of(createOrderItem(1).setBoxes(
                Set.of(new OrderItemBox().setUnits(Set.of(createOrderItemBoxStorageUnit(place, 1))))
            )))
            .setOrderContacts(createOrderContacts())
            .setPickupPoint(createPickupPoint())
            .setComment("test-comment")
            .setMaxAbsentItemsPricePercent(new BigDecimal("12"))
            .setReturnSortingCenterId(10001L)
            .setReturnSortingCenterWarehouse(createScReturnWarehouse())
            .setPlatformClient(PlatformClient.BERU);
    }

    public static void addPersonal(Order order) {
        order.getRecipient()
            .setPersonalAddressId("personal-address-id")
            .setPersonalEmailId("personal-email-id")
            .setPersonalFullnameId("personal-fullname-id")
            .setPersonalGpsId("personal-gps-id");
        order.getOrderContacts().forEach(c -> c.getContact().setPersonalPhoneId("personal-phone-id"));
    }

    @Nonnull
    public static OrderItemBoxStorageUnit createOrderItemBoxStorageUnit(StorageUnit storageUnit, Integer count) {
        return new OrderItemBoxStorageUnit().setCount(count).setStorageUnit(storageUnit);
    }

    @Nonnull
    private static Location createScReturnWarehouse() {
        return new Location()
            .setWarehouseId(1L)
            .setWarehouseExternalId("return-external-id")
            .setWarehouseWorkTime(
                createSchedule()
            )
            .setAddress(createReturnWarehouseAddress())
            .setPhones(createReturnFfPhones())
            .setContact(new Contact().setFio(createPerson()));
    }

    @Nonnull
    private static PickupPoint createPickupPoint() {
        return new PickupPoint().setPickupPointId(1L).setExternalId("externalId-1");
    }

    @Nonnull
    private static Collection<OrderContact> createOrderContacts() {
        return List.of(
            new OrderContact()
                .setContactType(ContactType.RECIPIENT)
                .setContact(createOrderContact())
        );
    }

    @Nonnull
    private static Recipient createRecipient() {
        return new Recipient()
            .setFio(
                new Fio()
                    .setFirstName("test-first-name")
                    .setLastName("test-last-name")
                    .setMiddleName("test-middle-name")
            )
            .setEmail("test-email@test-domain.com");
    }

    @Nonnull
    private static Monetary createMonetary(int scale) {
        return new Monetary()
            .setCurrency("RUB")
            .setExchangeRate(BigDecimal.ONE)
            .setValue(BigDecimal.TEN.multiply(BigDecimal.valueOf(scale)));
    }

    @Nonnull
    private static Korobyte createKorobyte(int scale) {
        return new Korobyte()
            .setLength(scale)
            .setHeight(2 * scale)
            .setWidth(3 * scale)
            .setWeightGross(BigDecimal.valueOf(4 * scale));
    }

    @Nonnull
    public static List<WaybillSegment> createDsFfWaybillSegments(StorageUnit rootUnit) {
        return List.of(
            new WaybillSegment()
                .setId(1L)
                .setSegmentType(SegmentType.SORTING_CENTER)
                .setPartnerType(PartnerType.SORTING_CENTER)
                .setPartnerId(2L)
                .setWaybillSegmentIndex(0)
                .setWaybillShipment(createScWaybillShipment(createLocalDate()))
                .setStorageUnit(rootUnit),
            new WaybillSegment()
                .setId(2L)
                .setSegmentType(SegmentType.PICKUP)
                .setPartnerType(PartnerType.DELIVERY)
                .setPartnerId(20L)
                .setWaybillSegmentIndex(1)
                .setWaybillShipment(createDsImportWaybillShipment(createLocalDate()))
                .setStorageUnit(rootUnit)
                .setPartnerInfo(createDelivery())
        );
    }

    @Nonnull
    public static List<WaybillSegment> createDsFfWaybillSegmentsMultiSc(StorageUnit rootUnit) {
        return List.of(
            new WaybillSegment()
                .setId(1L)
                .setSegmentType(SegmentType.SORTING_CENTER)
                .setPartnerType(PartnerType.SORTING_CENTER)
                .setPartnerId(2L)
                .setWaybillSegmentIndex(0)
                .setWaybillShipment(createScWaybillShipment(createLocalDate()))
                .setStorageUnit(rootUnit),
            new WaybillSegment()
                .setId(2L)
                .setSegmentType(SegmentType.SORTING_CENTER)
                .setPartnerType(PartnerType.SORTING_CENTER)
                .setPartnerId(3L)
                .setWaybillSegmentIndex(1)
                .setWaybillShipment(
                    createScWaybillShipment(createLocalDate())
                        .setLocationFrom(createScLocationTo(201L).setWarehouseWorkTime(createSchedule()))
                )
                .setStorageUnit(rootUnit),
            new WaybillSegment()
                .setId(3L)
                .setSegmentType(SegmentType.SORTING_CENTER)
                .setPartnerType(PartnerType.SORTING_CENTER)
                .setPartnerId(4L)
                .setWaybillSegmentIndex(2)
                .setWaybillShipment(
                    createScWaybillShipment(createLocalDate())
                        .setLocationFrom(createScLocationTo(202L).setWarehouseWorkTime(createSchedule()))
                )
                .setStorageUnit(rootUnit),
            new WaybillSegment()
                .setId(4L)
                .setSegmentType(SegmentType.SORTING_CENTER)
                .setPartnerType(PartnerType.SORTING_CENTER)
                .setPartnerId(5L)
                .setWaybillSegmentIndex(3)
                .setWaybillShipment(
                    createScWaybillShipment(createLocalDate())
                        .setLocationFrom(createScLocationTo(203L).setWarehouseWorkTime(createSchedule()))
                )
                .setStorageUnit(rootUnit),
            new WaybillSegment()
                .setId(5L)
                .setSegmentType(SegmentType.PICKUP)
                .setPartnerType(PartnerType.DELIVERY)
                .setPartnerId(20L)
                .setWaybillSegmentIndex(4)
                .setWaybillShipment(createDsImportWaybillShipment(createLocalDate()))
                .setStorageUnit(rootUnit)
                .setPartnerInfo(createDelivery())
        );
    }

    @Nonnull
    public static List<WaybillSegment> createDsFfWaybillSegmentsWithWithdrawShipment(StorageUnit rootUnit) {
        return List.of(
            new WaybillSegment()
                .setId(1L)
                .setSegmentType(SegmentType.FULFILLMENT)
                .setPartnerType(PartnerType.FULFILLMENT)
                .setPartnerId(2L)
                .setWaybillSegmentIndex(0)
                .setStorageUnit(rootUnit),
            new WaybillSegment()
                .setId(2L)
                .setSegmentType(SegmentType.PICKUP)
                .setPartnerType(PartnerType.DELIVERY)
                .setPartnerId(20L)
                .setWaybillSegmentIndex(1)
                .setWaybillShipment(createDsWithdrawWaybillShipment(createLocalDate()))
                .setStorageUnit(rootUnit)
                .setPartnerInfo(createDelivery())
        );
    }

    @Nonnull
    public static List<WaybillSegment> createDsFfWaybillSegmentsWithoutShipmentType(StorageUnit rootUnit) {
        return List.of(
            new WaybillSegment()
                .setId(1L)
                .setSegmentType(SegmentType.FULFILLMENT)
                .setPartnerType(PartnerType.FULFILLMENT)
                .setPartnerId(2L)
                .setWaybillSegmentIndex(0)
                .setStorageUnit(rootUnit),
            new WaybillSegment()
                .setId(2L)
                .setSegmentType(SegmentType.PICKUP)
                .setPartnerType(PartnerType.DELIVERY)
                .setPartnerId(20L)
                .setWaybillSegmentIndex(1)
                .setWaybillShipment(createDsWaybillShipmentWithoutType(createLocalDate()))
                .setStorageUnit(rootUnit)
                .setPartnerInfo(createDelivery())
        );
    }

    @Nonnull
    public static List<WaybillSegment> createDsFfWaybillSegments(
        StorageUnit rootUnit,
        Long dropshipId,
        PartnerSubtype deliverySubtype,
        String instruction
    ) {
        return List.of(
            createDsWaybillSegment(rootUnit)
                .setPartnerType(PartnerType.DROPSHIP)
                .setId(1L)
                .setPartnerId(dropshipId)
                .setWaybillSegmentIndex(0),
            createDsWaybillSegment(rootUnit, instruction)
                .setPartnerType(PartnerType.DELIVERY)
                .setId(2L)
                .setPartnerId(1L)
                .setPartnerSubtype(deliverySubtype)
                .setWaybillSegmentIndex(1)
        );
    }

    @Nonnull
    public static List<WaybillSegment> createDropshipMovementPickupWaybill(
        StorageUnit rootUnit,
        String pickupInstruction
    ) {
        WaybillSegment pickupSegment = createDsWaybillSegment(rootUnit);
        pickupSegment.getWaybillShipment().getLocationTo().setInstruction(pickupInstruction);
        return List.of(
            createDsWaybillSegment(rootUnit)
                .setPartnerType(PartnerType.DROPSHIP)
                .setSegmentType(SegmentType.FULFILLMENT)
                .setWaybillSegmentIndex(0),
            createDsWaybillSegment(rootUnit)
                .setPartnerSubtype(PartnerSubtype.TAXI_EXPRESS)
                .setSegmentType(SegmentType.MOVEMENT)
                .setWaybillSegmentIndex(1),
            pickupSegment.setWaybillSegmentIndex(2)
        );
    }

    @Nonnull
    public static List<WaybillSegment> createDropshipMovementExpressWaybill(
        StorageUnit rootUnit,
        String expressSegmentExternalId,
        SegmentType expressSegmentType
    ) {
        return List.of(
            createDsWaybillSegment(rootUnit)
                .setPartnerType(PartnerType.DROPSHIP)
                .setSegmentType(SegmentType.FULFILLMENT)
                .addTag(WaybillSegmentTag.EXPRESS)
                .setWaybillSegmentIndex(0),
            createDsWaybillSegment(rootUnit)
                .setPartnerSubtype(PartnerSubtype.TAXI_EXPRESS)
                .setSegmentType(expressSegmentType)
                .setExternalId(expressSegmentExternalId)
                .addTags(Set.of(WaybillSegmentTag.CALL_COURIER, WaybillSegmentTag.EXPRESS))
                .setWaybillSegmentIndex(1)
        );
    }

    @Nonnull
    public static List<WaybillSegment> createDropshipMovementPickupCourierWaybill(StorageUnit rootUnit) {
        return List.of(
            createDsWaybillSegment(rootUnit)
                .setPartnerType(PartnerType.DROPSHIP)
                .setSegmentType(SegmentType.FULFILLMENT)
                .setWaybillSegmentIndex(0),
            createDsWaybillSegment(rootUnit)
                .setPartnerSubtype(PartnerSubtype.TAXI_EXPRESS)
                .setSegmentType(SegmentType.MOVEMENT)
                .setWaybillSegmentIndex(1),
            createDsWaybillSegment(rootUnit)
                .setWaybillSegmentIndex(2),
            createDsWaybillSegment(rootUnit)
                .setSegmentType(SegmentType.COURIER)
                .setWaybillSegmentIndex(3)
        );
    }

    @Nonnull
    public static List<WaybillSegment> createDsFfWaybillSegmentsWithDropoff(StorageUnit rootUnit) {
        return List.of(
            new WaybillSegment()
                .setId(1L)
                .setSegmentType(SegmentType.SORTING_CENTER)
                .setPartnerType(PartnerType.DELIVERY)
                .setPartnerId(2L)
                .setWaybillSegmentIndex(0)
                .setWaybillShipment(createScWaybillShipment(createLocalDate()))
                .setStorageUnit(rootUnit),
            new WaybillSegment()
                .setId(2L)
                .setSegmentType(SegmentType.PICKUP)
                .setPartnerType(PartnerType.DELIVERY)
                .setPartnerId(20L)
                .setWaybillSegmentIndex(1)
                .setWaybillShipment(createDsImportWaybillShipment(createLocalDate()))
                .setStorageUnit(rootUnit)
                .setPartnerInfo(createDelivery())
        );
    }

    @Nonnull
    public static List<WaybillSegment> createDsWaybillSegments(@Nullable StorageUnit rootUnit) {
        return List.of(createDsWaybillSegment(rootUnit));
    }

    @Nonnull
    public static WaybillSegment createDsWaybillSegment(@Nullable StorageUnit rootUnit) {
        return new WaybillSegment().setPartnerType(PartnerType.DELIVERY)
            .setId(1L)
            .setSegmentType(SegmentType.PICKUP)
            .setPartnerId(1L)
            .setWaybillSegmentIndex(0)
            .setWaybillShipment(createDsImportWaybillShipment(createLocalDate()))
            .setStorageUnit(rootUnit);
    }

    @Nonnull
    public static WaybillSegment createDsWaybillSegment(@Nullable StorageUnit rootUnit, String instruction) {
        return new WaybillSegment().setPartnerType(PartnerType.DELIVERY)
            .setId(1L)
            .setSegmentType(SegmentType.PICKUP)
            .setPartnerId(1L)
            .setWaybillSegmentIndex(0)
            .setWaybillShipment(
                createDsImportWaybillShipment(createLocalDate()).setLocationFrom(
                    createDsLocationFrom().setWarehouseWorkTime(createSchedule()).setInstruction(instruction)
                )
            )
            .setStorageUnit(rootUnit);
    }

    @Nonnull
    public static ru.yandex.market.logistics.lom.entity.Location createExpressScReturnWarehouse() {
        return new ru.yandex.market.logistics.lom.entity.Location()
            .setWarehouseId(1L)
            .setWarehouseExternalId("express-return-external-id")
            .setWarehouseWorkTime(createSchedule())
            .setAddress(createExpressReturnWarehouseAddress())
            .setPhones(createReturnFfPhones())
            .setContact(new Contact().setFio(createPerson()));
    }

    @Nonnull
    private static Address createExpressReturnWarehouseAddress() {
        return new Address()
            .setCountry("Россия")
            .setLocality("express-return-settlement")
            .setRegion("Московская область")
            .setLatitude(BigDecimal.valueOf(1))
            .setLongitude(BigDecimal.valueOf(1))
            .setGeoId(1)
            .setStreet("express-return-street")
            .setHouse("express-return-house")
            .setHousing("express-return-housing")
            .setBuilding("express-return-building")
            .setRoom("express-return-apartment")
            .setSettlement("express-return-settlement");
    }

    @Nonnull
    private static Sender createSender() {
        return new Sender()
            .setId(1L)
            .setName("sender-name")
            .setTaxSystem(TaxSystem.OSN)
            .setPhone(
                new ru.yandex.market.logistics.lom.entity.embedded.Phone()
                    .setPhoneNumber("+7 (495) 999 9999")
                    .setAdditionalNumber("12345")
            )
            .setUrl("www.sender-url.com");
    }

    @Nonnull
    private static WaybillSegment.PartnerInfo createDelivery() {
        return new WaybillSegment.PartnerInfo()
            .setName("sdek")
            .setBalanceClientId(200600L)
            .setCredentials(createCredentials());
    }

    @Nonnull
    private static SortedSet<Cost.ServiceCost> createServiceCosts() {
        TreeSet<Cost.ServiceCost> serviceCosts = new TreeSet<>();
        serviceCosts.add(new Cost.ServiceCost().setCode(ShipmentOption.CHECK).setCost(BigDecimal.valueOf(0)));
        serviceCosts.add(new Cost.ServiceCost().setCode(ShipmentOption.CASH_SERVICE).setCost(BigDecimal.valueOf(1.0)));
        return serviceCosts;
    }

    @Nonnull
    private static WaybillSegment.WaybillShipment createScWaybillShipment(LocalDate deliveryDate) {
        return new WaybillSegment.WaybillShipment()
            .setType(ShipmentType.IMPORT)
            .setDate(deliveryDate)
            .setDateTime(createOffsetDateTime())
            .setLocationFrom(createShopLocation().setWarehouseWorkTime(createSchedule()))
            .setLocationTo(createScLocationTo(7L).setWarehouseWorkTime(createSchedule()));
    }

    @Nonnull
    private static WaybillSegment.WaybillShipment createDsImportWaybillShipment(LocalDate deliveryDate) {
        return new WaybillSegment.WaybillShipment()
            .setType(ShipmentType.IMPORT)
            .setDate(deliveryDate)
            .setLocationFrom(createDsLocationFrom().setWarehouseWorkTime(createSchedule()))
            .setLocationTo(createDsLocationTo().setWarehouseWorkTime(createDsSchedule()));
    }

    @Nonnull
    private static WaybillSegment.WaybillShipment createDsWithdrawWaybillShipment(LocalDate deliveryDate) {
        return new WaybillSegment.WaybillShipment()
            .setType(ShipmentType.WITHDRAW)
            .setDate(deliveryDate)
            .setLocationFrom(createDsLocationFrom().setWarehouseWorkTime(createSchedule()))
            .setLocationTo(createDsLocationTo().setWarehouseWorkTime(createDsSchedule()));
    }

    @Nonnull
    private static WaybillSegment.WaybillShipment createDsWaybillShipmentWithoutType(LocalDate deliveryDate) {
        return new WaybillSegment.WaybillShipment()
            .setDate(deliveryDate)
            .setLocationFrom(createDsLocationFrom().setWarehouseWorkTime(createSchedule()))
            .setLocationTo(createDsLocationTo().setWarehouseWorkTime(createDsSchedule()));
    }

    @Nonnull
    private static Contact createContact() {
        return new Contact().setFio(createPerson()).setPhone(createContactPhone());
    }

    @Nonnull
    private static Contact createShopContact() {
        return new Contact().setFio(createShopPerson()).setPhone(createContactPhone());
    }

    @Nonnull
    private static Contact createOrderContact() {
        return new Contact().setFio(createPerson()).setPhone(createContactPhone()).setExtension("12345");
    }

    @Nonnull
    private static String createContactPhone() {
        return "+7 (495) 999 9999";
    }

    @Nonnull
    private static Fio createPerson() {
        return new Fio().setFirstName("Иван").setLastName("Иванов").setMiddleName("Иванович");
    }

    @Nonnull
    private static Fio createShopPerson() {
        return new Fio().setFirstName("Магазин").setLastName("Магазинов").setMiddleName("Магазинович");
    }

    @Nonnull
    private static Set<Phone> createPhones() {
        return Set.of(createPhone());
    }

    @Nonnull
    private static Set<Phone> createReturnFfPhones() {
        return Set.of(createReturnFfPhone());
    }

    @Nonnull
    private static Set<Phone> createShopPhones() {
        return Set.of(createShopPhone());
    }

    @Nonnull
    private static TimeInterval createTimeInterval() {
        return new TimeInterval().setFrom(LocalTime.of(10, 0)).setTo(LocalTime.of(18, 0));
    }

    @Nonnull
    public static Location createScLocationFrom(long warehouseId) {
        return new Location()
            .setType(LocationType.WAREHOUSE)
            .setWarehouseId(warehouseId)
            .setContact(createContact())
            .setPhones(createPhones())
            .setIncorporation("ooo magaz")
            .setWarehouseExternalId("external-id")
            .setAddress(createScWarehouseFromAddress());
    }

    @Nonnull
    public static Location createShopLocation() {
        return new Location()
            .setType(LocationType.WAREHOUSE)
            .setWarehouseId(4L)
            .setContact(createShopContact())
            .setPhones(createShopPhones())
            .setWarehouseExternalId("shop-external-id")
            .setAddress(createScWarehouseFromAddress());
    }

    @Nonnull
    public static Location createScLocationTo(long warehouseId) {
        return new Location()
            .setType(LocationType.WAREHOUSE)
            .setWarehouseId(warehouseId)
            .setWarehouseExternalId("external-id")
            .setPhones(createPhones())
            .setIncorporation("ooo sc")
            .setContact(createContact()).setAddress(createScWarehouseToAddress());
    }

    @Nonnull
    private static Location createDsLocationFrom() {
        return new Location()
            .setType(LocationType.WAREHOUSE)
            .setWarehouseId(3L)
            .setContact(createContact())
            .setPhones(Set.of(createScWarehousePhone()))
            .setIncorporation("ooo magaz")
            .setWarehouseExternalId("sc-external-id")
            .setAddress(createDsWarehouseFromAddress());
    }

    @Nonnull
    private static Location createDsLocationTo() {
        return new Location()
            .setType(LocationType.PICKUP)
            .setWarehouseId(1L)
            .setWarehouseExternalId("externalId-1")
            .setPhones(Set.of(createDsWarehousePhone()))
            .setIncorporation("ooo sc")
            .setContact(createContact()).setAddress(createPickupAddress());
    }

    @Nonnull
    private static Address createReturnWarehouseAddress() {
        return new Address()
            .setCountry("Россия")
            .setLocality("return-settlement")
            .setRegion("Московская область")
            .setLatitude(BigDecimal.valueOf(1))
            .setLongitude(BigDecimal.valueOf(1))
            .setGeoId(1)
            .setStreet("return-street")
            .setHouse("return-house")
            .setHousing("return-housing")
            .setBuilding("return-building")
            .setRoom("return-apartment")
            .setSettlement("return-settlement");
    }

    @Nonnull
    private static Address createScWarehouseToAddress() {
        return new Address()
            .setCountry("Россия")
            .setLocality("sc-settlement")
            .setRegion("Московская область")
            .setLatitude(BigDecimal.valueOf(3))
            .setLongitude(BigDecimal.valueOf(3))
            .setGeoId(3)
            .setStreet("sc-street")
            .setHouse("sc-house")
            .setHousing("sc-housing")
            .setBuilding("sc-building")
            .setRoom("sc-apartment")
            .setSettlement("sc-settlement");
    }

    @Nonnull
    private static Address createScWarehouseFromAddress() {
        return new Address()
            .setCountry("Россия")
            .setLocality("shop-settlement")
            .setRegion("Московская область")
            .setLatitude(BigDecimal.valueOf(4))
            .setLongitude(BigDecimal.valueOf(4))
            .setGeoId(4)
            .setStreet("shop-street")
            .setHouse("shop-house")
            .setHousing("shop-housing")
            .setBuilding("shop-building")
            .setRoom("shop-apartment")
            .setSettlement("shop-settlement");
    }

    @Nonnull
    public static Address createPickupAddress() {
        return new Address()
            .setBuilding("pickup-building")
            .setCountry("Россия")
            .setLocality("pickup-settlement")
            .setRegion("Московская область")
            .setHouse("pickup-house")
            .setHousing("pickup-housing")
            .setGeoId(5)
            .setLatitude(BigDecimal.valueOf(5))
            .setLongitude(BigDecimal.valueOf(5))
            .setRoom("pickup-apartment")
            .setSettlement("pickup-settlement")
            .setStreet("pickup-street");
    }

    @Nonnull
    public static Address createRecipientAddress() {
        return new Address()
            .setBuilding("recipient-building")
            .setCountry("Россия")
            .setLocality("recipient-settlement")
            .setRegion("Московская область")
            .setHouse("recipient-house")
            .setHousing("recipient-housing")
            .setGeoId(5)
            .setLatitude(BigDecimal.valueOf(5))
            .setLongitude(BigDecimal.valueOf(5))
            .setRoom("recipient-apartment")
            .setSettlement("recipient-settlement")
            .setStreet("recipient-street");
    }

    @Nonnull
    private static Address createDsWarehouseFromAddress() {
        return new Address()
            .setBuilding("sc-building")
            .setCountry("Россия")
            .setLocality("sc-settlement")
            .setRegion("Московская область")
            .setHouse("sc-house")
            .setHousing("sc-housing")
            .setGeoId(3)
            .setLatitude(BigDecimal.valueOf(3))
            .setLongitude(BigDecimal.valueOf(3))
            .setRoom("sc-apartment")
            .setSettlement("sc-settlement")
            .setStreet("sc-street");
    }

    @Nonnull
    private static Cost createCost() {
        return new Cost()
            .setPaymentMethod(PaymentMethod.CARD)
            .setCashServicePercent(BigDecimal.valueOf(10))
            .setAssessedValue(BigDecimal.valueOf(1001))
            .setAmountPrepaid(BigDecimal.ZERO)
            .setItemsSum(BigDecimal.valueOf(2000))
            .setDelivery(BigDecimal.valueOf(10))
            .setDeliveryForCustomer(BigDecimal.valueOf(2000))
            .setIsFullyPrepaid(false)
            .setTotal(BigDecimal.valueOf(4000))
            .setServices(createServiceCosts())
            .setTariffId(1L);
    }

    @Nonnull
    public static Set<WarehouseWorkTime> createSchedule() {
        return Set.of(new WarehouseWorkTime().setInterval(createTimeInterval()).setDay(1));
    }

    @Nonnull
    private static Set<WarehouseWorkTime> createDsSchedule() {
        return Set.of(
            new WarehouseWorkTime()
                .setInterval(
                    new TimeInterval()
                        .setFrom(LocalTime.of(10, 0))
                        .setTo(LocalTime.of(23, 0))
                )
                .setDay(1)
        );
    }

    @Nonnull
    private static Phone createPhone() {
        return new Phone()
            .setNumber("+7 (495) 999 9999")
            .setAdditional("12345");
    }

    @Nonnull
    private static Phone createReturnFfPhone() {
        return new Phone()
            .setNumber("+7 999 111 1111")
            .setAdditional("111");
    }

    @Nonnull
    private static Phone createScWarehousePhone() {
        return new Phone()
            .setNumber("+7 999 333 3333")
            .setAdditional("333");
    }

    @Nonnull
    private static Phone createShopPhone() {
        return new Phone()
            .setNumber("+7 999 444 4444")
            .setAdditional("444");
    }

    @Nonnull
    private static Phone createDsWarehousePhone() {
        return new Phone()
            .setNumber("+7 923 243 5555")
            .setAdditional("777");
    }

    @Nonnull
    private static Credentials createCredentials() {
        return new Credentials()
            .setIncorporation("credentials-incorporation")
            .setLegalForm("IP")
            .setOgrn("credentials-ogrn")
            .setUrl("credentials-url")
            .setInn("credentials-inn")
            .setName("credentials-name")
            .setEmail("credentials-email@test-domain.com");
    }

    @Nonnull
    public static Shipment createShipment(ShipmentType type) {
        return new Shipment()
            .setId(10L)
            .setPartnerType(PartnerType.SORTING_CENTER)
            .setShipmentDate(createLocalDate())
            .setShipmentType(type);
    }

    @Nonnull
    private static LocalDate createLocalDate() {
        return LocalDate.of(2019, 6, 11);
    }

    @Nonnull
    public static OffsetDateTime createOffsetDateTime() {
        return OffsetDateTime.of(LocalDateTime.of(2019, 6, 11, 9, 21, 0), ZoneOffset.ofHours(3));
    }

    @Nonnull
    public static Registry createRegistry() {
        return new Registry()
            .setId(1L)
            .setStatus(RegistryStatus.PROCESSING)
            .setShipment(createShipment(ShipmentType.WITHDRAW));
    }

    @Nonnull
    public static DeliveryInterval createDsDeliveryInterval() {
        return new DeliveryInterval()
            .setStartTime(LocalTime.of(14, 0, 0))
            .setEndTime(LocalTime.of(17, 0, 0))
            .setDateMin(LocalDate.of(2019, 6, 6))
            .setDateMax(LocalDate.of(2019, 6, 6));
    }

    @Nonnull
    public static ShipmentApplication createFfShipmentApplication(ShipmentType shipmentType) {
        Shipment shipment = createShipment(shipmentType)
            .setId(1L)
            .setPartnerIdTo(20L);
        ShipmentApplication application = new ShipmentApplication()
            .setId(1L)
            .setShipment(shipment)
            .setKorobyte(createKorobyte(1))
            .setComment("ds shipment application")
            .setCourier(createCourier())
            .setInterval(createTimeInterval());

        shipment.setShipmentApplications(Set.of(application));
        return application;
    }

    @Nonnull
    private static Contact createCourier() {
        return createContact().setPhone("+7 (495) 999 9999").setExtension("12345");
    }

    @Nonnull
    public static SortedSet<Cost.ServiceCost> allServices() {
        return Arrays.stream(ShipmentOption.values())
            .map(ConverterTestEntitiesFactory::createService)
            .collect(Collectors.toCollection(TreeSet::new));
    }

    @Nonnull
    private static Cost.ServiceCost createService(ShipmentOption shipmentOption) {
        Cost.ServiceCost service = new Cost.ServiceCost().setCode(shipmentOption).setCost(BigDecimal.ONE);
        if (shipmentOption == ShipmentOption.DELIVERY) {
            service.setTaxes(ImmutableSortedSet.of(VatType.VAT_20));
        }
        return service;
    }

    @Nonnull
    public static OrderItem createOrderItem(int id) {
        return createOrderItem(id, null);
    }

    @Nonnull
    public static OrderItem createOrderItem(int id, @Nullable Boolean removableIfAbsent) {
        return new OrderItem()
            .setId((long) id)
            .setName("item " + id)
            .setVendorId(100L * id)
            .setArticle("item article " + id)
            .setCount(id)
            .setDimensions(createKorobyte(id))
            .setPrice(createMonetary(id))
            .setAssessedValue(createMonetary(id + 1))
            .setVatType(VatType.NO_VAT)
            .setRemovableIfAbsent(removableIfAbsent)
            .setInstances(createInstances(id))
            .setCategoryName("Телефоны")
            .setCargoTypes(EnumSet.of(CargoType.TECH_AND_ELECTRONICS))
            .setSupplierName("Имя поставщика")
            .setSupplierPhone("+79876543210")
            .setSupplierInn("1231231234");
    }

    @Nonnull
    public static List<Map<String, String>> createInstances(int id) {
        return Stream
            .iterate(1, n -> n + 1)
            .limit(id)
            .map(integer -> Map.of("cis", "123abc"))
            .collect(Collectors.toList());
    }

    @Nonnull
    public static StorageUnit createStorageUnit(StorageUnitType type, int id) {
        return createStorageUnit(type, id, id);
    }

    @Nonnull
    public static StorageUnit createStorageUnit(StorageUnitType type, int id, int dimensionsScale) {
        return new StorageUnit()
            .setId((long) id)
            .setPartnerId(100L + id)
            .setExternalId("place external id " + id)
            .setDimensions(createKorobyte(dimensionsScale))
            .setUnitType(type);
    }

    @Nonnull
    public static StorageUnit createSinglePlaceRoot() {
        return createStorageUnit(StorageUnitType.ROOT, 0, 0)
            .setChildren(Set.of(createStorageUnit(StorageUnitType.PLACE, 1, 1)));
    }
}
