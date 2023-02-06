package ru.yandex.market.delivery.mdbapp.integration.enricher;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import steps.LocationSteps;
import steps.logisticsPointSteps.LogisticPointSteps;
import steps.orderSteps.AddressSteps;
import steps.orderSteps.OrderSteps;
import steps.shopSteps.PaymentStatusSteps;
import steps.shopSteps.ShopSteps;

import ru.yandex.common.util.language.LanguageCode;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelItem;
import ru.yandex.market.checkout.checkouter.delivery.tariff.TariffData;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.VatType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.delivery.mdbapp.components.geo.Location;
import ru.yandex.market.delivery.mdbapp.components.service.marketid.LegalInfoReceiver;
import ru.yandex.market.delivery.mdbapp.components.service.translate.TranslateService;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.DeliveryTariff;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.DeliveryTariffId;
import ru.yandex.market.delivery.mdbapp.components.storage.repository.DeliveryTariffRepository;
import ru.yandex.market.delivery.mdbapp.exception.PaymentMethodException;
import ru.yandex.market.delivery.mdbapp.exception.TariffException;
import ru.yandex.market.delivery.mdbapp.integration.converter.DeliveryTypeConverter;
import ru.yandex.market.delivery.mdbapp.integration.converter.KorobyteConverter;
import ru.yandex.market.delivery.mdbapp.integration.converter.PaymentMethodConverter;
import ru.yandex.market.delivery.mdbapp.integration.converter.PosteRestanteHouseConverter;
import ru.yandex.market.delivery.mdbapp.integration.converter.RecipientConverter;
import ru.yandex.market.delivery.mdbapp.integration.converter.RedOrderItemConverter;
import ru.yandex.market.delivery.mdbapp.integration.converter.SenderConverter;
import ru.yandex.market.delivery.mdbapp.integration.converter.TaxConverter;
import ru.yandex.market.delivery.mdbapp.integration.enricher.fetcher.LocationFetcher;
import ru.yandex.market.delivery.mdbapp.integration.enricher.fetcher.ShopWarehouseFetcher;
import ru.yandex.market.delivery.mdbapp.integration.payload.ExtendedOrder;
import ru.yandex.market.delivery.mdbapp.integration.payload.LogisticsPoint;
import ru.yandex.market.delivery.mdbapp.integration.payload.ReturnInletData;
import ru.yandex.market.id.LegalInfo;
import ru.yandex.market.id.MarketAccount;
import ru.yandex.market.logistic.gateway.common.model.delivery.CargoType;
import ru.yandex.market.logistic.gateway.common.model.delivery.CustomsTranslation;
import ru.yandex.market.logistic.gateway.common.model.delivery.DateTime;
import ru.yandex.market.logistic.gateway.common.model.delivery.Item;
import ru.yandex.market.logistic.gateway.common.model.delivery.Korobyte;
import ru.yandex.market.logistic.gateway.common.model.delivery.Order;
import ru.yandex.market.logistic.gateway.common.model.delivery.Person;
import ru.yandex.market.logistic.gateway.common.model.delivery.PersonalDataStatus;
import ru.yandex.market.logistic.gateway.common.model.delivery.Phone;
import ru.yandex.market.logistic.gateway.common.model.delivery.Recipient;
import ru.yandex.market.logistic.gateway.common.model.delivery.RecipientData;
import ru.yandex.market.logistic.gateway.common.model.delivery.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.delivery.Sender;
import ru.yandex.market.logistic.gateway.common.model.delivery.Service;
import ru.yandex.market.logistic.gateway.common.model.delivery.ServiceType;
import ru.yandex.market.logistic.gateway.common.model.delivery.Supplier;
import ru.yandex.market.logistic.gateway.common.model.delivery.Tax;
import ru.yandex.market.logistic.gateway.common.model.delivery.TaxType;
import ru.yandex.market.logistic.gateway.common.model.delivery.Taxation;
import ru.yandex.market.logistic.gateway.common.model.delivery.TimeInterval;
import ru.yandex.market.logistic.gateway.common.model.delivery.UnitId;
import ru.yandex.market.logistic.gateway.common.model.delivery.VatValue;
import ru.yandex.market.logistic.gateway.common.model.delivery.Warehouse;
import ru.yandex.market.logistic.gateway.common.model.delivery.WorkTime;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.mbi.api.client.entity.outlets.Outlet;
import ru.yandex.market.mbi.api.client.entity.outlets.Point;
import ru.yandex.market.mbi.api.client.entity.outlets.ShopWarehouse;
import ru.yandex.market.mbi.api.client.entity.shops.Shop;
import ru.yandex.market.mbi.api.client.entity.shops.ShopOrgInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.checkout.checkouter.pay.PaymentMethod.CARD_ON_DELIVERY;
import static ru.yandex.market.delivery.mdbapp.integration.enricher.LgwOrderParcelEnricher.TIME_FORMATTER;
import static steps.orderSteps.itemSteps.ItemsSteps.getOrderItem;

