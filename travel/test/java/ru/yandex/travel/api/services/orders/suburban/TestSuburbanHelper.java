package ru.yandex.travel.api.services.orders.suburban;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;
import org.javamoney.moneta.Money;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.api.config.common.EncryptionConfigurationProperties;
import ru.yandex.travel.api.endpoints.generic_booking_flow.model.suburban.CreateSuburbanServiceData;
import ru.yandex.travel.api.endpoints.generic_booking_flow.model.suburban.SuburbanServiceInfoDTO;
import ru.yandex.travel.api.infrastucture.ApiTokenEncrypter;
import ru.yandex.travel.api.services.dictionaries.train.station.MockTrainStationDataProvider;
import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.commons.proto.ProtoUtils;
import ru.yandex.travel.dicts.rasp.proto.TStation;
import ru.yandex.travel.orders.commons.proto.EServiceType;
import ru.yandex.travel.orders.commons.proto.TSuburbanTestContext;
import ru.yandex.travel.orders.proto.TCreateServiceReq;
import ru.yandex.travel.orders.proto.TOrderServiceInfo;
import ru.yandex.travel.orders.proto.TServiceInfo;
import ru.yandex.travel.orders.workflow.orderitem.generic.proto.EOrderItemState;
import ru.yandex.travel.suburban.model.AeroexpressReservation;
import ru.yandex.travel.suburban.model.MovistaReservation;
import ru.yandex.travel.suburban.model.SuburbanDicts;
import ru.yandex.travel.suburban.model.SuburbanReservation;
import ru.yandex.travel.suburban.model.WicketDevice;
import ru.yandex.travel.suburban.model.WicketType;
import ru.yandex.travel.suburban.partners.SuburbanCarrier;
import ru.yandex.travel.suburban.partners.SuburbanProvider;
import ru.yandex.travel.testing.misc.TestResources;

import static org.assertj.core.api.Assertions.assertThat;

public class TestSuburbanHelper {
    private static ObjectMapper objMapper = createObjectMapper();
    private static ApiTokenEncrypter apiTokenEncrypter = createApiTokenEncrypter();
    private SuburbanHelper suburbanHelper;

    @Before
    public void setUp() {
        List<TStation> stations = new ArrayList<>();

        stations.add(TStation.newBuilder().setId(9600721).setTitleDefault("Одинцово").build());
        stations.add(TStation.newBuilder().setId(2000006).setTitleDefault("Москва (Белорусский вокзал)").build());
        stations.add(TStation.newBuilder().setId(9602496).setTitleDefault("Санкт-Петербург (Витебский вокзал)").build());
        stations.add(TStation.newBuilder().setId(9602675).setTitleDefault("Царское Село").build());
        stations.add(TStation.newBuilder().setId(9600213).setTitleDefault("Шереметьево").build());

        var stationDict = MockTrainStationDataProvider.INSTANCE.build(stations);

        suburbanHelper = new SuburbanHelper(stationDict, apiTokenEncrypter);
    }

    @Test
    @SneakyThrows
    public void testBuildSuburbanServicesReqs() {
        CreateSuburbanServiceData subService = fromJson(
                "suburban/CreateSuburbanMovistaServiceData.json", CreateSuburbanServiceData.class);
        List<TCreateServiceReq> services = suburbanHelper.buildSuburbanServicesReqs(Arrays.asList(subService));
        assertThat(services).hasSize(1);
        var service = services.get(0);
        assertThat(service.getServiceType()).isEqualTo(EServiceType.PT_SUBURBAN);
        SuburbanReservation payload = ProtoUtils.fromTJson(service.getSourcePayload(), SuburbanReservation.class);

        assertThat(payload.getStationFrom().getId()).isEqualTo(9600721);
        assertThat(payload.getStationFrom().getTitleDefault()).isEqualTo("Одинцово");
        assertThat(payload.getStationTo().getId()).isEqualTo(2000006);
        assertThat(payload.getStationTo().getTitleDefault()).isEqualTo("Москва (Белорусский вокзал)");
        assertThat(payload.getPrice()).isEqualTo(Money.of(72.0, ProtoCurrencyUnit.RUB));
        assertThat(payload.getSuburbanDicts().getStations()).isEqualTo(Arrays.asList(
                SuburbanDicts.Station.builder().raspID(9600721).build(),
                SuburbanDicts.Station.builder().raspID(2000006).build()
        ));

        assertThat(payload.getMovistaReservation().getDate()).isEqualTo(LocalDate.of(2021, 2, 20));
        assertThat(payload.getMovistaReservation().getStationFromExpressId()).isEqualTo(2000055);
        assertThat(payload.getMovistaReservation().getStationToExpressId()).isEqualTo(2000007);
        assertThat(payload.getMovistaReservation().getWicket()).isEqualTo(
                WicketDevice.builder().type(WicketType.VALIDATOR).deviceType("some device").build());

        assertThat(payload.getImReservation()).isNull();
        assertThat(payload.getAeroexpressReservation()).isNull();

        // check that jackson magic works
        toJson(subService);
        ProtoUtils.toTJson(payload);
    }

