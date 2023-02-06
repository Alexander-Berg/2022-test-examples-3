package ru.yandex.market.tpl.core.domain.order.create_task;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import ru.yandex.market.tpl.api.model.order.partner.create_task.PartnerOrderCreateTaskItemStatus;
import ru.yandex.market.tpl.api.model.order.partner.create_task.PartnerOrderCreateTaskResponseDto;
import ru.yandex.market.tpl.api.model.order.partner.create_task.PartnerOrderCreateTaskStatus;
import ru.yandex.market.tpl.core.CoreTestV2;
import ru.yandex.market.tpl.core.domain.order.create_task.model.OrderCreateTaskItemStatus;
import ru.yandex.market.tpl.core.domain.order.create_task.model.OrderCreateTaskStatus;
import ru.yandex.market.tpl.core.domain.order.create_task.model.OrderCreateTaskType;
import ru.yandex.market.tpl.core.domain.order.create_task.params.OrderCreateTaskItemParams;
import ru.yandex.market.tpl.core.domain.order.create_task.params.OrderCreateTaskParams;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTestV2
@RequiredArgsConstructor
public class OrderCreateTaskPartnerServiceTest {

    private static final long DELIVERY_SERVICE_ID = 239L;

    private final OrderCreateTaskPartnerService partnerService;
    private final OrderCreateTaskCommandService commandService;

    private Long taskId;
    private Long taskItemId;

    @Test
    void testGetTask() {
        createTask();
        assertThat(partnerService.getTask(DELIVERY_SERVICE_ID, taskId)).isEqualTo(buildExpectedDto());
    }

    @Test
    void testGetTasks() {
        createTask();
        assertThat(partnerService.getTasks(DELIVERY_SERVICE_ID, Pageable.unpaged()).getContent())
                .isEqualTo(List.of(buildExpectedDto()));
    }

    private void createTask() {
        var task = commandService.create(OrderCreateTaskParams.builder()
                        .partnerId(DELIVERY_SERVICE_ID)
                        .status(OrderCreateTaskStatus.PROCESSING)
                        .type(OrderCreateTaskType.EXCEL_MODEL_DTO)
                        .attemptsMade(3)
                        .items(List.of(OrderCreateTaskItemParams.builder()
                                .status(OrderCreateTaskItemStatus.FORMAT_ERROR)
                                .payload(new byte[]{1, 2, 3})
                                .build()))
                        .build(),
                Object.class);
        taskId = task.getId();
        taskItemId = task.getItems().get(0).getId();
    }

    private PartnerOrderCreateTaskResponseDto buildExpectedDto() {
        return PartnerOrderCreateTaskResponseDto.builder()
                .id(taskId)
                .status(PartnerOrderCreateTaskStatus.PROCESSING)
                .items(List.of(PartnerOrderCreateTaskResponseDto.Item.builder()
                        .id(taskItemId)
                        .status(PartnerOrderCreateTaskItemStatus.FORMAT_ERROR)
                        .build()))
                .build();
    }

}
