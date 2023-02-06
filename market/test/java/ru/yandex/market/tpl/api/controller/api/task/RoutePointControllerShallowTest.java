package ru.yandex.market.tpl.api.controller.api.task;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.tpl.api.BaseShallowTest;
import ru.yandex.market.tpl.api.WebLayerTest;
import ru.yandex.market.tpl.api.controller.api.RoutePointController;
import ru.yandex.market.tpl.api.facade.RoutePointFacade;
import ru.yandex.market.tpl.api.model.order.OrderBatchDto;
import ru.yandex.market.tpl.api.model.order.OrderDto;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.PlaceDto;
import ru.yandex.market.tpl.api.model.order.destination.DestinationOrderDto;
import ru.yandex.market.tpl.api.model.order.destination.ScanTaskDestinationDto;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointAddressDto;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointDto;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointListDto;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointStatus;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointSummaryDto;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.api.model.task.OrderPickupTaskDto;
import ru.yandex.market.tpl.api.model.task.OrderPickupTaskStatus;
import ru.yandex.market.tpl.api.model.task.OrderScanTaskDto;
import ru.yandex.market.tpl.api.model.task.PlaceForScanDto;
import ru.yandex.market.tpl.api.model.task.TaskType;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.service.locker.LockerDeliveryService;
import ru.yandex.market.tpl.core.service.usershift.ArriveAtRoutePointService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author kukabara
 */
@WebLayerTest(RoutePointController.class)
class RoutePointControllerShallowTest extends BaseShallowTest {

    @MockBean
    private UserShiftCommandService commandService;
    @MockBean
    private ArriveAtRoutePointService arriveAtRoutePointService;
    @MockBean
    private LockerDeliveryService lockerDeliveryService;
    @MockBean
    private RoutePointFacade routePointFacade;

