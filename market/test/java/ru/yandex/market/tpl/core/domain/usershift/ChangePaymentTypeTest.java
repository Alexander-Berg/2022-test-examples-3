package ru.yandex.market.tpl.core.domain.usershift;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import ru.yandex.market.tpl.api.model.order.OrderChequeType;
import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.shift.UserShiftStatus;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderCheque;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderHistoryEventRepository;
import ru.yandex.market.tpl.core.domain.order.OrderManager;
import ru.yandex.market.tpl.core.domain.order.events.OrderChequeRemoteCreatedEvent;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.test.ClockUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.api.model.order.partner.OrderEventType.PAYMENT_TYPE_CHANGED;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@CoreTest
public class ChangePaymentTypeTest {

    private final TestUserHelper testUserHelper;
    private final UserShiftCommandDataHelper helper;
    private final OrderHistoryEventRepository orderHistoryEventRepository;
    private final OrderGenerateService orderGenerateService;
    private final UserShiftCommandService commandService;
    private final UserShiftRepository userShiftRepository;
    private final OrderManager orderManager;
    private final ChequeCreatedTestListener chequeCreatedTestListener;
    private UserShift userShift;
    private List<Order> orders;

    @MockBean
    private Clock clock;

    @BeforeEach
    void init() {
        ClockUtil.initFixed(clock);
        User user = testUserHelper.findOrCreateUser(35236L);

        Shift shift = testUserHelper.findOrCreateOpenShift(LocalDate.now());
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentStatus(OrderPaymentStatus.UNPAID)
                .paymentType(OrderPaymentType.CARD)
                .build());

