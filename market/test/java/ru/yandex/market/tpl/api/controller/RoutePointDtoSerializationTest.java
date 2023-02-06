package ru.yandex.market.tpl.api.controller;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.api.model.order.OrderDto;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointDto;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointStatus;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus;
import ru.yandex.market.tpl.api.model.task.TaskType;
import ru.yandex.market.tpl.core.util.ObjectMappers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author ungomma
 */
class RoutePointDtoSerializationTest {

    private ObjectMapper objectMapper = ObjectMappers.TPL_API_OBJECT_MAPPER;

    @Test
    void shouldRestoreDeliveryTaskDto() throws Exception {

        OrderDeliveryTaskDto taskDto = buildTaskDto();

        var string = objectMapper.writeValueAsString(taskDto);

        OrderDeliveryTaskDto result = objectMapper.readValue(string, OrderDeliveryTaskDto.class);

        assertThat(result).isEqualTo(taskDto);
        assertThat(result).isEqualToComparingFieldByField(taskDto);
    }

    @Test
    void shouldRestoreRoutePointDto() throws Exception {

        OrderDeliveryTaskDto taskDto = buildTaskDto();

        RoutePointDto routePointDto = new RoutePointDto();
        routePointDto.setId(555L);
        routePointDto.setStatus(RoutePointStatus.IN_PROGRESS);
        routePointDto.setTasks(List.of(taskDto));

        var string = objectMapper.writeValueAsString(routePointDto);

        RoutePointDto result = objectMapper.readValue(string, RoutePointDto.class);

        assertThat(result).isEqualTo(routePointDto);
        assertThat(result).isEqualToComparingFieldByField(routePointDto);

        assertThat(result.getTasks().get(0)).isInstanceOf(OrderDeliveryTaskDto.class);
    }

    private OrderDeliveryTaskDto buildTaskDto() {
        var orderDto = new OrderDto();
        orderDto.setExternalOrderId("order123");

        var taskDto = new OrderDeliveryTaskDto();
        taskDto.setId(125L);
        taskDto.setName("Test task");
        taskDto.setStatus(OrderDeliveryTaskStatus.DELIVERY_FAILED);
        taskDto.setOrder(orderDto);
        taskDto.setType(TaskType.ORDER_DELIVERY);
        return taskDto;
    }

}