public class LgwOrderParcelEnricherTest {

    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    private static final String SERVICE_NAME = "Доставка";

    private static final String SERVICE_CHECK_NAME = "Проверка заказа перед оплатой";

    private static final String WAREHOUSE_CONTACT_NAME = "Антон";
    private static final String WAREHOUSE_CONTACT_SURNAME = "Левин";

    private static final String TARIFF_CODE = "TARIFF_CODE_1";

    private static final long ORDER_ITEM_ID = 1;

    private static final long ORDER_ITEM_HS_CODE = 156;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private OrderParcelToLgwOrderBuilderConverterInterface redOrderParcelConverter;

    @Mock
    private LocationFetcher locationFetcher;

    private SenderConverter senderConverter;

    @Mock
    private ShopWarehouseFetcher shopWarehouseFetcher;

    @Mock
    private DeliveryTariffRepository deliveryTariffRepository;

    @Mock
    private TranslateService translateService;

    private ExtendedOrder extendedOrder;

    private Parcel parcel;

    private TaxConverter taxConverter = new TaxConverter();
    private KorobyteConverter korobyteConverter = new KorobyteConverter();
    private PaymentMethodConverter paymentMethodConverter = new PaymentMethodConverter();
    private DeliveryTypeConverter deliveryTypeConverter = new DeliveryTypeConverter();
    private RecipientConverter recipientConverter = new RecipientConverter();
    private LegalInfoReceiver legalInfoReceiver = mock(LegalInfoReceiver.class);
    private PosteRestanteHouseConverter posteRestanteHouseConverter = new PosteRestanteHouseConverter(Set.of(1005117L));

    @Before
    public void setup() {
        extendedOrder = new ExtendedOrder();
        extendedOrder.setOrder(OrderSteps.getOrderWithParcelBoxes());

        parcel = extendedOrder.getOrder().getDelivery().getParcels().get(0);
        parcel.setWeight(100L);
        parcel.setParcelItems(Collections.singletonList(new ParcelItem(1L, 2)));

        extendedOrder.setOrderData(new ExtendedOrder.OrderData()
            .setLocationTo(getLocationTo())
            .setLocationFrom(getLocationFrom()));

        extendedOrder.setInlet(LogisticPointSteps.getDefaultOutlet());
        extendedOrder.setOutlet(LogisticPointSteps.getDefaultOutlet());
        extendedOrder.setReturnInletData(new ReturnInletData()
            .setInlet(LogisticPointSteps.getDefaultOutlet())
            .setLocation(LocationSteps.getLocation()));

        extendedOrder.getOrder().setPaymentMethod(CARD_ON_DELIVERY);

        Shop shop = ShopSteps.getDefaultShop(
            extendedOrder.getOrder().getShopId(),
            Collections.singletonList(new ShopOrgInfo(
                "zao",
                "1023500000160",
                "some company name",
                "fact address",
                "juri address",
                "partner_interface",
                "registration_number",
                "info_url"
            )),
            PaymentStatusSteps.getPaymentStatus()
        );
        extendedOrder.setShop(shop);
        extendedOrder.getOrder().getDelivery().setShopAddress(AddressSteps.getAddress());

        when(locationFetcher.fetchNullable(any(Shop.class))).thenReturn(getDefaultLocation());
        when(shopWarehouseFetcher.fetch(any(Outlet.class)))
            .thenReturn(new ShopWarehouse(1L, 1L, new Point(null, null, null, null, null, "Full Name"), true, true));

        DeliveryTariff tariff = new DeliveryTariff();
        tariff.setTariffCode(TARIFF_CODE);
        when(deliveryTariffRepository.findById(any(DeliveryTariffId.class))).thenReturn(Optional.of(tariff));

        senderConverter = new SenderConverter(locationFetcher, taxConverter);
        redOrderParcelConverter = new RedOrderParcelConverter(
            translateService,
            new RedOrderItemConverter(taxConverter, korobyteConverter),
            paymentMethodConverter,
            deliveryTypeConverter,
            recipientConverter,
            senderConverter,
            deliveryTariffRepository,
            legalInfoReceiver,
            posteRestanteHouseConverter
        );
    }

