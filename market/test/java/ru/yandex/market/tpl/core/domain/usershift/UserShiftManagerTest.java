package ru.yandex.market.tpl.core.domain.usershift;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.shift.UserShiftStatus;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliverySubtaskStatus;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturn;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnStatus;
import ru.yandex.market.tpl.core.domain.clientreturn.CreatedSource;
import ru.yandex.market.tpl.core.domain.clientreturn.repository.ClientReturnRepository;
import ru.yandex.market.tpl.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.shift.UserShiftTestHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.additional_data.UserShiftAdditionalDataRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.tracking.TrackingRepository;
import ru.yandex.market.tpl.core.query.usershift.UserShiftQueryService;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class UserShiftManagerTest extends TplAbstractTest {
    private final TestUserHelper testUserHelper;
    private final TestDataFactory testDataFactory;
    private final Clock clock;
    private final UserShiftCommandService commandService;
    private final UserShiftRepository userShiftRepository;
    private final UserShiftManager userShiftManager;
    private final TransactionTemplate transactionTemplate;
    private final PickupPointRepository pickupPointRepository;
    private final ClientReturnRepository clientReturnRepository;
    private final UserShiftCommandService userShiftCommandService;
    private final UserShiftQueryService userShiftQueryService;
    private final TrackingRepository trackingRepository;
    private final UserShiftAdditionalDataRepository userShiftAdditionalDataRepository;
    private final ClientReturnGenerator clientReturnGenerator;
    private final UserShiftCommandDataHelper userShiftCommandDataHelper;
    private final UserShiftTestHelper userShiftTestHelper;

    @Test
    void closeUserShiftWithDeliveryTaskAndDropshipTask() {
        User user = testUserHelper.findOrCreateUser(902934450L);
        UserShift userShift = testUserHelper.createEmptyShift(user, LocalDate.now(clock));

        LockerDeliveryTask deliveryTask = testDataFactory.createLockerDeliveryTask(user, userShift.getId(), 3457L,
                DeliveryService.DEFAULT_DS_ID);
        CollectDropshipTask collectDropshipTask = testDataFactory.createCollectDropshipTask(user, userShift.getId());
        LockerDeliveryTask deliveryTask2 = testDataFactory.createLockerDeliveryTask(user, userShift.getId(), 3458L,
                DeliveryService.DEFAULT_DS_ID);

        long userShiftId = userShift.getId();
        commandService.checkin(user, new UserShiftCommand.CheckIn(userShiftId));
        commandService.startShift(user, new UserShiftCommand.Start(userShiftId));
        testUserHelper.finishPickupAtStartOfTheDay(userShift);
        userShift = userShiftRepository.findById(userShiftId).orElseThrow();

        //Проверяем перед закрытием смены, что задания еще не выполнены и смена открыта
        assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.ON_TASK);
        assertThat(deliveryTask.isInTerminalStatus()).isFalse();
        assertThat(collectDropshipTask.isInTerminalStatus()).isFalse();
        assertThat(deliveryTask2.isInTerminalStatus()).isFalse();

        //Тестируемый метод
        userShiftManager.finishTasksAndCloseShift(userShiftId, "TEST", Source.SYSTEM);

        //Проверяем, что все задания отменены и смена закрыта
        transactionTemplate.execute(status -> {
            UserShift updateUserShift = userShiftRepository.findById(userShiftId).orElseThrow();
            List<CollectDropshipTask> collectDropshipTaskList = updateUserShift.streamCollectDropshipTasks()
                    .collect(Collectors.toList());
            List<LockerDeliveryTask> lockerDeliveryTaskList = updateUserShift.streamLockerDeliveryTasks()
                    .collect(Collectors.toList());

            assertThat(collectDropshipTaskList.size()).isEqualTo(1);
            assertThat(lockerDeliveryTaskList.size()).isEqualTo(2);
            collectDropshipTaskList.forEach(dropshipTask -> assertThat(dropshipTask.isInTerminalStatus()).isTrue());
            collectDropshipTaskList.forEach(locker -> assertThat(locker.isInTerminalStatus()).isTrue());
            assertThat(updateUserShift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_CLOSED);
            return null;
        });
    }

    @Test
    void closeUserShiftWithLockerDeliveryTaskAndClientReturn() {
        User user = testUserHelper.findOrCreateUser(902934450L);
        UserShift userShift = testUserHelper.createEmptyShift(user, LocalDate.now(clock));

        LockerDeliveryTask deliveryTask = testDataFactory.createLockerDeliveryTask(user, userShift.getId(), 3457L,
                DeliveryService.DEFAULT_DS_ID);


        var clientReturn = transactionTemplate.execute(ts -> {
            var cr = clientReturnGenerator.generate();
            cr.setExternalReturnId("return_id");
            cr.setStatus(ClientReturnStatus.RECEIVED);
            cr.setBarcode("barcode");
            cr.setCreatedSource(CreatedSource.PVZ);
            cr.setPickupPoint(pickupPointRepository.findByIdOrThrow(deliveryTask.getPickupPointId()));
            return clientReturnRepository.save(cr);
        });


        userShiftCommandService.createPickupSubtaskClientReturn(
                null,
                new UserShiftCommand.CreatePickupSubtaskClientReturn(
                        userShift.getId(),
                        deliveryTask.getRoutePoint().getId(),
                        deliveryTask.getId(),
                        clientReturn.getId(),
                        LockerDeliverySubtaskStatus.FINISHED,
                        Source.SYSTEM
                )
        );

        long userShiftId = userShift.getId();
        commandService.checkin(user, new UserShiftCommand.CheckIn(userShiftId));
        commandService.startShift(user, new UserShiftCommand.Start(userShiftId));
        testUserHelper.finishPickupAtStartOfTheDay(userShift);

        testUserHelper.finishDelivery(deliveryTask.getRoutePoint(), null, null, false);

        //Тестируемый метод
        userShiftManager.finishTasksAndCloseShift(userShiftId, "TEST", Source.SYSTEM);
        commandService.finishUserShift(new UserShiftCommand.Finish(userShiftId));

        //Проверяем, что все задания отменены и смена закрыта и клиентский возврат переведен в статус доставлен на СЦ
        transactionTemplate.execute(status -> {
            UserShift updateUserShift = userShiftRepository.findById(userShiftId).orElseThrow();
            List<LockerDeliveryTask> lockerDeliveryTaskList = updateUserShift.streamLockerDeliveryTasks()
                    .collect(Collectors.toList());

            ClientReturn foundClientReturn = clientReturnRepository.findByIdOrThrow(clientReturn.getId());

            assertThat(lockerDeliveryTaskList.size()).isEqualTo(1);
            assertThat(updateUserShift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_FINISHED);
            assertThat(foundClientReturn.getStatus()).isEqualTo(ClientReturnStatus.DELIVERED_TO_SC);
            return null;
        });
    }

    @Test
    void closeUserShiftWitClientReturn() {
        var user = testUserHelper.findOrCreateUser(902934450L);
        var shift = testUserHelper.findOrCreateOpenShift(LocalDate.now(clock));
        var clientReturn = clientReturnGenerator.generateReturnFromClient();
        var command = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(userShiftCommandDataHelper.clientReturn("addrRet", 14, clientReturn.getId()))
                .build();

        var userShiftId = userShiftTestHelper.start(command);
        var userShift = userShiftRepository.findByIdOrThrow(userShiftId);
        var task = transactionTemplate.execute(
                cmd -> userShiftRepository.findByIdOrThrow(userShiftId).streamOrderDeliveryTasks()
                        .filter(OrderDeliveryTask::isClientReturn)
                        .findFirst()
                        .orElseThrow()
        );

        commandService.checkin(user, new UserShiftCommand.CheckIn(userShiftId));
        commandService.startShift(user, new UserShiftCommand.Start(userShiftId));
        testUserHelper.finishPickupAtStartOfTheDay(userShift);

        testUserHelper.finishDelivery(task.getRoutePoint(), null, null, false);

        //Тестируемый метод
        userShiftManager.finishTasksAndCloseShift(userShiftId, "TEST", Source.SYSTEM);
        commandService.finishUserShift(new UserShiftCommand.Finish(userShiftId));

        //Проверяем, что все задания отменены и смена закрыта и клиентский возврат переведен в статус доставлен на СЦ
        transactionTemplate.executeWithoutResult(status -> {
            var updateUserShift = userShiftRepository.findById(userShiftId).orElseThrow();
            var foundClientReturn = clientReturnRepository.findByIdOrThrow(clientReturn.getId());

            assertThat(updateUserShift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_FINISHED);
            assertThat(foundClientReturn.getStatus()).isEqualTo(ClientReturnStatus.DELIVERED_TO_SC);
        });
    }

    @Test
    void testUserShiftDeleted() {
        User user = testUserHelper.findOrCreateUser(902934450L);
        UserShift userShift = testUserHelper.createEmptyShift(user, LocalDate.now(clock));

        testDataFactory.createLockerDeliveryTask(user, userShift.getId(), 3457L,
                DeliveryService.DEFAULT_DS_ID);
        testDataFactory.createCollectDropshipTask(user, userShift.getId());
        testDataFactory.createLockerDeliveryTask(user, userShift.getId(), 3458L,
                DeliveryService.DEFAULT_DS_ID);

        long userShiftId = userShift.getId();
        transactionTemplate.execute(s -> {
            userShiftManager.removeUserShift(userShiftRepository.findByIdOrThrow(userShiftId));
            return null;
        });

        transactionTemplate.executeWithoutResult(
                cmd -> {
                    assertThat(userShiftRepository.findById(userShiftId)).isEmpty();
                    assertThat(trackingRepository.findByUserShiftId(userShiftId)).isEmpty();
                    assertThat(userShiftAdditionalDataRepository.findByUserShiftId(userShiftId)).isEmpty();
                }
        );
    }
}