        Order order2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentStatus(OrderPaymentStatus.UNPAID)
                .paymentType(OrderPaymentType.CARD)
                .build());

        orders = new ArrayList<>();
        orders.add(order);
        orders.add(order2);

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskOrderPickup(Instant.now()))
                .routePoint(helper.taskUnpaid("addr1", 12, orders.get(0).getId()))
                .routePoint(helper.taskUnpaid("addr3", 12, orders.get(1).getId()))
                .mergeStrategy(SimpleStrategies.NO_MERGE)
                .build();

        userShift = userShiftRepository.findById(commandService.createUserShift(createCommand)).orElseThrow();
        when(clock.withZone(userShift.getZoneId())).thenReturn(clock);
        testUserHelper.checkinAndFinishPickup(userShift);
        userShift = userShiftRepository.findById(userShift.getId()).orElseThrow();

    }

    @Test
    void shouldChangePaymentTypeInShiftClosed() {
        RoutePoint rp = Objects.requireNonNull(userShift.getCurrentRoutePoint());
        testUserHelper.finishDelivery(rp, null, OrderPaymentType.CARD, true);
        rp = Objects.requireNonNull(userShift.getCurrentRoutePoint());
        testUserHelper.finishDelivery(rp, null, OrderPaymentType.CARD, true);
        testUserHelper.finishFullReturnAtEnd(userShift);
        assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_CLOSED);

        checkPaymentTypeChange(0, 10, OrderPaymentType.CASH);
        checkPaymentTypeChange(0, 30, OrderPaymentType.CARD);
        checkPaymentTypeChange(1, 40, OrderPaymentType.CASH);
        checkPaymentTypeChange(0, 50, OrderPaymentType.CASH);
        checkPaymentTypeChange(1, 60, OrderPaymentType.CARD);

        assertThat(orderHistoryEventRepository.findAllByOrderId(orders.get(1)
                .getId()).stream().filter(event -> event.getType() == PAYMENT_TYPE_CHANGED).count())
                .isEqualTo(2);
        assertThat(orderHistoryEventRepository.findAllByOrderId(orders.get(0)
                .getId()).stream().filter(event -> event.getType() == PAYMENT_TYPE_CHANGED).count())
                .isEqualTo(3);
    }

    @Test
    void shouldChangePaymentTypeInShiftFinished() {
        RoutePoint rp = Objects.requireNonNull(userShift.getCurrentRoutePoint());
        testUserHelper.finishDelivery(rp, null, OrderPaymentType.CARD, true);
        rp = Objects.requireNonNull(userShift.getCurrentRoutePoint());
        testUserHelper.finishDelivery(rp, null, OrderPaymentType.CARD, true);
        testUserHelper.finishFullReturnAtEnd(userShift);
        assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_CLOSED);
        testUserHelper.finishUserShift(userShift);
        var user = userShift.getUser();

        checkPaymentTypeChange(0, 10, OrderPaymentType.CASH);
        checkPaymentTypeChange(0, 30, OrderPaymentType.CARD);
        checkPaymentTypeChange(1, 40, OrderPaymentType.CASH);
        checkPaymentTypeChange(0, 50, OrderPaymentType.CASH);
        checkPaymentTypeChange(1, 60, OrderPaymentType.CARD);
    }

    @Test
    void shouldChangePaymentTypeInShiftOnTask() {
        RoutePoint rp = Objects.requireNonNull(userShift.getCurrentRoutePoint());
        testUserHelper.finishDelivery(rp, null, OrderPaymentType.CASH, true);
        rp = Objects.requireNonNull(userShift.getCurrentRoutePoint());
        testUserHelper.finishDelivery(rp, null, OrderPaymentType.CASH, true);

        assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.ON_TASK);
        var user = userShift.getUser();

        checkPaymentTypeChange(0, 10, OrderPaymentType.CARD);
        checkPaymentTypeChange(0, 30, OrderPaymentType.CASH);
        checkPaymentTypeChange(1, 40, OrderPaymentType.CARD);
        checkPaymentTypeChange(0, 50, OrderPaymentType.CARD);
        checkPaymentTypeChange(1, 60, OrderPaymentType.CASH);

    }

    private void checkPaymentTypeChange(int orderIndex, int plusMinutes, OrderPaymentType expectedPaymentType) {
        var order = orders.get(orderIndex);
        var existedChequeIds = StreamEx.of(order.getCheques()).map(OrderCheque::getId).toSet();
        ClockUtil.initFixed(clock, LocalDateTime.now().plusMinutes(plusMinutes));
        chequeCreatedTestListener.clear();

        orderManager.changeOrderPaymentType(order.getId());

        assertThat(orders.get(orderIndex).getPaymentType()).isEqualTo(expectedPaymentType);

        var newCheques = StreamEx.of(order.getCheques())
                .filter(c -> !existedChequeIds.contains(c.getId()))
                .toList();

        assertThat(newCheques.size()).isEqualTo(2);
        var returnCheque = newCheques.get(0).getChequeType() == OrderChequeType.RETURN
                ? newCheques.get(0) : newCheques.get(1);
        var sellCheque = newCheques.get(0).getChequeType() == OrderChequeType.SELL
                ? newCheques.get(0) : newCheques.get(1);

        assertThat(returnCheque.getChequeType()).isEqualTo(OrderChequeType.RETURN);
        assertThat(sellCheque.getChequeType()).isEqualTo(OrderChequeType.SELL);

        var originCheque = StreamEx.of(order.getCheques())
                .filterBy(OrderCheque::getId, returnCheque.getOriginalChequeId())
                .findFirst()
                .orElseThrow();

        var events = chequeCreatedTestListener.getEvents();
        assertThat(events.size()).isEqualTo(2);
        var returnChequeEvent = events.get(0);
        var sellChequeEvent = events.get(1);
        assertThat(returnChequeEvent.getCheque().getPaymentType()).isEqualTo(originCheque.getPaymentType());
        assertThat(sellChequeEvent.getCheque().getPaymentType()).isEqualTo(expectedPaymentType);
        assertThat(returnChequeEvent.getOriginChequeFSD()).isEqualTo(originCheque.getFiscalSignOfTheDocument());
        assertThat(sellChequeEvent.getOriginChequeFSD()).isEqualTo(originCheque.getFiscalSignOfTheDocument());

    }

    /**
     * Бин для перехвата событий создания чеков в тестах
     */
    @Component
    static class ChequeCreatedTestListener {

        @Getter
        private final List<OrderChequeRemoteCreatedEvent> events = new ArrayList<>();

        @EventListener
        public void handleEvent(OrderChequeRemoteCreatedEvent event) {
            events.add(event);
        }

        private void clear() {
            this.events.clear();
        }

    }

}
