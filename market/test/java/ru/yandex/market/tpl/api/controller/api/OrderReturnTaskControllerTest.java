package ru.yandex.market.tpl.api.controller.api;

import java.time.Clock;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.api.BaseShallowTest;
import ru.yandex.market.tpl.api.WebLayerTest;
import ru.yandex.market.tpl.api.facade.OrderReturnTaskFacade;
import ru.yandex.market.tpl.api.model.task.OrderReturnTaskDto;
import ru.yandex.market.tpl.api.model.task.OrderReturnTaskStatus;
import ru.yandex.market.tpl.api.model.task.OrderScanTaskDto;
import ru.yandex.market.tpl.api.model.task.PlaceForScanDto;
import ru.yandex.market.tpl.api.model.task.TaskType;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnQueryService;
import ru.yandex.market.tpl.core.domain.clientreturn.mapper.ClientReturnMapper;
import ru.yandex.market.tpl.core.domain.clientreturn.repository.ClientReturnRepository;
import ru.yandex.market.tpl.core.domain.dropoffcargo.repository.DropoffCargoRepository;
import ru.yandex.market.tpl.core.domain.movement.MovementRepository;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.order.RoutePointPostponementHistoryEventRepository;
import ru.yandex.market.tpl.core.domain.order.difference.OrderHistoryService;
import ru.yandex.market.tpl.core.domain.partial_return_order.PartialReturnOrderQueryService;
import ru.yandex.market.tpl.core.domain.sc.model.ScOrderRepository;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.external.routing.vrp.mapper.location.DurationCalculator;
import ru.yandex.market.tpl.core.query.common.mapper.TagsDtoMapper;
import ru.yandex.market.tpl.core.query.order.mapper.OrderDeliveryMapper;
import ru.yandex.market.tpl.core.query.order.mapper.OrderDtoMapper;
import ru.yandex.market.tpl.core.query.usershift.mapper.LocationDetailsDtoMapper;
import ru.yandex.market.tpl.core.query.usershift.mapper.LockerMapper;
import ru.yandex.market.tpl.core.query.usershift.mapper.OrderPlaceMapper;
import ru.yandex.market.tpl.core.query.usershift.mapper.TaskDtoMapper;
import ru.yandex.market.tpl.core.service.dropoff.MovementCargoSupportService;
import ru.yandex.market.tpl.core.service.order.CallRequirementResolver;
import ru.yandex.market.tpl.core.service.partial_return.PartialReturnOrderService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.tpl.api.model.task.OrderScanTaskDto.ScanOrderDisplayMode.OK;

/**
 * @author valter
 */
@WebLayerTest(OrderReturnTaskController.class)
class OrderReturnTaskControllerTest extends BaseShallowTest {

    @MockBean
    private UserShiftCommandService commandService;
    @MockBean
    private OrderRepository orderRepository;
    @MockBean
    private ScOrderRepository scOrderRepository;
    @MockBean
    private Clock clock;
    @MockBean
    private OrderDtoMapper orderDtoMapper;
    @MockBean
    private TagsDtoMapper tagsDtoMapper;
    @MockBean
    private LockerMapper lockerMapper;
    @MockBean
    private OrderPlaceMapper orderPlaceMapper;
    @MockBean
    private OrderHistoryService orderHistoryService;
    @SpyBean
    private TaskDtoMapper taskDtoMapper;
    @MockBean
    private MovementRepository movementRepository;
    @MockBean
    private LocationDetailsDtoMapper locationDetailsDtoMapper;
    @MockBean
    private ClientReturnRepository clientReturnRepository;
    @MockBean
    private ClientReturnQueryService clientReturnQueryService;
    @MockBean
    private OrderDeliveryMapper orderDeliveryMapper;
    @MockBean
    private ClientReturnMapper clientReturnMapper;
    @MockBean
    private RoutePointPostponementHistoryEventRepository postponementRepository;
    @MockBean
    private UserShiftRepository userShiftRepository;
    @MockBean
    private UserPropertyService userPropertyService;
    @MockBean
    private CallRequirementResolver callRequirementResolver;
    @MockBean
    private PartialReturnOrderService partialReturnOrderService;
    @MockBean
    private PartialReturnOrderQueryService partialReturnOrderQueryService;
    @MockBean
    private DropoffCargoRepository dropoffCargoRepository;
    @MockBean
    private MovementCargoSupportService movementCargoSupportService;
    @MockBean
    private DurationCalculator durationCalculator;
    @MockBean
    private OrderReturnTaskFacade orderReturnTaskFacade;

    @Test
    void startTask() throws Exception {
        long uid = 8347L;
        doReturn(orderReturnTaskDto()).when(orderReturnTaskFacade).startTask(anyLong(), any());
        mockMvc.perform(
                        post("/api/tasks/order-return/4765/start")
                                .header("Authorization", "OAuth uid-" + uid)
                )
                .andExpect(status().is2xxSuccessful())
                .andDo(print())
                .andExpect(content().json("" +
                        "{\n" +
                        "    \"id\": 2562,\n" +
                        "    \"name\": \"Возврат заказов\",\n" +
                        "    \"type\": \"ORDER_RETURN\",\n" +
                        "    \"status\": \"NOT_STARTED\",\n" +
                        "    \"orders\": [\n" +
                        "        {\n" +
                        "            \"externalOrderId\": \"1574247346365\",\n" +
                        "            \"places\": [\n" +
                        "               {\n" +
                        "                   \"barcode\": \"1574247346365\"\n" +
                        "               }\n" +
                        "            ]\n" +
                        "        },\n" +
                        "        {\n" +
                        "            \"externalOrderId\": \"1574247346460\",\n" +
                        "            \"places\": [\n" +
                        "               {\n" +
                        "                   \"barcode\": \"1574247346460\"\n" +
                        "               }\n" +
                        "            ]\n" +
                        "        }\n" +
                        "    ],\n" +
                        "    \"completedOrders\": [],\n" +
                        "    \"skippedOrders\": []\n" +
                        "}"
                ));
    }

