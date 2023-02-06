package ru.yandex.market.tpl.api.controller.api;

import java.time.Clock;
import java.util.List;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.api.BaseShallowTest;
import ru.yandex.market.tpl.api.WebLayerTest;
import ru.yandex.market.tpl.api.facade.OrderPickupTaskFacade;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.task.OrderPickupTaskDto;
import ru.yandex.market.tpl.api.model.task.OrderPickupTaskStatus;
import ru.yandex.market.tpl.api.model.task.OrderScanTaskDto;
import ru.yandex.market.tpl.api.model.task.PlaceForScanDto;
import ru.yandex.market.tpl.api.model.task.TaskType;
import ru.yandex.market.tpl.common.db.exception.TplEntityNotFoundException;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnQueryService;
import ru.yandex.market.tpl.core.domain.clientreturn.mapper.ClientReturnMapper;
import ru.yandex.market.tpl.core.domain.clientreturn.repository.ClientReturnRepository;
import ru.yandex.market.tpl.core.domain.dropoffcargo.repository.DropoffCargoRepository;
import ru.yandex.market.tpl.core.domain.movement.MovementRepository;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.order.RoutePointPostponementHistoryEventRepository;
import ru.yandex.market.tpl.core.domain.order.difference.OrderHistoryService;
import ru.yandex.market.tpl.core.domain.partial_return_order.PartialReturnOrderQueryService;
import ru.yandex.market.tpl.core.domain.routing.RerouteUserShiftHelper;
import ru.yandex.market.tpl.core.domain.sc.model.ScOrderRepository;
import ru.yandex.market.tpl.core.domain.transferact.service.TransferActCancellationService;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.location.ReceptionService;
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
import ru.yandex.market.tpl.core.service.usershift.additionaldata.vehicle.AdditionalVehicleDataService;
import ru.yandex.market.tpl.core.service.vehicle.VehicleDtoMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.tpl.api.model.task.OrderScanTaskDto.ScanOrderDisplayMode.OK;

/**
 * @author valter
 */
@WebLayerTest(OrderPickupTaskController.class)
class OrderPickupTaskControllerTest extends BaseShallowTest {

    @MockBean
    private ReceptionService receptionService;
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
    private RerouteUserShiftHelper rerouteUserShiftHelper;
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
    private MovementCargoSupportService MovementCargoSupportService;
    @MockBean
    private DropoffCargoRepository dropoffCargoRepository;
    @MockBean
    private DurationCalculator durationCalculator;
    @MockBean
    private VehicleDtoMapper vehicleDtoMapper;
    @MockBean
    private AdditionalVehicleDataService additionalVehicleDataService;
    @MockBean
    private TransferActCancellationService transferActCancellationService;
    @MockBean
    private OrderPickupTaskFacade orderPickupTaskFacade;

    @Test
    @SneakyThrows
    void getById_success() {
        var taskId = 4765L;

        var orderPickupTask = orderPickupTaskDto();

        doReturn(orderPickupTask).when(orderPickupTaskFacade).getById(eq(taskId), any());

        mockMvc.perform(
                        get("/api/tasks/order-pickup/" + taskId)
                                .header("Authorization", "OAuth uid")
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(orderPickupTaskJson));
    }

    @Test
    @SneakyThrows
    void getById_404() {
        var taskId = 4765L;
        doThrow(new TplEntityNotFoundException("", "")).when(orderPickupTaskFacade).getById(eq(taskId), any());
        mockMvc.perform(
                        get("/api/tasks/order-pickup/" + taskId)
                                .header("Authorization", "OAuth uid")
                )
                .andExpect(status().is4xxClientError());
    }

    @Test
    void startTask() throws Exception {
        long uid = 8347L;
        doReturn(orderPickupTaskDto()).when(orderPickupTaskFacade).startTask(anyLong(), any());
        mockMvc.perform(
                        post("/api/tasks/order-pickup/4765/start")
                                .header("Authorization", "OAuth uid-" + uid)
                )
                .andExpect(status().is2xxSuccessful())
                .andDo(print())
                .andExpect(content().json(orderPickupTaskJson));
    }

    private OrderScanTaskDto<?> orderPickupTaskDto() {
        var orders = List.of(
                new OrderScanTaskDto.OrderForScanDto(false, "1574247346365", "1574247346365", OK, "Доставка в 12:20 " +
                        "Сканируйте следующую!", null, null, null, List.of(new PlaceForScanDto(
                        "1574247346365")),
                        OrderFlowStatus.TRANSPORTATION_RECIPIENT, null, null),
                new OrderScanTaskDto.OrderForScanDto(false, "1574247346460", "1574247346460", OK, "Доставка в 15:20 " +
                        "Сканируйте следующую!", null, null, null, List.of(new PlaceForScanDto(
                        "1574247346460")),
                        OrderFlowStatus.TRANSPORTATION_RECIPIENT, null, null)
        );
        return orderPickupTaskDto(false, orders);
    }

