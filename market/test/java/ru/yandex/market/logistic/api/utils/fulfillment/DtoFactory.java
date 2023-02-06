package ru.yandex.market.logistic.api.utils.fulfillment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;

import ru.yandex.market.logistic.api.model.fulfillment.Barcode;
import ru.yandex.market.logistic.api.model.fulfillment.BarcodeSource;
import ru.yandex.market.logistic.api.model.fulfillment.Car;
import ru.yandex.market.logistic.api.model.fulfillment.CargoType;
import ru.yandex.market.logistic.api.model.fulfillment.CargoTypes;
import ru.yandex.market.logistic.api.model.fulfillment.Courier;
import ru.yandex.market.logistic.api.model.fulfillment.Delivery;
import ru.yandex.market.logistic.api.model.fulfillment.DeliveryType;
import ru.yandex.market.logistic.api.model.fulfillment.DocData;
import ru.yandex.market.logistic.api.model.fulfillment.DocTemplate;
import ru.yandex.market.logistic.api.model.fulfillment.DocTemplateType;
import ru.yandex.market.logistic.api.model.fulfillment.Email;
import ru.yandex.market.logistic.api.model.fulfillment.Item;
import ru.yandex.market.logistic.api.model.fulfillment.ItemPlace;
import ru.yandex.market.logistic.api.model.fulfillment.Korobyte;
import ru.yandex.market.logistic.api.model.fulfillment.LegalForm;
import ru.yandex.market.logistic.api.model.fulfillment.Location;
import ru.yandex.market.logistic.api.model.fulfillment.Order;
import ru.yandex.market.logistic.api.model.fulfillment.Param;
import ru.yandex.market.logistic.api.model.fulfillment.PartnerInfo;
import ru.yandex.market.logistic.api.model.fulfillment.PaymentType;
import ru.yandex.market.logistic.api.model.fulfillment.Person;
import ru.yandex.market.logistic.api.model.fulfillment.PersonalLocation;
import ru.yandex.market.logistic.api.model.fulfillment.PersonalPhone;
import ru.yandex.market.logistic.api.model.fulfillment.PersonalPhysicalPersonSender;
import ru.yandex.market.logistic.api.model.fulfillment.PersonalRecipient;
import ru.yandex.market.logistic.api.model.fulfillment.Phone;
import ru.yandex.market.logistic.api.model.fulfillment.PhysicalPersonSender;
import ru.yandex.market.logistic.api.model.fulfillment.Place;
import ru.yandex.market.logistic.api.model.fulfillment.Recipient;
import ru.yandex.market.logistic.api.model.fulfillment.RegisterType;
import ru.yandex.market.logistic.api.model.fulfillment.RegisterUnit;
import ru.yandex.market.logistic.api.model.fulfillment.RegisterUnitType;
import ru.yandex.market.logistic.api.model.fulfillment.RemainingLifetimes;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.ReturnInfo;
import ru.yandex.market.logistic.api.model.fulfillment.ReturnType;
import ru.yandex.market.logistic.api.model.fulfillment.Sender;
import ru.yandex.market.logistic.api.model.fulfillment.Service;
import ru.yandex.market.logistic.api.model.fulfillment.ServiceType;
import ru.yandex.market.logistic.api.model.fulfillment.ShelfLife;
import ru.yandex.market.logistic.api.model.fulfillment.ShelfLives;
import ru.yandex.market.logistic.api.model.fulfillment.Tax;
import ru.yandex.market.logistic.api.model.fulfillment.TaxType;
import ru.yandex.market.logistic.api.model.fulfillment.Taxation;
import ru.yandex.market.logistic.api.model.fulfillment.TransportationRegister;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.logistic.api.model.fulfillment.UnitOperationType;
import ru.yandex.market.logistic.api.model.fulfillment.VatValue;
import ru.yandex.market.logistic.api.model.fulfillment.Warehouse;
import ru.yandex.market.logistic.api.model.fulfillment.WorkTime;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.logistic.api.utils.TimeInterval;

