package ru.yandex.market.tpl.core.query.usershift.routepoint;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointAbstractDto;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointClientDeliveryDto;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserProperties;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftReassignManager;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.query.usershift.UserShiftQueryService;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;


@RequiredArgsConstructor
public class WholesalerRoutePointQueryServiceTest extends TplAbstractTest {
    private final OrderGenerateService orderGenerateService;
    private final Clock clock;
    private final TestUserHelper testUserHelper;
    private final SortingCenterService sortingCenterService;
    private final TestDataFactory testDataFactory;
    private final UserShiftRepository userShiftRepository;
    private final UserShiftReassignManager userShiftReassignManager;
    private final RoutePointQueryService routePointQueryService;
    private final TransactionTemplate transactionTemplate;
    private final SessionFactory sessionFactory;
    private final UserShiftQueryService userShiftQueryService;
    private final ClientReturnGenerator clientReturnGenerator;
    private final UserShiftCommandService commandService;
    private final UserPropertyService userPropertyService;


    private final static int COUNT_ORDERS = 20;

    private User user;
    private Shift shift;
    private UserShift userShift;

    @BeforeEach
    void init() {
        transactionTemplate.execute(ts -> {
            user = testUserHelper.findOrCreateUser(1L);
            shift = testUserHelper.findOrCreateOpenShiftForSc(LocalDate.now(clock),
                    sortingCenterService.findSortCenterForDs(239).getId());
            userShift = userShiftRepository
                    .findByIdOrThrow(testDataFactory.createEmptyShift(shift.getId(), user));
            userPropertyService.addPropertyToUser(user, UserProperties.CLIENT_RETURN_ADD_TO_MULTI_ITEM_ENABLED, true);


            List<Order> orders = generateMultiOrders(COUNT_ORDERS);
            orders.forEach(
                    o -> userShiftReassignManager.assign(userShift, o)
            );

            testUserHelper.checkinAndFinishPickup(userShift);
            return null;
        });
    }

    @Test
    void justWorkWithoutTransaction() {
        var userShift = userShiftRepository.findByIdOrThrow(this.userShift.getId());
        routePointQueryService.getRoutePointInfoPossibleCompressed(userShift.getCurrentRoutePoint().getId(), user,
                null);
    }

    @Test
    @DisplayName("Количество sql запросов при получении клиентского routePoint'а")
    void countQueriesWhenGetCompressedClientRoutePoint() {
        transactionTemplate.execute(ts -> {
            Statistics stats = sessionFactory.getStatistics();
            stats.setStatisticsEnabled(true);

            var userShift = userShiftRepository.findByIdOrThrow(this.userShift.getId());
            RoutePointAbstractDto routePointInfoPossibleCompressed =
                    routePointQueryService.getRoutePointInfoPossibleCompressed(userShift.getCurrentRoutePoint().getId(), user, null);
            long from = 10;
            long to = 15;
            assertThat(stats.getQueryExecutionCount())
                    .withFailMessage(
                            "Возможно появилось N+1 запросов! Этот метод должен быть оптимально написан, поэтому " +
                                    "данные " +
                                    "необходимо вытскивать батчами. Count queries expected: %s-%s, but now: %s.",
                            from, to, stats.getQueryExecutionCount())
                    .isBetween(from, to);
            stats.setStatisticsEnabled(false);
            return null;
        });
    }

    private List<Order> generateMultiOrders(int count) {
        AddressGenerator.AddressGenerateParam addressGenerateParam = AddressGenerator.AddressGenerateParam.builder()
                .geoPoint(GeoPointGenerator.generateLonLat())
                .street("Колотушкина")
                .house("1")
                .build();
        List<Order> orders = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            var multiOrder = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                    .externalOrderId("451234" + i)
                    .buyerYandexUid(1L)
                    .deliveryDate(LocalDate.now(clock))
                    .deliveryServiceId(239L)
                    .deliveryInterval(LocalTimeInterval.valueOf("10:00-13:00"))
                    .addressGenerateParam(addressGenerateParam)
                    .flowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED)
                    .build());
            orders.add(multiOrder);
        }
        return orders;
    }

}