    private OrderScanTaskDto<?> orderPickupTaskDto(boolean finished, List<OrderScanTaskDto.OrderForScanDto> orders) {
        var pickupTaskDto = new OrderPickupTaskDto();
        pickupTaskDto.setId(2562);
        pickupTaskDto.setName("Забор заказов");
        pickupTaskDto.setType(TaskType.ORDER_PICKUP);
        pickupTaskDto.setStatus(finished
                ? OrderPickupTaskStatus.PARTIALLY_FINISHED
                : OrderPickupTaskStatus.NOT_STARTED);
        pickupTaskDto.setOrders(orders);
        pickupTaskDto.setCompletedOrders(finished ? orders.subList(0, 1) : List.of());
        pickupTaskDto.setSkippedOrders(finished ? orders.subList(1, 2) : List.of());
        return pickupTaskDto;
    }

    private String orderPickupTaskJson = "{\n" +
            "    \"id\": 2562,\n" +
            "    \"name\": \"Забор заказов\",\n" +
            "    \"type\": \"ORDER_PICKUP\",\n" +
            "    \"status\": \"NOT_STARTED\",\n" +
            "    \"orders\": [\n" +
            "        {\n" +
            "            \"displayMode\" : \"OK\",\n" +
            "            \"text\": \"Доставка в 12:20 Сканируйте следующую!\",\n" +
            "            \"externalOrderId\": \"1574247346365\",\n" +
            "            \"places\": [\n" +
            "               {\n" +
            "                   \"barcode\": \"1574247346365\"\n" +
            "               }\n" +
            "            ]\n" +
            "        },\n" +
            "        {\n" +
            "            \"displayMode\" : \"OK\",\n" +
            "            \"text\": \"Доставка в 15:20 Сканируйте следующую!\",\n" +
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
            "}";


    @Test
    void pickupOrders() throws Exception {
        long uid = 8347L;
        var orders = List.of(
                new OrderScanTaskDto.OrderForScanDto(false, "92342", "92342", OK, "Доставка в 12:20 Сканируйте " +
                        "следующую!", null, null, null, List.of(new PlaceForScanDto("92342")),
                        OrderFlowStatus.TRANSPORTATION_RECIPIENT, null, null),
                new OrderScanTaskDto.OrderForScanDto(false, "83475", "83475", OK, "Доставка в 14:20 Сканируйте " +
                        "следующую!", null, null, null, List.of(new PlaceForScanDto("83475")),
                        OrderFlowStatus.TRANSPORTATION_RECIPIENT, null, null)
        );
        var response = orderPickupTaskDto(true, orders);
        doReturn(response).when(orderPickupTaskFacade).pickupOrders(anyLong(), any(), any());
        mockMvc.perform(
                        post("/api/tasks/order-pickup/4765/finish")
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
                                        "      \"displayMode\" : \"ERROR\",\n" +
                                        "      \"text\": \"Доставка в 14:20 Сканируйте следующую!\",\n" +
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
                .andExpect(content().json(
                        "{\n" +
                        "  \"id\": 2562,\n" +
                        "  \"name\": \"Забор заказов\",\n" +
                        "  \"type\": \"ORDER_PICKUP\",\n" +
                        "  \"status\": \"PARTIALLY_FINISHED\",\n" +
                        "  \"orders\": [\n" +
                        "    {\n" +
                        "      \"externalOrderId\": \"92342\",\n" +
                        "      \"multiOrderId\": \"92342\",\n" +
                        "      \"isMultiOrder\": false,\n" +
                        "      \"places\": [\n" +
                        "        {\n" +
                        "          \"barcode\": \"92342\"\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"externalOrderId\": \"83475\",\n" +
                        "      \"multiOrderId\": \"83475\",\n" +
                        "      \"isMultiOrder\": false,\n" +
                        "      \"places\": [\n" +
                        "        {\n" +
                        "          \"barcode\": \"83475\"\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ],\n" +
                        "  \"completedOrders\": [" +
                        "\n" +
                        "    {\n" +
                        "      \"externalOrderId\": \"92342\",\n" +
                        "      \"multiOrderId\": \"92342\",\n" +
                        "      \"isMultiOrder\": false,\n" +
                        "      \"places\": [\n" +
                        "        {\n" +
                        "          \"barcode\": \"92342\"\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ],\n" +
                        "  \"skippedOrders\": [" +
                        "\n" +
                        "    {\n" +
                        "      \"externalOrderId\": \"83475\",\n" +
                        "      \"multiOrderId\": \"83475\",\n" +
                        "      \"isMultiOrder\": false,\n" +
                        "      \"places\": [\n" +
                        "        {\n" +
                        "          \"barcode\": \"83475\"\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}"
                ));
    }

    @Test
    @SneakyThrows
    void updateVehicleData() {
        long uid = 8347L;
        mockMvc.perform(
                        post("/api/tasks/order-pickup/4765/update-vehicle-data")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("" +
                                        "{\n" +
                                        "    \"vehicleInstance\": {\n" +
                                        "        \"vehicleInstanceId\": 1,\n" +
                                        "        \"registrationNumber\": \"A001MP\",\n" +
                                        "        \"registrationNumberRegion\" : \"777\" \n" +
                                        "    }\n" +
                                        "}"
                                )
                                .header("Authorization", "OAuth uid-" + uid)
                )
                .andExpect(status().is2xxSuccessful());
    }

}