/**
 * Утилитный класс для генерации различного рода тестовых DTO.
 *
 * @author avetokhin 26.12.18.
 */
public final class DtoFactory {

    private DtoFactory() {
        throw new UnsupportedOperationException();
    }

    /**
     * Сгенерировать список {@link UnitId} указанного размера.
     */
    public static List<UnitId> generateUnitIds(int size) {
        return IntStream.range(0, size)
            .mapToObj(index -> new UnitId.UnitIdBuilder((long) index, "article" + index).setId("id" + index).build())
            .collect(Collectors.toList());
    }

    /**
     * Устаревший элемент. Актуальный Warehouse
     * Пункт, из которого нужно забрать заказ.
     */
    private static Location createLocationFrom() {
        return new Location.LocationBuilder("Россия", "Москва и Московская область", "Котельники")
            .setFederalDistrict("Центральный федеральный округ")
            .setSubRegion("Городской округ Котельники")
            .setLocationId(21651)
            .build();
    }

    public static Location createLocationTo() {
        return createLocationTo("Центральный федеральный округ");
    }

    private static PersonalLocation createPersonalLocationTo() {
        return new PersonalLocation.PersonalLocationBuilder("location_to_address_id")
            .setPersonalGpsId("location_to_gps_id")
            .build();
    }

    private static PersonalPhysicalPersonSender createPersonalPhysicalPersonSender() {
        return new PersonalPhysicalPersonSender.PersonalPhysicalPersonSenderBuilder(
            "sender-fullname-id",
            new PersonalPhone.PersonalPhoneBuilder("sender-phone-id").setAdditional("123").build()
        ).build();
    }

    /**
     * Пункт, в который нужно доставить заказ.
     */
    private static Location createLocationTo(String federalDistrict) {
        return new Location.LocationBuilder("Россия", "Москва и Московская область", "Москва")
            .setFederalDistrict(federalDistrict)
            .setSubRegion("Городской округ")
            .setSettlement("Поселение")
            .setStreet("9-я Северная линия")
            .setHouse("23,3")
            .setBuilding("1")
            .setHousing("2")
            .setRoom("98")
            .setZipCode("123456")
            .setPorch("1")
            .setFloor(5)
            .setMetro("Дмитровская")
            .setLat(BigDecimal.valueOf(55.733957))
            .setLng(BigDecimal.valueOf(37.588274))
            .setLocationId(213)
            .build();
    }

    /**
     * Список товаров.
     */
    public static List<Item> createItems() {
        List<Item> items = new ArrayList<>();
        items.add(createFirstItemBuilder().build());
        items.add(createSecondItemBuilder().build());
        return items;
    }

    public static List<Item> createItemsWithRemainingLifetimes() {
        List<Item> items = new ArrayList<>();
        items.add(createFirstItemBuilder()
            .setRemainingLifetimes(createRemainingLifetimes(10, 50, 20, 40))
            .build());
        items.add(createSecondItemBuilder().build());

        return items;
    }

    /**
     * Список товаров с невалидным значением процетов остаточного срока годности на отгрузку.
     */
    public static List<Item> createItemWithInvalidLifetimePercentage() {
        return Arrays.asList(
            createFirstItemBuilder()
                .setRemainingLifetimes(createRemainingLifetimes(10, 100, 20, 40))
                .build()
        );
    }

    /**
     * Список товаров с отсутствием остаточного срока годности на отгрузку.
     */
    public static List<Item> createItemWithEmptyOutboundRemainingLifetimes() {
        return Arrays.asList(
            createFirstItemBuilder()
                .setRemainingLifetimes(createRemainingLifetimes(10, 30, null, null))
                .build()
        );
    }

