package ru.yandex.market.delivery.transport_manager.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.delivery.gruzin.model.UnitCargoType;
import ru.yandex.market.delivery.transport_manager.admin.enums.AdminClientName;
import ru.yandex.market.delivery.transport_manager.admin.enums.AdminCountType;
import ru.yandex.market.delivery.transport_manager.admin.enums.AdminLogPointFeatureType;
import ru.yandex.market.delivery.transport_manager.admin.enums.AdminMovementSource;
import ru.yandex.market.delivery.transport_manager.admin.enums.AdminRegisterStatus;
import ru.yandex.market.delivery.transport_manager.admin.enums.AdminRegisterType;
import ru.yandex.market.delivery.transport_manager.admin.enums.AdminRoutePointType;
import ru.yandex.market.delivery.transport_manager.admin.enums.AdminRouteScheduleStatus;
import ru.yandex.market.delivery.transport_manager.admin.enums.AdminRouteScheduleSubtype;
import ru.yandex.market.delivery.transport_manager.admin.enums.AdminRouteScheduleType;
import ru.yandex.market.delivery.transport_manager.admin.enums.AdminShipmentType;
import ru.yandex.market.delivery.transport_manager.admin.enums.AdminTaskState;
import ru.yandex.market.delivery.transport_manager.admin.enums.AdminTaskType;
import ru.yandex.market.delivery.transport_manager.admin.enums.AdminTransportationScheme;
import ru.yandex.market.delivery.transport_manager.admin.enums.AdminTransportationSearchMethod;
import ru.yandex.market.delivery.transport_manager.admin.enums.AdminTransportationStatus;
import ru.yandex.market.delivery.transport_manager.admin.enums.AdminTransportationSubstatus;
import ru.yandex.market.delivery.transport_manager.admin.enums.AdminTransportationSubtype;
import ru.yandex.market.delivery.transport_manager.admin.enums.AdminTransportationTaskStatus;
import ru.yandex.market.delivery.transport_manager.admin.enums.AdminTransportationType;
import ru.yandex.market.delivery.transport_manager.admin.enums.AdminTripStatus;
import ru.yandex.market.delivery.transport_manager.domain.dto.CalendaringStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.DistributionCenterUnitCargoType;
import ru.yandex.market.delivery.transport_manager.domain.entity.DistributionCenterUnitType;
import ru.yandex.market.delivery.transport_manager.domain.entity.MovementStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.OrderOperationTransportationType;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationByEntityMethod;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationScheme;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationSubstatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationTaskStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.courier.MovementCourier;
import ru.yandex.market.delivery.transport_manager.domain.entity.dbqueue.DbQueueTaskState;
import ru.yandex.market.delivery.transport_manager.domain.entity.logistic_point_feature.FeatureType;
import ru.yandex.market.delivery.transport_manager.domain.entity.route.RoutePointType;
import ru.yandex.market.delivery.transport_manager.domain.entity.route.RouteStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.route_schedule.RouteScheduleStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.route_schedule.RouteScheduleSubtype;
import ru.yandex.market.delivery.transport_manager.domain.entity.route_schedule.RouteScheduleType;
import ru.yandex.market.delivery.transport_manager.domain.entity.tag.TagCode;
import ru.yandex.market.delivery.transport_manager.domain.entity.trip.TripStatus;
import ru.yandex.market.delivery.transport_manager.domain.enums.ClientName;
import ru.yandex.market.delivery.transport_manager.domain.enums.ConfigTransportationType;
import ru.yandex.market.delivery.transport_manager.domain.enums.CountType;
import ru.yandex.market.delivery.transport_manager.domain.enums.IdType;
import ru.yandex.market.delivery.transport_manager.domain.enums.RegisterStatus;
import ru.yandex.market.delivery.transport_manager.domain.enums.RegisterType;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationSource;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationSubtype;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationType;
import ru.yandex.market.delivery.transport_manager.domain.enums.UnitType;
import ru.yandex.market.delivery.transport_manager.model.dto.MovementCourierDto;
import ru.yandex.market.delivery.transport_manager.model.dto.route.RouteStatusDto;
import ru.yandex.market.delivery.transport_manager.model.dto.route.schedule.RouteScheduleStatusDto;
import ru.yandex.market.delivery.transport_manager.model.dto.route.schedule.RouteScheduleSubtypeDto;
import ru.yandex.market.delivery.transport_manager.model.dto.route.schedule.RouteScheduleTypeDto;
import ru.yandex.market.delivery.transport_manager.model.enums.PartnerType;
import ru.yandex.market.delivery.transport_manager.queue.task.TaskType;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.logistic.api.model.common.MovementSubtype;
import ru.yandex.market.logistic.api.model.fulfillment.RegisterUnitType;
import ru.yandex.market.logistic.gateway.common.model.common.TripType;