    @Test
    void routePointSingle() throws Exception {
        long routePointId = 12312L;
        when(routePointFacade.getRoutePointInfo(anyLong(), any())).thenReturn(
                new RoutePointDto(
                        1L,
                        RoutePointType.ORDER_PICKUP,
                        RoutePointStatus.IN_PROGRESS,
                        "Льва Толстого д. 16, кв. 1",
                        routePointAddressDto(),
                        List.of(orderPickupTaskDto()),
                        List.of(),
                        Instant.parse("2019-08-07T16:15:37.498398Z"),
                        List.of(new RoutePointDto.Action(RoutePointDto.ActionType.SWITCH)),
                        null,
                        null,
                        false,
                        null,
                        Set.of()
                )
        );

        mockMvc.perform(get("/api/route-points/" + routePointId)
                .header("Authorization", "OAuth uid-1")
        )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json("{\n" +
                        "  \"id\": 1,\n" +
                        "  \"type\": \"ORDER_PICKUP\",\n" +
                        "  \"status\": \"IN_PROGRESS\",\n" +
                        "  \"name\": \"Льва Толстого д. 16, кв. 1\",\n" +
                        "  \"address\": {\n" +
                        "    \"addressString\": \"Льва Толстого д. 16, кв. 1\",\n" +
                        "    \"latitude\": 55.737991,\n" +
                        "    \"longitude\": 37.744751\n" +
                        "  },\n" +
                        "  \"tasks\": [\n" +
                        "    {\n" +
                        "      \"id\": 2562,\n" +
                        "      \"name\": \"Забор заказов\",\n" +
                        "      \"type\": \"ORDER_PICKUP\",\n" +
                        "      \"status\": \"NOT_STARTED\",\n" +
                        "      \"orders\": [\n" +
                        "        {\n" +
                        "          \"displayMode\" : \"OK\",\n" +
                        "          \"text\": \"Заказ перенесён, доставка сегодня не нужна!\",\n" +
                        "          \"externalOrderId\": \"1574247346365\",\n" +
                        "          \"multiOrderId\": \"1574247346365\",\n" +
                        "          \"isMultiOrder\": false\n" +
                        "        }\n" +
                        "      ],\n" +
                        "      \"completedOrders\": [],\n" +
                        "      \"skippedOrders\": [],\n" +
                        "      \"destinations\": [\n" +
                        "        {\n" +
                        "          \"batches\": [\n" +
                        "            {\n" +
                        "              \"barcode\": \"SC_LOT_123456\",\n" +
                        "              \"orders\": [\n" +
                        "                {\n" +
                        "                  \"externalOrderId\": \"1574247346365\",\n" +
                        "                  \"places\": [\n" +
                        "                    {\n" +
                        "                      \"barcode\": \"1574247346365\"\n" +
                        "                    }\n" +
                        "                  ]\n" +
                        "                }\n" +
                        "              ]\n" +
                        "            }\n" +
                        "          ],\n" +
                        "          \"outsideOrders\": [\n" +
                        "            {\n" +
                        "              \"externalOrderId\": \"1574247346365\",\n" +
                        "              \"places\": [\n" +
                        "                {\n" +
                        "                  \"barcode\": \"1574247346365\"\n" +
                        "                }\n" +
                        "              ]\n" +
                        "            }\n" +
                        "          ]\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ],\n" +
                        "  \"expectedDate\": \"2019-08-07T16:15:37.498398Z\",\n" +
                        "  \"actions\": [{\"type\":\"SWITCH\"}]\n" +
                        "}\n"
                ));
    }

    private OrderScanTaskDto<?> orderPickupTaskDto() {
        var task = new OrderPickupTaskDto();
        task.setId(2562);
        task.setName("Забор заказов");
        task.setType(TaskType.ORDER_PICKUP);
        task.setStatus(OrderPickupTaskStatus.NOT_STARTED);
        task.setOrders(List.of(new OrderScanTaskDto.OrderForScanDto(
                false, "1574247346365", "1574247346365", OrderScanTaskDto.ScanOrderDisplayMode.OK,
                "Заказ перенесён, доставка сегодня не нужна!", null, null, null,
                List.of(new PlaceForScanDto("1574247346365")),
                OrderFlowStatus.TRANSPORTATION_RECIPIENT, null, null
        )));
        task.setCompletedOrders(List.of());
        task.setSkippedOrders(List.of());
        Set<OrderBatchDto> batches = Set.of(
                OrderBatchDto.builder()
                        .barcode("SC_LOT_123456")
                        .orders(Set.of(
                                OrderDto.builder()
                                        .externalOrderId("1574247346365")
                                        .places(List.of(
                                                PlaceDto.builder()
                                                        .barcode("1574247346365")
                                                        .build()
                                        ))
                                        .build()
                        ))
                        .build()
        );
        task.setDestinations(List.of(
                ScanTaskDestinationDto.builder()
                        .batches(batches)
                        .outsideOrders(Set.of(
                                DestinationOrderDto.builder()
                                        .externalOrderId("1574247346365")
                                        .places(List.of(
                                                PlaceDto.builder()
                                                        .barcode("1574247346365")
                                                        .build()
                                        ))
                                        .build()
                        ))
                        .build()
                )
        );
        return task;
    }

    private RoutePointAddressDto routePointAddressDto() {
        RoutePointAddressDto addressDto = new RoutePointAddressDto();
        addressDto.setAddressString("Льва Толстого д. 16, кв. 1");
        addressDto.setLatitude(new BigDecimal("55.737991"));
        addressDto.setLongitude(new BigDecimal("37.744751"));
        return addressDto;
    }

    @Test
    void shouldReturnRoutePointSummaries() throws Exception {
        when(routePointFacade.getRoutePointsSummaries(any())).thenReturn(
                new RoutePointListDto(1,
                        LocalTime.parse("07:15:37.498398"),
                        List.of(new RoutePointSummaryDto(
                        1L,
                        RoutePointType.DELIVERY,
                        RoutePointStatus.NOT_STARTED,
                        "Склад \"Беру!\"",
                        routePointAddressDto(),
                        Instant.parse("2019-08-07T16:15:37.498398Z"),
                        new RoutePointSummaryDto.ItemCount(
                                10,
                                1,
                                2
                        )
                )))
        );

        mockMvc.perform(get("/api/route-points")
                .header("Authorization", "OAuth uid-1")
        )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json("{\n" +
                        "  \"count\": 1," +
                        "  \"loadingStartTime\": \"07:15:37.498398\"," +
                        "  \"routePoints\": [\n" +
                        "    {\n" +
                        "      \"id\": 1,\n" +
                        "      \"type\": \"DELIVERY\",\n" +
                        "      \"status\": \"NOT_STARTED\",\n" +
                        "      \"name\": \"Склад \\\"Беру!\\\"\",\n" +
                        "      \"address\": {\n" +
                        "        \"addressString\": \"Льва Толстого д. 16, кв. 1\",\n" +
                        "        \"longitude\": 37.744751,\n" +
                        "        \"latitude\": 55.737991\n" +
                        "      },\n" +
                        "      \"expectedDate\": \"2019-08-07T16:15:37.498398Z\",\n" +
                        "      \"itemCount\":{" +
                        "        \"notStarted\": 10,\n" +
                        "        \"success\": 1,\n" +
                        "        \"fail\": 2\n" +
                        "      }\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}",
                        true
                ));
    }

}