    private Location getDefaultLocation() {
        return new Location().setCountry("Russia").setRegion("Moscow").setSubRegion("Moscowskay Oblast")
            .setFederalDistrict("Arbat").setLocality("Москва");
    }

    private Location getLocationFrom() {
        return getDefaultLocation();
    }

    private Location getLocationTo() {
        return getDefaultLocation();
    }

    private ru.yandex.market.logistic.gateway.common.model.delivery.Location getParcelLocationFrom() {
        return new
            ru.yandex.market.logistic.gateway.common.model.delivery.Location.
                LocationBuilder("Russia", "Москва", "Moscow")
            .setLocationId(0)
            .setSubRegion("Moscowskay Oblast")
            .setFederalDistrict("Arbat")
            .setLat(BigDecimal.valueOf(55.755826))
            .setLng(BigDecimal.valueOf(37.6173))
            .setStreet("Льва Толстого")
            .setBuilding("4")
            .setHouse("16/2к3")
            .setHousing("3")
            .setZipCode("117208")
            .build();
    }

    private ru.yandex.market.logistic.gateway.common.model.delivery.Location getParcelLocationFromWithNotAllFields() {
        return new ru.yandex.market.logistic.gateway.common.model.delivery.Location.
            LocationBuilder(
            "Russia", "Москва", "Moscow")
            .setSubRegion("Moscowskay Oblast")
            .setFederalDistrict("Arbat")
            .build();
    }

    private ru.yandex.market.logistic.gateway.common.model.delivery.Location getParcelLocationToPickup() {
        return new ru.yandex.market.logistic.gateway.common.model.delivery.Location.
            LocationBuilder(
            "Russia", "Москва", "Moscow")
            .setLocationId(0)
            .setSubRegion("Moscowskay Oblast")
            .setFederalDistrict("Arbat")
            .setLat(BigDecimal.valueOf(55.755826))
            .setLng(BigDecimal.valueOf(37.6173))
            .setStreet("Льва Толстого")
            .setHouse("16/2к3")
            .setHousing("3")
            .setBuilding("4")
            .setZipCode("117208")
            .build();
    }

    private ru.yandex.market.logistic.gateway.common.model.delivery.Location getParcelLocationToPickupInRussian() {
        return new ru.yandex.market.logistic.gateway.common.model.delivery.Location.
            LocationBuilder(
            "Россия", "Москва", "Москва и Московская область")
            .setLocationId(213)
            .setSubRegion("Москва")
            .setFederalDistrict("Центральный федеральный округ")
            .setLat(BigDecimal.valueOf(55.75395965576172))
            .setLng(BigDecimal.valueOf(37.620391845703125))
            .setStreet("Льва Толстого")
            .setHouse("16/2к3")
            .setHousing("3")
            .setBuilding("4")
            .setZipCode("117208")
            .build();
    }

    private ru.yandex.market.logistic.gateway.common.model.delivery.Location getParcelLocationToNotPickup() {
        return new ru.yandex.market.logistic.gateway.common.model.delivery.Location.
            LocationBuilder(
            "Russia", "Москва", "Moscow")
            .setLocationId(0)
            .setSubRegion("Moscowskay Oblast")
            .setFederalDistrict("Arbat")
            .setLat(BigDecimal.valueOf(76.589))
            .setLng(BigDecimal.valueOf(32.416))
            .setStreet("Льва Толстого")
            .setHouse("15в/3")
            .setHousing("422")
            .setRoom("22")
            .setZipCode("630090")
            .setFloor(4)
            .setPorch("6")
            .setMetro("Парк Культуры")
            .setIntercom("+71234567809")
            .build();
    }

    private ru.yandex.market.logistic.gateway.common.model.delivery.Location getOutletLocation() {
        return new ru.yandex.market.logistic.gateway.common.model.delivery.Location.LocationBuilder(
            "Россия",
            "Москва",
            "Москва и Московская область"
        )
            .setLocationId(213)
            .setSubRegion("Москва")
            .setFederalDistrict("Центральный федеральный округ")
            .setLat(BigDecimal.valueOf(55.755826))
            .setLng(BigDecimal.valueOf(37.6173))
            .setStreet("Льва Толстого")
            .setHouse("16/2к3")
            .setHousing("3")
            .setBuilding("4")
            .setZipCode("117208")
            .build();
    }