    private static Item.ItemBuilder createFirstItemBuilder() {
        return new Item.ItemBuilder("Nordic Хлопья пшенные, 500 г", 1, BigDecimal.valueOf(75))
            .setUnitId(new UnitId.UnitIdBuilder(465852L, "000139.би").setId("159346127").build())
            .setArticle("475690493303.000139.би")
            .setCargoType(CargoType.UNKNOWN)
            .setCargoTypes(new CargoTypes(Collections.singletonList(CargoType.ART)))
            .setVendorCodes(Collections.singletonList("73410624900"))
            .setInboundServices(createServices())
            .setUntaxedPrice(BigDecimal.valueOf(90))
            .setBarcodes(Collections.singletonList(
                new Barcode.BarcodeBuilder("54321")
                    .setSource(BarcodeSource.PARTNER)
                    .setType("Some type")
                    .build()
            ))
            .setDescription("Описание товара")
            .setKorobyte(createKorobyte())
            .setHasLifeTime(true)
            .setLifeTime(10)
            .setBoxCount(2)
            .setComment("Комментарий к заказу")
            .setRemovableIfAbsent(true)
            .setTax(new Tax(TaxType.VAT, VatValue.TEN))
            .setUnitOperationType(UnitOperationType.CROSSDOCK)
            .setCheckImei(1)
            .setImeiMask(".+")
            .setCheckSn(1)
            .setSnMask(".+")
            .setInstances(createInstances())
            .setBase64EncodedInstances(createBase64EncodedInstances());
    }

    private static Item.ItemBuilder createSecondItemBuilder() {
        return new Item.ItemBuilder(
            "Novosvit Лифтинг-полоски для области вокруг глаз (12 шт.)",
            1,
            BigDecimal.valueOf(226)
        )
            .setUnitId(new UnitId.UnitIdBuilder(549309L, "4607086562215").setId("100561421890").build())
            .setArticle("475690556620.4607086562215")
            .setCargoType(CargoType.UNKNOWN)
            .setTax(new Tax(TaxType.VAT, VatValue.TWENTY))
            .setUnitOperationType(UnitOperationType.FULFILLMENT);
    }

    public static List<Map<String, String>> createInstances() {
        return Arrays.asList(
            new HashMap<String, String>() {{
                put("CIS", "123345-abcDEF");
            }},
            new HashMap<String, String>() {{
                put("CIS", "abcDEF-123345");
            }}
        );
    }

    public static List<Map<String, String>> createBase64EncodedInstances() {
        return Arrays.asList(
            new HashMap<String, String>() {{
                put("CIS", "MTIzMzQ1LWFiY0RFRg==");
                put("CIS_FULL", "MTIzMzQ1LWFiY0RFRi1YWVow");
            }},
            new HashMap<String, String>() {{
                put("CIS", "YWJjREVGLTEyMzM0NQ==");
                put("CIS_FULL", "YWJjREVGLTEyMzM0NS0wWllY");
            }}
        );
    }

    private static RemainingLifetimes createRemainingLifetimes(
        Integer inboundDays,
        Integer inboundPercentage,
        Integer outboundDays,
        Integer outboundPercentage
    ) {
        return new RemainingLifetimes(
            createShelfLives(inboundDays, inboundPercentage),
            createShelfLives(outboundDays, outboundPercentage)
        );
    }

    private static ShelfLives createShelfLives(Integer days, Integer percentage) {
        return new ShelfLives(
            Optional.ofNullable(days).map(ShelfLife::new).orElse(null),
            Optional.ofNullable(percentage).map(ShelfLife::new).orElse(null)
        );
    }

    /**
     * Получатель заказа.
     */
    public static Recipient createRecipient() {
        return new Recipient.RecipientBuilder(
            new Person.PersonBuilder("Наталья").setSurname("Иванова").build(),
            Collections.singletonList(new Phone.PhoneBuilder("79991234567").build())
        )
            .setEmail(new Email("test@yandex.ru"))
            .build();
    }

    /**
     * Персональные данные получателя заказа.
     */
    private static PersonalRecipient createPersonalRecipient() {
        List<PersonalPhone> personalPhones = Arrays.asList(
            new PersonalPhone("phone_id_1", "123"),
            new PersonalPhone("phone_id_2", null)
        );
        return new PersonalRecipient("fio_id_1", personalPhones, "email_id_1");
    }

