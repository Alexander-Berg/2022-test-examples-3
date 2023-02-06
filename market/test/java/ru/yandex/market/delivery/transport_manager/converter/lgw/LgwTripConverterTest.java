package ru.yandex.market.delivery.transport_manager.converter.lgw;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.converter.prefix.IdPrefixConverter;
import ru.yandex.market.delivery.transport_manager.domain.entity.Address;
import ru.yandex.market.delivery.transport_manager.domain.entity.LogisticsPointMetadata;
import ru.yandex.market.delivery.transport_manager.domain.entity.Movement;
import ru.yandex.market.delivery.transport_manager.domain.entity.Phone;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationLegalInfo;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.route_schedule.RouteSchedule;
import ru.yandex.market.delivery.transport_manager.domain.entity.route_schedule.RouteScheduleStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.route_schedule.RouteScheduleSubtype;
import ru.yandex.market.delivery.transport_manager.domain.entity.route_schedule.RouteScheduleType;
import ru.yandex.market.delivery.transport_manager.domain.entity.trip.Trip;
import ru.yandex.market.delivery.transport_manager.domain.entity.trip.TripPoint;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationSubtype;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationType;
import ru.yandex.market.delivery.transport_manager.dto.logistics_point.LogisticsPointAdditionalData;
import ru.yandex.market.delivery.transport_manager.dto.trip.TripExtended;
import ru.yandex.market.delivery.transport_manager.util.ProxyUtil;
import ru.yandex.market.delivery.transport_manager.util.TimeUtil;
import ru.yandex.market.logistic.gateway.common.model.common.DateTimeInterval;
import ru.yandex.market.logistic.gateway.common.model.common.LegalEntity;
import ru.yandex.market.logistic.gateway.common.model.common.LegalForm;
import ru.yandex.market.logistic.gateway.common.model.common.Location;
import ru.yandex.market.logistic.gateway.common.model.common.LogisticPoint;
import ru.yandex.market.logistic.gateway.common.model.common.MovementType;
import ru.yandex.market.logistic.gateway.common.model.common.Party;
import ru.yandex.market.logistic.gateway.common.model.common.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.common.TripInfo;
import ru.yandex.market.logistic.gateway.common.model.common.TripType;

class LgwTripConverterTest {

    private final LgwCommonResourceIdConverter resourceIdConverter =
        new LgwCommonResourceIdConverter(new IdPrefixConverter());
    private final LgwTripConverter tripConverter = ProxyUtil.createWithSelfReference(
        tripConverter -> new LgwTripConverter(
            resourceIdConverter,
            new LgwMovementConverter(
                resourceIdConverter,
                new LgwPartyConverter(
                    resourceIdConverter,
                    new LgwLegalEntityConverter(),
                    new LgwLogisticPointConverter(
                        new LgwLocationConverter(), resourceIdConverter, new LgwPhoneConverter()
                    )
                ),
                new WeightVolumeConverter(),
                tripConverter
            )
        ), LgwTripConverter.class);


    @Test
    void ok() {
        Assertions.assertThat(tripConverter.trip(
            new TripExtended(
                new Trip().setId(20L)
                    .setStartDate(LocalDate.parse("2021-12-31"))
                    .setPoints(Set.of(
                        TripPoint.builder().transportationUnitId(1000L).id(1L).index(1).build(),
                        TripPoint.builder().transportationUnitId(2000L).id(2L).index(2).build(),
                        TripPoint.builder().transportationUnitId(3000L).id(3L).index(3).build(),
                        TripPoint.builder().transportationUnitId(4000L).id(4L).index(4).build()
                    ))
                    .setRouteScheduleId(200L),
                null,
                routeSchedule().getPrice()
            ),
            3000L,
            4000L,
            TransportationSubtype.MAIN
        )).isEqualTo(
            new TripInfo.TripInfoBuilder()
                .setTripId(ResourceId.builder().setYandexId("TMT20").build())
                .setFromIndex(3)
                .setToIndex(4)
                .setTotalCount(4)
                .setPrice(10_000L)
                .setType(TripType.MAIN)
                .build()
        );
    }

