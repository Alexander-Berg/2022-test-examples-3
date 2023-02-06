package ru.yandex.market.delivery.mdbapp.integration.enricher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.runners.MockitoJUnitRunner;

import ru.yandex.common.util.language.LanguageCode;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelItem;
import ru.yandex.market.checkout.checkouter.delivery.tariff.TariffData;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.delivery.mdbapp.components.service.marketid.LegalInfoReceiver;
import ru.yandex.market.delivery.mdbapp.components.service.translate.TranslateService;
import ru.yandex.market.delivery.mdbapp.components.storage.repository.DeliveryTariffRepository;
import ru.yandex.market.delivery.mdbapp.exception.PaymentMethodException;
import ru.yandex.market.delivery.mdbapp.integration.converter.DeliveryTypeConverter;
import ru.yandex.market.delivery.mdbapp.integration.converter.KorobyteConverter;
import ru.yandex.market.delivery.mdbapp.integration.converter.PaymentMethodConverter;
import ru.yandex.market.delivery.mdbapp.integration.converter.PosteRestanteHouseConverter;
import ru.yandex.market.delivery.mdbapp.integration.converter.RecipientConverter;
import ru.yandex.market.delivery.mdbapp.integration.converter.RedOrderItemConverter;
import ru.yandex.market.delivery.mdbapp.integration.converter.SenderConverter;
import ru.yandex.market.delivery.mdbapp.integration.converter.TaxConverter;
import ru.yandex.market.delivery.mdbapp.integration.payload.CreateLgwDsOrder;
import ru.yandex.market.delivery.mdbapp.integration.payload.ExtendedOrder;
import ru.yandex.market.delivery.mdbapp.integration.payload.ExtendedParcelOrder;
import ru.yandex.market.delivery.mdbapp.integration.payload.ReturnInletData;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.delivery.CargoType;
import ru.yandex.market.logistic.gateway.common.model.delivery.CustomsTranslation;
import ru.yandex.market.logistic.gateway.common.model.delivery.DateTime;
import ru.yandex.market.logistic.gateway.common.model.delivery.DeliveryType;
import ru.yandex.market.logistic.gateway.common.model.delivery.Item;
import ru.yandex.market.logistic.gateway.common.model.delivery.Korobyte;
import ru.yandex.market.logistic.gateway.common.model.delivery.Location;
import ru.yandex.market.logistic.gateway.common.model.delivery.PaymentMethod;
import ru.yandex.market.logistic.gateway.common.model.delivery.Person;
import ru.yandex.market.logistic.gateway.common.model.delivery.PersonalDataStatus;
import ru.yandex.market.logistic.gateway.common.model.delivery.Phone;
import ru.yandex.market.logistic.gateway.common.model.delivery.Recipient;
import ru.yandex.market.logistic.gateway.common.model.delivery.RecipientData;
import ru.yandex.market.logistic.gateway.common.model.delivery.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.delivery.Sender;
import ru.yandex.market.logistic.gateway.common.model.delivery.TimeInterval;
import ru.yandex.market.logistic.gateway.common.model.delivery.TransitData;
import ru.yandex.market.logistic.gateway.common.model.delivery.UnitId;
import ru.yandex.market.logistic.gateway.common.model.delivery.Warehouse;
import ru.yandex.market.logistic.gateway.common.model.delivery.WorkTime;
import ru.yandex.market.mbi.api.client.entity.shops.Shop;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.checkout.checkouter.pay.PaymentMethod.CARD_ON_DELIVERY;
import static ru.yandex.market.checkout.checkouter.pay.PaymentMethod.UNKNOWN;
import static steps.logisticsPointSteps.LogisticPointSteps.getDefaultOutlet;
import static steps.orderSteps.OrderSteps.getFilledOrder;
import static steps.shopSteps.ShopSteps.getDefaultShop;

@RunWith(MockitoJUnitRunner.class)
public class CrossDockOrderEnricherTest {

    private static final String TARIFF_CODE = "NRM";

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private SenderConverter senderConverter;

    @InjectMocks
    private CrossDockOrderEnricher enricher;

    @Mock
    private DeliveryTariffRepository deliveryTariffRepository;

    @Mock
    private TranslateService translateService;

    private TaxConverter taxConverter = new TaxConverter();
    private KorobyteConverter korobyteConverter = new KorobyteConverter();
    private PaymentMethodConverter paymentMethodConverter = new PaymentMethodConverter();
    private DeliveryTypeConverter deliveryTypeConverter = new DeliveryTypeConverter();
    private RecipientConverter recipientConverter = new RecipientConverter();
    private PosteRestanteHouseConverter posteRestanteHouseConverter = new PosteRestanteHouseConverter(Set.of(1005117L));

