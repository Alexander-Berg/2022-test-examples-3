package ru.yandex.market.tpl.carrier.core.domain.run;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.carrier.core.CoreTestV2;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.Transport;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.carrier.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.common.util.DateTimeUtil;

@CoreTestV2
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class RelayRunHappyPathTest {
    private final TestUserHelper testUserHelper;
    private final RunHelper runHelper;
    private final UserShiftCommandService userShiftCommandService;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final RunGenerator runGenerator;
    private final RunRepository runRepository;

    private final TransactionTemplate transactionTemplate;

    Company company;
    DeliveryService deliveryService;
    Run run1;
    Run run2;
    User user1;
    User user2;
    Transport transport1;
    Transport transport2;

    OrderWarehouse warehouseTo;


    @BeforeEach
    void setup() {

        company = testUserHelper.findOrCreateCompany("new_company");
        deliveryService = testUserHelper.deliveryService(10001L, Set.of(company));

        user1 = testUserHelper.findOrCreateUser(12L, company.getName());
        user2 = testUserHelper.findOrCreateUser(13L, company.getName(), "+79199199919");

        transport1 = testUserHelper.findOrCreateTransport("first_tr", company.getName());
        transport2 = testUserHelper.findOrCreateTransport("second_tr", company.getName());

        warehouseTo = orderWarehouseGenerator.generateWarehouse();

        run1 = runGenerator.generate(RunGenerator.RunGenerateParam.builder()
                .externalId("asd1")
                .deliveryServiceId(123L)
                .campaignId(company.getCampaignId())
                .runDate(LocalDate.of(2021, 8, 5))
                .item(new RunGenerator.RunItemGenerateParam(
                        MovementCommand.Create.builder()
                                .externalId("123")
                                .orderWarehouse(orderWarehouseGenerator.generateWarehouse())
                                .orderWarehouseTo(warehouseTo)
                                .deliveryIntervalFrom(ZonedDateTime.of(2021, 8, 5, 4, 0, 0, 0,
                                        DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                .deliveryIntervalTo(ZonedDateTime.of(2021, 8, 5, 5, 0, 0, 0,
                                        DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                .weight(new BigDecimal("1000"))
                                .build(),
                        1,
                        0,
                        1)
                )
                .build()
        );

        run2 = runGenerator.generate(RunGenerator.RunGenerateParam.builder()
                .externalId("asd2")
                .deliveryServiceId(123L)
                .campaignId(company.getCampaignId())
                .runDate(LocalDate.of(2021, 8, 5))
                .items(List.of(new RunGenerator.RunItemGenerateParam(
                                MovementCommand.Create.builder()
                                        .externalId("124_1")
                                        .orderWarehouse(orderWarehouseGenerator.generateWarehouse())
                                        .orderWarehouseTo(warehouseTo)
                                        .deliveryIntervalFrom(ZonedDateTime.of(2021, 8, 5, 4, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .deliveryIntervalTo(ZonedDateTime.of(2021, 8, 5, 5, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .weight(new BigDecimal("1000"))
                                        .build(),
                                1,
                                0,
                                2),
                        new RunGenerator.RunItemGenerateParam(
                                MovementCommand.Create.builder()
                                        .externalId("124_2")
                                        .orderWarehouse(orderWarehouseGenerator.generateWarehouse())
                                        .orderWarehouseTo(warehouseTo)
                                        .deliveryIntervalFrom(ZonedDateTime.of(2021, 8, 5, 7, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .deliveryIntervalTo(ZonedDateTime.of(2021, 8, 5, 8, 0, 0, 0,
                                                DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                        .weight(new BigDecimal("1000"))
                                        .build(),
                                2,
                                1,
                                3)

                ))
                .build()
        );

        runHelper.assignUserShifts(run1.getId(), List.of(
                new RunHelper.AssignUserShift(null, user1.getId(), transport1.getId(), 1, false),
                new RunHelper.AssignUserShift(null, user2.getId(), transport2.getId(), 1, true))
        );

        run1 = runRepository.findByIdOrThrow(run1.getId());
    }

    @Test
    @Transactional
    void simpleRelayRunTest() {
        var firstUS = run1.getFirstAssignedShift();
        var secondUS = run1.getLastAssignedShift();
        testUserHelper.openShift(user1, firstUS.getId());
        testUserHelper.finishCollectDropships(firstUS.getFirstRoutePoint());
        userShiftCommandService.finishDriverChangeTask(user1, new UserShiftCommand.FinishDriverChangeTask(
                firstUS.getId(), firstUS.getCurrentRoutePoint().getId(),
                firstUS.getCurrentRoutePoint().getDriverChangeTask().getId()
        ));

        testUserHelper.openShift(user2, secondUS.getId());
        userShiftCommandService.finishDriverChangeTask(user2, new UserShiftCommand.FinishDriverChangeTask(
                secondUS.getId(), secondUS.getFirstRoutePoint().getId(),
                secondUS.getFirstRoutePoint().getDriverChangeTask().getId()
        ));

        testUserHelper.finishFullReturnAtEnd(secondUS.getId());

    }

    @Test
    @Transactional
    void simpleRelayRunTestWithExtraArrive() {
        var firstUS = run1.getFirstAssignedShift();
        var secondUS = run1.getLastAssignedShift();
        testUserHelper.openShift(user1, firstUS.getId());
        testUserHelper.finishCollectDropships(firstUS.getFirstRoutePoint());

        testUserHelper.arriveAtRoutePoint(firstUS.getCurrentRoutePoint());
        userShiftCommandService.finishDriverChangeTask(user1, new UserShiftCommand.FinishDriverChangeTask(
                firstUS.getId(), firstUS.getCurrentRoutePoint().getId(),
                firstUS.getCurrentRoutePoint().getDriverChangeTask().getId()
        ));

        testUserHelper.openShift(user2, secondUS.getId());
        userShiftCommandService.finishDriverChangeTask(user2, new UserShiftCommand.FinishDriverChangeTask(
                secondUS.getId(), secondUS.getFirstRoutePoint().getId(),
                secondUS.getFirstRoutePoint().getDriverChangeTask().getId()
        ));

        testUserHelper.finishFullReturnAtEnd(secondUS.getId());

        run1 = runRepository.findByIdOrThrow(run1.getId());
        Assertions.assertThat(run1.isInTerminalStatus()).isTrue();

    }

    @Test
    @Transactional
    void testRunWithAssigningAfterPassingLastPoint() {
        runHelper.assignUserShifts(run2.getId(), List.of(
                new RunHelper.AssignUserShift(null, user1.getId(), transport1.getId(), 1, false)
        ));

        run2 = runRepository.findByIdOrThrow(run2.getId());

        var firstUS = run2.getFirstAssignedShift();

        testUserHelper.openShift(user1, firstUS.getId());
        testUserHelper.finishCollectDropships(firstUS.getFirstRoutePoint());
        testUserHelper.finishCollectDropships(firstUS.getCurrentRoutePoint());
        userShiftCommandService.finishDriverChangeTask(user1, new UserShiftCommand.FinishDriverChangeTask(
                firstUS.getId(), firstUS.getCurrentRoutePoint().getId(),
                firstUS.getCurrentRoutePoint().getDriverChangeTask().getId()
        ));

        Assertions.assertThat(run2.isInTerminalStatus()).isFalse();

        runHelper.assignUserShifts(run2.getId(), List.of(
                new RunHelper.AssignUserShift(null, user2.getId(), transport2.getId(), 2, true)
        ));

        run2 = runRepository.findByIdOrThrow(run2.getId());

        Assertions.assertThat(run2.getUserShifts()).hasSize(2);

        var secondUS = run2.getLastAssignedShift();

        testUserHelper.openShift(user2, secondUS.getId());
        userShiftCommandService.finishDriverChangeTask(user2, new UserShiftCommand.FinishDriverChangeTask(
                secondUS.getId(), secondUS.getFirstRoutePoint().getId(),
                secondUS.getFirstRoutePoint().getDriverChangeTask().getId()
        ));
        run2 = runRepository.findByIdOrThrow(run2.getId());
        Assertions.assertThat(run2.isInTerminalStatus()).isFalse();
    }
}
