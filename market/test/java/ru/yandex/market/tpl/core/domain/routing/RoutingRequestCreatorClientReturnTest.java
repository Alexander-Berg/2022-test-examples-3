
package ru.yandex.market.tpl.core.domain.routing;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.common.util.datetime.RelativeTimeInterval;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturn;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties;
import ru.yandex.market.tpl.core.domain.shift.CreateShiftRoutingRequestCommandFactory;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.shift.UserShiftTestHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.RouteTaskTimes;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.external.routing.api.CreateUserShiftRoutingRequestCommand;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequestItemType;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;
import ru.yandex.market.tpl.core.test.ClockUtil;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties.CLIENT_RETURN_ADD_TO_MVRP_REQUEST_ENABLED;

@RequiredArgsConstructor
public class RoutingRequestCreatorClientReturnTest extends TplAbstractTest {


    private final CreateShiftRoutingRequestCommandFactory createShiftRoutingRequestCommandFactory;
    private final SortingCenterPropertyService sortingCenterPropertyService;
    private final UserShiftRoutingRequestCreator userShiftRoutingRequestCreator;
    private final ClientReturnGenerator clientReturnGenerator;
    private final OrderGenerateService orderGenerateService;
    private final TransactionTemplate transactionTemplate;
    private final UserShiftTestHelper userShiftTestHelper;
    private final UserShiftRepository userShiftRepository;
    private final UserShiftCommandService commandService;
    private final UserShiftCommandDataHelper helper;
    private final TestUserHelper testUserHelper;
    private final Clock clock;

    private static final long SORTING_CENTER_ID = 47819L;

    private User user;
    private Shift shift;
    private Order order;
    private ClientReturn clientReturn;


    @BeforeEach
    void init() {
        ClockUtil.initFixed(clock, LocalDateTime.of(LocalDate.now(), LocalTime.of(7, 0)));
        var sc = testUserHelper.sortingCenter(SORTING_CENTER_ID);
        sortingCenterPropertyService.upsertPropertyToSortingCenter(sc, CLIENT_RETURN_ADD_TO_MVRP_REQUEST_ENABLED, true);
        user = testUserHelper.findOrCreateUser(1L);
        shift = testUserHelper.findOrCreateOpenShiftForSc(LocalDate.now(clock), SORTING_CENTER_ID);
        order = orderGenerateService.createOrder();
        clientReturn = clientReturnGenerator.generateReturnFromClient(LocalDateTime.now(clock),
                LocalDateTime.now(clock).plusHours(2));

    }

    @DisplayName("Проверяем, что из 1 клиентского возврата и 1 доставки формируется верный запрос на " +
            "перемаршрутизацию.")
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void singleClientReturnRerouteRequest(boolean enableRoutable) {
        enableRoutableRouting(enableRoutable);
        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.clientReturn("addr4", 11, clientReturn.getId()))
                .routePoint(helper.taskPrepaid("addr3", 12, order.getId()))
                .build();

        long userShiftId = userShiftTestHelper.start(createCommand);

        var pickupPointIdsWithActiveSurveyTasks =
                createShiftRoutingRequestCommandFactory.getPickupPointIdsWithActiveSurveyTasks();

        var rerouteRequest = userShiftRoutingRequestCreator.createUserShiftRoutingRequest(
                CreateUserShiftRoutingRequestCommand.builder()
                        .userShiftId(userShiftId)
                        .additionalTimeForSurvey(10L)
                        .isAsyncReroute(false)
                        .pickupPointIdsWithActiveSurveyTasks(pickupPointIdsWithActiveSurveyTasks)
                        .shuffleInTransit(false)
                        .build()
        );

