package ru.yandex.market.tpl.carrier.core.movement;

import java.math.BigDecimal;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.carrier.core.CoreTestV2;
import ru.yandex.market.tpl.carrier.core.domain.movement.Movement;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommandService;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.run.RunRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.RunStatus;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.carrier.core.domain.usershift.RoutePointType;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftStatus;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseAddress;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.address.AddressGenerator;

@CoreTestV2
@RequiredArgsConstructor(onConstructor_=@Autowired)
public class MovementCancelMultipleItemsTest {

    private final RunGenerator runGenerator;
    private final RunHelper runHelper;
    private final TestUserHelper testUserHelper;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final AddressGenerator addressGenerator;
    private final MovementCommandService movementCommandService;
    private final UserShiftRepository userShiftRepository;
    private final RunRepository runRepository;
    private final TransactionTemplate transactionTemplate;

    private Run run;
    private Movement movement1;
    private Movement movement2;
    private static final String ADDRESS_1 = "ул. Пушкина, д. Колотушкина";
    private static final String ADDRESS_2 = "проспект Мира, д. Кефира";

    @BeforeEach
    void setUp() {
        run = runGenerator.generate(b ->
                b.clearItems()
                        .item(RunGenerator.RunItemGenerateParam.builder()
                                .movement(MovementCommand.Create.builder()
                                        .externalId("movement1")
                                        .orderWarehouse(orderWarehouseGenerator.generateWarehouse())
                                        .orderWarehouseTo(orderWarehouseGenerator.generateWarehouse())
                                        .build())
                                .fromIndex(0)
                                .toIndex(3)
                                .orderNumber(1)
                                .build())
                        .item(RunGenerator.RunItemGenerateParam.builder()
                                .movement(MovementCommand.Create.builder()
                                        .externalId("movement2")
                                        .orderWarehouse(orderWarehouseGenerator.generateWarehouse(
                                                w -> w.setAddress(new OrderWarehouseAddress(
                                                        ADDRESS_1,
                                                        "Россия",
                                                        "Москва",
                                                        "Москва",
                                                        "Москва",
                                                        "Пушкина",
                                                        "Колотушкина",
                                                        null,
                                                        null,
                                                        BigDecimal.TEN,
                                                        BigDecimal.TEN
                                                )))
                                        )
                                        .orderWarehouseTo(orderWarehouseGenerator.generateWarehouse(
                                                w -> w.setAddress(new OrderWarehouseAddress(
                                                        ADDRESS_2,
                                                        "Россия",
                                                        "Москва",
                                                        "Москва",
                                                        "Москва",
                                                        "Мира",
                                                        "Кефира",
                                                        null,
                                                        null,
                                                        BigDecimal.TEN,
                                                        BigDecimal.TEN
                                                )))
                                        )
                                        .build())
                                .fromIndex(1)
                                .toIndex(2)
                                .orderNumber(2)
                                .build())
        );

        movement1 = run.streamMovements().filterBy(Movement::getExternalId, "movement1").findAny().orElseThrow();
        movement2 = run.streamMovements().filterBy(Movement::getExternalId, "movement2").findAny().orElseThrow();
    }

    @Test
    void shouldCancelRunItemCorrectlyBeforeAssignment() {
        movementCommandService.cancel(movement1);

        var user = testUserHelper.findOrCreateUser(1L);
        var transport = testUserHelper.findOrCreateTransport();

        var userShiftId = runHelper.assignUserAndTransport(run, user, transport).getId();

        transactionTemplate.execute(tc -> {
            testUserHelper.openShift(user, userShiftId);

            var userShift = userShiftRepository.findByIdOrThrow(userShiftId);

            RoutePoint routePoint = userShift.getCurrentRoutePoint();
            Assertions.assertThat(routePoint).isNotNull();
            Assertions.assertThat(routePoint.getType()).isEqualTo(RoutePointType.COLLECT_DROPSHIP);
            Assertions.assertThat(routePoint.getAddressString()).isEqualTo(ADDRESS_1);

            testUserHelper.finishCollectDropships(routePoint);

            userShift = userShiftRepository.findByIdOrThrow(userShift.getId());

            routePoint = userShift.getCurrentRoutePoint();
            Assertions.assertThat(routePoint).isNotNull();
            Assertions.assertThat(routePoint.getType()).isEqualTo(RoutePointType.ORDER_RETURN);
            Assertions.assertThat(routePoint.getAddressString()).isEqualTo(ADDRESS_2);

            testUserHelper.finishFullReturnAtEnd(userShift);

            userShift = userShiftRepository.findByIdOrThrow(userShift.getId());
            Assertions.assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_FINISHED);
            run = runRepository.findByIdOrThrow(run.getId());
            Assertions.assertThat(run.getStatus()).isEqualTo(RunStatus.COMPLETED);
            return null;
        });
    }

    @Test
    void shouldCancelRunItemCorrectlyAfterAssignment() {

        var user = testUserHelper.findOrCreateUser(1L);
        var transport = testUserHelper.findOrCreateTransport();
        long userShiftId = runHelper.assignUserAndTransport(run, user, transport).getId();

        movementCommandService.cancel(movement1);

        transactionTemplate.execute(tc -> {
            testUserHelper.openShift(user, userShiftId);

            var userShift = userShiftRepository.findByIdOrThrow(userShiftId);

            RoutePoint routePoint = userShift.getCurrentRoutePoint();
            Assertions.assertThat(routePoint).isNotNull();
            Assertions.assertThat(routePoint.getType()).isEqualTo(RoutePointType.COLLECT_DROPSHIP);
            Assertions.assertThat(routePoint.getAddressString()).isEqualTo(ADDRESS_1);

            testUserHelper.finishCollectDropships(routePoint);

            userShift = userShiftRepository.findByIdOrThrow(userShift.getId());

            routePoint = userShift.getCurrentRoutePoint();
            Assertions.assertThat(routePoint).isNotNull();
            Assertions.assertThat(routePoint.getType()).isEqualTo(RoutePointType.ORDER_RETURN);
            Assertions.assertThat(routePoint.getAddressString()).isEqualTo(ADDRESS_2);

            testUserHelper.finishFullReturnAtEnd(userShift);

            userShift = userShiftRepository.findByIdOrThrow(userShift.getId());
            Assertions.assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_FINISHED);
            run = runRepository.findByIdOrThrow(run.getId());
            Assertions.assertThat(run.getStatus()).isEqualTo(RunStatus.COMPLETED);
            return null;
        });
    }

}
