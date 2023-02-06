package ru.yandex.market.sc.internal.controller;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.persistence.EntityManager;

import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.order.OrderCommandService;
import ru.yandex.market.sc.core.domain.order.model.ApiOrderStatus;
import ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus;
import ru.yandex.market.sc.core.domain.order.repository.ScOrder;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderRepository;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.route.repository.Route;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * @author valter
 */
@SuppressWarnings("unused")
@Slf4j
@ScIntControllerTest
public class OrderControllerFullTest {

    private static final long UID = 111L;

    @Autowired
    MockMvc mockMvc;
    @Autowired
    TestFactory testFactory;
    @Autowired
    OrderCommandService orderCommandService;
    @Autowired
    EntityManager entityManager;
    @MockBean
    Clock clock;
    @Autowired
    TransactionTemplate transactionTemplate;
    @Autowired
    ScOrderRepository scOrderRepository;

    SortingCenter sortingCenter;
    User user;
    Cell cell;

    static Stream<TestStatusParams> statusParamsProvider() {
        return Stream.of(

                // ORDER_CREATED_FF=101
                new TestStatusParams(ScOrderFFStatus.ORDER_CREATED_FF,
                        "noPreparation", ApiOrderStatus.KEEP),
                new TestStatusParams(ScOrderFFStatus.ORDER_CREATED_FF,
                        "updateForTodayDelivery", ApiOrderStatus.SORT_TO_COURIER),
                new TestStatusParams(ScOrderFFStatus.ORDER_CREATED_FF,
                        "updateForTomorrowDelivery", ApiOrderStatus.KEEP),

                // ORDER_CANCELLED_FF=105
                new TestStatusParams(ScOrderFFStatus.ORDER_CANCELLED_FF,
                        "accept", ApiOrderStatus.SORT_TO_WAREHOUSE),
                new TestStatusParams(ScOrderFFStatus.ORDER_CANCELLED_FF,
                        "switchClockToTomorrow", ApiOrderStatus.KEEP),

                // ORDER_ARRIVED_TO_SO_WAREHOUSE=110
                new TestStatusParams(ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE,
                        "noPreparation", ApiOrderStatus.SORT_TO_COURIER),
                new TestStatusParams(ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE,
                        "switchClockToTomorrow", ApiOrderStatus.KEEP),
                new TestStatusParams(ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE,
                        "updateForTodayDelivery", ApiOrderStatus.SORT_TO_COURIER),
                new TestStatusParams(ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE,
                        "updateForTomorrowDelivery", ApiOrderStatus.KEEP),

                // ORDER_AWAITING_CLARIFICATION_FF=117
                new TestStatusParams(ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF,
                        "noPreparation", ApiOrderStatus.KEEP),
                new TestStatusParams(ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF,
                        "updateForTodayDelivery", ApiOrderStatus.SORT_TO_COURIER),
                new TestStatusParams(ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF,
                        "updateForTomorrowDelivery", ApiOrderStatus.KEEP),

                // ORDER_READY_TO_BE_SEND_TO_SO_FF=120
                new TestStatusParams(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF,
                        "noPreparation", ApiOrderStatus.OK),
                new TestStatusParams(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF,
                        "removeFromCell",
                        ApiOrderStatus.SORT_TO_COURIER
                ),

                new TestStatusParams(ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF,
                        "switchClockToTomorrow", ApiOrderStatus.KEEP),

                // ORDER_PREPARED_TO_BE_SEND_TO_SO=120
                new TestStatusParams(ScOrderFFStatus.ORDER_PREPARED_TO_BE_SEND_TO_SO,
                        "noPreparation", ApiOrderStatus.OK),
                new TestStatusParams(ScOrderFFStatus.ORDER_PREPARED_TO_BE_SEND_TO_SO,
                        "removeFromCell",
                        ApiOrderStatus.SORT_TO_COURIER // Коробка находится в ячейке - можно отгружать
                ),
                new TestStatusParams(ScOrderFFStatus.ORDER_PREPARED_TO_BE_SEND_TO_SO,
                        "switchClockToTomorrow", ApiOrderStatus.KEEP),

                // ORDER_SHIPPED_TO_SO_FF=130
                new TestStatusParams(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF,
                        "noPreparation", ApiOrderStatus.KEEP),
                new TestStatusParams(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF,
                        "switchClockToTomorrow", ApiOrderStatus.KEEP),

                // SO_GOT_INFO_ABOUT_PLANNED_RETURN=160
                new TestStatusParams(ScOrderFFStatus.SO_GOT_INFO_ABOUT_PLANNED_RETURN,
                        "noPreparation", ApiOrderStatus.SORT_TO_WAREHOUSE),
                new TestStatusParams(ScOrderFFStatus.SO_GOT_INFO_ABOUT_PLANNED_RETURN,
                        "switchClockToTomorrowAndRescheduleReturns",
                        ApiOrderStatus.SORT_TO_WAREHOUSE),

                // RETURNED_ORDER_AT_SO_WAREHOUSE=170
                new TestStatusParams(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE,
                        "noPreparation", ApiOrderStatus.SORT_TO_WAREHOUSE),
                new TestStatusParams(ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE,
                        "switchClockToTomorrowAndRescheduleReturns",
                        ApiOrderStatus.SORT_TO_WAREHOUSE),

                // RETURNED_ORDER_READY_TO_BE_SENT_TO_IM=175
                new TestStatusParams(ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM,
                        "noPreparation", ApiOrderStatus.OK),
                new TestStatusParams(ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM,
                        "removeFromCell",
                        ApiOrderStatus.SORT_TO_WAREHOUSE
                        ),
                new TestStatusParams(ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM,
                        "switchClockToTomorrowAndRescheduleReturns",
                        ApiOrderStatus.OK), // they are already in designated cell

                // RETURNED_ORDER_DELIVERED_TO_IM=180
                new TestStatusParams(ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM,
                        "noPreparation", ApiOrderStatus.WRONG_STATUS)
        );
    }

