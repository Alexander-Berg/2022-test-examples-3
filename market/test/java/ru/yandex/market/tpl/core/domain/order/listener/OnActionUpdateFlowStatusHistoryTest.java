package ru.yandex.market.tpl.core.domain.order.listener;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.shift.ShiftStatus;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.base.fsm.StatusTransition;
import ru.yandex.market.tpl.core.domain.base.fsm.StatusTransitionType;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderFlowStatusHistory;
import ru.yandex.market.tpl.core.domain.order.OrderFlowStatusHistoryRepository;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.events.OrderFlowStatusChangedEvent;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static ru.yandex.market.tpl.core.domain.partner.DeliveryService.DEFAULT_DS_ID;

@RequiredArgsConstructor
public class OnActionUpdateFlowStatusHistoryTest extends TplAbstractTest {

    private final OrderGenerateService orderGenerateService;
    private final Clock clock;
    private final TestUserHelper userHelper;
    private final SortingCenterService sortingCenterService;
    private final OrderFlowStatusHistoryRepository orderFlowStatusHistoryRepository;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final SortingCenterPropertyService sortingCenterPropertyService;
    private final TransactionTemplate transactionTemplate;
    private final OnActionUpdateFlowStatusHistory onActionUpdateFlowStatusHistory;

    private Order order;
    private LocalDate now;

    @BeforeEach
    public void setUp() {
        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                sortingCenterService.findSortCenterForDs(DEFAULT_DS_ID),
                SortingCenterProperties.GET_RESCHEDULE_DATE_FROM_COMBINATOR_ENABLED,
                false);
        now = LocalDate.ofInstant(Instant.now(clock),
                ZoneId.of("Europe/Moscow").getRules().getOffset(Instant.now()));
        order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .flowStatus(OrderFlowStatus.SORTING_CENTER_ARRIVED)
                .deliveryDate(now)
                .deliveryServiceId(DEFAULT_DS_ID)
                .build());
    }

    @Test
    public void processEventArrivedToSc_WithoutExistsShift() {
        transactionTemplate.execute(ts -> {
            onActionUpdateFlowStatusHistory.processEventArrivedToSc(new OrderFlowStatusChangedEvent(order,
                    new StatusTransition<OrderFlowStatus, Source>(OrderFlowStatus.SORTING_CENTER_CREATED,
                            OrderFlowStatus.SORTING_CENTER_ARRIVED, StatusTransitionType.MANUAL, Source.SORT_CENTER)));
            return null;
        });

        List<OrderFlowStatusHistory> history = orderFlowStatusHistoryRepository.findByOrderIdHistory(order.getId());
        Assertions.assertEquals(history.size(), 1);
    }

    @Test
    public void processEventArrivedToSc_WithShiftInStatusCreated() {

        userHelper.findOrCreateShiftForScWithStatus(now,
                sortingCenterService.findSortCenterForDs(DEFAULT_DS_ID).getId(), ShiftStatus.CREATED);
        transactionTemplate.execute(ts -> {
            onActionUpdateFlowStatusHistory.processEventArrivedToSc(new OrderFlowStatusChangedEvent(order,
                    new StatusTransition<OrderFlowStatus, Source>(OrderFlowStatus.SORTING_CENTER_CREATED,
                            OrderFlowStatus.SORTING_CENTER_ARRIVED, StatusTransitionType.MANUAL, Source.SORT_CENTER)));
            return null;
        });

        List<OrderFlowStatusHistory> history = orderFlowStatusHistoryRepository.findByOrderIdHistory(order.getId());
        Assertions.assertEquals(history.size(), 1);
    }

    @Test
    public void processEventArrivedToSc_WithOpenShift() {

        userHelper.findOrCreateOpenShiftForSc(now,
                sortingCenterService.findSortCenterForDs(DEFAULT_DS_ID).getId());

        OrderFlowStatusChangedEvent event = new OrderFlowStatusChangedEvent(
                order,
                new StatusTransition<>(
                        OrderFlowStatus.SORTING_CENTER_CREATED,
                        OrderFlowStatus.SORTING_CENTER_ARRIVED,
                        StatusTransitionType.NORMAL,
                        Source.SORT_CENTER
                )
        );
        transactionTemplate.execute(ts -> {
            onActionUpdateFlowStatusHistory.processEventArrivedToSc(event);
            return null;
        });
        dbQueueTestUtil.assertTasksHasSize(QueueType.RESCHEDULE_ORDER, 1);
        dbQueueTestUtil.executeAllQueueItems(QueueType.RESCHEDULE_ORDER);

        List<OrderFlowStatusHistory> history = orderFlowStatusHistoryRepository.findByOrderIdHistory(order.getId());
        Assertions.assertEquals(history.size(), 3);
        Assertions.assertEquals(history.get(1).getOrderFlowStatusAfter(),
                OrderFlowStatus.DELIVERY_DATE_UPDATED_BY_SHOP);
    }

}
