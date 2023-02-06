package ru.yandex.market.delivery.transport_manager.converter.les;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.domain.entity.TimeSlot;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.courier.MovementCourier;
import ru.yandex.market.delivery.transport_manager.domain.entity.courier.MovementCourierStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.trip.TripPoint;
import ru.yandex.market.delivery.transport_manager.model.enums.OwnershipType;
import ru.yandex.market.delivery.transport_manager.util.TimeUtil;
import ru.yandex.market.logistics.les.tm.dto.MovementCourierDto;
import ru.yandex.market.logistics.les.tm.dto.TransportationPartnerExtendedInfoDto;
import ru.yandex.market.logistics.les.tm.dto.TripPointDto;
import ru.yandex.market.logistics.les.tm.enums.TransportationUnitType;

class LesConverterTest {
    private LesConverter lesConverter;

    @BeforeEach
    void setUp() {
        lesConverter = new LesConverter();
    }

    @Test
    void convertCourier() {
        Assertions
            .assertThat(lesConverter.convert(new MovementCourier(
                1L,
                100L,
                "123",
                "Иван",
                "Петров",
                "Николаевич",
                "FM",
                "Volvo",
                "A123BC197",
                "AB0000197",
                OwnershipType.RENT,
                "+7 (916) 123-45-67",
                "+7 (926) 123-45-67",
                null,
                null,
                MovementCourierStatus.NEW,
                MovementCourier.Unit.ALL,
                LocalDateTime.of(2021, 12, 1, 12, 0),
                LocalDateTime.of(2021, 12, 1, 12, 0)
            )))
            .isEqualTo(new MovementCourierDto(
                "123",
                "Иван",
                "Петров",
                "Николаевич",
                "FM",
                "A123BC197",
                "Volvo",
                "AB0000197",
                "+7 (916) 123-45-67",
                "+7 (926) 123-45-67",
                ru.yandex.market.logistics.les.tm.enums.OwnershipType.RENT,
                null
            ));
    }

    @Test
    void convertPoint() {
        Assertions
            .assertThat(lesConverter.convert(
                new TripPoint()
                    .setId(100L),
                new TransportationUnit()
                    .setId(11L)
                    .setPartnerId(101L)
                    .setType(ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType.OUTBOUND)
                    .setPlannedIntervalStart(LocalDateTime.of(2022, 1, 12, 12, 0)),
                10000L,
                ""
            ))
            .isEqualTo(new TripPointDto(
                100L,
                OffsetDateTime.of(2022, 1, 12, 12, 0, 0, 0, TimeUtil.DEFAULT_ZONE_OFFSET),
                10000L,
                TransportationUnitType.OUTBOUND,
                101L,
                "",
                "TMU11"
            ));
    }

    @Test
    void convertPointWithSlot() {
        Assertions
            .assertThat(lesConverter.convert(
                new TripPoint()
                    .setId(100L),
                new TransportationUnit()
                    .setId(11L)
                    .setPartnerId(101L)
                    .setType(ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType.OUTBOUND)
                    .setPlannedIntervalStart(LocalDateTime.of(2022, 1, 12, 12, 0))
                    .setBookedTimeSlot(new TimeSlot()
                        .setFromDate(LocalDateTime.of(2022, 1, 12, 14, 0, 0, 0))
                        .setZoneId(TimeUtil.DEFAULT_ZONE_OFFSET.getId())
                    ),
                10000L,
                ""
            ))
            .isEqualTo(new TripPointDto(
                100L,
                OffsetDateTime.of(2022, 1, 12, 14, 0, 0, 0, TimeUtil.DEFAULT_ZONE_OFFSET),
                10000L,
                TransportationUnitType.OUTBOUND,
                101L,
                "",
                "TMU11"
            ));
    }

    @Test
    void convertMovementPartnerInfo() {
        Assertions
            .assertThat(lesConverter.convert(1L, "ТК Доедем"))
            .isEqualTo(new TransportationPartnerExtendedInfoDto(1L, "ТК Доедем", null));
    }
}