    @Before
    public void setup() {
        when(senderConverter.convert(any(Shop.class), any(Order.class))).thenReturn(getSender());
        when(translateService.translate(any(String.class), any(String.class)))
            .thenAnswer(invocation -> Optional.of("translated " + invocation.getArgument(0)));

        RedOrderParcelConverter lgwOrderParcelEnricher = new RedOrderParcelConverter(
            translateService,
            new RedOrderItemConverter(taxConverter, korobyteConverter),
            paymentMethodConverter,
            deliveryTypeConverter,
            recipientConverter,
            senderConverter,
            deliveryTariffRepository,
            mock(LegalInfoReceiver.class),
            posteRestanteHouseConverter
        );
        enricher = new CrossDockOrderEnricher(lgwOrderParcelEnricher);
    }

    @Test
    public void orderToDsmOrderParcelEnrich() {
        ExtendedParcelOrder extendedOrderSingleParcel = getExtendedOrderSingleParcel();
        extendedOrderSingleParcel.getExtendedOrder().getOrder().setRgb(Color.RED);
        assertThat(enricher.enrich(extendedOrderSingleParcel))
            .as("Got correct compoundOrder after enrichment")
            .isEqualToComparingFieldByFieldRecursively(getRedOrderRequest());
    }

    @Test(expected = PaymentMethodException.class)
    public void compoundOrderEnricherWrongPaymentType() {
        enricher.enrich(getExtendedOrderWrongPaymentType());
    }

    private CreateLgwDsOrder getRedOrderRequest() {

        Location locationTo = new Location.LocationBuilder("Russia", "Москва", "Москва и Московская область")
            .setSubRegion("Moscow")
            .setStreet("Льва Толстого")
            .setHouse("15в/3")
            .setHousing("422")
            .setRoom("22")
            .setZipCode("630090")
            .setPorch("6")
            .setFloor(4)
            .setMetro("Парк Культуры")
            .setLocationId(1)
            .setLat(BigDecimal.valueOf(76.589))
            .setLng(BigDecimal.valueOf(32.416))
            .setIntercom("+71234567809")
            .build();

        Location locationFrom = new Location.LocationBuilder("Russia", "Москва", "Москва и Московская область")
            .setLocationId(1)
            .setSubRegion("Moscow")
            .setStreet("Льва Толстого")
            .setBuilding("4")
            .setHouse("16/2к3")
            .setHousing("3")
            .setZipCode("117208")
            .setLat(BigDecimal.valueOf(55.755826))
            .setLng(BigDecimal.valueOf(37.6173))
            .build();

        ResourceId resourceOrderId = new ResourceId.ResourceIdBuilder().setYandexId("12").build();

        Korobyte koroByte = new Korobyte.KorobyteBuilder()
            .setWidth(1)
            .setHeight(1)
            .setLength(1)
            .setWeightGross(BigDecimal.valueOf(0.001))
            .build();

        Item item = new Item.ItemBuilder(
            "offer name",
            2,
            BigDecimal.valueOf(123.0)
        )
            .setArticle("123123")
            .setCargoType(CargoType.UNKNOWN)
            .setCargoTypes(Collections.singletonList(CargoType.UNKNOWN))
            .setKorobyte(new Korobyte.KorobyteBuilder().build())
            .setUnitId(new UnitId.UnitIdBuilder(1L, "123").build())
            .setTransitData(new TransitData(Collections.singletonList(new CustomsTranslation(
                LanguageCode.EN.getValue(), "translated offer name", "translated category full name"
            ))))
            .setCategoryName("category full name")
            .build();

        Person fio = new Person.PersonBuilder("RecipientFirstName", "RecipientLastName")
            .setPatronymic("RecipientMiddleName")
            .build();
        Recipient recipient = new Recipient.RecipientBuilder(
            fio,
            Collections.singletonList(new Phone.PhoneBuilder("71234567891").build()))
            .setEmail("test-recipient@test.com")
            .setRecipientData(new RecipientData(new ResourceId.ResourceIdBuilder().setYandexId("12").build()))
            .setPersonalDataStatus(PersonalDataStatus.NO_DATA)
            .build();

        ru.yandex.market.logistic.gateway.common.model.delivery.Order.OrderBuilder orderBuilder =
            new ru.yandex.market.logistic.gateway.common.model.delivery.Order.OrderBuilder(
                resourceOrderId,
                locationTo,
                locationFrom,
                koroByte,
                Collections.singletonList(item),
                TARIFF_CODE,
                new BigDecimal("246.00"),
                PaymentMethod.CARD,
                DeliveryType.COURIER,
                new BigDecimal("123"),
                recipient,
                BigDecimal.valueOf(246.0),
                getSender()
            );

        orderBuilder.setShipmentDate(new DateTime("2016-03-21T00:00:00+00:00"));
        orderBuilder.setAmountPrepaid(BigDecimal.ZERO);
        orderBuilder.setShipmentPointCode("630060");
        orderBuilder.setParcelId(new ResourceId.ResourceIdBuilder().setYandexId("32").build());
        orderBuilder.setComment("notes");
        orderBuilder.setShipmentPointCode("630060");
        orderBuilder.setWarehouse(getWarehouse(locationFrom));
        orderBuilder.setServices(null);
        orderBuilder.setPickupPointCode("630060");
        orderBuilder.setCargoType(CargoType.UNKNOWN);
        orderBuilder.setCargoCost(BigDecimal.valueOf(246.0));

        return new CreateLgwDsOrder(orderBuilder.build(), new Partner(987L));
    }

