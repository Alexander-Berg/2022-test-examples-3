package ru.yandex.market.tpl.api.facade;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mapstruct.ap.internal.util.Collections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.BaseApiTest;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryFailReasonDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskError;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserRepository;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTaskRepository;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class OrderDeliveryTaskFacadeTest extends BaseApiTest {
    private final OrderDeliveryTaskFacade orderDeliveryTaskFacade;
    private final TestUserHelper testUserHelper;
    private final OrderGenerateService orderGenerateService;
    private final OrderDeliveryTaskRepository orderDeliveryTaskRepository;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;
    private final UserRepository userRepository;

    @Test
    void deliveryTasksFailed() {
        User user = userHelper.findOrCreateUser(4693856394098L);

        List<List<Long>> allTaskIds = transactionTemplate.execute(status -> {
            User transactionUser = userRepository.getById(user.getId());
            UserShift userShift = userHelper.createEmptyShift(transactionUser, LocalDate.now(clock));
            //Создаем задачи, две упадут с ошибкой, две нет
            Order orderError1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                    .build());
            OrderDeliveryTask orderDeliveryTaskError1 =
                    (OrderDeliveryTask) testUserHelper.addDeliveryTaskToShift(transactionUser,
                            userShift, orderError1);
            orderDeliveryTaskError1.finish(Instant.now(clock));
            orderDeliveryTaskRepository.save(orderDeliveryTaskError1);

            Order order1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                    .build());
            OrderDeliveryTask orderDeliveryTask1 =
                    (OrderDeliveryTask) testUserHelper.addDeliveryTaskToShift(transactionUser,
                            userShift, order1);

            Order orderError2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                    .build());
            OrderDeliveryTask orderDeliveryTaskError2 =
                    (OrderDeliveryTask) testUserHelper.addDeliveryTaskToShift(transactionUser,
                            userShift, orderError2);
            orderDeliveryTaskError2.finish(Instant.now(clock));
            orderDeliveryTaskRepository.save(orderDeliveryTaskError2);

            Order order2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                    .build());
            OrderDeliveryTask orderDeliveryTask2 =
                    (OrderDeliveryTask) testUserHelper.addDeliveryTaskToShift(transactionUser,
                            userShift, order2);


            userHelper.checkinAndFinishPickup(userShift);
            userHelper.arriveAtRoutePoint(orderDeliveryTask1.getRoutePoint());
            return List.of(List.of(orderDeliveryTask1.getId(), orderDeliveryTask2.getId()),
                    List.of(orderDeliveryTaskError1.getId(), orderDeliveryTaskError2.getId()));
        });


        OrderDeliveryFailReasonDto failReasonDto = new OrderDeliveryFailReasonDto();
        failReasonDto.setReason(OrderDeliveryTaskFailReasonType.ORDER_IS_DAMAGED);
        var result = orderDeliveryTaskFacade.deliveryTasksFailed(
                Collections.join(allTaskIds.get(0), allTaskIds.get(1)), failReasonDto, user);
        Set<Long> allTaskResultIds =
                result.getTasks().stream().map(OrderDeliveryTaskDto::getId).collect(Collectors.toSet());
        Assertions.assertThat(allTaskResultIds).containsExactlyInAnyOrderElementsOf(Collections.join(allTaskIds.get(0), allTaskIds.get(1)));
        Set<Long> errorTasksIds =
                result.getErrors().stream().map(OrderDeliveryTaskError::getId).collect(Collectors.toSet());
        Assertions.assertThat(errorTasksIds).containsExactlyInAnyOrderElementsOf(allTaskIds.get(1));

        transactionTemplate.execute(status -> {
            Set<OrderDeliveryTaskStatus> goodStatuses =
                    orderDeliveryTaskRepository.findAllById(allTaskIds.get(0)).stream().map(OrderDeliveryTask::getStatus).collect(Collectors.toSet());
            Set<OrderDeliveryTaskStatus> badStatuses =
                    orderDeliveryTaskRepository.findAllById(allTaskIds.get(1)).stream().map(OrderDeliveryTask::getStatus).collect(Collectors.toSet());
            Assertions.assertThat(goodStatuses).allMatch(stat -> stat == OrderDeliveryTaskStatus.DELIVERY_FAILED);
            Assertions.assertThat(badStatuses).allMatch(stat -> stat == OrderDeliveryTaskStatus.DELIVERED);

            return status;
        });
    }
}

