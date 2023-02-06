package ru.yandex.market.tpl.core.domain.usershift;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryRescheduleReasonType;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.usershift.commands.DeliveryReschedule;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.test.ClockUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.common.util.DateTimeUtil.todayAtHour;

/**
 * @author kukabara
 */
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@CoreTest
public class UserShiftRescheduleSeveralRoutePointsTest {

    private final TestUserHelper testUserHelper;
    private final UserShiftCommandDataHelper helper;
    private final OrderGenerateService orderGenerateService;
    private final UserShiftCommandService commandService;
    private final UserShiftRepository userShiftRepository;
    private final ConfigurationServiceAdapter configurationServiceAdapter;

    private User user;
    private UserShift userShift;
    private Map<Long, Order> orderMap = new HashMap<>();

    @MockBean
    private Clock clock;

    @BeforeEach
    void init() {
        configurationServiceAdapter.mergeValue(ConfigurationProperties.RESCHEDULE_ENABLED, true);

        ClockUtil.initFixed(clock);
        user = testUserHelper.findOrCreateUser(35236L);

        Shift shift = testUserHelper.findOrCreateOpenShift(LocalDate.now(clock));
        UserShiftCommand.Create.CreateBuilder builder = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .mergeStrategy(SimpleStrategies.BY_DATE_INTERVAL_MERGE);
        var map = new LinkedHashMap<GeoPoint, Integer>() {
            {
                put(GeoPoint.ofLatLon(55.734427, 37.641852), 10);
                put(GeoPoint.ofLatLon(55.736794, 37.618741), 11);
                put(GeoPoint.ofLatLon(55.745068, 37.603487), 12);
                put(GeoPoint.ofLatLon(55.729006, 37.622513), 13);
                put(GeoPoint.ofLatLon(55.741850, 37.629295), 14);
                put(GeoPoint.ofLatLon(55.751999, 37.617734), 15);
                put(GeoPoint.ofLatLon(55.733969, 37.587093), 16);
            }
        };
        map.forEach((geoPoint, expectedHour) -> {
            IntStream.range(0, 2).forEach(i -> {
                Order order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                        .externalOrderId(expectedHour + "_" + i)
                        .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                                .geoPoint(geoPoint)
                                .build())
                        .paymentType(OrderPaymentType.PREPAID)
                        .deliveryInterval(LocalTimeInterval.valueOf("10:00-18:00"))
                        .build());
                orderMap.put(order.getId(), order);
                builder.routePoint(helper.taskPrepaid("addr-" + order.getId(), order,
                        DateTimeUtil.todayAtHour(expectedHour, clock), false));
            });
        });
        userShift = userShiftRepository.findById(commandService.createUserShift(builder.build())).orElseThrow();
        testUserHelper.checkinAndFinishPickup(userShift);
    }

    @Test
    @Disabled
    void shouldRescheduleAfterNearestPoint() {
        RoutePoint rp = userShift.getCurrentRoutePoint();
        assertThat(rp.getType()).isEqualTo(RoutePointType.DELIVERY);

        assertThat(getOrderIds(userShift))
                .containsExactly("10_0", "10_1", "11_0", "11_1", "12_0", "12_1", "13_0", "13_1",
                        "14_0", "14_1", "15_0", "15_1", "16_0", "16_1");
        reschedule(rp, 14);
        assertThat(getOrderIds(userShift))
                .describedAs("" + getOrderIds(userShift))
                .containsExactly("10_1", "11_0", "11_1", "12_0", "12_1", "13_0", "13_1",
                        "10_0",
                        "14_0", "14_1", "15_0", "15_1", "16_0", "16_1");
    }

    @DisplayName("Проверяем, что при переносе не поставим точку следующей, а поставим как минимум через одну")
    @Test
    void shouldRescheduleAfterNext() {
        RoutePoint rp = userShift.getCurrentRoutePoint();
        assertThat(rp.getType()).isEqualTo(RoutePointType.DELIVERY);

        assertThat(getOrderIds(userShift))
                .containsExactly("10_0", "10_1", "11_0", "11_1", "12_0", "12_1", "13_0", "13_1",
                        "14_0", "14_1", "15_0", "15_1", "16_0", "16_1");
        reschedule(rp, 10);
        assertThat(getOrderIds(userShift))
                .describedAs("" + getOrderIds(userShift))
                .containsExactly("10_1", "11_0", "11_1", "10_0", "12_0", "12_1", "13_0", "13_1",
                        "14_0", "14_1", "15_0", "15_1", "16_0", "16_1");
    }

    private void reschedule(RoutePoint rp, int hour) {
        Instant intervalFrom = todayAtHour(hour, clock);
        commandService.rescheduleDeliveryTask(user,
                new UserShiftCommand.RescheduleOrderDeliveryTask(
                        userShift.getId(), rp.getId(),
                        rp.streamDeliveryTasks().findFirst().orElseThrow().getId(),
                        DeliveryReschedule.fromCourier(user,
                                intervalFrom,
                                todayAtHour(hour + 2, clock), OrderDeliveryRescheduleReasonType.DELIVERY_DELAY),
                        Instant.now(clock),
                        userShift.getZoneId()
                ));
    }

    /**
     * @return список заказов в том порядке, в котором они будут выдаваться курьеру.
     */
    private List<String> getOrderIds(UserShift userShift) {
        return userShift.streamDeliveryRoutePoints()
                .sortedBy(RoutePoint::getExpectedDateTime)
                .flatMap(RoutePoint::streamDeliveryTasks)
                .sortedBy(DeliveryTask::getExpectedDeliveryTime)
                .map(t -> t.getOrderIds().iterator().next())
                .map(orderId -> orderMap.get(orderId).getExternalOrderId())
                .collect(Collectors.toList());
    }

}