    private OrderScanTaskDto<?> orderReturnTaskDto() {
        var orders = List.of(
                new OrderScanTaskDto.OrderForScanDto(
                        false, "1574247346365", "1574247346365", OK,
                        "Заказ перенесён, доставка сегодня не нужна!", null, null, null,
                        List.of(new PlaceForScanDto("1574247346365")),
                        null, null, null
                ),
                new OrderScanTaskDto.OrderForScanDto(
                        false, "1574247346460", "1574247346460", OK,
                        "Доставка в 12:20 Сканируйте следующую!", null, null, null,
                        List.of(new PlaceForScanDto("1574247346460")),
                        null, null, null
                )
        );
        return orderReturnTaskDto(false, orders);
    }

    private OrderScanTaskDto<OrderReturnTaskStatus> orderReturnTaskDto(
            boolean finished, List<OrderScanTaskDto.OrderForScanDto> orders
    ) {
        var taskDto = new OrderReturnTaskDto();
        taskDto.setId(2562);
        taskDto.setName("Возврат заказов");
        taskDto.setType(TaskType.ORDER_RETURN);
        taskDto.setStatus(finished
                ? OrderReturnTaskStatus.READY_TO_FINISH
                : OrderReturnTaskStatus.NOT_STARTED);
        taskDto.setOrders(orders);
        taskDto.setCompletedOrders(finished ? orders.subList(0, 1) : List.of());
        taskDto.setSkippedOrders(finished ? orders.subList(1, 2) : List.of());
        return taskDto;
    }

    @Test
    void returnOrders() throws Exception {
        long uid = 8347L;
        var orders = List.of(
                new OrderScanTaskDto.OrderForScanDto(false, "92342", "92342", OK, "Доставка в 12:20 Сканируйте " +
                        "следующую!", null, null, null, List.of(new PlaceForScanDto("92342")),
                        null, null, null),
                new OrderScanTaskDto.OrderForScanDto(false, "83475", "83475", OK, "Доставка в 13:20 Сканируйте " +
                        "следующую!", null, null, null, List.of(new PlaceForScanDto("83475")),
                        null, null, null)
        );
        var response = orderReturnTaskDto(true, orders);
        doReturn(response).when(orderReturnTaskFacade).returnOrders(anyLong(), any(), any());
        mockMvc.perform(
                        post("/api/tasks/order-return/4765/finish")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("" +
                                        "{\n" +
                                        "  \"skippedOrders\": [\n" +
                                        "    {\n" +
                                        "      \"displayMode\" : \"OK\",\n" +
                                        "      \"text\": \"Доставка в 12:20 Сканируйте следующую!\",\n" +
                                        "      \"externalOrderId\": \"83475\"\n" +
                                        "    }\n" +
                                        "  ],\n" +
                                        "  \"completedOrders\": [\n" +
                                        "    {\n" +
                                        "      \"externalOrderId\": \"92342\"\n" +
                                        "    }\n" +
                                        "  ],\n" +
                                        "  \"comment\": \"Не было этих коробок, я не виноват\"\n" +
                                        "}"
                                )
                                .header("Authorization", "OAuth uid-" + uid)
                )
                .andExpect(status().is2xxSuccessful())
                .andDo(print())
                .andExpect(content().json("" +
                        "{\n" +
                        "    \"id\": 2562,\n" +
                        "    \"name\": \"Возврат заказов\",\n" +
                        "    \"type\": \"ORDER_RETURN\",\n" +
                        "    \"status\": \"READY_TO_FINISH\",\n" +
                        "    \"orders\": [\n" +
                        "        {\n" +
                        "            \"externalOrderId\": \"92342\",\n" +
                        "            \"places\": [\n" +
                        "               {\n" +
                        "                   \"barcode\": \"92342\"\n" +
                        "               }\n" +
                        "            ]\n" +
                        "        },\n" +
                        "        {\n" +
                        "            \"externalOrderId\": \"83475\",\n" +
                        "            \"places\": [\n" +
                        "               {\n" +
                        "                   \"barcode\": \"83475\"\n" +
                        "               }\n" +
                        "            ]\n" +
                        "        }\n" +
                        "    ],\n" +
                        "    \"completedOrders\": [" +
                        "        {\n" +
                        "            \"externalOrderId\": \"92342\",\n" +
                        "            \"places\": [\n" +
                        "               {\n" +
                        "                   \"barcode\": \"92342\"\n" +
                        "               }\n" +
                        "            ]\n" +
                        "        }\n" +
                        "    ],\n" +
                        "    \"skippedOrders\": [" +
                        "        {\n" +
                        "            \"externalOrderId\": \"83475\",\n" +
                        "            \"places\": [\n" +
                        "               {\n" +
                        "                   \"barcode\": \"83475\"\n" +
                        "               }\n" +
                        "            ]\n" +
                        "        }\n" +
                        "    ]\n" +
                        "}"
                ));
    }

}
