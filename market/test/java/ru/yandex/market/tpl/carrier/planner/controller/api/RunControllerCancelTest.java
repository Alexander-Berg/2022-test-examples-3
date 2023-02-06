package ru.yandex.market.tpl.carrier.planner.controller.api;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.tpl.carrier.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.carrier.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.carrier.core.domain.duty.DutyGenerator;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementType;
import ru.yandex.market.tpl.carrier.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.Transport;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftStatus;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;
import ru.yandex.mj.generated.server.model.RunCancelStatusDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_=@Autowired)
public class RunControllerCancelTest extends BasePlannerWebTest {

    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final RunGenerator runGenerator;
    private final DutyGenerator dutyGenerator;
    private final RunHelper runHelper;
    private final TestUserHelper testUserHelper;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final UserShiftRepository userShiftRepository;

    private Run run;
    private User user;
    private Transport transport;

    private OrderWarehouse warehouseFrom1;
    private OrderWarehouse warehouseTo1;

    @BeforeEach
    void setUp() {
        configurationServiceAdapter.mergeValue(
                ConfigurationProperties.MILLIS_TO_ALLOW_ACTUAL_ARRIVAL_TIME_EDIT_AFTER_EXPECTED_TIME, 600000); //10 mins

        warehouseFrom1 = orderWarehouseGenerator.generateWarehouse();
        warehouseTo1 = orderWarehouseGenerator.generateWarehouse();

        user = testUserHelper.findOrCreateUser(UID);
        transport = testUserHelper.findOrCreateTransport();
    }


    @SneakyThrows
    @ParameterizedTest
    @EnumSource(RunCancelStatusDto.class)
    void shouldCancelFromCreated(RunCancelStatusDto statusDto) {
        run = runGenerator.generate((rgp) -> rgp
                .clearItems()
                .item(new RunGenerator.RunItemGenerateParam(
                        MovementCommand.Create.builder()
                                .orderWarehouse(warehouseFrom1)
                                .orderWarehouseTo(warehouseTo1)
                                .type(MovementType.LINEHAUL)
                                .build(),
                        1,
                        null,
                        null
                ))
            );
        cancelForActions(statusDto)
                .andExpect(status().isOk());
    }

    @SneakyThrows
    @ParameterizedTest
    @EnumSource(RunCancelStatusDto.class)
    void shouldCancelFromConfirmed(RunCancelStatusDto statusDto) {
        run = runGenerator.generate((rgp) -> rgp
                .clearItems()
                .item(new RunGenerator.RunItemGenerateParam(
                        MovementCommand.Create.builder()
                                .orderWarehouse(warehouseFrom1)
                                .orderWarehouseTo(warehouseTo1)
                                .type(MovementType.LINEHAUL)
                                .build(),
                        1,
                        null,
                        null
                ))
        );
        run = runHelper.confirm(run);
        cancelForActions(statusDto)
                .andExpect(status().isOk());
    }

