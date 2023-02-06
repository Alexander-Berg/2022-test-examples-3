package ru.yandex.market.tpl.tms.executor.shift;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.shift.UserShiftStatus;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnStatus;
import ru.yandex.market.tpl.core.domain.clientreturn.repository.ClientReturnRepository;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.ScanRequest;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.tms.test.TplTmsAbstractTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.tpl.api.model.order.OrderPaymentType.CASH;

/**
 * @author sekulebyakin
 */
@RequiredArgsConstructor
public class FinishUserShiftExecutorTest extends TplTmsAbstractTest {

    private final FinishUserShiftExecutor executor;
    private final UserShiftCommandService userShiftCommandService;
    private final ClientReturnRepository clientReturnRepository;
    private final OrderGenerateService orderGenerateService;
    private final TestUserHelper testUserHelper;
    private final UserShiftCommandDataHelper helper;
    private final UserShiftRepository userShiftRepository;
    private final TestableClock clock;
    private final TransactionTemplate transactionTemplate;
    private final ClientReturnGenerator clientReturnGenerator;

    private final ZoneId offset = ZoneOffset.of("+03:00");
    private final Instant now = Instant.now().plus(Duration.ofDays(5));
    private final LocalDate shiftDate = LocalDate.ofInstant(now, offset);

    @Value("${tpl.core.delayToFinishUserShiftInMinutes:30}")
    private long delayToFinishUserShiftInMinutes;
    private UserShift userShift1;
    private UserShift userShift2;

    @BeforeEach
    void setup() {
        Mockito.reset(clock);
        var nowTime = Instant.now().plus(Duration.ofMinutes(delayToFinishUserShiftInMinutes + 10));
        clock.setFixed(nowTime, offset);
    }

    /**
     * Тест проверяет, что ошибка при попытке завершения одного юзер шифта не влияет на завершение других
     */
    @Test
    void finishShiftSeparateTransactionTest() {
        // Тестовые данные для смены. Два юзер шифта, первый их них с возвратом
        var testData = prepareTestShift();

        // при попытке завершения смены ожидаем получить ошибку, один юзер шифт должен завершиться успешно, другой нет
        assertThrows(RuntimeException.class, () -> executor.doRealJob(null));

        userShift1 = userShiftRepository.findByIdOrThrow(testData.getUserShift1Id());
        userShift2 = userShiftRepository.findByIdOrThrow(testData.getUserShift2Id());

        // проверяем что у первого юзер шифта статус не изменился - в нем была ошибка, второй завершился успешно
        // и записан в бд
        assertThat(userShift1.getStatus()).isEqualTo(UserShiftStatus.SHIFT_CLOSED);
        assertThat(userShift2.getStatus()).isEqualTo(UserShiftStatus.SHIFT_FINISHED);
    }


    private TestShiftData prepareTestShift() {
        return transactionTemplate.execute((status) -> {
            var shift = testUserHelper.findOrCreateOpenShiftForSc(shiftDate, TestUserHelper.TEST_SERVICE_CENTER_ID);
            var user1 = testUserHelper.findOrCreateUser(123L, shiftDate);
            var user2 = testUserHelper.findOrCreateUser(234L, shiftDate);
            clock.setFixed(now, offset);
            var order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                    .deliveryDate(LocalDate.now(clock))
                    .paymentStatus(OrderPaymentStatus.UNPAID)
                    .paymentType(CASH)
                    .build());

            userShift1 = userShiftRepository.findByIdOrThrow(
                    userShiftCommandService.createUserShift(UserShiftCommand.Create.builder()
                            .userId(user1.getId())
                            .shiftId(shift.getId())
                            .routePoint(helper.taskOrderPickup(now))
                            .routePoint(helper.taskUnpaid("addr", 14, order.getId()))
                            .mergeStrategy(SimpleStrategies.NO_MERGE)
                            .build()
                    ));

            // Заносим в БД возврат с некорректным для закрытия смены статусом для вызова ошибки в обработчике
            // ивента завершения смены
            var clientReturn = clientReturnGenerator.generate();
            clientReturn.setStatus(ClientReturnStatus.CANCELLED);
            clientReturn = clientReturnRepository.save(clientReturn);

            userShiftCommandService.checkin(userShift1.getUser(), new UserShiftCommand.CheckIn(userShift1.getId()));
            userShiftCommandService.startShift(user1, new UserShiftCommand.Start(userShift1.getId()));

            testUserHelper.finishPickupAtStartOfTheDay(userShift1, true);
            testUserHelper.finishDelivery(Objects.requireNonNull(userShift1.getCurrentRoutePoint()), true);

            var returnPoint = userShift1.streamReturnRoutePoints().findFirst().orElseThrow();

            userShiftCommandService.arriveAtRoutePoint(
                    user1,
                    new UserShiftCommand.ArriveAtRoutePoint(
                            userShift1.getId(),
                            returnPoint.getId(),
                            helper.getLocationDto(userShift1.getId())
                    )
            );

            var point = userShift1.getCurrentRoutePoint();
            userShiftCommandService.startOrderReturn(user1, new UserShiftCommand.StartScan(
                    userShift1.getId(), returnPoint.getId(), returnPoint.getOrderReturnTask().getId()));
            userShiftCommandService.finishReturnOrders(user1, new UserShiftCommand.FinishScan(
                    userShift1.getId(),
                    returnPoint.getId(),
                    returnPoint.getOrderReturnTask().getId(),
                    ScanRequest.builder()
                            .successfullyScannedClientReturns(List.of(clientReturn.getId()))
                            .finishedAt(Instant.now(clock))
                            .build()
            ));
            userShiftCommandService.finishReturnTask(user1, new UserShiftCommand.FinishReturnTask(
                    userShift1.getId(),
                    returnPoint.getId(),
                    point.streamTasks().findFirst().orElseThrow().getId()
            ));


            userShift2 = testUserHelper.createEmptyShift(user2, shift);
            userShiftCommandService.closeShift(new UserShiftCommand.Close(userShift2.getId()));
            return new TestShiftData(shift.getId(), userShift1.getId(), userShift2.getId());
        });
    }

    @Getter
    @RequiredArgsConstructor
    private static class TestShiftData {
        private final long shiftId;
        private final long userShift1Id;
        private final long userShift2Id;
    }

}
