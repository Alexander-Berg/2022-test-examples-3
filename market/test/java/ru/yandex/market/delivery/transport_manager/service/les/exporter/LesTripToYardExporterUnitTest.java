package ru.yandex.market.delivery.transport_manager.service.les.exporter;

import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.delivery.transport_manager.converter.les.LesConverter;
import ru.yandex.market.delivery.transport_manager.domain.entity.Movement;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationPartnerInfo;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.courier.MovementCourier;
import ru.yandex.market.delivery.transport_manager.service.TransportationPartnerInfoService;
import ru.yandex.market.logistics.les.tm.dto.MovementCourierDto;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

class LesTripToYardExporterUnitTest {
    private LesTripToYardExporter exporter;
    private TransportationPartnerInfoService
        transportationPartnerInfoService;

    @BeforeEach
    void setUp() {
        transportationPartnerInfoService = Mockito.mock(TransportationPartnerInfoService.class);
        exporter = new LesTripToYardExporter(
            null,
            null,
            transportationPartnerInfoService,
            null,
            null,
            new LesConverter(),
            null
        );
    }

    @Test
    void getMovementPartnerId() {
        Movement movement1 = new Movement().setPartnerId(1L);
        Movement movement2 = new Movement().setPartnerId(1L);
        Assertions.assertThat(
            exporter.getMovementPartnerId(List.of(
                new Transportation().setMovement(movement1),
                new Transportation().setMovement(movement1),
                new Transportation().setMovement(movement2)
            ))
        ).isEqualTo(1L);
    }

    @Test
    void getMovementPartnerIdMissing() {
        Movement movement1 = new Movement();
        Movement movement2 = new Movement();
        Assertions.assertThat(
            exporter.getMovementPartnerId(List.of(
                new Transportation().setMovement(movement1),
                new Transportation().setMovement(movement1),
                new Transportation().setMovement(movement2)
            ))
        ).isNull();
    }

    @Test
    void getMovementPartnerIdNull() {
        Assertions.assertThat(
            exporter.getMovementPartnerId(List.of())
        ).isNull();
    }

    @Test
    void getMovementPartnerIdMultiple() {
        Movement movement1 = new Movement().setPartnerId(1L);
        Movement movement2 = new Movement().setPartnerId(2L);
        Assertions.assertThatThrownBy(
                () -> exporter.getMovementPartnerId(List.of(
                    new Transportation().setMovement(movement1),
                    new Transportation().setMovement(movement1),
                    new Transportation().setMovement(movement2)
                )))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getMovementPartnerNameNull() {
        Assertions.assertThat(
            exporter.getMovementPartnerName(
                List.of(
                    new Transportation(),
                    new Transportation(),
                    new Transportation()
                ),
                null
            )
        ).isNull();
    }

    @Test
    void getMovementPartnerNameNoTransportations() {
        Assertions.assertThat(
            exporter.getMovementPartnerName(
                List.of(),
                10L
            )
        ).isNull();
    }

    @Test
    void getMovementPartnerName() {
        Mockito.when(transportationPartnerInfoService.get(Mockito.eq(List.of(1L, 2L, 3L)), Mockito.eq(10L)))
            .thenReturn(List.of(
                new TransportationPartnerInfo(10L, 1L, "ТК Доедем", PartnerType.DELIVERY),
                new TransportationPartnerInfo(10L, 2L, "ТК Доедем", PartnerType.DELIVERY),
                new TransportationPartnerInfo(10L, 3L, "Старое название", PartnerType.DELIVERY)
            ));
        Assertions.assertThat(
            exporter.getMovementPartnerName(
                List.of(
                    new Transportation().setId(1L),
                    new Transportation().setId(2L),
                    new Transportation().setId(3L)
                ),
                10L
            )
        ).isEqualTo("ТК Доедем");
    }

    @Test
    void groupByIndex() {
        Assertions.assertThat(
                exporter.groupByIndex(List.of(
                    new MovementCourier().setId(1L).setExternalId("1"),
                    new MovementCourier().setId(2L).setExternalId("1"),
                    new MovementCourier().setId(3L).setExternalId("2")
                ))
            )
            .isEqualTo(Map.of(
                0L, new MovementCourierDto("1", null, null, null, null, null, null, null, null, null, null, null),
                1L, new MovementCourierDto("2", null, null, null, null, null, null, null, null, null, null, null)
            ));
    }

    @Test
    void getUnitIdToCourierIndexMap() {
        Assertions.assertThat(
                exporter.getUnitIdToCourierIndexMap(
                    List.of(
                        new Transportation().setMovement(new Movement().setId(1L))
                            .setOutboundUnit(new TransportationUnit().setId(1L))
                            .setInboundUnit(new TransportationUnit().setId(2L)),
                        new Transportation().setMovement(new Movement().setId(1L))
                            .setOutboundUnit(new TransportationUnit().setId(3L))
                            .setInboundUnit(new TransportationUnit().setId(4L)),
                        new Transportation().setMovement(new Movement().setId(2L))
                            .setOutboundUnit(new TransportationUnit().setId(5L))
                            .setInboundUnit(new TransportationUnit().setId(6L)),
                        new Transportation().setMovement(new Movement().setId(3L))
                            .setOutboundUnit(new TransportationUnit().setId(7L))
                            .setInboundUnit(new TransportationUnit().setId(8L))
                    ),
                    Map.of(
                        1L, new MovementCourier().setId(1L).setExternalId("1").setMovementId(1L),
                        2L, new MovementCourier().setId(2L).setExternalId("1").setMovementId(2L),
                        3L, new MovementCourier().setId(3L).setExternalId("2").setMovementId(3L)
                    ),
                    Map.of(
                        0L,
                        new MovementCourierDto("1", null, null, null, null, null, null, null, null, null, null, null),
                        1L,
                        new MovementCourierDto("2", null, null, null, null, null, null, null, null, null, null, null)
                    )
                )
            )
            .isEqualTo(Map.of(
                1L, 0L,
                2L, 0L,
                3L, 0L,
                4L, 0L,
                5L, 0L,
                6L, 0L,
                7L, 1L,
                8L, 1L
            ));
    }

    @Test
    void firstOrNull() {
        Assertions.assertThat(
            exporter.<Integer>firstOrNull(List.of())
        ).isNull();
    }

    @Test
    void firstOrNullValue() {
        Assertions.assertThat(
            exporter.<Integer>firstOrNull(List.of(1, 2, 3))
        ).isEqualTo(1);
    }
}
