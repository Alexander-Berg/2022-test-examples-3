package ru.yandex.market.tpl.core.domain.order.listener;

import java.sql.Date;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistic.api.model.common.OrderStatusType;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.sc.OrderStatusUpdate;
import ru.yandex.market.tpl.core.domain.sc.ScManager;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
class OnScOrderEventActionRegisterTest extends TplAbstractTest {

    private static final long DELIVERY_SERVICE_ID = 239L;
    private static final long SORTING_CENTER_ID = 47819L;

    private final TestUserHelper userHelper;
    private final OrderGenerateService orderGenerateService;
    private final Clock clock;
    private final ScManager scManager;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final OrderRepository orderRepository;

    private Order order;

    @BeforeEach
    public void init() {
        transactionTemplate.execute(action -> {
            LocalDate now = LocalDate.now(clock);
            User user = userHelper.findOrCreateUser(35237L, now);
            order = orderGenerateService.createOrder(
                    OrderGenerateService.OrderGenerateParam.builder()
                            .deliveryServiceId(DELIVERY_SERVICE_ID)
                            .deliveryDate(now)
                            .flowStatus(OrderFlowStatus.CREATED)
                            .build()
            );

            Shift shift = userHelper.findOrCreateOpenShiftForSc(now, SORTING_CENTER_ID);
            UserShift userShift = userHelper.createEmptyShift(user, shift);
            userHelper.addDeliveryTaskToShift(user, userShift, order);

            jdbcTemplate.update("UPDATE orders SET created_at = :createdAt WHERE id IN (:ids)",
                    new MapSqlParameterSource()
                            .addValue("createdAt", Date.from(Instant.now(clock).minusSeconds(1)))
                            .addValue("ids", List.of(order.getId()))
            );

            Instant instant = Instant.now(clock);
            scManager.createOrders(instant);
            scManager.updateWhenCreatedOrder(order.getExternalOrderId(), "", SORTING_CENTER_ID);
            scManager.updateWhenCreatedOrder(order.getExternalOrderId(), "", SortingCenter.DEFAULT_SC_ID);
            return null;
        });
    }

    @Test
    void statusUpdatedWithNewParentScCheck() {
        var instant = Instant.now(clock);
        scManager.updateOrderStatuses(order.getExternalOrderId(), SORTING_CENTER_ID,
                List.of(
                        new OrderStatusUpdate(OrderStatusType.ORDER_CREATED_FF.getCode(), instant),
                        new OrderStatusUpdate(OrderStatusType.ORDER_ARRIVED_TO_SO_WAREHOUSE.getCode(), instant),
                        new OrderStatusUpdate(OrderStatusType.ORDER_READY_TO_BE_SEND_TO_SO_FF.getCode(), instant)
                )
        );

        order = orderRepository.findByIdOrThrow(order.getId());
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_PREPARED);
    }

    @Test
    void statusNotUpdatedWhenReceiveEventFromParentSc() {
        var instant = Instant.now(clock);
        scManager.updateOrderStatuses(order.getExternalOrderId(), SortingCenter.DEFAULT_SC_ID,
                List.of(
                        new OrderStatusUpdate(OrderStatusType.ORDER_CREATED_FF.getCode(), instant),
                        new OrderStatusUpdate(OrderStatusType.ORDER_ARRIVED_TO_SO_WAREHOUSE.getCode(), instant),
                        new OrderStatusUpdate(OrderStatusType.ORDER_READY_TO_BE_SEND_TO_SO_FF.getCode(), instant)
                )
        );

        order = orderRepository.findByIdOrThrow(order.getId());
        assertThat(order.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_CREATED);
    }

}
