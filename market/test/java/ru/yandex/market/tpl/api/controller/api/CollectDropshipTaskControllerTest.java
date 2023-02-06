package ru.yandex.market.tpl.api.controller.api;

import java.time.Clock;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.api.BaseShallowTest;
import ru.yandex.market.tpl.api.WebLayerTest;
import ru.yandex.market.tpl.api.model.order.LocationDetailsDto;
import ru.yandex.market.tpl.api.model.task.CollectDropshipTaskDto;
import ru.yandex.market.tpl.api.model.task.CollectDropshipTaskStatus;
import ru.yandex.market.tpl.api.model.task.TaskType;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnQueryService;
import ru.yandex.market.tpl.core.domain.clientreturn.mapper.ClientReturnMapper;
import ru.yandex.market.tpl.core.domain.clientreturn.repository.ClientReturnRepository;
import ru.yandex.market.tpl.core.domain.dropoffcargo.repository.DropoffCargoRepository;
import ru.yandex.market.tpl.core.domain.ds.DsZoneOffsetCachingService;
import ru.yandex.market.tpl.core.domain.movement.MovementRepository;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.order.RoutePointPostponementHistoryEventRepository;
import ru.yandex.market.tpl.core.domain.order.difference.OrderHistoryService;
import ru.yandex.market.tpl.core.domain.partial_return_order.PartialReturnOrderQueryService;
import ru.yandex.market.tpl.core.domain.sc.model.ScOrderRepository;
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
import ru.yandex.market.tpl.core.service.order.CallRequirementResolver;
import ru.yandex.market.tpl.core.service.partial_return.PartialReturnOrderService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author valter
 */
@SuppressWarnings("unused")
@WebLayerTest(CollectDropshipTaskController.class)
class CollectDropshipTaskControllerTest extends BaseShallowTest {

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
    @SpyBean
    private TaskDtoMapper taskDtoMapper;
    @MockBean
    private OrderPlaceMapper orderPlaceMapper;
    @MockBean
    private OrderHistoryService orderHistoryService;
    @MockBean
    private MovementRepository movementRepository;
    @SpyBean
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
    private DsZoneOffsetCachingService dsZoneOffsetCachingService;
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
    private DurationCalculator durationCalculator;

    @Test
    void cancelTask() throws Exception {
        long uid = 8347L;
        doReturn(199L).when(queryService).getCurrentShiftId(any());
        var response = taskDto();
        doReturn(response).when(queryService).getTaskInfo(any(), anyLong(), anyLong());
        doReturn(Map.of(
                "83475", 1L,
                "92342", 2L
        )).when(taskDtoMapper).getOrdersMap(eq(List.of("92342", "83475")));
        mockMvc.perform(
                post("/api/route-points/199/tasks/collect-dropship/4765/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "OAuth uid-" + uid)
                        .content("{\"reason\": \"не вышло\"}")
        )
                .andExpect(status().is2xxSuccessful())
                .andDo(print())
                .andExpect(content().json("" +
                        "{\n" +
                        "  \"id\": 2562,\n" +
                        "  \"name\": \"Забор дропшипов\",\n" +
                        "  \"type\": \"COLLECT_DROPSHIP\",\n" +
                        "  \"status\": \"CANCELLED\",\n" +
                        "  \"contact\": \"Иван Дропшипов\",\n" +
                        "  \"address\": \"г. Москва, Пушкина, д. Колотушкина, кв. 10, этаж 1\",\n" +
                        "  \"phones\": [\n" +
                        "    \"223322223322\"\n" +
                        "  ],\n" +
                        "  \"description\": \"Спросить старшего\",\n" +
                        "  \"workingHours\": \"09:27 - 10:27\"\n" +
                        "}"
                ));
    }

    private CollectDropshipTaskDto taskDto() {
        var taskDto = new CollectDropshipTaskDto();
        taskDto.setId(2562);
        taskDto.setName("Забор дропшипов");
        taskDto.setType(TaskType.COLLECT_DROPSHIP);
        taskDto.setStatus(CollectDropshipTaskStatus.CANCELLED);

        var locationDetailsDto = new LocationDetailsDto();
        locationDetailsDto.setContact("Иван Дропшипов");
        locationDetailsDto.setAddress("г. Москва, Пушкина, д. Колотушкина, кв. 10, этаж 1");
        locationDetailsDto.setPhones(List.of("223322223322"));
        locationDetailsDto.setDescription("Спросить старшего");
        locationDetailsDto.setWorkingHours("09:27 - 10:27");

        taskDto.setLocationDetails(locationDetailsDto);
        return taskDto;
    }

}