    /**
     * Данные о магазине.
     */
    public static Sender createSender() {
        return new Sender.SenderBuilder(
            new ResourceId.ResourceIdBuilder().setYandexId("431782").build(),
            "ООО «Яндекс Маркет»",
            "test.ru"
        )
            .setPartnerId("1234567")
            .setLegalForm(LegalForm.OOO)
            .setPhones(Collections.singletonList(new Phone.PhoneBuilder("8 800 234-27-12").build()))
            .setName("БЕРУ")
            .setEmail("test@test.ru")
            .setContact(createPerson())
            .setOgrn("1167746491395")
            .setInn("2342342342356")
            .setAddress(createLocationTo())
            .setType("ip")
            .setTaxation(Taxation.OSN)
            .build();
    }

    private static Person createPerson() {
        return new Person.PersonBuilder("Иван").setSurname("Иванов").setPatronymic("Иванович").build();
    }

    /**
     * Вес и габариты объекта.
     */
    public static Korobyte createKorobyte() {
        return new Korobyte.KorobyteBuiler(45, 16, 21, BigDecimal.valueOf(3.2))
            .setWeightNet(BigDecimal.valueOf(2))
            .setWeightTare(BigDecimal.valueOf(1.2))
            .build();
    }

    private static List<Place> createPlaces() {
        return Collections.singletonList(
            new Place.PlaceBuilder(new ResourceId.ResourceIdBuilder().setPartnerId("2222").build())
                .setKorobyte(createKorobyte())
                .setItemPlaces(Collections.singletonList(
                    new ItemPlace(new UnitId.UnitIdBuilder(0L, "75690200345480.Checkouter-test-20")
                        .setId("123id")
                        .build(),
                        1)))
                .build());
    }

    /**
     * Данные о службе доставки.
     */
    private static Delivery createDelivery(Courier courier) {
        return new Delivery.DeliveryBuilder("maschrout",
            Collections.singletonList(new Phone.PhoneBuilder("79099999999").build()),
            "contract #1",
            Collections.singletonList(
                new DocTemplate("Doc #1", 1, DocTemplateType.DOCUMENT, "Template #1")),
            1)
            .setDeliveryId(new ResourceId.ResourceIdBuilder().setYandexId("50").build())
            .setIntakeTime(createIntakeTime())
            .setCourier(courier)
            .build();
    }

    private static List<WorkTime> createIntakeTime() {
        return Collections.singletonList(
            new WorkTime(1, Arrays.asList(
                new TimeInterval("11:00:00+03:00/15:00:00+03:00"),
                new TimeInterval("16:00:00+03:00/20:00:00+03:00")
            )));
    }

    /**
     * Данные о складе.
     */
    public static Warehouse createWarehouse() {
        List<Phone> phones = new ArrayList<>();
        phones.add(new Phone.PhoneBuilder("78006008076").build());
        phones.add(new Phone.PhoneBuilder("78006008076").build());
        List<TimeInterval> timeInterval =
            Collections.singletonList(new TimeInterval("03:00:00+03:00/02:59:00+03:00"));
        List<WorkTime> workTimes =
            IntStream.rangeClosed(1, 7).mapToObj(i -> new WorkTime(i, timeInterval)).collect(Collectors.toList());
        return new Warehouse.WarehouseBuilder(new ResourceId.ResourceIdBuilder()
            .setYandexId("32832385")
            .setPartnerId("main")
            .setFulfillmentId("main")
            .build(),
            new Location.LocationBuilder("Россия", "Москва и Московская область", "Котельники")
                .setFederalDistrict("Центральный федеральный округ")
                .setSubRegion("Городской округ Котельники")
                .setStreet("Яничкин проезд")
                .setHouse("7")
                .setBuilding("")
                .setHousing("")
                .setLocationId(21651)
                .build(),
            workTimes,
            "ООО ТЕСТ")
            .setResourceId(new ResourceId.ResourceIdBuilder()
                .setYandexId("32832385")
                .setPartnerId("main")
                .setFulfillmentId("main")
                .build()
            )
            .setContact(createPerson())
            .setPhones(phones)
            .setInstruction("Слева от входа")
            .build();
    }