public class EnumMapperTest {

    @ParameterizedTest
    @MethodSource("getAdminEnums")
    void adminEnumMapping(Class<? extends Enum<?>> e1, Class<? extends Enum<?>> e2) {
        Assertions.assertEquals(getConstants(e1), getConstants(e2));
    }

    @ParameterizedTest
    @MethodSource("getModelEnums")
    void modelEnumMapping(Class<? extends Enum<?>> e1, Class<? extends Enum<?>> e2) {
        Assertions.assertEquals(getConstants(e1), getConstants(e2));
    }

    @ParameterizedTest
    @MethodSource("containsTest")
    void enumMappingWithContains(Class<? extends Enum<?>> subset, Class<? extends Enum<?>> set) {
        Assertions.assertEquals(Collections.emptySet(), Sets.difference(getConstants(subset), getConstants(set)));
    }

    private static Set<String> getConstants(Class<? extends Enum<?>> e1) {
        return Arrays.stream(e1.getEnumConstants()).map(Enum::name).collect(Collectors.toSet());
    }

    private static Stream<Arguments> getAdminEnums() {
        return Stream.of(
            Arguments.of(
                TransportationTaskStatus.class,
                AdminTransportationTaskStatus.class
            ),
            Arguments.of(
                TransportationStatus.class,
                AdminTransportationStatus.class
            ),
            Arguments.of(
                RegisterStatus.class,
                AdminRegisterStatus.class
            ),
            Arguments.of(
                RegisterType.class,
                AdminRegisterType.class
            ),
            Arguments.of(
                TransportationScheme.class,
                AdminTransportationScheme.class
            ),
            Arguments.of(
                OrderOperationTransportationType.class,
                AdminShipmentType.class
            ),
            Arguments.of(
                ClientName.class,
                AdminClientName.class
            ),
            Arguments.of(
                TransportationSource.class,
                AdminMovementSource.class
            ),
            Arguments.of(
                TransportationType.class,
                AdminTransportationType.class
            ),
            Arguments.of(
                TransportationSubstatus.class,
                AdminTransportationSubstatus.class
            ),
            Arguments.of(
                TransportationByEntityMethod.class,
                AdminTransportationSearchMethod.class
            ),
            Arguments.of(
                TaskType.class,
                AdminTaskType.class
            ),
            Arguments.of(
                DbQueueTaskState.class,
                AdminTaskState.class
            ),
            Arguments.of(
                FeatureType.class,
                AdminLogPointFeatureType.class
            ),
            Arguments.of(
                CountType.class,
                AdminCountType.class
            ),
            Arguments.of(
                ru.yandex.market.delivery.transport_manager.model.enums.CountType.class,
                AdminCountType.class
            ),
            Arguments.of(
                RouteScheduleType.class,
                AdminRouteScheduleType.class
            ),
            Arguments.of(
                RouteScheduleSubtype.class,
                AdminRouteScheduleSubtype.class
            ),
            Arguments.of(
                RouteScheduleStatus.class,
                AdminRouteScheduleStatus.class
            ),
            Arguments.of(
                RoutePointType.class,
                AdminRoutePointType.class
            ),
            Arguments.of(
                TransportationSubtype.class,
                AdminTransportationSubtype.class
            ),
            Arguments.of(
                TripStatus.class,
                AdminTripStatus.class
            )
        );
    }