    static Stream<TestTransitionParams> transitionParamsProvider() {
        var cancel = List.of("cancelOrderFFApi", "cancelOrderManual");
        var update = List.of("updateOrderFFApi", "updateOrderManual");
        var returns = List.of("returnOrderFFApi", "returnOrderManual");
        var accept = List.of("acceptOrderManual");
        var sortToCourier = List.of("sortToCourierOrderManual");
        var sortToWarehouse = List.of("sortToWarehouseOrderManual");
        var keep = List.of("keepOrderManual");
        var prepareToShip = List.of("prepareToShipOrderManual");
        var ship = List.of("shipOrderManual");
        var acceptAndSortToCourier = List.of("acceptAndSortToCourierOrderManual");
        var acceptAndSortToWarehouse = List.of("acceptAndSortToWarehouseOrderManual");
        var acceptAndKeep = List.of("acceptAndKeepOrderManual");

        @Value
        class Tuple {

            List<String> transitionMethods;
            Function<String, TestTransitionParams> paramFactory;

        }

        return Stream.of(

                // ORDER_CREATED_FF=101
                new Tuple(cancel, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_CREATED_FF, "noPreparation",
                        tm, ScOrderFFStatus.ORDER_CANCELLED_FF, ApiOrderStatus.KEEP)),
                new Tuple(update, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_CREATED_FF, "noPreparation",
                        tm, ScOrderFFStatus.ORDER_CREATED_FF, ApiOrderStatus.SORT_TO_COURIER)),
                new Tuple(accept, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_CREATED_FF, "noPreparation",
                        tm, ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE, ApiOrderStatus.KEEP)),
                new Tuple(accept, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_CREATED_FF, "updateForTodayDelivery",
                        tm, ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE, ApiOrderStatus.SORT_TO_COURIER)),
                new Tuple(accept, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_CREATED_FF, "updateForTomorrowDelivery",
                        tm, ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE, ApiOrderStatus.KEEP)),
                new Tuple(accept, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_CREATED_FF, "switchClockToTomorrow",
                        tm, ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE, ApiOrderStatus.KEEP)),
                new Tuple(acceptAndKeep, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_CREATED_FF, "noPreparation",
                        tm, ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF, ApiOrderStatus.KEEP)),
                new Tuple(acceptAndKeep, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_CREATED_FF, "updateForTomorrowDelivery",
                        tm, ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF, ApiOrderStatus.KEEP)),
                new Tuple(acceptAndSortToCourier, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_CREATED_FF, "updateForTodayDelivery",
                        tm, ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF, ApiOrderStatus.OK)),

                // ORDER_CANCELLED_FF=105
                new Tuple(cancel, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_CANCELLED_FF, "noPreparation",
                        tm, ScOrderFFStatus.ORDER_CANCELLED_FF, ApiOrderStatus.KEEP)),
                new Tuple(accept, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_CANCELLED_FF, "noPreparation",
                        tm, ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE, ApiOrderStatus.SORT_TO_WAREHOUSE)),
                new Tuple(accept, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_CANCELLED_FF, "switchClockToTomorrow",
                        tm, ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE, ApiOrderStatus.SORT_TO_WAREHOUSE)),
                new Tuple(acceptAndKeep, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_CANCELLED_FF, "switchClockToTomorrow",
                        tm, ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE, ApiOrderStatus.SORT_TO_WAREHOUSE)),

                // ORDER_ARRIVED_TO_SO_WAREHOUSE=110
                new Tuple(acceptAndKeep, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE, "switchClockToTomorrow",
                        tm, ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF, ApiOrderStatus.KEEP)),
                new Tuple(acceptAndSortToCourier, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE, "noPreparation",
                        tm, ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF, ApiOrderStatus.OK)),
                new Tuple(keep, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE, "switchClockToTomorrow",
                        tm, ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF, ApiOrderStatus.KEEP)),
                new Tuple(sortToCourier, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE, "noPreparation",
                        tm, ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF, ApiOrderStatus.OK)),
                new Tuple(returns, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE, "noPreparation",
                        tm, ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE, ApiOrderStatus.SORT_TO_WAREHOUSE)),
                new Tuple(returns, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE, "switchClockToTomorrow",
                        tm, ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE, ApiOrderStatus.SORT_TO_WAREHOUSE)),
                new Tuple(cancel, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE, "noPreparation",
                        tm, ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE, ApiOrderStatus.SORT_TO_WAREHOUSE)),
                new Tuple(cancel, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE, "switchClockToTomorrow",
                        tm, ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE, ApiOrderStatus.SORT_TO_WAREHOUSE)),

                // ORDER_AWAITING_CLARIFICATION_FF=117
                new Tuple(cancel, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF, "noPreparation",
                        tm, ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE, ApiOrderStatus.SORT_TO_WAREHOUSE)),
                new Tuple(cancel, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF, "noPreparation",
                        tm, ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE, ApiOrderStatus.SORT_TO_WAREHOUSE)),
                new Tuple(cancel, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF, "switchClockToTomorrow",
                        tm, ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE, ApiOrderStatus.SORT_TO_WAREHOUSE)),
                new Tuple(cancel, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF, "updateForTodayDelivery",
                        tm, ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE, ApiOrderStatus.SORT_TO_WAREHOUSE)),
                new Tuple(cancel, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF, "updateForTomorrowDelivery",
                        tm, ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE, ApiOrderStatus.SORT_TO_WAREHOUSE)),
                new Tuple(returns, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF, "noPreparation",
                        tm, ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE, ApiOrderStatus.SORT_TO_WAREHOUSE)),
                new Tuple(returns, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF, "switchClockToTomorrow",
                        tm, ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE, ApiOrderStatus.SORT_TO_WAREHOUSE)),
                new Tuple(returns, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF, "updateForTodayDelivery",
                        tm, ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE, ApiOrderStatus.SORT_TO_WAREHOUSE)),
                new Tuple(returns, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF, "updateForTomorrowDelivery",
                        tm, ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE, ApiOrderStatus.SORT_TO_WAREHOUSE)),
                new Tuple(update, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF, "noPreparation",
                        tm, ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF, ApiOrderStatus.SORT_TO_COURIER)),
                new Tuple(accept, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF, "noPreparation",
                        tm, ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF, ApiOrderStatus.KEEP)),
                new Tuple(accept, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF, "updateForTodayDelivery",
                        tm, ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF, ApiOrderStatus.SORT_TO_COURIER)),
                new Tuple(accept, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF, "updateForTomorrowDelivery",
                        tm, ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF, ApiOrderStatus.KEEP)),
                new Tuple(acceptAndSortToCourier, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF, "updateForTodayDelivery",
                        tm, ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF, ApiOrderStatus.OK)),
                new Tuple(acceptAndKeep, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF, "updateForTomorrowDelivery",
                        tm, ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF, ApiOrderStatus.KEEP)),
                new Tuple(acceptAndKeep, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF, "noPreparation",
                        tm, ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF, ApiOrderStatus.KEEP)),
                new Tuple(sortToCourier, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF, "updateForTodayDelivery",
                        tm, ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF, ApiOrderStatus.OK)),

                // ORDER_READY_TO_BE_SEND_TO_SO_FF=120
                new Tuple(prepareToShip, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF, "noPreparation",
                        tm, ScOrderFFStatus.ORDER_PREPARED_TO_BE_SEND_TO_SO, ApiOrderStatus.OK)),
                new Tuple(ship, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF, "noPreparation",
                        tm, ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF, ApiOrderStatus.KEEP)),
                new Tuple(keep, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF, "switchClockToTomorrow",
                        tm, ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF, ApiOrderStatus.KEEP)),
                new Tuple(acceptAndKeep, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF, "switchClockToTomorrow",
                        tm, ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF, ApiOrderStatus.KEEP)),

                // ORDER_PREPARED_TO_BE_SEND_TO_SO=120
                new Tuple(ship, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_PREPARED_TO_BE_SEND_TO_SO, "noPreparation",
                        tm, ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF, ApiOrderStatus.KEEP)),
                new Tuple(keep, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_PREPARED_TO_BE_SEND_TO_SO, "switchClockToTomorrow",
                        tm, ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF, ApiOrderStatus.KEEP)),
                new Tuple(acceptAndKeep, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_PREPARED_TO_BE_SEND_TO_SO, "switchClockToTomorrow",
                        tm, ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF, ApiOrderStatus.KEEP)),

                // ORDER_SHIPPED_TO_SO_FF=130
                new Tuple(returns, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF, "noPreparation",
                        tm, ScOrderFFStatus.SO_GOT_INFO_ABOUT_PLANNED_RETURN, ApiOrderStatus.SORT_TO_WAREHOUSE)),
                new Tuple(returns, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF, "switchClockToTomorrow",
                        tm, ScOrderFFStatus.SO_GOT_INFO_ABOUT_PLANNED_RETURN, ApiOrderStatus.SORT_TO_WAREHOUSE)),
                new Tuple(cancel, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF, "noPreparation",
                        tm, ScOrderFFStatus.SO_GOT_INFO_ABOUT_PLANNED_RETURN, ApiOrderStatus.SORT_TO_WAREHOUSE)),
                new Tuple(cancel, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF, "switchClockToTomorrow",
                        tm, ScOrderFFStatus.SO_GOT_INFO_ABOUT_PLANNED_RETURN, ApiOrderStatus.SORT_TO_WAREHOUSE)),
                new Tuple(accept, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF, "noPreparation",
                        tm, ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF, ApiOrderStatus.KEEP)),
                new Tuple(accept, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF, "switchClockToTomorrow",
                        tm, ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF, ApiOrderStatus.KEEP)),
                new Tuple(acceptAndKeep, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF, "noPreparation",
                        tm, ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF, ApiOrderStatus.KEEP)),
                new Tuple(acceptAndKeep, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF, "switchClockToTomorrow",
                        tm, ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF, ApiOrderStatus.KEEP)),

                // SO_GOT_INFO_ABOUT_PLANNED_RETURN=160
                new Tuple(returns, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.SO_GOT_INFO_ABOUT_PLANNED_RETURN, "noPreparation",
                        tm, ScOrderFFStatus.SO_GOT_INFO_ABOUT_PLANNED_RETURN, ApiOrderStatus.SORT_TO_WAREHOUSE)),
                new Tuple(cancel, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.SO_GOT_INFO_ABOUT_PLANNED_RETURN, "noPreparation",
                        tm, ScOrderFFStatus.SO_GOT_INFO_ABOUT_PLANNED_RETURN, ApiOrderStatus.SORT_TO_WAREHOUSE)),
                new Tuple(accept, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.SO_GOT_INFO_ABOUT_PLANNED_RETURN, "noPreparation",
                        tm, ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE, ApiOrderStatus.SORT_TO_WAREHOUSE)),
                new Tuple(acceptAndSortToWarehouse, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.SO_GOT_INFO_ABOUT_PLANNED_RETURN, "noPreparation",
                        tm, ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM, ApiOrderStatus.OK)),
                new Tuple(acceptAndSortToWarehouse, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.SO_GOT_INFO_ABOUT_PLANNED_RETURN,
                        "switchClockToTomorrowAndRescheduleReturns",
                        tm, ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM, ApiOrderStatus.OK)),

                // RETURNED_ORDER_AT_SO_WAREHOUSE=170
                new Tuple(returns, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE, "noPreparation",
                        tm, ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE, ApiOrderStatus.SORT_TO_WAREHOUSE)),
                new Tuple(cancel, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE, "noPreparation",
                        tm, ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE, ApiOrderStatus.SORT_TO_WAREHOUSE)),
                new Tuple(keep, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE, "switchClockToTomorrow",
                        tm, ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE, ApiOrderStatus.KEEP)),
                new Tuple(sortToWarehouse, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE, "noPreparation",
                        tm, ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM, ApiOrderStatus.OK)),
                new Tuple(acceptAndSortToWarehouse, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE, "noPreparation",
                        tm, ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM, ApiOrderStatus.OK)),
                new Tuple(acceptAndKeep, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE, "switchClockToTomorrow",
                        tm, ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE, ApiOrderStatus.KEEP)),

                // RETURNED_ORDER_READY_TO_BE_SENT_TO_IM=175
                new Tuple(returns, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM, "noPreparation",
                        tm, ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM, ApiOrderStatus.OK)),
                new Tuple(cancel, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM, "noPreparation",
                        tm, ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM, ApiOrderStatus.OK)),
                new Tuple(ship, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM, "noPreparation",
                        tm, ScOrderFFStatus.RETURNED_ORDER_DELIVERED_TO_IM, ApiOrderStatus.WRONG_STATUS)),
                new Tuple(sortToWarehouse, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM, "noPreparation",
                        tm, ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM, ApiOrderStatus.OK)),
                new Tuple(sortToWarehouse, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM, "removeFromCell",
                        tm, ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM, ApiOrderStatus.OK)),
                new Tuple(keep, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM, "switchClockToTomorrow",
                        tm, ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM, ApiOrderStatus.KEEP)),
                new Tuple(acceptAndSortToWarehouse, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM, "noPreparation",
                        tm, ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM, ApiOrderStatus.OK)),
                new Tuple(acceptAndSortToWarehouse, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM, "removeFromCell",
                        tm, ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM, ApiOrderStatus.OK)),
                new Tuple(acceptAndKeep, (tm) -> new TestTransitionParams(
                        ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM, "switchClockToTomorrow",
                        tm, ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM, ApiOrderStatus.KEEP))
        )
                .flatMap(t -> t.getTransitionMethods().stream().map(t.getParamFactory()));
    }

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        user = testFactory.storedUser(sortingCenter, 1234L);
        cell = testFactory.storedActiveCell(sortingCenter);
        testFactory.storedUser(sortingCenter, UID);
        doReturn(LocalDate.of(2020, 12, 21).atStartOfDay().toInstant(ZoneOffset.UTC))
                .when(clock).instant();
        doReturn(ZoneId.of("UTC")).when(clock).getZone();
    }

    @SneakyThrows
    @ParameterizedTest
    @MethodSource("statusParamsProvider")
    void testStatus(TestStatusParams params) {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED, "false");
        ScOrder order = createOrderInStatus(params.getStatus());
        OrderControllerFullTest.class.getDeclaredMethod(params.getPreparationMethodName(), ScOrder.class)
                .invoke(this, order);
        checkOrderInApi(order, params.getStatusInApi());
    }

    @SneakyThrows
    @ParameterizedTest
    @MethodSource("transitionParamsProvider")
    void testTransition(TestTransitionParams params) {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.BUFFER_RETURNS_ENABLED, "false");
        ScOrder order = createOrderInStatus(params.getStatusBefore());
        long orderId = order.getId();
        OrderControllerFullTest.class.getDeclaredMethod(params.getPreparationMethodName(), ScOrder.class)
                .invoke(this, order);
        order = testFactory.getOrder(orderId);
        OrderControllerFullTest.class.getDeclaredMethod(params.getTransitionsMethodName(), ScOrder.class)
                .invoke(this, order);
        order = transactionTemplate.execute(ts -> {
            var actualOrder = testFactory.getOrder(orderId);
            assertThat(actualOrder.getOrderStatus()).isEqualTo(params.getStatusAfter());
            return actualOrder;
        });
        checkOrderInApi(Objects.requireNonNull(order), params.getStatusAfterInApi());
    }

    @SneakyThrows
    private void checkOrderInApi(ScOrder order, ApiOrderStatus status) {

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/manual/orders" +
                                        "?externalId=" + order.getExternalId() + "&uid=" + UID)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().json(expectedOrderJson(order, status), false));
    }

    private String expectedOrderJson(ScOrder order, ApiOrderStatus status) {
        StringBuilder result = new StringBuilder("{" +
                "\"id\":" + order.getId() + "," +
                "\"externalId\":\"" + order.getExternalId() + "\"," +
                "\"status\":\"" + status + "\"");
        result.append("}");
        return result.toString();
    }

    private void noPreparation(ScOrder order) {
        // do nothing
    }

    private void removeFromCell(ScOrder order) {
        orderCommandService.returnToBuffer(order.getId(), user);
    }

    private void accept(ScOrder order) {
        testFactory.accept(order);
    }

    private void updateForTodayDelivery(ScOrder order) {
        testFactory.updateForTodayDelivery(order);
    }

    private void updateForTomorrowDelivery(ScOrder order) {
        testFactory.updateForTodayDelivery(order);
        doReturn(clock.instant().minus(1, ChronoUnit.DAYS)).when(clock).instant();
    }

    private void switchClockToTomorrow(ScOrder order) {
        doReturn(clock.instant().plus(1, ChronoUnit.DAYS)).when(clock).instant();
    }

    private void switchClockToTomorrowAndRescheduleReturns(ScOrder order) {
        doReturn(clock.instant().plus(1, ChronoUnit.DAYS)).when(clock).instant();
        testFactory.rescheduleSortReturns(sortingCenter);
    }

    private void cancelOrderFFApi(ScOrder order) {
        ffApiRequest(String.format(fileContent("ff_cancel_order.xml"), order.getSortingCenter().getToken(),
                order.getExternalId()));
    }

    private void updateOrderFFApi(ScOrder order) {
        ffApiRequest(String.format(fileContent("ff_update_order.xml"), order.getSortingCenter().getToken(),
                order.getExternalId()));
    }

    private void returnOrderFFApi(ScOrder order) {
        ffApiRequest(String.format(fileContent("ff_return_order.xml"), order.getSortingCenter().getToken(),
                order.getExternalId()));
    }

    @SneakyThrows
    private void cancelOrderManual(ScOrder order) {
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/manual/orders/cancel?externalOrderId=" + order.getExternalId())
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());
    }

    @SneakyThrows
    private void updateOrderManual(ScOrder order) {
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/manual/orders/updateShipmentDate" +
                                        "?externalOrderId=" + order.getExternalId() + "&shipmentDate=" + LocalDate.now(clock))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/manual/orders/updateCourier" +
                                        "?externalOrderId=" + order.getExternalId())
                                .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"1\",\"name\":\"Иван Пивовар Таранов\"}")
        )
                .andExpect(status().isOk());
    }

    @SneakyThrows
    private void returnOrderManual(ScOrder order) {
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/manual/orders/return?externalOrderId=" +
                                        order.getExternalId() + "&uid=" + user.getUid())
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());
    }

    @SneakyThrows
    private void acceptOrderManual(ScOrder order) {
        List<Place> places = testFactory.orderPlaces(order);
        assertThat(places).hasSize(1);
        String finalUrl = "/manual/orders/accept" +
                "?externalOrderId=" + order.getExternalId() + "&uid=" + user.getUid()
                    + "&externalPlaceId=" + places.get(0).getMainPartnerCode();

        mockMvc.perform(
                        MockMvcRequestBuilders.post(finalUrl)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());

    }

    @SneakyThrows
    private void sortToCourierOrderManual(ScOrder order) {
        Cell cell = testFactory.determineRouteCell(
                testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow(), order);

        List<Place> places = testFactory.orderPlaces(order);
        assertThat(places).hasSize(1);

        String finalUrl = "/manual/orders/sort" +
                "?externalOrderId=" + order.getExternalId() + "&uid=" + user.getUid()
                + "&externalPlaceId=" + places.get(0).getMainPartnerCode()
                + "&cellId=" + Objects.requireNonNull(cell).getId();

        mockMvc.perform(
                        MockMvcRequestBuilders.post(finalUrl)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());
    }

    @SneakyThrows
    private void sortToWarehouseOrderManual(ScOrder order) {
        Cell cell = testFactory.determineRouteCell(
                testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow(), order);

        List<Place> places = testFactory.orderPlaces(order);
        assertThat(places).hasSize(1);

        String finalUrl = "/manual/orders/sort" +
                "?externalOrderId=" + order.getExternalId() + "&uid=" + user.getUid() +
                "&cellId=" + Objects.requireNonNull(cell).getId() +
                "&externalPlaceId=" + places.get(0).getMainPartnerCode();
        mockMvc.perform(
                        MockMvcRequestBuilders.post(finalUrl)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());
    }

    @SneakyThrows
    private void prepareToShipOrderManual(ScOrder order) {
        Cell cell = testFactory.determineRouteCell(
                testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow(), order);
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();

        List<Place> places = testFactory.orderPlaces(order);
        assertThat(places).hasSize(1);

        String finalUrl = "/manual/orders/preship" +
                "?externalOrderId=" + order.getExternalId() + "&uid=" + user.getUid() +
                "&cellId=" + Objects.requireNonNull(cell).getId() + "" +
                "&routeId=" + testFactory.getRouteIdForSortableFlow(route) +
                "&externalPlaceId=" + places.get(0).getMainPartnerCode();

        mockMvc.perform(
                        MockMvcRequestBuilders.post(finalUrl)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());

    }

    @SneakyThrows
    private void keepOrderManual(ScOrder order) {
        List<Place> places = testFactory.orderPlaces(order);
        var cell = testFactory.storedCell(sortingCenter, "b-1", CellType.BUFFER);
        assertThat(places).hasSize(1);
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/manual/orders/keep" +
                                "?externalOrderId=" + order.getExternalId() +
                                "&uid=" + user.getUid() +
                                "&cellId=" + cell.getId() +
                                "&externalPlaceId=" + places.get(0).getMainPartnerCode())
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());
    }

    @SneakyThrows
    private void shipOrderManual(ScOrder order) {
        Route route = order.getOrderStatus().getOutgoingRouteType() != null
                && order.getOrderStatus().getOutgoingRouteType().isCourier()
                ? testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow()
                : testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        Cell cell = testFactory.determineRouteCell(route, order);
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/manual/routes/"
                                        + testFactory.getRouteIdForSortableFlow(route) + "/ship?uid=" + UID
                                + "&scId="+sortingCenter.getId()
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"ordersShipped\":[\"" + order.getExternalId() + "\"], " +
                                        "\"cellId\":" + Objects.requireNonNull(cell).getId() + "}")
                )
                .andDo(rh -> System.out.println("Response : "+rh.getResponse()))
                .andExpect(status().isOk());
    }

    @SneakyThrows
    private void acceptAndKeepOrderManual(ScOrder order) {

        List<Place> places = testFactory.orderPlaces(order);
        assertThat(places).hasSize(1);

        String finalUrl = "/manual/orders/acceptAndSort" +
                "?externalOrderId=" + order.getExternalId() + "&uid=" + user.getUid()
                + "&externalPlaceId=" + places.get(0).getMainPartnerCode()
                + "&cellId=" + testFactory.storedCell(sortingCenter, "cell_number").getId();

        mockMvc.perform(
                        MockMvcRequestBuilders.post(finalUrl)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());
    }

    @SneakyThrows
    private void acceptAndSortToWarehouseOrderManual(ScOrder order) {
        Route route = testFactory.findOutgoingWarehouseRoute(testFactory.getOrderLikeForRouteLookup(order))
                .or(() -> testFactory.findPossibleRouteForCancelledOrder(order)).orElseThrow();
        Optional<Cell> cell = testFactory.findRouteCell(route, order);

        List<Place> places = testFactory.orderPlaces(order);
        assertThat(places).hasSize(1);

        String finalUrl =  "/manual/orders/acceptAndSort" +
                "?externalOrderId=" + order.getExternalId() + "&uid=" + user.getUid() +
                (cell.map(value -> ("&cellId=" + value.getId())).orElse("")) +
                "&externalPlaceId=" + places.get(0).getMainPartnerCode();

        mockMvc.perform(
                        MockMvcRequestBuilders.post(finalUrl)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());


    }

    @SneakyThrows
    private void acceptAndSortToCourierOrderManual(ScOrder order) {
        Cell cell = testFactory.determineRouteCell(
                testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow(), order);

        List<Place> places = testFactory.orderPlaces(order);
        assertThat(places).hasSize(1);

        String finalUrl = "/manual/orders/acceptAndSort" +
                "?externalOrderId=" + order.getExternalId() + "&uid=" + user.getUid()
                + "&externalPlaceId=" + places.get(0).getMainPartnerCode()
                + "&cellId=" + Objects.requireNonNull(cell).getId();

        mockMvc.perform(
                        MockMvcRequestBuilders.post(finalUrl)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());
    }

    @SneakyThrows
    private String fileContent(String fileName) {
        return IOUtils.toString(
                Objects.requireNonNull(
                        TestFactory.class.getClassLoader().getResourceAsStream(fileName)
                ),
                StandardCharsets.UTF_8
        );
    }

    @SneakyThrows
    private void ffApiRequest(String body) {
        mockMvc.perform(
                MockMvcRequestBuilders.post("/fulfillment/query-gateway")
                        .contentType(MediaType.TEXT_XML)
                        .content(body)
        )
                .andExpect(status().isOk())
                .andExpect(xpath("/root/requestState/isError").string("false"));
    }

    private ScOrder createOrderInStatus(ScOrderFFStatus status) {
        return createOrderInStatus(status, true);
    }

    private ScOrder createDropshipOrderInStatus(ScOrderFFStatus status) {
        return createOrderInStatus(status, false);
    }

    public ScOrder createOrderInStatus(ScOrderFFStatus status, boolean hasMultipleCoueries) {
        ScOrder order;
        switch (status) {
            case ORDER_CREATED_FF:
                order = testFactory.create(order(sortingCenter).dsType(hasMultipleCoueries ?
                                DeliveryServiceType.LAST_MILE_COURIER : DeliveryServiceType.TRANSIT).build())
                        .get();
                break;
            case ORDER_CANCELLED_FF:
                order = testFactory.create(order(sortingCenter).dsType(hasMultipleCoueries ?
                                DeliveryServiceType.LAST_MILE_COURIER : DeliveryServiceType.TRANSIT).build())
                        .cancel().get();
                break;
            case ORDER_ARRIVED_TO_SO_WAREHOUSE:
                order = testFactory.createForToday(
                                order(sortingCenter).dsType(hasMultipleCoueries ?
                                        DeliveryServiceType.LAST_MILE_COURIER : DeliveryServiceType.TRANSIT).build())
                        .accept().get();
                break;
            case ORDER_AWAITING_CLARIFICATION_FF:
                order = testFactory.createForToday(
                                order(sortingCenter).dsType(hasMultipleCoueries ?
                                        DeliveryServiceType.LAST_MILE_COURIER : DeliveryServiceType.TRANSIT).build())
                        .accept().sort().ship().accept().get();
                break;
            case ORDER_READY_TO_BE_SEND_TO_SO_FF:
                order = testFactory.createForToday(
                                order(sortingCenter).dsType(hasMultipleCoueries ?
                                        DeliveryServiceType.LAST_MILE_COURIER : DeliveryServiceType.TRANSIT).build())
                        .accept().sort().get();
                break;
            case ORDER_PREPARED_TO_BE_SEND_TO_SO:
                order = testFactory.createForToday(
                                order(sortingCenter).dsType(hasMultipleCoueries ?
                                        DeliveryServiceType.LAST_MILE_COURIER : DeliveryServiceType.TRANSIT).build())
                        .accept().sort().prepare().get();
                break;
            case ORDER_SHIPPED_TO_SO_FF:
                order = testFactory.createForToday(
                                order(sortingCenter).dsType(hasMultipleCoueries ?
                                        DeliveryServiceType.LAST_MILE_COURIER : DeliveryServiceType.TRANSIT).build())
                        .accept().sort().ship().get();
                break;
            case SO_GOT_INFO_ABOUT_PLANNED_RETURN:
                order = testFactory.createForToday(
                                order(sortingCenter).dsType(hasMultipleCoueries ?
                                        DeliveryServiceType.LAST_MILE_COURIER : DeliveryServiceType.TRANSIT).build())
                        .accept().sort().ship().makeReturn().get();
                break;
            case RETURNED_ORDER_AT_SO_WAREHOUSE:
                order = testFactory.createForToday(
                                order(sortingCenter).dsType(hasMultipleCoueries ?
                                        DeliveryServiceType.LAST_MILE_COURIER : DeliveryServiceType.TRANSIT).build())
                        .accept().sort().ship().accept().makeReturn().get();
                break;
            case RETURNED_ORDER_READY_TO_BE_SENT_TO_IM:
                order = testFactory.createForToday(
                                order(sortingCenter).dsType(hasMultipleCoueries ?
                                        DeliveryServiceType.LAST_MILE_COURIER : DeliveryServiceType.TRANSIT).build())
                        .accept().sort().ship().makeReturn().accept().sort()
                        .get();
                break;
            case RETURNED_ORDER_DELIVERED_TO_IM:
                order = testFactory.createForToday(
                                order(sortingCenter).dsType(hasMultipleCoueries ?
                                        DeliveryServiceType.LAST_MILE_COURIER : DeliveryServiceType.TRANSIT).build())
                        .accept().sort().ship().makeReturn().accept().sort().ship().get();
                break;
            default:
                throw new UnsupportedOperationException("Can't create order in status " + status);
        }
        assertThat(order.getOrderStatus()).isEqualTo(status);
        return order;
    }

    @Value
    public static class TestStatusParams {

        ScOrderFFStatus status;
        String preparationMethodName;
        ApiOrderStatus statusInApi;

    }

    @Value
    public static class TestTransitionParams {

        ScOrderFFStatus statusBefore;
        String preparationMethodName;
        String transitionsMethodName;
        ScOrderFFStatus statusAfter;
        ApiOrderStatus statusAfterInApi;

    }

}
