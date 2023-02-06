package ru.yandex.market.global.checkout.executor;

import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import ru.yandex.market.global.checkout.BaseFunctionalTest;
import ru.yandex.market.global.checkout.domain.order.OrderModel;
import ru.yandex.market.global.checkout.domain.order.OrderQueryService;
import ru.yandex.market.global.checkout.factory.TestOrderFactory;
import ru.yandex.market.global.common.test.TestClock;
import ru.yandex.market.global.db.jooq.enums.EOrderDeliverySchedulingType;
import ru.yandex.market.global.db.jooq.enums.EOrderState;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.global.checkout.TestTimeUtil.getTimeInDefaultTZ;
import static ru.yandex.market.global.db.jooq.enums.EOrderState.PROCESSING;
import static ru.yandex.market.global.db.jooq.enums.EOrderState.SCHEDULED;

@Import(ReferralRewardExecutor.class)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class StartScheduledOrdersExecutorTest extends BaseFunctionalTest {

    private final TestClock clock;
    private final StartScheduledOrdersExecutor executor;

    private final TestOrderFactory orderFactory;
    private final OrderQueryService orderQueryService;

    @Test
    void testScheduledOrderStartedProperly() {
        OrderModel readyToStartOrder = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(o -> o.setOrderState(EOrderState.SCHEDULED))
                .setupDelivery(d -> d
                        .setDeliverySchedulingType(EOrderDeliverySchedulingType.TO_REQUESTED_TIME)
                        .setRequestedDeliveryTime(getTimeInDefaultTZ(
                                LocalDateTime.of(2022, 2, 11, 14, 30))))
                .build());

        OrderModel startLaterOrder = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(o -> o.setOrderState(EOrderState.SCHEDULED))
                .setupDelivery(d -> d
                        .setDeliverySchedulingType(EOrderDeliverySchedulingType.TO_REQUESTED_TIME)
                        .setRequestedDeliveryTime(getTimeInDefaultTZ(
                                LocalDateTime.of(2022, 2, 11, 15, 30))))
                .build());

        clock.setTime(getTimeInDefaultTZ(
                LocalDateTime.of(2022, 2, 11, 14, 0)).toInstant());
        executor.doRealJob(null);

        assertThat(orderQueryService.get(readyToStartOrder.getOrder().getId()).getOrderState()).isEqualTo(PROCESSING);
        assertThat(orderQueryService.get(startLaterOrder.getOrder().getId()).getOrderState()).isEqualTo(SCHEDULED);
    }

}