    @SneakyThrows
    @ParameterizedTest
    @EnumSource(RunCancelStatusDto.class)
    void shouldCancelFromAssigned(RunCancelStatusDto statusDto) {
        run = runGenerator.generate((rgp) -> rgp
                .clearItems()
                .item(new RunGenerator.RunItemGenerateParam(
                        MovementCommand.Create.builder()
                                .orderWarehouse(warehouseFrom1)
                                .orderWarehouseTo(warehouseTo1)
                                .type(MovementType.LINEHAUL)
                                .build(),
                        1,
                        null,
                        null
                ))
        );
        run = runHelper.confirm(run);
        UserShift userShift = runHelper.assignUserAndTransport(run, user, transport);
        cancelForActions(statusDto)
                .andExpect(status().isOk());
        userShift = userShiftRepository.findByIdOrThrow(userShift.getId());
        Assertions.assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_FINISHED);
    }

    @SneakyThrows
    @ParameterizedTest
    @EnumSource(RunCancelStatusDto.class)
    void shouldCancelFromStartedButNoPointsVisited(RunCancelStatusDto statusDto) {
        run = runGenerator.generate((rgp) -> rgp
                .clearItems()
                .item(new RunGenerator.RunItemGenerateParam(
                        MovementCommand.Create.builder()
                                .orderWarehouse(warehouseFrom1)
                                .orderWarehouseTo(warehouseTo1)
                                .type(MovementType.LINEHAUL)
                                .build(),
                        1,
                        null,
                        null
                ))
        );
        run = runHelper.confirm(run);
        UserShift userShift = runHelper.assignUserAndTransport(run, user, transport);
        testUserHelper.openShift(user, userShift.getId());
        cancelForActions(statusDto)
                .andExpect(status().isOk());
        userShift = userShiftRepository.findByIdOrThrow(userShift.getId());
        Assertions.assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_FINISHED);
    }

    @SneakyThrows
    @ParameterizedTest
    @EnumSource(RunCancelStatusDto.class)
    void shouldNotCancelFromStartedAndPointVisited(RunCancelStatusDto statusDto) {
        run = runGenerator.generate((rgp) -> rgp
                .clearItems()
                .item(new RunGenerator.RunItemGenerateParam(
                        MovementCommand.Create.builder()
                                .orderWarehouse(warehouseFrom1)
                                .orderWarehouseTo(warehouseTo1)
                                .type(MovementType.LINEHAUL)
                                .build(),
                        1,
                        null,
                        null
                ))
        );
        run = runHelper.confirm(run);
        UserShift userShift = runHelper.assignUserAndTransport(run, user, transport);
        testUserHelper.openShift(user, userShift.getId());
        testUserHelper.finishCollectDropships(userShift.streamRoutePoints().iterator().next());
        cancelForActions(statusDto)
                .andExpect(status().isBadRequest());
    }

    @SneakyThrows
    @ParameterizedTest
    @EnumSource(RunCancelStatusDto.class)
    void shouldBeAbleToCancelDutyRun(RunCancelStatusDto statusDto) {
        var duty = dutyGenerator.generate(db -> db.deliveryServiceId(DeliveryService.DEFAULT_DS_ID).name("test_duty"));
        var runDuty = duty.getRunDuty().get(0);
        run = runDuty.getRun();
        cancelForActions(statusDto)
                .andExpect(status().isOk());
    }

    @SneakyThrows
    @ParameterizedTest
    @EnumSource(RunCancelStatusDto.class)
    void shouldBeAbleToCancelConfirmedDutyRun(RunCancelStatusDto statusDto) {
        var duty = dutyGenerator.generate(db -> db.deliveryServiceId(DeliveryService.DEFAULT_DS_ID).name("test_duty"));
        var runDuty = duty.getRunDuty().get(0);
        run = runDuty.getRun();
        run = runHelper.confirm(run);
        cancelForActions(statusDto)
                .andExpect(status().isOk());
    }

    @SneakyThrows
    @ParameterizedTest
    @EnumSource(RunCancelStatusDto.class)
    void shouldBeAbleToCancelAssignedDutyRun(RunCancelStatusDto statusDto) {
        var duty = dutyGenerator.generate(db -> db.deliveryServiceId(DeliveryService.DEFAULT_DS_ID).name("test_duty"));
        var runDuty = duty.getRunDuty().get(0);
        run = runDuty.getRun();
        run = runHelper.confirm(run);
        runHelper.assignUserAndTransport(run, user, transport);
        cancelForActions(statusDto)
                .andExpect(status().isOk());
    }

    @SneakyThrows
    @ParameterizedTest
    @EnumSource(RunCancelStatusDto.class)
    void shouldBeAbleToCancelStartedDutyRun(RunCancelStatusDto statusDto) {
        var duty = dutyGenerator.generate(db -> db.deliveryServiceId(DeliveryService.DEFAULT_DS_ID).name("test_duty"));
        var runDuty = duty.getRunDuty().get(0);
        run = runDuty.getRun();
        run = runHelper.confirm(run);
        var userShift = runHelper.assignUserAndTransport(run, user, transport);
        testUserHelper.openShift(user, userShift.getId());
        cancelForActions(statusDto)
                .andExpect(status().isOk());
    }

    @SneakyThrows
    @ParameterizedTest
    @EnumSource(RunCancelStatusDto.class)
    void shouldBeAbleToCancelDutyRunOnTask(RunCancelStatusDto statusDto) {
        var duty = dutyGenerator.generate(db -> db.deliveryServiceId(DeliveryService.DEFAULT_DS_ID).name("test_duty"));
        var runDuty = duty.getRunDuty().get(0);
        run = runDuty.getRun();
        run = runHelper.confirm(run);
        var userShift = runHelper.assignUserAndTransport(run, user, transport);
        testUserHelper.openShift(user, userShift.getId());
        testUserHelper.arriveAtRoutePoint(userShift.streamRoutePoints().findFirst().get());
        cancelForActions(statusDto)
                .andExpect(status().isOk());
    }


    private ResultActions cancelForActions(RunCancelStatusDto statusDto) throws Exception {
        return mockMvc.perform(post("/internal/runs/{id}/cancel", run.getId())
                .param("status", statusDto.name()));
    }
}