    private Item getItem() {
        return new Item.ItemBuilder("offer name", 2, BigDecimal.valueOf(123.0))
            .setArticle("123123")
            .setKorobyte(new Korobyte.KorobyteBuilder().build())
            .setNameEnglish("english offer name")
            .setItemDescriptionEnglish("english offer description")
            .setCargoType(CargoType.FOOD)
            .setCargoTypes(Collections.singletonList(CargoType.FOOD))
            .setHsCode(String.valueOf(ORDER_ITEM_HS_CODE))
            .setCategoryName("offer category name")
            .setUnitId(new UnitId.UnitIdBuilder(1L, "123").build())
            .build();
    }

    private Recipient getRecipient() {
        return new Recipient.RecipientBuilder(
            new Person("RecipientFirstName", "RecipientLastName", "RecipientMiddleName"),
            Collections.singletonList(new Phone("71234567891", null))
        )
            .setEmail("test-recipient@test.com")
            .setRecipientData(new RecipientData(new ResourceId.ResourceIdBuilder().setYandexId("123").build()))
            .setPersonalDataStatus(PersonalDataStatus.NO_DATA)
            .build();
    }

    private Sender.SenderBuilder getSenderWithType(String incorporation) {
        return new Sender.SenderBuilder(
            incorporation,
            "1023500000160"
        )
            .setId(ResourceId.builder().setYandexId("10210930").build())
            .setName("name")
            .setType("zao")
            .setAddress(getParcelLocationFromWithNotAllFields())
            .setPhones(Collections.singletonList(new Phone("+71234567890", null)))
            .setTaxation(Taxation.ENVD);
    }

    private Service buildDeliveryService() {
        return new Service.ServiceBuilder(false)
            .setTaxes(Collections.singletonList(new Tax(TaxType.VAT, VatValue.EIGHTEEN)))
            .setName(SERVICE_NAME)
            .setCode(ServiceType.DELIVERY)
            .build();
    }

    private Service buildCheckService() {
        return new Service.ServiceBuilder(false)
            .setName(SERVICE_CHECK_NAME)
            .setCode(ServiceType.CHECK)
            .build();
    }

    private Warehouse getWarehouse() {
        return new Warehouse.WarehouseBuilder(
            ResourceId.builder().setYandexId("1").setPartnerId("630060").build(),
            getOutletLocation(),
            getScheduleWorkTime()
        )
            .setContact(new Person(WAREHOUSE_CONTACT_NAME, WAREHOUSE_CONTACT_SURNAME, null))
            .setPhones(Collections.singletonList(new Phone("4567890", "123")))
            .build();
    }

    private List<WorkTime> getScheduleWorkTime() {
        return Arrays.asList(
            new WorkTime(1, Collections.singletonList(new TimeInterval("00:08+03:00/02:08+03:00"))),
            new WorkTime(2, Arrays.asList(
                new TimeInterval("00:08+03:00/02:08+03:00"),
                new TimeInterval("01:08+03:00/03:08+03:00")
            )),
            new WorkTime(3, Arrays.asList(
                new TimeInterval("00:08+03:00/02:08+03:00"),
                new TimeInterval("01:08+03:00/03:08+03:00")
            )),
            new WorkTime(4, Arrays.asList(
                new TimeInterval("00:08+03:00/02:08+03:00"),
                new TimeInterval("01:08+03:00/03:08+03:00")
            )),
            new WorkTime(5, Arrays.asList(
                new TimeInterval("00:08+03:00/02:08+03:00"),
                new TimeInterval("01:08+03:00/03:08+03:00")
            )),
            new WorkTime(6, Arrays.asList(
                new TimeInterval("00:08+03:00/02:08+03:00"),
                new TimeInterval("01:08+03:00/03:08+03:00")
            )),
            new WorkTime(7, Arrays.asList(
                new TimeInterval("00:08+03:00/02:08+03:00"),
                new TimeInterval("01:08+03:00/03:08+03:00")
            )),
            new WorkTime(8, Arrays.asList(
                new TimeInterval("00:08+03:00/02:08+03:00"),
                new TimeInterval("01:08+03:00/03:08+03:00")
            ))
        );
    }

    @Test
    public void parcelEnricherTestLocationToDelivery() {
        extendedOrder.getOrder().getDelivery().setType(DeliveryType.DELIVERY);
        Order order = redOrderParcelConverter.convert(extendedOrder, parcel).build();

        assertThat(order.getLocationTo())
            .as("Location to")
            .isEqualToComparingFieldByFieldRecursively(getParcelLocationToNotPickup());
    }

