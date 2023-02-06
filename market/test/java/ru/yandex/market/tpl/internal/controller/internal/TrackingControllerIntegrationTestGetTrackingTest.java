package ru.yandex.market.tpl.internal.controller.internal;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.tracking.TrackingDto;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.task.TaskOrderDeliveryRepository;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.DeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.util.AddDeliveryTaskHelper;
import ru.yandex.market.tpl.core.service.tracking.TrackingService;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.internal.controller.BaseTplIntWebTest;
import ru.yandex.market.tpl.internal.controller.TplIntWebTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TplIntWebTest
@RequiredArgsConstructor(onConstructor_={@Autowired})
public class TrackingControllerIntegrationTestGetTrackingTest extends BaseTplIntWebTest {
    private static final long UID = 2234562L;
    private static final long SORTING_CENTER_ID = 47819L;

    private final OrderGenerateService orderGenerateService;
    private final UserShiftCommandService userShiftCommandService;
    private final TrackingService trackingService;
    private final AddDeliveryTaskHelper addDeliveryTaskHelper;
    private final TaskOrderDeliveryRepository taskOrderDeliveryRepository;
    private final TestUserHelper testUserHelper;
    private final ObjectMapper tplObjectMapper;
    private final Clock clock;

    private LocalDate deliveryDate;
    private List<Order> orders;
    private List<Long> taskIds;
    private String trackingId;

    private User user;
    private UserShift userShift;

    @BeforeEach
    void setUp() {
        GeoPoint geoPoint = GeoPoint.ofLatLon(new BigDecimal("55.787878"), new BigDecimal("37.656565"));
        deliveryDate = LocalDate.now(clock);
        LocalTimeInterval deliveryInterval = new LocalTimeInterval(
                LocalTime.of(15, 0), LocalTime.of(18, 0)
        );
        // Это время которое считает маршртуизация
        Instant expectedTimeArrival = ZonedDateTime.of(
                LocalDateTime.of(deliveryDate, LocalTime.of(15, 43)), DateTimeUtil.DEFAULT_ZONE_ID
        ).toInstant();

        Instant expectedDeliveryTime = ZonedDateTime.of(
                LocalDateTime.of(deliveryDate, LocalTime.of(15, 55)), DateTimeUtil.DEFAULT_ZONE_ID
        ).toInstant();

        orders = LongStream.rangeClosed(123456, 123460)
                .mapToObj(String::valueOf)
                .map(orderId -> orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                        .externalOrderId(orderId)
                        .deliveryDate(deliveryDate)
                        .deliveryInterval(deliveryInterval)
                        .deliveryServiceId(TestDataFactory.DELIVERY_SERVICE_ID)
                        .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                                .geoPoint(geoPoint)
                                .street("Колотушкина")
                                .house("1")
                                .build())
                        .build()))
                .collect(Collectors.toList());

        user = testUserHelper.findOrCreateUser(UID);
        Shift shift = testUserHelper.findOrCreateOpenShiftForSc(LocalDate.now(clock), SORTING_CENTER_ID);
        userShift = testUserHelper.createEmptyShift(user, shift);

        taskIds = StreamEx.of(orders)
                .map(o -> userShiftCommandService.addDeliveryTask(user, addDeliveryTaskHelper.createAddDeliveryTaskCommand(
                        userShift, o, expectedTimeArrival, expectedDeliveryTime, false
                )))
                .map(DeliveryTask::getId)
                .toList();

        testUserHelper.openShift(user, userShift.getId());
        testUserHelper.finishPickupAtStartOfTheDay(userShift);

        List<OrderDeliveryTask> tasks = taskOrderDeliveryRepository.findAllById(taskIds);
        trackingId = trackingService.getTrackingLinkByOrder(orders.get(0).getExternalOrderId())
                .orElseThrow();

        Assertions.assertThat(tasks)
                .allMatch(OrderDeliveryTask::isPartOfMultiOrder);

        Set<String> multiOrderIds = StreamEx.of(tasks).map(OrderDeliveryTask::getMultiOrderId).toSet();
        Assertions.assertThat(multiOrderIds).hasSize(1);
    }

    @Test
    void shouldShowTrackingIfSiblingOrderIsNotDelivered() throws Exception {
        var firstTaskId = taskIds.get(0);
        var task = taskOrderDeliveryRepository.findById(firstTaskId).orElseThrow();

        userShiftCommandService.failDeliveryTask(user, new UserShiftCommand.FailOrderDeliveryTask(
                userShift.getId(),
                task.getRoutePoint().getId(),
                task.getId(),
                new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.CANCEL_ORDER, "Не хочу, не буду")
        ));

        var responseString = mockMvc.perform(get("/internal/tracking/{id}", trackingId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var trackingDto = tplObjectMapper.readValue(responseString, TrackingDto.class);
        Assertions.assertThat(trackingDto.getOrders()).hasSize(4);
    }
}
