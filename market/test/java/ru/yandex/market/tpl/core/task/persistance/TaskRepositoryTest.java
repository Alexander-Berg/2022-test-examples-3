package ru.yandex.market.tpl.core.task.persistance;

import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.task.persistence.FlowTaskEntity;
import ru.yandex.market.tpl.core.task.projection.CreateActionRequest;
import ru.yandex.market.tpl.core.task.projection.TaskActionStatus;
import ru.yandex.market.tpl.core.task.projection.TaskActionType;
import ru.yandex.market.tpl.core.task.projection.TaskFlowType;
import ru.yandex.market.tpl.core.task.projection.TaskStatus;
import ru.yandex.market.tpl.core.task.service.TaskRepository;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author sekulebyakin
 */
@RequiredArgsConstructor
public class TaskRepositoryTest extends TplAbstractTest {

    private final TaskRepository taskRepository;
    private final TransactionTemplate transactionTemplate;
    private final OrderGenerateService orderGenerateService;
    private final TestUserHelper testUserHelper;
    private final TestDataFactory testDataFactory;

    @Test
    void saveDataTest() {
        var pickupPoint = testDataFactory.createPickupPoint(
                PartnerSubType.LOCKER, 123L, DeliveryService.DEFAULT_DS_ID
        );
        var taskId = transactionTemplate.execute(status -> {
            var user = testUserHelper.findOrCreateUser(123L);
            var order = orderGenerateService.createOrder();
            var userShift = testUserHelper.createOpenedShift(user, order, LocalDate.now());
            var routePoint = userShift.streamDeliveryRoutePoints().findFirst().orElseThrow();

            var task = new FlowTaskEntity();
            task.init(routePoint, TaskFlowType.TEST_FLOW, pickupPoint.getId());
            taskRepository.saveTask(task);
            return task.getId();
        });
        assertThat(taskId).isNotNull();

        transactionTemplate.execute(status -> {
            var actionRequests = List.of(
                    new CreateActionRequest(TaskActionType.EMPTY_ACTION, 1),
                    new CreateActionRequest(TaskActionType.EMPTY_ACTION, 2),
                    new CreateActionRequest(TaskActionType.EMPTY_ACTION, 3)
            );
            return taskRepository.createActions(taskId, actionRequests);
        });

        transactionTemplate.execute(status -> {
            var task = taskRepository.getTask(taskId);
            assertThat(task.getFlowType()).isEqualTo(TaskFlowType.TEST_FLOW);
            assertThat(task.getStatus()).isEqualTo(TaskStatus.NOT_STARTED);
            assertThat(task.getPickupPointId()).isEqualTo(pickupPoint.getId());

            var actions = taskRepository.getTaskActions(taskId);
            assertThat(actions).hasSize(3);
            for (int i = 0; i < actions.size(); i++) {
                var action = actions.get(i);
                assertThat(action.getId()).isNotNull();
                assertThat(action.getType()).isEqualTo(TaskActionType.EMPTY_ACTION);
                assertThat(action.getStatus()).isEqualTo(TaskActionStatus.CREATED);
                assertThat(action.getOrdinal()).isEqualTo(i + 1);
            }
            return null;
        });
    }

}