    private static Courier.CourierBuilder courierBuilderWithCommonFields(Long id) {
        return new Courier.CourierBuilder(
            Collections.singletonList(
                new Person.PersonBuilder("Иван")
                    .setPatronymic("Васильевич")
                    .setSurname("Фулфилментов")
                    .setId(id)
                    .build()))
            .setCar(createCar())
            .setPhone(new Phone.PhoneBuilder("+7 (495) 999-88-77").build());
    }

    /**
     * Данные о курьере.
     */
    public static Courier createCourier(Long id) {
        return courierBuilderWithCommonFields(id).build();
    }

    /**
     * Данные о машине.
     */
    private static Car createCar() {
        return new Car.CarBuilder("А019МР199").setDescription("Моя зеленая тачка").build();
    }

    /**
     * Данные о возврате
     */
    private static ReturnInfo createReturnInfo(ReturnType returnType) {
        return new ReturnInfo.ReturnInfoBuilder(
            createPartnerInfo("partnerToId", "incorporationTo"),
            returnType
        ).setPartnerTransporter(
            createPartnerInfo("partnerTransporterId", "incorporationTransporter")
        ).build();
    }

    /**
     * Данные о партнёре
     */
    private static PartnerInfo createPartnerInfo(String partnerId, String incorporation) {
        return new PartnerInfo.PartnerInfoBuilder(partnerId, incorporation).build();
    }

    /**
     * Данные об услуге.
     */
    private static List<Service> createServices() {
        return Arrays.asList(
            new Service.ServiceBuilder(ServiceType.SORT, false)
                .setName("Название услуги для сортировки")
                .setDescription("Описание услуги для сортировки")
                .build(),
            new Service.ServiceBuilder(ServiceType.STORE_DEFECTIVE_ITEMS_SEPARATELY, true)
                .setName("Название услуги для брака")
                .setDescription("Описание услуги для брака")
                .build()
        );
    }

    private static List<Param> createParams() {
        return Arrays.asList(
            new Param.ParamBuilder("key1").setValue("value1").setComment("comment1").build(),
            new Param.ParamBuilder("key2").setValue("value2").setComment("comment2").build()
        );
    }

    public static Order createOrder() {
        return createOrder(PaymentType.PREPAID, createLocationTo(), null, createRecipient(), null, null, null);
    }

    public static Order createOrder(PaymentType paymentType) {
        return createOrder(paymentType, createLocationTo(), null, createRecipient(), null, null, null);
    }