    @Test
    void okWithPartnerId() {
        Assertions.assertThat(tripConverter.trip(
            new TripExtended(
                new Trip().setId(20L)
                    .setStartDate(LocalDate.parse("2021-12-31"))
                    .setExternalId("10001")
                    .setPoints(Set.of(
                        TripPoint.builder().transportationUnitId(1000L).id(1L).index(1).build(),
                        TripPoint.builder().transportationUnitId(2000L).id(2L).index(2).build(),
                        TripPoint.builder().transportationUnitId(3000L).id(3L).index(3).build(),
                        TripPoint.builder().transportationUnitId(4000L).id(4L).index(4).build()
                    ))
                    .setRouteScheduleId(200L),
                null,
                routeSchedule().getPrice()
            ),
            3000L,
            4000L,
            TransportationSubtype.MAIN
        )).isEqualTo(
            new TripInfo.TripInfoBuilder()
                .setTripId(ResourceId.builder().setYandexId("TMT20").setPartnerId("10001").build())
                .setFromIndex(3)
                .setToIndex(4)
                .setTotalCount(4)
                .setPrice(10_000L)
                .setType(TripType.MAIN)
                .build()
        );
    }

    @Test
    void okWithRoute() {
        Assertions.assertThat(tripConverter.trip(
            // todo TMSUPP-162 replace on RouteScheduleTripFactory
            new TripExtended(
                new Trip().setId(20L)
                    .setStartDate(LocalDate.parse("2021-12-31"))
                    .setPoints(Set.of(
                        new TripPoint().setTransportationUnitId(1000L).setId(1L).setIndex(1),
                        new TripPoint().setTransportationUnitId(2000L).setId(2L).setIndex(2),
                        new TripPoint().setTransportationUnitId(3000L).setId(3L).setIndex(3),
                        new TripPoint().setTransportationUnitId(4000L).setId(4L).setIndex(4)
                    ))
                    .setRouteScheduleId(200L),
                "routeName",
                routeSchedule().getPrice()
            ),
            3000L,
            4000L,
            TransportationSubtype.MAIN
        )).isEqualTo(
            new TripInfo.TripInfoBuilder()
                .setTripId(ResourceId.builder().setYandexId("TMT20").build())
                .setRouteName("routeName")
                .setFromIndex(3)
                .setToIndex(4)
                .setTotalCount(4)
                .setPrice(10_000L)
                .setType(TripType.MAIN)
                .build()
        );
    }