    private ExtendedParcelOrder getExtendedOrderSingleParcel() {
        TariffData tariffData = new TariffData();
        tariffData.setCustomsLanguage(LanguageCode.EN);
        tariffData.setNeedTranslationForCustom(true);
        tariffData.setTariffCode(TARIFF_CODE);

        Order order = getFilledOrder();
        order.setId(12L);
        order.setPaymentMethod(CARD_ON_DELIVERY);
        order.getDelivery().setTariffData(tariffData);
        order.getItems().forEach(oi -> oi.setCategoryFullName("category full name"));

        ExtendedOrder extendedOrder = new ExtendedOrder();
        extendedOrder.setOrder(order);
        extendedOrder.setOrderData(getOrderData());
        extendedOrder.setShop(getDefaultShop());
        extendedOrder.setOutlet(getDefaultOutlet());
        extendedOrder.setInlet(getDefaultOutlet());

        ReturnInletData returnInletData = new ReturnInletData();
        returnInletData.setLocation(getLocation());
        returnInletData.setInlet(getDefaultOutlet());
        extendedOrder.setReturnInletData(returnInletData);

        order.getItems().forEach(item -> {
            item.setShopSku("123");
            item.setSupplierId(1L);
        });

        order.getDelivery().getParcels().get(0).setId(32L);
        order.getDelivery().getParcels().get(0).setParcelItems(
            Collections.singletonList(new ParcelItem(1L, 2)));
        order.getDelivery().getParcels().get(0).setShipmentDate(LocalDate.parse("2016-03-21"));

        return new ExtendedParcelOrder(order.getDelivery().getParcels().get(0), extendedOrder);
    }

    private ExtendedParcelOrder getExtendedOrderWrongPaymentType() {
        ExtendedParcelOrder extendedParcelOrder = getExtendedOrderSingleParcel();

        extendedParcelOrder.getExtendedOrder().getOrder().setPaymentMethod(UNKNOWN);

        return extendedParcelOrder;
    }

    private Sender getSender() {
        ResourceId resourceId = new ResourceId.ResourceIdBuilder()
            .setYandexId("1")
            .setDeliveryId("2")
            .build();

        return new Sender.SenderBuilder("", "Org")
            .setId(resourceId)
            .build();
    }

    private ExtendedOrder.OrderData getOrderData() {
        ExtendedOrder.OrderData orderData = new ExtendedOrder.OrderData();
        orderData.setLocationFrom(getLocation());
        orderData.setLocationTo(getLocation());

        return orderData;
    }

    private ru.yandex.market.delivery.mdbapp.components.geo.Location getLocation() {
        ru.yandex.market.delivery.mdbapp.components.geo.Location location =
            new ru.yandex.market.delivery.mdbapp.components.geo.Location();

        location.setCountry("Russia");
        location.setRegion("Москва и Московская область");
        location.setSubRegion("Moscow");
        location.setLocality("RU");
        location.setId(1);

        return location;
    }

    private Warehouse getWarehouse(Location location) {
        List<WorkTime> workTimes = new ArrayList<>();

        List<TimeInterval> timeIntervals = Arrays.asList(new TimeInterval("00:08+03:00/02:08+03:00"),
            new TimeInterval("01:08+03:00/03:08+03:00"));
        workTimes.add(new WorkTime(1, Arrays.asList(new TimeInterval("00:08+03:00/02:08+03:00"))));
        workTimes.addAll(
            IntStream.rangeClosed(2, 8).mapToObj(day -> new WorkTime(day, timeIntervals)).collect(Collectors.toList()));

        return new Warehouse.WarehouseBuilder(
            ResourceId.builder().setYandexId("1").setPartnerId("630060").build(),
            location,
            workTimes
        )
            .setContact(new Person("Антон", "Левин", null))
            .setPhones(Collections.singletonList(new Phone("4567890", "123")))
            .build();
    }
}
