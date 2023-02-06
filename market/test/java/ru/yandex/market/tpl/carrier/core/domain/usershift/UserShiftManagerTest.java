package ru.yandex.market.tpl.carrier.core.domain.usershift;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.carrier.core.CoreTestV2;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.Transport;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.common.util.DateTimeUtil;

@RequiredArgsConstructor(onConstructor_=@Autowired)

@CoreTestV2
class UserShiftManagerTest {

    private final TestUserHelper testUserHelper;
    private final RunGenerator runGenerator;
    private final RunHelper runHelper;
    private final Clock clock;
    private final UserShiftRepository userShiftRepository;
    private final UserShiftManager userShiftManager;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final TransactionTemplate transactionTemplate;

    private User user;
    private Transport transport;

    private Run run1;
    private Run run2;

    @BeforeEach
    void setUp() {
        user = testUserHelper.findOrCreateUser(1L);
        transport = testUserHelper.findOrCreateTransport();

        run1 = runGenerator.generate(rgp -> rgp
                .clearItems()
                .item(RunGenerator.RunItemGenerateParam.builder()
                .movement(MovementCommand.Create.builder()
                    .orderWarehouseTo(orderWarehouseGenerator.generateWarehouse())
                    .deliveryIntervalFrom(ZonedDateTime.of(LocalDate.now(clock), LocalTime.of(0, 30), DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                    .build())
                .build()));

        run2 = runGenerator.generate(rgp -> rgp
                .clearItems()
                .item(RunGenerator.RunItemGenerateParam.builder()
                .movement(MovementCommand.Create.builder()
                        .orderWarehouseTo(orderWarehouseGenerator.generateWarehouse())
                        .deliveryIntervalFrom(ZonedDateTime.of(LocalDate.now(clock), LocalTime.of(9, 0), DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                        .build())
                .build()));
    }

    @Test
    void shouldSwitchShiftOnShiftClosed() {
        UserShift userShift = runHelper.assignUserAndTransport(run2, user, transport);

        testUserHelper.openShift(user, userShift.getId());

        UserShift userShift2 = runHelper.assignUserAndTransport(run1, user, transport);
        long userShift2Id = userShift2.getId();

        Assertions.assertThat(userShiftRepository.findCurrentUserShift(user).orElseThrow())
                .isEqualTo(userShift);

        transactionTemplate.execute(tc -> {
            userShiftManager.switchActiveUserShift(user, userShift2Id);
            return null;
        });

        Assertions.assertThat(userShiftRepository.findCurrentUserShift(user).orElseThrow())
                .isEqualTo(userShift2);

        testUserHelper.openShift(user, userShift2.getId());
        testUserHelper.finishCollectDropships(userShift2.streamCollectDropshipTasks().findFirst().orElseThrow().getRoutePoint());
        testUserHelper.finishFullReturnAtEnd(userShift2);

        userShift2 = userShiftRepository.findByIdOrThrow(userShift2.getId());
        Assertions.assertThat(userShift2.getStatus()).isEqualTo(UserShiftStatus.SHIFT_FINISHED);
        Assertions.assertThat(userShift2.isActive()).isFalse();

        userShift = userShiftRepository.findByIdOrThrow(userShift.getId());
        Assertions.assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_CREATED);
        Assertions.assertThat(userShift2.isActive()).isFalse();

    }

}