    @Test
    void failedPreconditionsNoPoints() {
        Assertions.assertThatThrownBy(() -> tripConverter.trip(
                new TripExtended(
                    new Trip().setId(20L)
                        .setStartDate(LocalDate.parse("2021-12-31"))
                        .setPoints(Set.of())
                        .setRouteScheduleId(200L),
                    null,
                    routeSchedule().getPrice()
                ),
                1000L,
                2000L,
                TransportationSubtype.MAIN
            ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Trip 20 must have unit 1000 in points: []");
    }

    @Test
    void failedPreconditionsWrongPoints() {
        Assertions.assertThatThrownBy(() -> tripConverter.trip(
                new TripExtended(
                    new Trip().setId(20L)
                        .setStartDate(LocalDate.parse("2021-12-31"))
                        .setPoints(Set.of(
                            TripPoint.builder().transportationUnitId(4000L).id(1L).index(0).build(),
                            TripPoint.builder().transportationUnitId(5000L).id(2L).index(1).build()
                        ))
                        .setRouteScheduleId(200L),
                    null,
                    routeSchedule().getPrice()
                ),
                1000L,
                2000L,
                TransportationSubtype.MAIN
            ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(
                "Trip 20 must have unit 1000 in points: [TripPoint(id=1, tripId=null, index=0, " +
                    "transportationUnitId=4000), TripPoint(id=2, tripId=null, " +
                    "index=1, transportationUnitId=5000)]");
    }

    @SuppressWarnings("checkstyle:MethodLength")
    @Test
    void trip() {
        ru.yandex.market.logistic.gateway.common.model.delivery.Trip lgwTrip = tripConverter.trip(
            new TripExtended(
                new Trip()
                    .setId(1L)
                    .setExternalId("12345")
                    .setPoints(Set.of(
                        new TripPoint().setTripId(1L).setIndex(0).setTransportationUnitId(1L),
                        new TripPoint().setTripId(1L).setIndex(1).setTransportationUnitId(2L)
                    )),
                "ABC",
                100L
            ),
            List.of(
                new Transportation()
                    .setId(1L)
                    .setTransportationType(TransportationType.XDOC_TRANSPORT)
                    .setOutboundUnit(new TransportationUnit()
                        .setId(1L)
                        .setPartnerId(1L)
                        .setLogisticPointId(101L)
                        .setPlannedIntervalStart(LocalDateTime.of(2020, 10, 11, 12, 0))
                        .setPlannedIntervalEnd(LocalDateTime.of(2020, 10, 11, 13, 0))
                    )
                    .setMovement(new Movement()
                        .setId(3L)
                        .setExternalId("3")
                        .setPartnerId(3L)
                        .setPlannedIntervalStart(LocalDateTime.of(2020, 10, 11, 12, 0))
                        .setPlannedIntervalEnd(LocalDateTime.of(2020, 10, 12, 13, 0))
                        .setMaxPallet(33)
                        .setPrice(2000L)
                    )
                    .setInboundUnit(new TransportationUnit()
                        .setId(2L)
                        .setPartnerId(2L)
                        .setLogisticPointId(102L)
                        .setPlannedIntervalStart(LocalDateTime.of(2020, 10, 12, 12, 0))
                        .setPlannedIntervalEnd(LocalDateTime.of(2020, 10, 12, 13, 0))
                    )
            ),
            Map.of(
                101L, new LogisticsPointAdditionalData(
                    new LogisticsPointMetadata()
                        .setPartnerId(1L)
                        .setLogisticsPointId(101L)
                        .setTransportationUnitId(1L)
                        .setName("Склад отправителя")
                        .setPhones(Set.of(
                            new Phone().setNumber("+7(999) 111-11-11")
                        )),
                    new Address().setLocationId(1),
                    new TransportationLegalInfo()
                        .setLegalName("1")
                        .setPartnerId(1L)
                        .setLegalAddress("первый адрес")
                        .setInn("111111")
                        .setOgrn("111111111")
                        .setLegalType("ООО")
                ),
                102L, new LogisticsPointAdditionalData(
                    new LogisticsPointMetadata()
                        .setPartnerId(2L)
                        .setLogisticsPointId(102L)
                        .setTransportationUnitId(2L)
                        .setName("Склад получателя")
                        .setPhones(Set.of(
                            new Phone().setNumber("+7(999) 222-22-22")
                        )),
                    new Address().setLocationId(2),
                    new TransportationLegalInfo()
                        .setLegalName("2")
                        .setPartnerId(2L)
                        .setLegalAddress("второй адрес")
                        .setInn("222222")
                        .setOgrn("222222222")
                        .setLegalType("ООО")
                )
            ),
            Map.of(
                1L, 1000
            )
        );

        ru.yandex.market.logistic.gateway.common.model.delivery.Trip expected =
            new ru.yandex.market.logistic.gateway.common.model.delivery.Trip(
                ResourceId.builder().setYandexId("TMT1").setPartnerId("12345").build(),
                List.of(new ru.yandex.market.logistic.gateway.common.model.common.Movement(
                    ResourceId.builder().setYandexId("TMM3").setPartnerId("3").build(),
                    new DateTimeInterval(
                        OffsetDateTime.of(2020, 10, 11, 12, 0, 0, 0, TimeUtil.DEFAULT_ZONE_OFFSET),
                        OffsetDateTime.of(2020, 10, 12, 13, 0, 0, 0, TimeUtil.DEFAULT_ZONE_OFFSET)
                    ),
                    BigDecimal.valueOf(0.001),
                    null,
                    new Party(
                        LogisticPoint
                            .builder(ResourceId.builder().setYandexId("101").build())
                            .setName("Склад отправителя")
                            .setLocation(Location.builder(null, null, null).build())
                            .setPhones(List.of(
                                ru.yandex.market.logistic.gateway.common.model.common.Phone
                                    .builder("+7(999) 111-11-11")
                                    .build()
                            ))
                            .setLocation(Location.builder(null, null, null).setLocationId(1L).build())
                            .build(),
                        LegalEntity.builder()
                            .setLegalForm(LegalForm.OOO)
                            .setLegalName("1")
                            .setInn("111111")
                            .setOgrn("111111111")
                            .setAddress(
                                ru.yandex.market.logistic.gateway.common.model.common.Address
                                    .builder("первый адрес")
                                    .build()
                            ).build(),
                        ResourceId.builder().setYandexId("1").build()
                    ),
                    new Party(
                        LogisticPoint
                            .builder(ResourceId.builder().setYandexId("102").build())
                            .setName("Склад получателя")
                            .setLocation(Location.builder(null, null, null).build())
                            .setPhones(List.of(
                                ru.yandex.market.logistic.gateway.common.model.common.Phone
                                    .builder("+7(999) 222-22-22")
                                    .build()
                            ))
                            .setLocation(Location.builder(null, null, null).setLocationId(2L).build())
                            .build(),
                        LegalEntity.builder()
                            .setLegalForm(LegalForm.OOO)
                            .setLegalName("2")
                            .setInn("222222")
                            .setOgrn("222222222")
                            .setAddress(
                                ru.yandex.market.logistic.gateway.common.model.common.Address
                                    .builder("второй адрес")
                                    .build()
                            ).build(),
                        ResourceId.builder().setYandexId("2").build()
                    ),
                    null,
                    33,
                    new DateTimeInterval(
                        OffsetDateTime.of(2020, 10, 11, 12, 0, 0, 0, TimeUtil.DEFAULT_ZONE_OFFSET),
                        OffsetDateTime.of(2020, 10, 11, 13, 0, 0, 0, TimeUtil.DEFAULT_ZONE_OFFSET)
                    ),
                    new DateTimeInterval(
                        OffsetDateTime.of(2020, 10, 12, 12, 0, 0, 0, TimeUtil.DEFAULT_ZONE_OFFSET),
                        OffsetDateTime.of(2020, 10, 12, 13, 0, 0, 0, TimeUtil.DEFAULT_ZONE_OFFSET)
                    ),
                     new TripInfo(
                         ResourceId.builder().setYandexId("TMT1").setPartnerId("12345").build(),
                         "ABC",
                         0,
                         1,
                         2,
                         100L,
                         null
                     ),
                    List.of(),
                    MovementType.XDOC_TRANSPORT,
                    null
                ))
            );

        Assertions.assertThat(lgwTrip).isEqualTo(expected);
    }

    RouteSchedule routeSchedule() {
        return RouteSchedule.builder()
            .routeId(1L)
            .price(10_000L)
            .type(RouteScheduleType.LINEHAUL)
            .subtype(RouteScheduleSubtype.MAIN)
            .status(RouteScheduleStatus.ACTIVE)
            .daysOfWeek(List.of(DayOfWeek.MONDAY))
            .points(Set.of())
            .startDate(LocalDate.of(2021, 1, 1))
            .build();
    }
}