        var items = rerouteRequest.getItems();
        assertThat(items).hasSize(2);
        var clientReturnItem =
                items.stream().filter(it -> it.getType() == RoutingRequestItemType.CLIENT_RETURN).collect(Collectors.toList());
        assertThat(clientReturnItem).hasSize(1);
    }

    @DisplayName("Проверка, что несколько клиентских возвратов, которые можно совместить по времени и адресу" +
            "совмещаются при генерации запроса на ремаршрутизацию")
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void multipleClientReturnsRerouteRequest(boolean enableRoutable) {
        enableRoutableRouting(enableRoutable);
        var clientReturn2 = clientReturnGenerator.generateReturnFromClient(LocalDateTime.now(clock),
                LocalDateTime.now(clock).plusHours(1));
        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.clientReturn("addr4", 13, clientReturn.getId()))
                .routePoint(helper.clientReturn("addr4", 13, clientReturn2.getId()))
                .routePoint(helper.taskPrepaid("addr3", 13, order.getId()))
                .build();

        long userShiftId = userShiftTestHelper.start(createCommand);
        var minInterval = clientReturn2.getArriveInterval(ZoneOffset.UTC);

        var pickupPointIdsWithActiveSurveyTasks =
                createShiftRoutingRequestCommandFactory.getPickupPointIdsWithActiveSurveyTasks();

        var rerouteRequest = userShiftRoutingRequestCreator.createUserShiftRoutingRequest(
                CreateUserShiftRoutingRequestCommand.builder()
                        .userShiftId(userShiftId)
                        .additionalTimeForSurvey(10L)
                        .isAsyncReroute(false)
                        .pickupPointIdsWithActiveSurveyTasks(pickupPointIdsWithActiveSurveyTasks)
                        .shuffleInTransit(false)
                        .build()
        );

        var items = rerouteRequest.getItems();
        assertThat(items).hasSize(2);

        var clientReturnItems =
                items.stream().filter(it -> it.getType() == RoutingRequestItemType.CLIENT_RETURN).collect(Collectors.toList());
        assertThat(clientReturnItems).hasSize(1);

        var clientReturnItem = clientReturnItems.get(0);
        assertThat(clientReturnItem.getInterval()).isEqualTo(RelativeTimeInterval.fromInterval(minInterval,
                ZoneOffset.UTC));
        assertThat(clientReturnItem.getSubTaskCount()).isEqualTo(2);
    }

    @DisplayName("Проверка, что после исполнения ремаршрутизации, время тасок в шифте меняется относительно значений " +
            "в карте тасок и времен доставки")
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void rerouteChangesDeliveryTime(boolean enableRoutable) {
        enableRoutableRouting(enableRoutable);
        //Подготавливаем смену
        var clientReturn = clientReturnGenerator.generateReturnFromClient(LocalDateTime.now(clock),
                LocalDateTime.now(clock).plusHours(2));
        var clientReturn2 = clientReturnGenerator.generateReturnFromClient(LocalDateTime.now(clock),
                LocalDateTime.now(clock).plusHours(1));
        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.clientReturn("addr4", 11, clientReturn.getId()))
                .routePoint(helper.clientReturn("addr4", 12, clientReturn2.getId()))
                .routePoint(helper.taskPrepaid("addr3", 13, order.getId()))
                .build();

        long userShiftId = userShiftTestHelper.start(createCommand);

        var pickupPointIdsWithActiveSurveyTasks =
                createShiftRoutingRequestCommandFactory.getPickupPointIdsWithActiveSurveyTasks();

        //Подготавливаем запрос на ремаршрутизацию
        var rerouteRequest = userShiftRoutingRequestCreator.createUserShiftRoutingRequest(
                CreateUserShiftRoutingRequestCommand.builder()
                        .userShiftId(userShiftId)
                        .additionalTimeForSurvey(10L)
                        .isAsyncReroute(false)
                        .pickupPointIdsWithActiveSurveyTasks(pickupPointIdsWithActiveSurveyTasks)
                        .shuffleInTransit(false)
                        .build()
        );

        var clientReturnRouteTaskTime = new RouteTaskTimes(
                clientReturn2.getArriveIntervalFrom().toInstant(ZoneOffset.UTC).plus(1, ChronoUnit.HOURS),
                clientReturn2.getArriveIntervalTo().toInstant(ZoneOffset.UTC).plus(1, ChronoUnit.HOURS)
        );

        Map<Long, RouteTaskTimes> taskIdToRouteTaskTimeMap = transactionTemplate.execute(
                cmd -> {
                    var userShift = userShiftRepository.findByIdOrThrow(userShiftId);
                    var orderTasks =
                            StreamEx.of(userShift.streamOrderDeliveryTasks()).remove(OrderDeliveryTask::isClientReturn).map(OrderDeliveryTask::getId).toList();
                    assertThat(orderTasks).hasSize(1);

                    var cr =
                            StreamEx.of(userShift.streamOrderDeliveryTasks()).filter(OrderDeliveryTask::isClientReturn).toList();
                    var crTasks = cr.stream().map(OrderDeliveryTask::getId).collect(Collectors.toList());
                    assertThat(crTasks).hasSize(2);

                    return Map.of(
                            orderTasks.get(0), new RouteTaskTimes(order.getDelivery().getDeliveryIntervalFrom(),
                                    order.getDelivery().getDeliveryIntervalTo()),
                            crTasks.get(0), clientReturnRouteTaskTime,
                            crTasks.get(1), clientReturnRouteTaskTime
                    );
                }
        );

        commandService.updateRoute(new UserShiftCommand.UpdateRoute(
                userShiftId, rerouteRequest.getRequestId(), SimpleStrategies.BY_DATE_INTERVAL_MERGE,
                taskIdToRouteTaskTimeMap
        ));


        transactionTemplate.executeWithoutResult(
                cmd -> {
                    var us = userShiftRepository.findByIdOrThrow(userShiftId);
                    assertThat(us.getProcessingId()).isEqualTo(rerouteRequest.getRequestId());

                    var crTasks =
                            us.streamOrderDeliveryTasks().filter(OrderDeliveryTask::isClientReturn).toList();
                    var expectedDeliveryTimes = StreamEx.of(crTasks).toMap(OrderDeliveryTask::getId,
                            OrderDeliveryTask::getExpectedDeliveryTime);

                    //Проверяем, что клиенткие возвраты соотвествуют времени доставки
                    var crId1 = crTasks.get(0);
                    var crId2 = crTasks.get(1);
                    assertThat(expectedDeliveryTimes.get(crId1)).isEqualTo(taskIdToRouteTaskTimeMap.get(crId1));
                    assertThat(expectedDeliveryTimes.get(crId2)).isEqualTo(taskIdToRouteTaskTimeMap.get(crId2));
                }
        );
    }

    private void enableRoutableRouting(boolean enable) {
        var sc = testUserHelper.sortingCenter(SORTING_CENTER_ID);
        sortingCenterPropertyService.upsertPropertyToSortingCenter(sc,
                SortingCenterProperties.ROUTABLE_ROUTING_ENABLED, enable);
        sortingCenterPropertyService.upsertPropertyToSortingCenter(sc,
                SortingCenterProperties.ROUTING_ORDERS_AS_ROUTABLE_ENABLED, enable);
        sortingCenterPropertyService.upsertPropertyToSortingCenter(sc,
                SortingCenterProperties.SPECIAL_REQUEST_ADD_TO_MVRP_REQUEST_ENABLED, enable);
    }
}
