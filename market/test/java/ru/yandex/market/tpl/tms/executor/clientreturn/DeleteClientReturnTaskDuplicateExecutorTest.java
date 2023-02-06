package ru.yandex.market.tpl.tms.executor.clientreturn;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturn;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.shift.UserShiftTestHelper;
import ru.yandex.market.tpl.core.domain.task.TaskOrderDeliveryRepository;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.tms.test.TplTmsAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType.CLIENT_RETURN_ALREADY_DELIVERED;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus.DELIVERY_FAILED;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.ENABLED_DELETE_CLIENT_RETURN_TASK_DUPLICATE_EXECUTOR;
import static ru.yandex.market.tpl.core.domain.partner.DeliveryService.FAKE_DS_ID;

@RequiredArgsConstructor
public class DeleteClientReturnTaskDuplicateExecutorTest extends TplTmsAbstractTest {

    private final DeleteClientReturnTaskDuplicateExecutor executor;
    private final ClientReturnGenerator clientReturnGenerator;
    private final TaskOrderDeliveryRepository taskOrderDeliveryRepository;
    private final UserShiftRepository userShiftRepository;
    private final TestUserHelper testUserHelper;
    private final UserShiftCommandDataHelper helper;
    private final UserShiftTestHelper userShiftTestHelper;
    private final TransactionTemplate transactionTemplate;
    private final ConfigurationProviderAdapter configurationProviderAdapter;

    @SneakyThrows
    @Test
    void cancelIfAlreadyDeliveredTest() {
        Mockito.when(
                configurationProviderAdapter.isBooleanEnabled(ENABLED_DELETE_CLIENT_RETURN_TASK_DUPLICATE_EXECUTOR))
                .thenReturn(true);

        LocalDate date = LocalDate.now();
        User user = testUserHelper.findOrCreateUser(1L);
        ClientReturn clientReturn = clientReturnGenerator.generateReturnFromClient(
                LocalDateTime.of(date, LocalTime.of(9, 0)),
                LocalDateTime.of(date, LocalTime.of(12, 0)),
                FAKE_DS_ID
        );

        Shift shift = testUserHelper.findOrCreateOpenShift(LocalDate.now());
        Shift shift2 = testUserHelper.findOrCreateOpenShift(LocalDate.now().plusDays(1));

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .active(true)
                .shiftId(shift.getId())
                .routePoint(helper.clientReturn("adr4", 14, clientReturn.getId()))
                .mergeStrategy(SimpleStrategies.BY_DATE_INTERVAL_MERGE)
                .build();

        var createCommand2 = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .active(true)
                .shiftId(shift2.getId())
                .routePoint(helper.clientReturn("adr4", 14, clientReturn.getId()))
                .mergeStrategy(SimpleStrategies.BY_DATE_INTERVAL_MERGE)
                .build();

        long userShiftId = userShiftTestHelper.start(createCommand);
        transactionTemplate.execute(state -> {
                    UserShift userShift = userShiftRepository.findById(userShiftId).orElseThrow();
                    testUserHelper.finishPickupAtStartOfTheDay(userShift, true);
                    testUserHelper.finishAllDeliveryTasks(userShift);
                    testUserHelper.finishFullReturnAtEnd(userShift);
                    testUserHelper.finishUserShift(userShift);
                    return state;
                }
        );
        userShiftTestHelper.start(createCommand2);
        executor.doRealJob(null);
        OrderDeliveryTask orderDeliveryTask = taskOrderDeliveryRepository.findAll().get(1);
        assertThat(orderDeliveryTask.getStatus()).isEqualTo(DELIVERY_FAILED);
        assertNotNull(orderDeliveryTask.getFailReason());
        assertThat(orderDeliveryTask.getFailReason().getType()).isEqualTo(CLIENT_RETURN_ALREADY_DELIVERED);
        assertFalse(orderDeliveryTask.getFailReason().getType().isCancelLogisticRequestRequired());
    }
}