    @Test
    public void parcelEnricherTestLocationToPost() {
        extendedOrder.getOrder().getDelivery().setType(DeliveryType.POST);
        Order order = redOrderParcelConverter.convert(extendedOrder, parcel).build();

        assertThat(order.getLocationTo())
            .as("Location to")
            .isEqualToComparingFieldByFieldRecursively(getParcelLocationToNotPickup());
    }

    @Test
    public void parcelEnricherTestLocationToPickup() {
        extendedOrder.getOrder().getDelivery().setType(DeliveryType.PICKUP);
        Order order = redOrderParcelConverter.convert(extendedOrder, parcel).build();

        assertThat(order.getLocationTo())
            .as("Location to")
            .isEqualToComparingFieldByFieldRecursively(getParcelLocationToPickup());
    }

    @Test
    public void parcelEnricherTestLocationFrom() {
        Order order = redOrderParcelConverter.convert(extendedOrder, parcel).build();

        assertThat(order.getLocationFrom())
            .as("Location from")
            .isEqualToComparingFieldByFieldRecursively(getParcelLocationFrom());
    }

    @Test
    public void parcelEnricherTestAmountPrepaid() {
        OrderItem orderItem = getOrderItem(1L);
        orderItem.setPrice(BigDecimal.valueOf(123.45));
        extendedOrder.getOrder().setItems(Collections.singletonList(orderItem));
        parcel.setParcelItems(Collections.singletonList(new ParcelItem(1L, 1)));

        extendedOrder.getOrder().setPaymentType(PaymentType.PREPAID);
        Order order = redOrderParcelConverter.convert(extendedOrder, parcel).build();

        assertThat(order.getAmountPrepaid()).as("Amount prepaid").isEqualTo(BigDecimal.valueOf(123.45));
    }

    @Test
    public void parcelEnricherTestAmountPrepaidPostPaid() {
        extendedOrder.getOrder().setPaymentType(PaymentType.POSTPAID);
        Order order = redOrderParcelConverter.convert(extendedOrder, parcel).build();

        assertThat(order.getAmountPrepaid()).as("Amount prepaid").isEqualTo(BigDecimal.ZERO);
    }

    @Test
    public void parcelEnricherTestCargoCost() {
        Order order = redOrderParcelConverter.convert(extendedOrder, parcel).build();

        assertThat(order.getCargoCost())
            .as("Cargo cost")
            .isEqualTo(BigDecimal.valueOf(123.0 * 2));
    }

    @Test
    public void parcelEnricherTestShipmentPointCode() {
        String shipmentPointCode = "AB45126YOURPARCEL195";
        LogisticsPoint inlet = extendedOrder.getInlet();
        LogisticsPoint newInlet = new LogisticsPoint(
            inlet.getId(),
            shipmentPointCode,
            inlet.getAddress(),
            inlet.getGeoInfo(),
            inlet.getPhoneNumbers(),
            inlet.getScheduleLines()
        );
        extendedOrder.setInlet(newInlet);
        Order order = redOrderParcelConverter.convert(extendedOrder, parcel).build();

        assertThat(order.getShipmentPointCode())
            .as("Shipment point code")
            .isEqualTo(shipmentPointCode);
    }

    @Test
    public void parcelEnricherTestDeliveryDate() {
        Date deliveryDate = new Date();
        DeliveryDates deliveryDates = new DeliveryDates();
        deliveryDates.setFromDate(deliveryDate);
        extendedOrder.getOrder().getDelivery().setDeliveryDates(deliveryDates);
        Order order = redOrderParcelConverter.convert(extendedOrder, parcel).build();

        assertThat(order.getDeliveryDate())
            .as("Delivery date")
            .isEqualTo(new DateTime(SIMPLE_DATE_FORMAT.format(deliveryDate)));
    }

    @Test
    public void parcelEnricherTestSupplierInn() {
        when(legalInfoReceiver.findAccountByPartnerIdAndPartnerType(1L, PartnerType.SUPPLIER.name()))
            .thenReturn(Optional.of(MarketAccount.newBuilder().setLegalInfo(
                LegalInfo.newBuilder().setInn("1231231234").build()
            ).build()));
        Order order = redOrderParcelConverter.convert(extendedOrder, parcel).build();

        assertThat(order.getItems())
            .as("Supplier INN")
            .extracting(Item::getSupplier)
            .extracting(Supplier::getInn)
            .containsExactly("1231231234");
    }

