package ru.yandex.market.tpl.core.query.usershift.mapper.expendable;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.task.expendable.ExpendableType;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturn;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnCommandService;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.domain.clientreturn.repository.ClientReturnRepository;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.shift.UserShiftTestHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftReassignManager;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.test.ClockUtil;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
class ExpendablesCollectorTest extends TplAbstractTest {

    private final ClientReturnGenerator clientReturnGenerator;
    private final ClientReturnCommandService clientReturnCommandService;
    private final ClientReturnRepository clientReturnRepository;
    private final UserShiftCommandDataHelper helper;
    private final UserShiftTestHelper userShiftTestHelper;
    private final TestUserHelper testUserHelper;
    private final SortingCenterService sortingCenterService;
    private final UserShiftReassignManager userShiftReassignManager;
    private final TransactionTemplate transactionTemplate;
    private final OrderGenerateService orderGenerateService;
    private final ExpendablesCollector expendablesCollector;

    private static final long DELIVERY_SERVICE_ID = 239L;
    private static final long SORTING_CENTER_ID = 47819L;

    private final UserShiftRepository userShiftRepository;
    private final Clock clock;

    Shift shift;
    User userA;
    User userB;
    Order order;
    Order orderSortingCenterCreated;
    Long deliveryServiceId;
    ClientReturn clientReturnA;
    ClientReturn clientReturnTwo;

    @BeforeEach
    void init() {
        ClockUtil.initFixed(clock, LocalDateTime.now(ZoneOffset.UTC));
        userA = testUserHelper.findOrCreateUser(1L);
        order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryServiceId(DELIVERY_SERVICE_ID)
                .paymentType(OrderPaymentType.CARD)
                .build());
        shift = testUserHelper.findOrCreateOpenShiftForSc(LocalDate.now(clock), SORTING_CENTER_ID);
        deliveryServiceId =
                sortingCenterService.findDsForSortingCenter(shift.getSortingCenter().getId()).get(0).getId();
        clientReturnA = clientReturnGenerator.generateReturnFromClient(deliveryServiceId);
    }

    @Test
    void getRequiredExpendablesForUserShift() {
        var userShiftA = testUserHelper.createEmptyShift(userA, shift);
        var expendables = expendablesCollector.getRequiredExpendablesForUserShift(userShiftA.getId());
        assertThat(expendables).isEmpty();
        userShiftReassignManager.reassignOrders(Set.of(order.getId()), Set.of(), Set.of(), userA.getId());
        expendables = expendablesCollector.getRequiredExpendablesForUserShift(userShiftA.getId());
        assertThat(expendables).extracting(Expendable::getType)
                .contains(ExpendableType.PAYMENT_TERMINAL);
        userShiftReassignManager.reassignOrders(Set.of(), Set.of(clientReturnA.getId()), Set.of(), userA.getId());
        expendables = expendablesCollector.getRequiredExpendablesForUserShift(userShiftA.getId());
        assertThat(expendables).extracting(Expendable::getType)
                .contains(
                        ExpendableType.PAYMENT_TERMINAL,
                        ExpendableType.RETURN_PACKING_TAPE,
                        ExpendableType.RETURN_BARCODE_SHEET
                );

    }

}