    @Test
    public void testBuildSuburbanImServicesReqs() {
        CreateSuburbanServiceData subService = fromJson(
                "suburban/CreateSuburbanImServiceData.json", CreateSuburbanServiceData.class);
        List<TCreateServiceReq> services = suburbanHelper.buildSuburbanServicesReqs(Arrays.asList(subService));
        assertThat(services).hasSize(1);
        var service = services.get(0);
        assertThat(service.getServiceType()).isEqualTo(EServiceType.PT_SUBURBAN);
        SuburbanReservation payload = ProtoUtils.fromTJson(service.getSourcePayload(), SuburbanReservation.class);

        assertThat(payload.getPrice()).isEqualTo(Money.of(72.0, ProtoCurrencyUnit.RUB));
        assertThat(payload.getStationFrom().getId()).isEqualTo(9602496);
        assertThat(payload.getStationFrom().getTitleDefault()).isEqualTo("Санкт-Петербург (Витебский вокзал)");
        assertThat(payload.getStationTo().getId()).isEqualTo(9602675);
        assertThat(payload.getStationTo().getTitleDefault()).isEqualTo("Царское Село");

        assertThat(payload.getImReservation().getStationFromExpressId()).isEqualTo(2004003);
        assertThat(payload.getImReservation().getStationToExpressId()).isEqualTo(2004182);
        assertThat(payload.getImReservation().getDate()).isEqualTo(LocalDate.of(2021, 8, 21));
        assertThat(payload.getImReservation().getTrainNumber()).isEqualTo("6707");
        assertThat(payload.getImReservation().getImProvider()).isEqualTo("P6");

        assertThat(payload.getSuburbanDicts().getStations().get(1)).isEqualTo(new SuburbanDicts.Station(9602675));

        assertThat(payload.getMovistaReservation()).isNull();
        assertThat(payload.getAeroexpressReservation()).isNull();

        // check that jackson magic works
        toJson(subService);
        ProtoUtils.toTJson(payload);
    }

    @Test
    public void testBuildSuburbanAeroexpressServicesReqs() {
        CreateSuburbanServiceData subService = fromJson(
                "suburban/CreateSuburbanAeroexpressServiceData.json", CreateSuburbanServiceData.class);
        List<TCreateServiceReq> services = suburbanHelper.buildSuburbanServicesReqs(Arrays.asList(subService));
        assertThat(services).hasSize(1);
        var service = services.get(0);
        assertThat(service.getServiceType()).isEqualTo(EServiceType.PT_SUBURBAN);
        SuburbanReservation payload = ProtoUtils.fromTJson(service.getSourcePayload(), SuburbanReservation.class);

        assertThat(payload.getPrice()).isEqualTo(Money.of(499, ProtoCurrencyUnit.RUB));
        assertThat(payload.getStationFrom().getId()).isEqualTo(2000006);
        assertThat(payload.getStationFrom().getTitleDefault()).isEqualTo("Москва (Белорусский вокзал)");
        assertThat(payload.getStationTo().getId()).isEqualTo(9600213);
        assertThat(payload.getStationTo().getTitleDefault()).isEqualTo("Шереметьево");

        assertThat(payload.getAeroexpressReservation().getMenuId()).isEqualTo(2);
        assertThat(payload.getAeroexpressReservation().getOrderType()).isEqualTo(42);
        assertThat(payload.getAeroexpressReservation().getDate()).isEqualTo(LocalDate.of(2022, 2, 20));

        assertThat(payload.getSuburbanDicts().getStations().get(1)).isEqualTo(new SuburbanDicts.Station(9600213));

        assertThat(payload.getMovistaReservation()).isNull();
        assertThat(payload.getImReservation()).isNull();

        // check that jackson magic works
        toJson(subService);
        ProtoUtils.toTJson(payload);
    }

    @Test
    @SneakyThrows
    public void testBuildSuburbanServicesReqsTestContext() {
        CreateSuburbanServiceData subService = fromJson(
                "suburban/CreateSuburbanMovistaServiceData.json", CreateSuburbanServiceData.class);
        subService.setTestContextToken(
                apiTokenEncrypter.toSuburbanTestContextToken(
                        TSuburbanTestContext.newBuilder().setTicketBody("4242").build()));

        TCreateServiceReq service = suburbanHelper.buildSuburbanServicesReqs(Arrays.asList(subService)).get(0);
        assertThat(service.getSuburbanTestContext()).isEqualTo(
                TSuburbanTestContext.newBuilder().setTicketBody("4242").build());
    }