    /**
     * Данные о заказе.
     */
    public static Order createOrder(
        PaymentType paymentType,
        Location locationTo,
        Courier courier,
        Recipient recipient,
        PersonalRecipient personalRecipient,
        PersonalLocation personalLocationTo,
        PersonalPhysicalPersonSender personalPhysicalPersonSender
    ) {
        return new Order.OrderBuilder(
            new ResourceId.ResourceIdBuilder().setYandexId("5927638").build(),
            locationTo,
            createItems(),
            createKorobyte(),
            BigDecimal.valueOf(1659),
            BigDecimal.valueOf(1908),
            paymentType,
            createDelivery(courier),
            DeliveryType.COURIER,
            BigDecimal.valueOf(249),
            new DocData.DocDataBuilder("test", 1).build(),
            createWarehouse(),
            createWarehouse(),
            recipient,
            BigDecimal.valueOf(0),
            BigDecimal.valueOf(1908)
        )
            .setPartnerLogisticPoint(
                new ResourceId.ResourceIdBuilder()
                    .setYandexId("10000010736")
                    .setPartnerId("172")
                    .build()
            )
            .setExternalId(new ResourceId.ResourceIdBuilder()
                .setYandexId("5927638")
                .setPartnerId("39292337")
                .setFulfillmentId("39292337")
                .build()
            )
            .setLocationFrom(createLocationFrom())
            .setPersonalLocationTo(personalLocationTo)
            .setWeight(BigDecimal.valueOf(3.2))
            .setLength(21)
            .setWidth(45)
            .setHeight(16)
            .setPlaces(createPlaces())
            .setCargoType(CargoType.UNKNOWN)
            .setDeliveryDate(DateTime.fromLocalDateTime(LocalDateTime.of(2019, 4, 3, 0, 0, 0)))
            .setServices(createServices())
            .setWaybill(createParams())
            .setPickupPointCode("pupcode")
            .setTariff("Доставка")
            .setShipmentDate(DateTime.fromLocalDateTime(LocalDateTime.of(2019, 4, 2, 0, 0, 0)))
            .setShipmentDateTime(DateTime.fromLocalDateTime(LocalDateTime.of(2019, 4, 2, 16, 37, 0)))
            .setDeliveryInterval(new TimeInterval("03:00:00+03:00/02:59:00+03:00"))
            .setSender(createSender())
            .setComment("Комментарий")
            .setMaxAbsentItemsPricePercent(new BigDecimal("20"))
            .setTags(Collections.singleton("EXPRESS"))
            .setReturnInfo(createReturnInfo(ReturnType.DROPOFF))
            .setPhysicalPersonSender(createPhysicalPersonSender())
            .setPersonalRecipient(personalRecipient)
            .setPersonalPhysicalPersonSender(personalPhysicalPersonSender)
            .build();
    }

    public static Order createOrderNotTrimmed() {
        Order order = createOrder(
            PaymentType.PREPAID,
            createLocationTo("   Центральный федеральный округ \t "),
            null,
            createRecipient(),
            null,
            null,
            null
        );
        return order;
    }

    public static Order createOrderWithPersonalData() {
        Order order = createOrder(
            PaymentType.PREPAID,
            createLocationTo(),
            null,
            createRecipient(), //TODO DELIVERY-45313 убрать после правок валидации
            createPersonalRecipient(),
            createPersonalLocationTo(),
            createPersonalPhysicalPersonSender()
        );
        return order;
    }

    public static ResourceId createOrderId() {
        return new ResourceId.ResourceIdBuilder().setYandexId("12345").build();
    }

    private static ResourceId createResourceId() {
        return createResourceId("2384813", null);
    }

    public static ResourceId createResourceId(String yandexId, String fulfillmentId) {
        return new ResourceId.ResourceIdBuilder().setYandexId(yandexId).setFulfillmentId(fulfillmentId).build();
    }

    @Nonnull
    public static RegisterUnit.RegisterUnitBuilder createRegisterUnitBuilder(String article) {
        return new RegisterUnit.RegisterUnitBuilder(
            RegisterUnitType.BOX
        )
            .setArticle(article)
            .setExternalId(createResourceId())
            .setAmount(2);
    }

    @Nonnull
    public static TransportationRegister createTransportationRegister() {
        return new TransportationRegister.TransportationRegisterBuilder(
            createResourceId("transportation-register-yandex-1", "transportation-fulfillment-1"),
            RegisterType.YANDEX,
            ImmutableList.of(
                createRegisterUnitBuilder("root-register-unit")
                    .setChildRegisterUnits(ImmutableList.of(
                        createRegisterUnitBuilder("child-register-unit-1").build(),
                        createRegisterUnitBuilder("child-register-unit-2").build()
                    ))
                    .build()
            )
        )
            .build();
    }

    @Nonnull
    private static PhysicalPersonSender createPhysicalPersonSender() {
        return new PhysicalPersonSender.PhysicalPersonSenderBuilder(
            createPerson(),
            new Phone.PhoneBuilder("89876543210").build()
        ).build();
    }
}