    @Test
    public void parcelEnricherTestDeliveryInterval() {
        DeliveryDates deliveryDates = new DeliveryDates();
        deliveryDates.setFromTime("13:31");
        deliveryDates.setToTime("17:45");
        extendedOrder.getOrder().getDelivery().setDeliveryDates(deliveryDates);
        Order order = redOrderParcelConverter.convert(extendedOrder, parcel).build();

        ZoneOffset offset = ZoneId.systemDefault().getRules().getOffset(Instant.now());

        //noinspection ConstantConditions
        assertThat(order.getDeliveryInterval())
            .as("Delivery interval")
            .isEqualTo(new TimeInterval(String.format(
                "%s/%s",
                deliveryDates.getFromTime().atOffset(offset).format(TIME_FORMATTER),
                deliveryDates.getToTime().atOffset(offset).format(TIME_FORMATTER)
            )));
    }

    @Test
    public void parcelEnricherTestCargoType() {
        Order order = redOrderParcelConverter.convert(extendedOrder, parcel).build();

        assertThat(order.getCargoType())
            .as("Cargo type")
            .isEqualTo(CargoType.UNKNOWN);
    }

    @Test
    public void parcelEnricherTestTariff() {
        TariffData tariffData = new TariffData();
        tariffData.setTariffCode(TARIFF_CODE);
        extendedOrder.getOrder().getDelivery().setTariffData(tariffData);
        Order order = redOrderParcelConverter.convert(extendedOrder, parcel).build();

        assertThat(order.getTariff())
            .as("Tariff")
            .isEqualTo(TARIFF_CODE);
    }

    @Test
    public void parcelEnricherTestTariffCodeIsNull() {
        TariffData tariffData = new TariffData();
        extendedOrder.getOrder().setRgb(Color.RED);
        extendedOrder.getOrder().getDelivery().setTariffData(tariffData);
        Order order = redOrderParcelConverter.convert(extendedOrder, parcel).build();

        assertThat(order.getTariff())
            .as("Tariff")
            .isEqualTo(TARIFF_CODE);
    }

    @Test
    public void parcelEnricherTestTariffDataIsNull() {
        extendedOrder.getOrder().setRgb(Color.RED);
        Order order = redOrderParcelConverter.convert(extendedOrder, parcel).build();

        assertThat(order.getTariff())
            .as("Tariff")
            .isEqualTo(TARIFF_CODE);
    }

    @Test(expected = TariffException.class)
    public void parcelEnricherTestTariffNotFound() {
        extendedOrder.getOrder().setRgb(Color.RED);
        when(deliveryTariffRepository.findById(any(DeliveryTariffId.class))).thenReturn(Optional.empty());
        redOrderParcelConverter.convert(extendedOrder, parcel).build();
    }

    @Test
    public void parcelEnricherTestKorobyte() {
        parcel.setWidth(100L);
        parcel.setHeight(200L);
        parcel.setDepth(300L);
        parcel.setWeight(400L);
        Order order = redOrderParcelConverter.convert(extendedOrder, parcel).build();

        assertThat(order.getKorobyte())
            .as("Korobyte")
            .isEqualToComparingFieldByFieldRecursively(
                new Korobyte(100, 200, 300, new BigDecimal("0.400"), null, null)
            );
    }

    @Test
    public void parcelEnricherTestPaymentMethod() {
        extendedOrder.getOrder().setPaymentMethod(PaymentMethod.CARD_ON_DELIVERY);
        Order order = redOrderParcelConverter.convert(extendedOrder, parcel).build();

        assertThat(order.getPaymentMethod())
            .as("Payment method")
            .isEqualTo(ru.yandex.market.logistic.gateway.common.model.delivery.PaymentMethod.CARD);
    }

    @Test(expected = PaymentMethodException.class)
    public void parcelEnricherTestPaymentMethodInvalid() {
        extendedOrder.getOrder().setPaymentMethod(PaymentMethod.UNKNOWN);
        redOrderParcelConverter.convert(extendedOrder, parcel);
    }

    @Test
    public void parcelEnricherTestDeliveryCost() {
        BigDecimal deliveryCost = BigDecimal.valueOf(146.0);
        extendedOrder.getOrder().getDelivery().setPrice(deliveryCost);
        Order order = redOrderParcelConverter.convert(extendedOrder, parcel).build();

        assertThat(order.getDeliveryCost())
            .as("Delivery cost")
            .isEqualTo(deliveryCost);
    }

    @Test
    public void parcelEnricherTestAssessedCost() {
        Order order = redOrderParcelConverter.convert(extendedOrder, parcel).build();

        assertThat(order.getAssessedCost())
            .as("Delivery cost")
            .isEqualByComparingTo(BigDecimal.valueOf(123.0 * 2));
    }

