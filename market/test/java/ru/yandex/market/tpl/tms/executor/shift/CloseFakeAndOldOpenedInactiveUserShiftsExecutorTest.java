package ru.yandex.market.tpl.tms.executor.shift;

import java.time.Clock;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.common.util.datetime.RelativeTimeInterval;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.tms.test.TplTmsAbstractTest;

import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.SELECT_OLD_CREATED_USER_SHIFTS_ENABLED;

@RequiredArgsConstructor
public class CloseFakeAndOldOpenedInactiveUserShiftsExecutorTest extends TplTmsAbstractTest {

    private final OrderGenerateService orderGenerateService;
    private final TestUserHelper userHelper;
    private final UserShiftCommandDataHelper helper;
    private final UserShiftCommandService commandService;
    private final UserShiftRepository repository;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;
    private final ConfigurationProviderAdapter configurationProviderAdapter;
    private final CloseFakeAndOldOpenedInactiveUserShiftsExecutor executor;
    private final JdbcTemplate jdbcTemplate;

    private User user;
    private Order order;
    private Shift shift;
    private UserShift userShift;

    /**
     * Создаем смену с заказом на забор и на доставку.
     */
    @BeforeEach
    void createShifts() {
        when(configurationProviderAdapter.isBooleanEnabled(SELECT_OLD_CREATED_USER_SHIFTS_ENABLED)).thenReturn(true);
        order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryDate(LocalDate.now(clock))
                .build());
        user = userHelper.findOrCreateUser(824125L, LocalDate.now(clock), RelativeTimeInterval.valueOf("00:00-19:00"));
        shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskOrderPickup(clock.instant()))
                .routePoint(helper.taskUnpaid("addr1", 12, order.getId()))
                .routePoint(helper.taskOrderPickup(clock.instant()))
                .mergeStrategy(SimpleStrategies.NO_MERGE)
                .build();

        userShift = repository.findById(commandService.createUserShift(createCommand)).orElseThrow();
    }

    //TODO    Даже пустой тест ломает другие тесты MARKETTPL-6657

//    @SneakyThrows
//    @Test
//    void shouldCancelOldOpenedUserShift() {
//        assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_CREATED);
//        jdbcTemplate.update("UPDATE shift SET shift_date = current_date - INTERVAL '2 days' WHERE id = ?",
//                shift.getId());
//        executor.doRealJob(null);
//        transactionTemplate.execute(ts -> {
//            userShift = repository.findById(userShift.getId()).orElseThrow();
//            assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_CLOSED);
//            userShift.streamDeliveryTasks().forEach(task -> assertThat(task.isInTerminalStatus()).isTrue());
//            return null;
//        });
//    }
//
//    @SneakyThrows
//    @Test
//    void shouldNotCancelFreshOpenedUserShift() {
//        assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_CREATED);
//        jdbcTemplate.update("UPDATE shift SET shift_date = current_date WHERE id = ?",
//                shift.getId());
//        executor.doRealJob(null);
//        transactionTemplate.execute(ts -> {
//            userShift = repository.findById(userShift.getId()).orElseThrow();
//            assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_CREATED);
//            userShift.streamDeliveryTasks().forEach(task -> assertThat(task.isInTerminalStatus()).isFalse());
//            return null;
//        });
//    }
//
//    @SneakyThrows
//    @Test
//    void shouldNotCancelOldOpenedUserShift() {
//        assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_CREATED);
//        jdbcTemplate.update("UPDATE shift SET shift_date = current_date - INTERVAL '20 days' WHERE id = ?",
//                shift.getId());
//        executor.doRealJob(null);
//        transactionTemplate.execute(ts -> {
//            userShift = repository.findById(userShift.getId()).orElseThrow();
//            assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_CREATED);
//            userShift.streamDeliveryTasks().forEach(task -> assertThat(task.isInTerminalStatus()).isFalse());
//            return null;
//        });
//    }
}
