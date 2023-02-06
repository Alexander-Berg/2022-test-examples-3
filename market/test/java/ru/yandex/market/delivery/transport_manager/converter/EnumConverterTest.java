package ru.yandex.market.delivery.transport_manager.converter;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.delivery.tracker.domain.enums.DeliveryTrackStatus;
import ru.yandex.market.delivery.transport_manager.admin.enums.AdminMovementStatus;
import ru.yandex.market.delivery.transport_manager.admin.enums.AdminTagCode;
import ru.yandex.market.delivery.transport_manager.domain.entity.EntityType;
import ru.yandex.market.delivery.transport_manager.domain.entity.StartrekEntityType;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationSubstatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType;
import ru.yandex.market.delivery.transport_manager.domain.entity.route_schedule.RouteScheduleType;
import ru.yandex.market.delivery.transport_manager.domain.enums.CargoType;
import ru.yandex.market.delivery.transport_manager.domain.enums.DimensionsClass;
import ru.yandex.market.delivery.transport_manager.domain.enums.OrderStatus;
import ru.yandex.market.delivery.transport_manager.domain.enums.SegmentType;
import ru.yandex.market.delivery.transport_manager.domain.enums.ShipmentType;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationType;
import ru.yandex.market.delivery.transport_manager.domain.enums.VatType;
import ru.yandex.market.delivery.transport_manager.model.dto.MovementCourierDto;
import ru.yandex.market.delivery.transport_manager.model.enums.MovementStatus;
import ru.yandex.market.delivery.transport_manager.model.enums.OwnershipType;
import ru.yandex.market.delivery.transport_manager.model.enums.PartnerType;
import ru.yandex.market.delivery.transport_manager.model.enums.TagCode;
import ru.yandex.market.delivery.transport_manager.service.calendaring.exception.BookingSlotNotFoundReason;
import ru.yandex.market.delivery.transport_manager.util.ConverterUtil;

public class EnumConverterTest {
    @ParameterizedTest
    @MethodSource("getTestCases")
    <F extends Enum<F>, T extends Enum<T>> void convertEnum(Class<F> from, Class<T> to, List<F> excluded) {
        Stream.of(from.getEnumConstants())
            .filter(value -> !excluded.contains(value))
            .forEach(value -> convert(value, to));
    }

    private static Stream<Arguments> getTestCases() {
        return Stream.of(
            Arguments.of(
                ru.yandex.market.logistics.management.entity.type.PartnerType.class,
                PartnerType.class,
                List.of(
                    ru.yandex.market.logistics.management.entity.type.PartnerType.XDOC,
                    ru.yandex.market.logistics.management.entity.type.PartnerType.DROPSHIP_BY_SELLER
                )
            ),
            Arguments.of(
                ru.yandex.market.logistics.lom.model.enums.PartnerType.class,
                PartnerType.class,
                Collections.emptyList()
            ),
            Arguments.of(
                ru.yandex.market.logistics.lom.model.enums.ShipmentType.class,
                ShipmentType.class,
                Collections.emptyList()
            ),
            Arguments.of(
                ru.yandex.market.logistics.lom.model.enums.SegmentType.class,
                SegmentType.class,
                Collections.emptyList()
            ),
            Arguments.of(
                ru.yandex.market.logistics.lom.model.enums.VatType.class,
                VatType.class,
                Collections.emptyList()
            ),
            Arguments.of(
                ru.yandex.market.logistics.lom.model.enums.CargoType.class,
                CargoType.class,
                Collections.emptyList()
            ),
            Arguments.of(
                ru.yandex.market.logistics.lom.model.enums.OrderStatus.class,
                OrderStatus.class,
                List.of(ru.yandex.market.logistics.lom.model.enums.OrderStatus.UNKNOWN)
            ),
            Arguments.of(
                BookingSlotNotFoundReason.class,
                TransportationSubstatus.class,
                Collections.emptyList()
            ),
            Arguments.of(
                EntityType.class,
                StartrekEntityType.class,
                List.of(EntityType.TRANSPORTATION_UNIT, EntityType.REGISTER, EntityType.TRIP)
            ),
            Arguments.of(
                RouteScheduleType.class,
                TransportationType.class,
                List.of(RouteScheduleType.COMMON)
            ),
            Arguments.of(
                DeliveryTrackStatus.class,
                ru.yandex.market.delivery.transport_manager.dto.tracker.TrackStatus.class,
                Collections.emptyList()
            ),
            Arguments.of(
                ru.yandex.market.delivery.tracker.domain.enums.EntityType.class,
                ru.yandex.market.delivery.transport_manager.dto.tracker.EntityType.class,
                List.of(
                    ru.yandex.market.delivery.tracker.domain.enums.EntityType.EXTERNAL_ORDER,
                    ru.yandex.market.delivery.tracker.domain.enums.EntityType.ORDER_RETURN
                )
            ),
            Arguments.of(
                ru.yandex.market.delivery.tracker.domain.enums.CheckpointStatus.class,
                ru.yandex.market.delivery.transport_manager.dto.tracker.CheckpointStatus.class,
                Collections.emptyList()
            ),
            Arguments.of(
                ru.yandex.market.delivery.transport_manager.domain.entity.CenterType.class,
                ru.yandex.market.delivery.transport_manager.dto.distribution_center.units.CenterType.class,
                Collections.emptyList()
            ),
            Arguments.of(
                ru.yandex.market.tpl.core.external.routing.api.DimensionsClass.class,
                DimensionsClass.class,
                Collections.emptyList()
            ),
            Arguments.of(
                TransportationUnitType.class,
                ru.yandex.market.logistics.les.tm.enums.TransportationUnitType.class,
                Collections.emptyList()
            ),
            Arguments.of(
                OwnershipType.class,
                ru.yandex.market.logistics.les.tm.enums.OwnershipType.class,
                Collections.emptyList()
            ),
            Arguments.of(
                    MovementCourierDto.Unit.class,
                    ru.yandex.market.logistics.les.tm.enums.TransportationUnit.class,
                    Collections.emptyList()
            ),
            Arguments.of(
                TagCode.class,
                AdminTagCode.class,
                Collections.emptyList()
            ),
            Arguments.of(
                MovementStatus.class,
                AdminMovementStatus.class,
                Collections.emptyList()
            )
        );
    }

    private <T extends Enum<T>> void convert(Enum<?> type, Class<T> clazz) {
        Optional.ofNullable(ConverterUtil.convert(type, clazz)).orElseThrow(
            () -> new IllegalArgumentException(String.format("Cannot convert '%s' to '%s'", type, clazz.getName()))
        );
    }
}
