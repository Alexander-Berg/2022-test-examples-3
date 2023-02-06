package ru.yandex.market.tpl.core.domain.order;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserUtil;
import ru.yandex.market.tpl.core.domain.usershift.commands.DeliveryReschedule;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryRescheduleReasonType.DELIVERY_DELAY;
import static ru.yandex.market.tpl.common.util.DateTimeUtil.todayAtHour;

/**
 * @author aostrikov
 */
@RequiredArgsConstructor
class DeliveryRescheduleTest extends TplAbstractTest {
    private static final User USER = UserUtil.createUserWithoutSchedule(1);

    private final TestUserHelper testUserHelper;
    private final OrderCommandService commandService;
    private final OrderGenerateService orderGenerateService;
    private final OrderRepository orderRepository;
    private final ConfigurationProviderAdapter configurationProviderAdapter;
    private final Clock clock;
    @SpyBean
    private SortingCenterService sortingCenterService;

    private Instant instant10;
    private Instant instant12;
    private Instant instant16;
    private Instant instant18;

    @BeforeEach
    void before() {
        instant10 = todayAtHour(10, clock);
        instant12 = todayAtHour(12, clock);
        instant16 = todayAtHour(16, clock);
        instant18 = todayAtHour(18, clock);
    }

    @Test
    void shouldRescheduleDeliveryTime() {
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .build());

        commandService.rescheduleDelivery(new OrderCommand.RescheduleDelivery(order.getId(),
                DeliveryReschedule.fromCourier(USER, instant10, instant12, DELIVERY_DELAY)));

        Order updatedOrder = orderRepository.findByIdOrThrow(order.getId());

        assertThat(updatedOrder.getDelivery().getDeliveryIntervalFrom()).isEqualTo(instant10);
        assertThat(updatedOrder.getDelivery().getDeliveryIntervalTo()).isEqualTo(instant12);

        commandService.rescheduleDelivery(new OrderCommand.RescheduleDelivery(order.getId(),
                DeliveryReschedule.fromCourier(USER, instant16, instant18, DELIVERY_DELAY)));

        updatedOrder = orderRepository.findByIdOrThrow(order.getId());
        assertThat(updatedOrder.getDelivery().getDeliveryIntervalFrom()).isEqualTo(instant16);
        assertThat(updatedOrder.getDelivery().getDeliveryIntervalTo()).isEqualTo(instant18);
    }

    @Test
    void shouldCreateDeliveryTaskForDbsOrderAfterReschedule() {
        testUserHelper.createOrFindDbsDeliveryService();
        doReturn(true).when(sortingCenterService).usePvz(any());

        LocalDate initialDeliveryDate = LocalDate.now(clock);
        Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryDate(initialDeliveryDate)
                .deliveryServiceId(TestUserHelper.DBS_DELIVERY_SERVICE_ID)
                .build());

        commandService.rescheduleDelivery(new OrderCommand.RescheduleDelivery(order.getId(),
                DeliveryReschedule.fromCourier(USER, instant16, instant18, DELIVERY_DELAY)));

        Order updatedOrder = orderRepository.findByIdOrThrow(order.getId());

        assertThat(updatedOrder.getDelivery().getDeliveryIntervalFrom()).isEqualTo(instant16);
        assertThat(updatedOrder.getDelivery().getDeliveryIntervalTo()).isEqualTo(instant18);

        commandService.rescheduleDelivery(new OrderCommand.RescheduleDelivery(order.getId(),
                DeliveryReschedule.fromCourier(USER, instant10, instant12, DELIVERY_DELAY)));

        updatedOrder = orderRepository.findByIdOrThrow(order.getId());

        assertThat(updatedOrder.getDelivery().getDeliveryIntervalFrom()).isEqualTo(instant10);
        assertThat(updatedOrder.getDelivery().getDeliveryIntervalTo()).isEqualTo(instant12);

        reset(configurationProviderAdapter);
    }
}
