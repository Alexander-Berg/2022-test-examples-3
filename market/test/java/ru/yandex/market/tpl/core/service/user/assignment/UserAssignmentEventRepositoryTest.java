package ru.yandex.market.tpl.core.service.user.assignment;

import java.time.Instant;
import java.util.function.Consumer;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserRepository;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class UserAssignmentEventRepositoryTest {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final UserAssignmentEventRepository userAssignmentEventRepository;
    private final TestDataFactory testDataFactory;
    private final TestUserHelper testUserHelper;

    @Test
    void findCurrentUserOrders() {
        User user = userRepository.save(testUserHelper.createUserWithoutSchedule(123L));
        Order order = orderRepository.save(testDataFactory.generateOrder(
                OrderGenerateService.OrderGenerateParam.builder().build())
        );
        Consumer<UserAssignmentEventType> saveEvent = et -> userAssignmentEventRepository.save(
                UserAssignmentEventLogEntry.builder()
                        .userId(user.getId())
                        .orderId(order.getId())
                        .date(Instant.now())
                        .eventType(et)
                        .build()
        );
        saveEvent.accept(UserAssignmentEventType.ASSIGNED);
        assertThat(orderRepository.findCurrentUserOrders(user.getId())).containsExactly(order);

        saveEvent.accept(UserAssignmentEventType.UNASSIGNED);
        assertThat(orderRepository.findCurrentUserOrders(user.getId())).isEmpty();

        saveEvent.accept(UserAssignmentEventType.ASSIGNED);
        assertThat(orderRepository.findCurrentUserOrders(user.getId())).containsExactly(order);
    }
}