    @Test
    public void testBuildSuburbanServicesReqsEmpty() {
        assertThat(suburbanHelper.buildSuburbanServicesReqs(new ArrayList<>())).hasSize(0);
    }

    @Test
    public void testBuildSuburbanServiceInfo() {
        MovistaReservation movistaReservation = MovistaReservation.builder()
                .date(LocalDate.of(2021, 8, 22))
                .wicket(WicketDevice.builder().type(WicketType.VALIDATOR).deviceType("some device").build())
                .orderId(4242)
                .ticketNumber(5678)
                .ticketBody("body42")
                .build();

        SuburbanReservation payload = SuburbanReservation.builder()
                .provider(SuburbanProvider.MOVISTA)
                .carrier(SuburbanCarrier.CPPK)
                .price(Money.of(72.5, ProtoCurrencyUnit.RUB))
                .stationFrom(SuburbanReservation.Station.builder().id(9600721).build())
                .stationTo(SuburbanReservation.Station.builder().id(2000006).build())
                .error(SuburbanReservation.Error.builder().message("Has some error").build())
                .movistaReservation(movistaReservation)
                .build();

        TOrderServiceInfo serviceInfo = TOrderServiceInfo.newBuilder()
                .setServiceType(EServiceType.PT_SUBURBAN)
                .setServiceInfo(TServiceInfo.newBuilder()
                        .setGenericOrderItemState(EOrderItemState.IS_CONFIRMED)
                        .setPayload(ProtoUtils.toTJson(payload)).build()).build();

        SuburbanServiceInfoDTO subService = suburbanHelper.buildSuburbanServiceInfo(serviceInfo);
        SuburbanServiceInfoDTO subServiceExpected = fromJson(
                "suburban/SuburbanServiceInfoDto.json", SuburbanServiceInfoDTO.class);

        assertThat(subService).isEqualTo(subServiceExpected);
    }

    @Test
    public void testBuildAeroexpressSuburbanServiceInfo() {
        AeroexpressReservation aeroexpressReservation = AeroexpressReservation.builder()
                .date(LocalDate.of(2022, 2, 20))
                .orderId(Long.valueOf(4242))
                .ticketId(Long.valueOf(5678))
                .ticketUrl("url42")
                .tariff("tariff")
                .stDepart("route")
                .tripDate("2022-02-21")
                .validUntil("2022-03-31")
                .menuId(22)
                .build();

        SuburbanReservation payload = SuburbanReservation.builder()
                .provider(SuburbanProvider.AEROEXPRESS)
                .carrier(SuburbanCarrier.AEROEXPRESS)
                .price(Money.of(499, ProtoCurrencyUnit.RUB))
                .stationFrom(SuburbanReservation.Station.builder().id(2000006).build())
                .stationTo(SuburbanReservation.Station.builder().id(9600213).build())
                .error(SuburbanReservation.Error.builder().message("Has some error").build())
                .aeroexpressReservation(aeroexpressReservation)
                .build();

        TOrderServiceInfo serviceInfo = TOrderServiceInfo.newBuilder()
                .setServiceType(EServiceType.PT_SUBURBAN)
                .setServiceInfo(TServiceInfo.newBuilder()
                        .setGenericOrderItemState(EOrderItemState.IS_CONFIRMED)
                        .setPayload(ProtoUtils.toTJson(payload)).build()).build();

        SuburbanServiceInfoDTO subService = suburbanHelper.buildSuburbanServiceInfo(serviceInfo);
        SuburbanServiceInfoDTO subServiceExpected = fromJson(
                "suburban/AeroexpressSuburbanServiceInfoDto.json", SuburbanServiceInfoDTO.class);

        assertThat(subService).isEqualTo(subServiceExpected);
    }


    @SneakyThrows
    private <T> T fromJson(String path, Class<T> resultClass) {
        var jsonString = TestResources.readResource(path);
        return objMapper.readValue(jsonString, resultClass);
    }

    @SneakyThrows
    private <T> String toJson(T obj) {
        return objMapper.writeValueAsString(obj);
    }

    private static ObjectMapper createObjectMapper() {
        return new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .setDefaultPropertyInclusion(JsonInclude.Include.NON_EMPTY)
                .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private static ApiTokenEncrypter createApiTokenEncrypter() {
        var props = new EncryptionConfigurationProperties();
        props.setEncryptionKey("123");
        return new ApiTokenEncrypter(props);
    }
}