    private static Stream<Arguments> getModelEnums() {
        return Stream.of(
            Arguments.of(
                MovementStatus.class,
                ru.yandex.market.delivery.transport_manager.model.enums.MovementStatus.class
            ),
            Arguments.of(
                TransportationScheme.class,
                ru.yandex.market.delivery.transport_manager.model.enums.TransportationScheme.class
            ),
            Arguments.of(
                TransportationSource.class,
                ru.yandex.market.delivery.transport_manager.model.enums.TransportationSource.class
            ),
            Arguments.of(
                TransportationType.class,
                ru.yandex.market.delivery.transport_manager.model.enums.TransportationType.class
            ),
            Arguments.of(
                TransportationUnitStatus.class,
                ru.yandex.market.delivery.transport_manager.model.enums.TransportationUnitStatus.class
            ),
            Arguments.of(
                RegisterType.class,
                ru.yandex.market.delivery.transport_manager.model.enums.RegisterType.class
            ),
            Arguments.of(
                RegisterStatus.class,
                ru.yandex.market.delivery.transport_manager.model.enums.RegisterStatus.class
            ),
            Arguments.of(
                UnitType.class,
                ru.yandex.market.delivery.transport_manager.model.enums.UnitType.class
            ),
            Arguments.of(
                IdType.class,
                ru.yandex.market.delivery.transport_manager.model.enums.IdType.class
            ),
            Arguments.of(
                TransportationStatus.class,
                ru.yandex.market.delivery.transport_manager.model.enums.TransportationStatus.class
            ),
            Arguments.of(
                TagCode.class,
                ru.yandex.market.delivery.transport_manager.model.enums.TagCode.class
            ),
            Arguments.of(
                CalendaringStatus.class,
                RequestStatus.class
            ),
            Arguments.of(
                RouteStatus.class,
                RouteStatusDto.class
            ),
            Arguments.of(
                RouteScheduleType.class,
                RouteScheduleTypeDto.class
            ),
            Arguments.of(
                RouteScheduleSubtype.class,
                RouteScheduleSubtypeDto.class
            ),
            Arguments.of(
                RouteScheduleStatus.class,
                RouteScheduleStatusDto.class
            ),
            Arguments.of(
                ru.yandex.market.delivery.transport_manager.domain.entity.PartnerType.class,
                PartnerType.class
            ),
            Arguments.of(
                    MovementCourier.Unit.class,
                    MovementCourierDto.Unit.class
            ),
            Arguments.of(
                UnitCargoType.class,
                DistributionCenterUnitCargoType.class
            ),
            Arguments.of(
                ru.yandex.market.delivery.transport_manager.dto.distribution_center.units.UnitCargoType.class,
                DistributionCenterUnitCargoType.class
            ),
            Arguments.of(
                UnitCargoType.class,
                ru.yandex.market.delivery.transport_manager.dto.distribution_center.units.UnitCargoType.class
            ),
            Arguments.of(
                TransportationSubtype.class,
                ru.yandex.market.delivery.transport_manager.model.enums.TransportationSubtype.class
            )
        );
    }

    private static Stream<Arguments> containsTest() {
        return Stream.of(
            Arguments.of(
                ConfigTransportationType.class,
                TransportationType.class
            ),
            Arguments.of(
                DistributionCenterUnitType.class,
                RegisterUnitType.class
            ),
            Arguments.of(
                ru.yandex.market.logistics.lom.model.enums.PartnerType.class,
                PartnerType.class
            ),
            Arguments.of(
                ru.yandex.market.logistic.gateway.common.model.common.PartialIdType.class,
                IdType.class
            ),
            Arguments.of(
                RouteScheduleSubtype.class,
                TransportationSubtype.class
            ),
            Arguments.of(
                TransportationSubtype.class,
                TripType.class
            ),
            Arguments.of(
                TransportationSubtype.class,
                MovementSubtype.class
            ),
            Arguments.of(
                ru.yandex.market.logistic.gateway.common.model.common.MovementSubtype.class,
                MovementSubtype.class
            )
        );
    }
}
