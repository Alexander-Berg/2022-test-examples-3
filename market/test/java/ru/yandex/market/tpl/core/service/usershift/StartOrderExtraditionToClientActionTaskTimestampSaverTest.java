package ru.yandex.market.tpl.core.service.usershift;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.task.ActionTimestampType;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.task.TaskOrderDeliveryRepository;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class StartOrderExtraditionToClientActionTaskTimestampSaverTest extends TplAbstractTest {
    private final ActionTimestampSaveService actionTimestampSaveService;

    private final TestUserHelper testUserHelper;
    private final OrderGenerateService orderGenerateService;
    private final TransactionTemplate transactionTemplate;
    private final TaskOrderDeliveryRepository taskOrderDeliveryRepository;

    private OrderDeliveryTask orderDeliveryTask;

    @BeforeEach
    void init() {
        transactionTemplate.execute(ts -> {
            var user = testUserHelper.findOrCreateUser(12321);
            var userShift = testUserHelper.createOpenedShift(user, orderGenerateService.createOrder(), LocalDate.now(), 47819L);
            orderDeliveryTask = userShift.streamOrderDeliveryTasks().findFirst().orElseThrow();
            return null;
        });
        OrderDeliveryTask task = taskOrderDeliveryRepository.findByIdOrThrow(orderDeliveryTask.getId());
        assertThat(task.getStartExtraditionAt()).isNull();
    }

    @Test
    void saveStartOrderExtraditionToClientActionTaskTimestamp() {
        actionTimestampSaveService.saveActionTimestamp(
                ActionTimestampType.START_ORDER_EXTRADITION_TO_CLIENT,
                orderDeliveryTask.getId(),
                null
        );

        var task = taskOrderDeliveryRepository.findByIdOrThrow(orderDeliveryTask.getId());
        assertThat(task.getStartExtraditionAt()).isNotNull();
    }

    @Test
    void saveStartOrderExtraditionToClientActionTaskTimestampTwoTimes() {
        actionTimestampSaveService.saveActionTimestamp(
                ActionTimestampType.START_ORDER_EXTRADITION_TO_CLIENT,
                orderDeliveryTask.getId(),
                null
        );
        Instant firstSave = taskOrderDeliveryRepository.findByIdOrThrow(orderDeliveryTask.getId()).getStartExtraditionAt();

        actionTimestampSaveService.saveActionTimestamp(
                ActionTimestampType.START_ORDER_EXTRADITION_TO_CLIENT,
                orderDeliveryTask.getId(),
                Instant.now().plus(1, ChronoUnit.MINUTES)
        );
        Instant secondSave = taskOrderDeliveryRepository.findByIdOrThrow(orderDeliveryTask.getId()).getStartExtraditionAt();

        assertThat(secondSave).isEqualTo(firstSave);
    }

}
