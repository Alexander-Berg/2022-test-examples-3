package ru.yandex.market.tpl.tms.service.external.sqs.mapper;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.domain.Pageable;

import ru.yandex.market.logistics.les.tm.ExternalMovementEvent;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.core.domain.ds.DsMovementManager;
import ru.yandex.market.tpl.core.domain.movement.Movement;
import ru.yandex.market.tpl.core.domain.movement.event.history.MovementHistoryEvent;
import ru.yandex.market.tpl.core.domain.movement.event.history.MovementHistoryEventRepository;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.factory.TestRoutePointFactory;
import ru.yandex.market.tpl.tms.test.TplTmsAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
class MovementStatusHistoryMapperTest extends TplTmsAbstractTest {

    private final TestUserHelper userHelper;
    private final TestDataFactory testDataFactory;
    private final TestRoutePointFactory testRoutePointFactory;
    private final PickupPointRepository pickupPointRepository;
    private final Clock clock;

    private final UserShiftCommandService userShiftCommandService;
    private final DsMovementManager dsMovementManager;
    private final MovementStatusHistoryMapper movementStatusHistoryMapper;
    private final MovementHistoryEventRepository movementHistoryEventRepository;

    private PickupPoint pickupPoint;
    private Movement movementDirectNew;
    private Movement movementDirectOld;
    private Movement movementReturn;
    private Long userShiftId;

    private User user;

    @BeforeEach
    void setUp() {
        pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.PVZ, 100500L, 1L)
        );

        clearAfterTest(pickupPoint);

        user = userHelper.findOrCreateUser(824125L, LocalDate.now(clock));

        Shift shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));

        movementDirectNew = testDataFactory.buildDropOffDirectMovement(pickupPoint.getLogisticPointId().toString());

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .active(true)
                .build();

        userShiftId = userShiftCommandService.createUserShift(createCommand);

        //generate Direct task (new verion)
        userShiftCommandService.addDeliveryTask(
                null,
                new UserShiftCommand.AddDeliveryTask(
                        userShiftId,
                        testRoutePointFactory.buildDropoffDirectRoutePointData(movementDirectNew, pickupPoint.getId()),
                        SimpleStrategies.BY_DATE_INTERVAL_MERGE
                )
        );

        //generate Return task
        movementReturn = testDataFactory.buildDropOffReturnMovement(pickupPoint.getLogisticPointId().toString());
        userShiftCommandService.addDeliveryTask(
                null,
                new UserShiftCommand.AddDeliveryTask(
                        userShiftId,
                        testRoutePointFactory.buildDropoffReturnRoutePointData(movementReturn, pickupPoint.getId()),
                        SimpleStrategies.BY_DATE_INTERVAL_MERGE
                )
        );

        //Generate Collect Dropship task (old version)...
        movementDirectOld = testDataFactory.buildDropOffDirectMovement(pickupPoint.getLogisticPointId().toString());
        userShiftCommandService.addCollectDropshipTask(
                null,
                new UserShiftCommand.AddCollectDropshipTask(
                        userShiftId,
                        testRoutePointFactory.buildCollectDropshipRoutePointData(movementDirectOld)
                )
        );
    }

    @ParameterizedTest
    @MethodSource("sourceMovementTypes")
    void map_withAssignedCourier(RequestMovmenetType requestMovmenetType) {
        //given
        var requestMovement = resolveRequestMovement(requestMovmenetType);
        MovementHistoryEvent event = movementHistoryEventRepository.findByMovementId(requestMovement.getId(),
                        Pageable.unpaged()).stream()
                .findFirst().orElseThrow();

        //when
        Optional<ExternalMovementEvent> externalMovementEventO = movementStatusHistoryMapper.map(event);
        //then
        assertThat(externalMovementEventO).isNotEmpty();
        var courier = externalMovementEventO.get().getCourierData();
        assertThat(courier).isNotNull();
        assertThat(courier.getName()).isEqualTo(user.getFirstName());
        assertThat(courier.getSurname()).isEqualTo(user.getLastName());
    }

    private Movement resolveRequestMovement(RequestMovmenetType requestMovmenetType) {
        Movement requestMovement;
        switch (requestMovmenetType) {
            case DIRECT_NEW:
                requestMovement = movementDirectNew;
                break;
            case DIRECT_OLD:
                requestMovement = movementDirectOld;
                break;
            case RETURN:
                requestMovement = movementReturn;
                break;
            default:
                throw new RuntimeException("Fix this test");
        }
        return requestMovement;
    }

    enum RequestMovmenetType {
        DIRECT_NEW,
        DIRECT_OLD,
        RETURN
    }

    public static Stream<Arguments> sourceMovementTypes() {
        return Arrays.stream(RequestMovmenetType.values()).map(Arguments::of);
    }
}