    @Test
    public void parcelEnricherTestTotalSingleParcelSingleItem() {
        OrderItem orderItem = getOrderItem(1L);
        orderItem.setPrice(BigDecimal.valueOf(123.45));
        extendedOrder.getOrder().setItems(Collections.singletonList(orderItem));
        parcel.setParcelItems(Collections.singletonList(new ParcelItem(1L, 1)));

        Order orderRed = redOrderParcelConverter.convert(extendedOrder, parcel).build();

        assertThat(orderRed.getTotal()).as("Parcel total").isEqualTo(BigDecimal.valueOf(123.45));
    }

    @Test
    public void parcelEnricherTestTotalSingleParcelMultiItem() {
        OrderItem orderItem1 = getOrderItem(1L);
        orderItem1.setFeedOfferId(new FeedOfferId("", 1L));
        OrderItem orderItem2 = getOrderItem(2L);
        orderItem2.setFeedOfferId(new FeedOfferId("", 2L));
        orderItem1.setPrice(BigDecimal.valueOf(123.45));
        orderItem2.setPrice(BigDecimal.valueOf(67.78));
        extendedOrder.getOrder().setItems(Arrays.asList(orderItem1, orderItem2));
        parcel.setParcelItems(Arrays.asList(new ParcelItem(1L, 1), new ParcelItem(2L, 2)));

        Order order = redOrderParcelConverter.convert(extendedOrder, parcel).build();

        assertThat(order.getTotal()).as("Parcel total")
            .isEqualTo(BigDecimal.valueOf(123.45 + 67.78 * 2)); //items without delivery
    }

    @Test
    public void parcelEnricherTestDeliveryType() {
        extendedOrder.getOrder().getDelivery().setType(DeliveryType.DELIVERY);
        Order order = redOrderParcelConverter.convert(extendedOrder, parcel).build();

        assertThat(order.getDeliveryType())
            .as("Delivery type")
            .isEqualTo(ru.yandex.market.logistic.gateway.common.model.delivery.DeliveryType.COURIER);
    }

    @Test
    public void parcelEnricherTestItems() {
        OrderItem orderItem = extendedOrder.getOrder().getItem(ORDER_ITEM_ID);
        orderItem.setEnglishName("english offer name");
        orderItem.setItemDescriptionEnglish("english offer description");
        orderItem.setCargoTypes(ImmutableSet.of(750));
        orderItem.setCategoryFullName("offer category name");
        orderItem.setHsCode(ORDER_ITEM_HS_CODE);

        orderItem.setSupplierId(1L);
        orderItem.setShopSku("123");

        Order order = redOrderParcelConverter.convert(extendedOrder, parcel).build();

        assertThat(order.getItems())
            .as("Items")
            .isEqualTo(Collections.singletonList(getItem()));
    }

    @Test
    public void parcelEnricherTestRecipient() {
        Order order = redOrderParcelConverter.convert(extendedOrder, parcel).build();

        assertThat(order.getRecipient())
            .as("Items")
            .isEqualToComparingFieldByFieldRecursively(getRecipient());
    }

    @Test
    public void parcelEnricherTestSenderWithType() {
        Order order = redOrderParcelConverter.convert(extendedOrder, parcel).build();

        assertThat(order.getSender())
            .as("Sender")
            .isEqualToComparingFieldByFieldRecursively(getSenderWithType("ЗАО some company name").build());
    }

    @Test
    public void parcelEnricherTestSenderWithoutType() {
        Shop shop = ShopSteps.getDefaultShop(
            extendedOrder.getOrder().getShopId(),
            Collections.singletonList(new ShopOrgInfo(
                "",
                "1023500000160",
                "some company name",
                "fact address",
                "juri address",
                "ya_money",
                "registration_number",
                "info_url"
            )),
            PaymentStatusSteps.getPaymentStatus()
        );
        extendedOrder.setShop(shop);

        Order order = redOrderParcelConverter.convert(extendedOrder, parcel).build();

        Sender sender = getSenderWithType("some company name")
            .setType("")
            .build();

        assertThat(order.getSender())
            .as("Sender")
            .isEqualToComparingFieldByFieldRecursively(sender);
    }

    @Test
    public void parcelEnricherTestShipmentDate() {
        Order order = redOrderParcelConverter.convert(extendedOrder, parcel).build();

        assertThat(order.getShipmentDate())
            .as("Shipment date")
            .isEqualTo(new DateTime("2016-03-21T00:00:00+00:00"));
    }

