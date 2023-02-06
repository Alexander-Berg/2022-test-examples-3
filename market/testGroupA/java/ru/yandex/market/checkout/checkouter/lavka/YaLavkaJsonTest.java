package ru.yandex.market.checkout.checkouter.lavka;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;

import ru.yandex.common.util.IOUtils;
import ru.yandex.common.util.region.Region;
import ru.yandex.common.util.region.RegionType;
import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.delivery.AddressImpl;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.geo.GeoRegionService;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.service.personal.PersonalDataRetrieveResult;
import ru.yandex.market.checkout.checkouter.service.personal.PersonalDataService;
import ru.yandex.market.checkout.checkouter.service.personal.model.PersAddress;
import ru.yandex.market.checkout.checkouter.service.personal.model.PersGps;
import ru.yandex.market.checkout.checkouter.service.yalavka.YaLavkaOrderProperties;
import ru.yandex.market.checkout.checkouter.service.yalavka.YaLavkaRequestBuilder;
import ru.yandex.market.common.taxi.model.DeliveryOptionsCheckRequest;
import ru.yandex.market.common.taxi.model.OrderCancellationRequest;
import ru.yandex.market.common.taxi.model.OrderReserveRequest;

import static org.mockito.ArgumentMatchers.any;
import static ru.yandex.common.util.region.RegionType.CITY;
import static ru.yandex.common.util.region.RegionType.CITY_DISTRICT;
import static ru.yandex.common.util.region.RegionType.SUBJECT_FEDERATION;
import static ru.yandex.common.util.region.RegionType.SUBJECT_FEDERATION_DISTRICT;
import static ru.yandex.common.util.region.RegionType.VILLAGE;

class YaLavkaJsonTest {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private static final String GPS = "12.123456789,10.987654321";

    private final PersonalDataService personalDataService = Mockito.mock(PersonalDataService.class);

    private final YaLavkaRequestBuilder requestBuilder = new YaLavkaRequestBuilder(
            getGeoRegionServiceMock(),
            new YaLavkaOrderProperties(
                    "08:00-12:00",
                    "09:00-21:00",
                    3
            ),
            personalDataService
    );

    private static Date localDateTimeToDate(String localDateTime) {
        return Date.from(
                ZonedDateTime.of(
                        LocalDateTime.parse(localDateTime),
                        ZoneId.systemDefault()
                ).toInstant()
        );
    }

    private static Order getSampleOrder() {
        Order order = new Order();
        order.setPaymentType(PaymentType.PREPAID);
        order.setId(123456L);
        order.setTotal(new BigDecimal("1000.12"));
        order.setBuyer(getSampleBuyer());

        Parcel parcel = new Parcel();
        parcel.setWeight(1500L);
        parcel.setDepth(30L);
        parcel.setHeight(15L);
        parcel.setWidth(20L);

        Delivery delivery = new Delivery();
        delivery.setParcels(List.of(parcel));
        delivery.setBuyerAddress(getSampleAddress());
        delivery.setDeliveryDates(new DeliveryDates(localDateTimeToDate("2020-09-23T00:00:00"), null));

        order.setDelivery(delivery);
        order.setNotes("cargo-return-on-point-B");

        return order;
    }

    private static Buyer getSampleBuyer() {
        Buyer buyer = new Buyer();
        buyer.setUid(56789L);
        buyer.setFirstName("Иван");
        buyer.setLastName("Иванов");
        buyer.setMiddleName("Иванович");
        buyer.setNormalizedPhone("79998881122");
        buyer.setEmail("ivanov@ivan.ov");
        return buyer;
    }

    private static Address getSampleAddress() {
        AddressImpl address = new AddressImpl();
        address.setGps(GPS);
        address.setStreet("Улица");
        address.setFloor("Этаж");
        address.setCountry("Страна");
        address.setEntrance("Подъезд");
        address.setBlock("Корпус");
        address.setBuilding("Строение");
        address.setHouse("Дом");
        address.setSubway("Метро");
        address.setCity("Нас. пункт");
        address.setApartment("Квартира");
        address.setEntryPhone("Домофон");
        address.setPreciseRegionId(777L);
        return address;
    }

    @Test
    void testReserveCancellationRequestSerialization() throws Exception {
        String actualJson = MAPPER.writeValueAsString(
                new OrderCancellationRequest(String.valueOf(1234567))
        );
        String expectedJson = readResourceFile("/json/yaLavkaReserveCancellationRequest.json");
        JSONAssert.assertEquals(expectedJson, actualJson, true);
    }

    @Test
    void testOrderReservationRequestSerialization() throws Exception {
        Order sampleOrder = getSampleOrder();
        Mockito.when(personalDataService.retrieve(any()))
                .thenReturn(new PersonalDataRetrieveResult(null, null, null,
                        PersAddress.convertToPersonal(sampleOrder.getDelivery().getBuyerAddress()),
                        PersGps.convertToPersonal("12.123456789,10.987654321")));

        OrderReserveRequest request = requestBuilder.buildOrderReserveRequest(sampleOrder);
        String actualJson = MAPPER.writeValueAsString(request);
        String expectedJson = readResourceFile("/json/yaLavkaOrderReservationRequest.json");
        JSONAssert.assertEquals(expectedJson, actualJson, true);
    }

    @Test
    void testOptionsCheckRequestSerialization() throws Exception {
        DeliveryOptionsCheckRequest request = requestBuilder.validateAndBuildOptionCheckRequest(
                List.of(
                        new BigDecimal("40.33"),
                        new BigDecimal("100.11"),
                        new BigDecimal("50.54")
                ),
                new BigDecimal("2.94"),
                GPS,
                List.of(
                        new DeliveryDates(localDateTimeToDate("2020-09-23T00:00:00"), null),
                        new DeliveryDates(localDateTimeToDate("2020-09-25T00:00:00"), null)
                )
        );

        String expectedJson = readResourceFile("/json/yaLavkaDeliveryOptionsCheckRequest.json");
        String actualJson = MAPPER.writeValueAsString(request);
        JSONAssert.assertEquals(expectedJson, actualJson, true);
    }

    private String readResourceFile(String filePath) throws IOException {
        return IOUtils.readInputStream(getClass().getResourceAsStream(filePath));
    }

    private GeoRegionService getGeoRegionServiceMock() {
        return new GeoRegionService() {
            @Nonnull
            @Override
            public List<Region> loadPathToRoot(long regionId, RegionType toTheNearest) {
                return List.of();
            }

            @Override
            public Map<RegionType, List<Region>> getUpperRegions(long regionId, RegionType toTheNearest) {
                return Map.of(
                        CITY, List.of(new Region(0, "Локация", CITY, null)),
                        VILLAGE, List.of(new Region(0, "Локация", VILLAGE, null)),
                        CITY_DISTRICT, List.of(new Region(0, "Район", CITY_DISTRICT, null)),
                        SUBJECT_FEDERATION, List.of(new Region(0, "Субъект", SUBJECT_FEDERATION, null)),
                        SUBJECT_FEDERATION_DISTRICT, List.of(new Region(0, "Субрегион", SUBJECT_FEDERATION_DISTRICT,
                                null))
                );
            }

            @Override
            public Optional<Region> getCountry(long regionId) {
                return Optional.empty();
            }

            @Override
            public ZoneId getRegionZone(int regionId, ZoneId defaultZoneId) {
                return defaultZoneId;
            }
        };
    }
}