    @Test
    public void parcelEnricherTestServices() {
        extendedOrder.getOrder().getDelivery().setVat(VatType.VAT_18);
        Order redOrder = redOrderParcelConverter.convert(extendedOrder, parcel).build();

        assertThat(redOrder.getServices()).as("Services for red order").isNull();
    }

    @Test
    public void parcelEnricherTestWarehouse() {
        Order order = redOrderParcelConverter.convert(extendedOrder, parcel).build();

        assertThat(order.getWarehouse())
            .as("Warehouse")
            .isEqualToComparingFieldByFieldRecursively(getWarehouse());
    }

    @Test
    public void parcelEnricherTestCustomsTranslation() {
        Mockito.when(translateService.translate("offer name", LanguageCode.RU.getValue()))
            .thenReturn(Optional.of("Оффер нейм"));
        Mockito.when(translateService.translate("category full name", LanguageCode.RU.getValue()))
            .thenReturn(Optional.of("Категори фулл нейм"));

        extendedOrder.getOrder().getItems().forEach(oi -> oi.setCategoryFullName("category full name"));

        TariffData tariffData = new TariffData();
        tariffData.setNeedTranslationForCustom(true);
        tariffData.setCustomsLanguage(LanguageCode.RU);

        extendedOrder.getOrder().getDelivery().setTariffData(tariffData);

        Order order = redOrderParcelConverter.convert(extendedOrder, parcel).build();

        assertThat(order.getItems().get(0).getTransitData())
            .as("TransitData")
            .isNotNull();

        assertThat(order.getItems().get(0).getTransitData().getCustomsTranslations())
            .as("CustomTranlsations")
            .hasSize(1);

        CustomsTranslation customsTranslation =
            order.getItems().get(0).getTransitData().getCustomsTranslations().get(0);
        assertThat(customsTranslation.getLanguageCode())
            .as("LanguageCode")
            .isEqualTo(LanguageCode.RU.getValue());
        assertThat(customsTranslation.getCategoryName())
            .as("CategoryName")
            .isEqualTo("Категори фулл нейм");
        assertThat(customsTranslation.getName())
            .as("Name")
            .isEqualTo("Оффер нейм");
    }

    @Test
    public void parcelEnricherTestCustomsTranslationSeveralLanguages() {
        Mockito.when(translateService.translate("offer name", LanguageCode.RU.getValue()))
            .thenReturn(Optional.of("Оффер нейм"));
        Mockito.when(translateService.translate("category full name", LanguageCode.RU.getValue()))
            .thenReturn(Optional.of("Категори фулл нейм"));

        Mockito.when(translateService.translate("offer name", LanguageCode.ZH.getValue()))
            .thenReturn(Optional.of("优惠名称"));
        Mockito.when(translateService.translate("category full name", LanguageCode.ZH.getValue()))
            .thenReturn(Optional.of("类别全名"));

        extendedOrder.getOrder().getItems().forEach(oi -> oi.setCategoryFullName("category full name"));

        TariffData tariffData = new TariffData();
        tariffData.setNeedTranslationForCustom(true);
        tariffData.setCustomsLanguages(EnumSet.of(LanguageCode.RU, LanguageCode.ZH));

        extendedOrder.getOrder().getDelivery().setTariffData(tariffData);

        Order order = redOrderParcelConverter.convert(extendedOrder, parcel).build();

        assertThat(order.getItems().get(0).getTransitData())
            .as("TransitData")
            .isNotNull();
        assertThat(order.getItems().get(0).getTransitData().getCustomsTranslations())
            .as("CustomTranlsations")
            .hasSize(2);

        CustomsTranslation chineseCustomsTranslation =
            order.getItems().get(0).getTransitData().getCustomsTranslations().get(0);
        assertThat(chineseCustomsTranslation.getLanguageCode())
            .as("LanguageCode")
            .isEqualTo(LanguageCode.ZH.getValue());
        assertThat(chineseCustomsTranslation.getCategoryName())
            .as("CategoryName")
            .isEqualTo("类别全名");
        assertThat(chineseCustomsTranslation.getName())
            .as("Name")
            .isEqualTo("优惠名称");

        CustomsTranslation russianCustomsTranslation =
            order.getItems().get(0).getTransitData().getCustomsTranslations().get(1);
        assertThat(russianCustomsTranslation.getLanguageCode())
            .as("LanguageCode")
            .isEqualTo(LanguageCode.RU.getValue());
        assertThat(russianCustomsTranslation.getCategoryName())
            .as("CategoryName")
            .isEqualTo("Категори фулл нейм");
        assertThat(russianCustomsTranslation.getName())
            .as("Name")
            .isEqualTo("Оффер нейм");
    }
}
