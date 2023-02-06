package ru.yandex.market.tpl.internal.controller.internal;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import one.util.streamex.StreamEx;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.common.util.geocoder.GeoClient;
import ru.yandex.common.util.geocoder.GeoObject;
import ru.yandex.market.tpl.api.model.order.CallRequirement;
import ru.yandex.market.tpl.api.model.order.OrderDeliveryStatus;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryRescheduleReasonType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.api.model.tracking.OrderDto;
import ru.yandex.market.tpl.api.model.tracking.TrackingCancelOrderDto;
import ru.yandex.market.tpl.api.model.tracking.TrackingClarifyAddressDto;
import ru.yandex.market.tpl.api.model.tracking.TrackingDto;
import ru.yandex.market.tpl.api.model.tracking.TrackingOrderCancelReason;
import ru.yandex.market.tpl.api.model.tracking.TrackingRescheduleDto;
import ru.yandex.market.tpl.common.db.exception.TplEntityNotFoundException;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderCommand;
import ru.yandex.market.tpl.core.domain.order.OrderCommandService;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
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
import ru.yandex.market.tpl.core.domain.usershift.tracking.Tracking;
import ru.yandex.market.tpl.core.domain.usershift.tracking.TrackingRepository;
import ru.yandex.market.tpl.core.domain.util.AddDeliveryTaskHelper;
import ru.yandex.market.tpl.core.service.tracking.TrackingService;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.internal.controller.BaseTplIntWebTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.tpl.api.model.task.call.CallToRecipientTaskStatus.SUCCESS;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.DO_NOT_CALL_ENABLED;

@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class TrackingControllerMultiOrderIntegrationTest extends BaseTplIntWebTest {
    private static final long SORTING_CENTER_ID = 47819L;
    private final ObjectMapper tplObjectMapper;
    private final TestUserHelper testUserHelper;
    private final OrderCommandService orderCommandService;
    private final UserShiftCommandService userShiftCommandService;
    private final AddDeliveryTaskHelper addDeliveryTaskHelper;
    private final OrderGenerateService orderGenerateService;
    private final TaskOrderDeliveryRepository taskOrderDeliveryRepository;
    private final OrderRepository orderRepository;
    private final TrackingService trackingService;
    private final GeoClient geoClient;
    private final Clock clock;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final TrackingRepository trackingRepository;

    LocalDate deliveryDate;
    Map<String, Order> orders;
    List<Long> taskIds;
    String trackingId;

    User user;
    UserShift userShift;
    List<OrderDeliveryTask> tasks;

    @BeforeEach
    void setUpThis() {
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
                .map(orderId -> {
                    return orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                            .buyerYandexUid(1L)
                            .externalOrderId(orderId)
                            .deliveryDate(deliveryDate)
                            .deliveryInterval(deliveryInterval)
                            .deliveryServiceId(TestDataFactory.DELIVERY_SERVICE_ID)
                            .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                                    .geoPoint(geoPoint)
                                    .street("Колотушкина")
                                    .house("1")
                                    .build())
                            .build());
                })
                .collect(Collectors.toMap(Order::getExternalOrderId, Function.identity()));

        user = testUserHelper.findOrCreateUser(UID);
        Shift shift = testUserHelper.findOrCreateOpenShiftForSc(LocalDate.now(clock), SORTING_CENTER_ID);
        userShift = testUserHelper.createEmptyShift(user, shift);

        taskIds = StreamEx.of(orders.values())
                .map(o -> userShiftCommandService.addDeliveryTask(user, addDeliveryTaskHelper.createAddDeliveryTaskCommand(
                        userShift, o, expectedTimeArrival, expectedDeliveryTime, false
                )))
                .map(DeliveryTask::getId)
                .toList();

        testUserHelper.openShift(user, userShift.getId());
        testUserHelper.finishPickupAtStartOfTheDay(userShift);

        tasks = taskOrderDeliveryRepository.findAllById(taskIds);
        trackingId = trackingService.getTrackingLinkByOrder(orders.keySet().iterator().next())
                .orElseThrow();

        Assertions.assertThat(tasks)
                .allMatch(OrderDeliveryTask::isPartOfMultiOrder);

        Set<String> multiOrderIds = StreamEx.of(tasks).map(OrderDeliveryTask::getMultiOrderId).toSet();
        Assertions.assertThat(multiOrderIds).hasSize(1);

        var geoObject = Mockito.mock(GeoObject.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(geoObject.getPoint()).thenReturn("22.34 56.78");
        Mockito.when(geoClient.find(Mockito.any(), Mockito.any()))
                .thenReturn(List.of(geoObject));
    }

    @SneakyThrows
    @Test
    void shouldGetAllOrdersInMultiOrderTracking() {
        String content = mockMvc.perform(get("/internal/tracking/{id}", trackingId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        TrackingDto trackingDto = tplObjectMapper.readValue(content, TrackingDto.class);
        List<OrderDto> trackingDtoOrders = trackingDto.getOrders();

        Assertions.assertThat(trackingDtoOrders).hasSize(orders.size());
    }

    @SneakyThrows
    @Test
    void shouldCancelAllOrdersInMultiOrderTracking() {
        TrackingCancelOrderDto cancelOrderDto = new TrackingCancelOrderDto();
        cancelOrderDto.setCancelReasonType(TrackingOrderCancelReason.CHANGED_MIND);

        mockMvc.perform(post("/internal/tracking/{id}/cancel", trackingId)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(tplObjectMapper.writeValueAsString(cancelOrderDto))
        )
                .andExpect(status().isOk());

        List<Order> orders = orderRepository.findAllById(StreamEx.of(this.orders.values()).map(Order::getId).toSet());
        Assertions.assertThat(orders).allMatch(o -> o.getDeliveryStatus() == OrderDeliveryStatus.CANCELLED);
        Assertions.assertThat(orders).allMatch(o -> o.getOrderFlowStatus() == OrderFlowStatus.READY_FOR_RETURN);
    }

    @SneakyThrows
    @Test
    void shouldCancelPartiallyIfSomeAlreadyCancelled() {
        long taskIdToCancel = taskIds.get(1);
        var taskToCancel = taskOrderDeliveryRepository.findByIdOrThrow(taskIdToCancel);

        userShiftCommandService.failDeliveryTask(user, new UserShiftCommand.FailOrderDeliveryTask(
            userShift.getId(),
            taskToCancel.getRoutePoint().getId(),
            taskIdToCancel,
            new OrderDeliveryFailReason(
                    OrderDeliveryTaskFailReasonType.CLIENT_REFUSED,
                    "Не хочу, не буду"
            )
        ));

        TrackingCancelOrderDto cancelOrderDto = new TrackingCancelOrderDto();
        cancelOrderDto.setCancelReasonType(TrackingOrderCancelReason.CHANGED_MIND);
        mockMvc.perform(post("/internal/tracking/{id}/cancel", trackingId)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(tplObjectMapper.writeValueAsString(cancelOrderDto))
        )
                .andExpect(status().isOk());
        List<Order> orders = orderRepository.findAllById(StreamEx.of(this.orders.values()).map(Order::getId).toSet());
        Assertions.assertThat(orders).allMatch(o -> o.getDeliveryStatus() == OrderDeliveryStatus.CANCELLED);
        Assertions.assertThat(orders).allMatch(o -> o.getOrderFlowStatus() == OrderFlowStatus.READY_FOR_RETURN);
    }

    @Test
    void shouldRescheduleAllOrdersInMultiOrderTracking() throws Exception {
        Instant newDeliveryIntervalFrom = ZonedDateTime.of(
                deliveryDate.plusDays(1), LocalTime.of(10, 0), DateTimeUtil.DEFAULT_ZONE_ID
        ).toInstant();
        Instant newDeliveryIntervalTo = ZonedDateTime.of(
                deliveryDate.plusDays(1), LocalTime.of(14, 0), DateTimeUtil.DEFAULT_ZONE_ID
        ).toInstant();

        TrackingRescheduleDto rescheduleDto = new TrackingRescheduleDto(
            newDeliveryIntervalFrom, newDeliveryIntervalTo
        );

        mockMvc.perform(post("/internal/tracking/{id}/reschedule", trackingId)
                .content(tplObjectMapper.writeValueAsString(rescheduleDto))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk());

        List<Order> orders = orderRepository.findAllById(StreamEx.of(this.orders.values()).map(Order::getId).toSet());
        Assertions.assertThat(orders).allMatch(o -> o.getDelivery().getDeliveryIntervalFrom().equals(newDeliveryIntervalFrom));
        Assertions.assertThat(orders).allMatch(o -> o.getDelivery().getDeliveryIntervalTo().equals(newDeliveryIntervalTo));
    }

    @SneakyThrows
    @Test
    void shouldClarifyAllOrdersInMultiOrderTracking() {
        String comment = "Позвоните, чтобы открыли шлагбаум";
        String entrance = "3";
        String floor = "4";
        String apartment = "36";
        String entryPhone = "360049";
        var newAddressDto = new TrackingClarifyAddressDto(
                entrance,
                floor,
                apartment,
                entryPhone,
                comment
        );


        mockMvc.perform(patch("/internal/tracking/{id}/address", trackingId)
                .content(tplObjectMapper.writeValueAsString(newAddressDto))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk());

        var orders = orderRepository.findAllById(StreamEx.of(this.orders.values()).map(Order::getId).toSet());

        Assertions.assertThat(orders)
                .allMatch(o -> comment.equals(o.getDelivery().getRecipientNotes())
                        && Boolean.TRUE.equals(o.getDelivery().getDeliveryAddress().getClarified())
                        && entrance.equals(o.getDelivery().getDeliveryAddress().getEntrance())
                        && floor.equals(o.getDelivery().getDeliveryAddress().getFloor())
                        && apartment.equals(o.getDelivery().getDeliveryAddress().getApartment())
                        && entryPhone.equals(o.getDelivery().getDeliveryAddress().getEntryPhone()));
    }

    @SneakyThrows
    @Test
    void shouldUpdateCallRequirementInMultiOrderTracking() {
        configurationServiceAdapter.mergeValue(DO_NOT_CALL_ENABLED, true);

        CallRequirement newCallRequirement = CallRequirement.DO_NOT_CALL;
        Tracking tracking = trackingRepository.findById(trackingId)
                .orElseThrow(() -> new TplEntityNotFoundException("tracking", trackingId));
        var orders = orderRepository.findAllById(StreamEx.of(this.orders.values()).map(Order::getId).toSet());

        Assertions.assertThat(tracking.getOrderDeliveryTask().getCallToRecipientTask().getStatus())
                .isEqualTo(SUCCESS);
        Assertions.assertThat(orders)
                .allMatch(o -> o.getDelivery().getCallRequirement() == null);

        mockMvc.perform(patch("/internal/tracking/{id}/callRequirement", trackingId)
                .param("callRequirement", newCallRequirement.name())
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk());

        orders = orderRepository.findAllById(StreamEx.of(this.orders.values()).map(Order::getId).toSet());

        Assertions.assertThat(orders)
                .allMatch(o -> newCallRequirement.equals(o.getDelivery().getCallRequirement()));

        tracking = trackingRepository.findById(trackingId)
                .orElseThrow(() -> new TplEntityNotFoundException("tracking", trackingId));

        Assertions.assertThat(tracking.getOrderDeliveryTask().getCallToRecipientTask().getStatus())
                .isEqualTo(SUCCESS);
    }

    @SneakyThrows
    @Test
    void shouldPartiallyClarifyAllOrdersInMultiOrderTracking() {
        long secondOrderId = Iterables.get(orders.values(), 1).getId();
        orderCommandService.updateFlowStatus(new OrderCommand.UpdateFlowStatus(
                secondOrderId,
                OrderFlowStatus.TRANSMITTED_TO_RECIPIENT
        ), Source.CLIENT);

        String comment = "Позвоните, чтобы открыли шлагбаум";
        String entrance = "3";
        String floor = "4";
        String apartment = "36";
        String entryPhone = "360049";
        var newAddressDto = new TrackingClarifyAddressDto(
                entrance,
                floor,
                apartment,
                entryPhone,
                comment
        );

        mockMvc.perform(patch("/internal/tracking/{id}/address", trackingId)
                .content(tplObjectMapper.writeValueAsString(newAddressDto))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk());

        var orders = orderRepository.findAllById(StreamEx.of(this.orders.values()).map(Order::getId).toSet());

        Assertions.assertThat(orders)
                .filteredOn(o -> o.getId() != secondOrderId)
                .allMatch(o -> comment.equals(o.getDelivery().getRecipientNotes())
                        && Boolean.TRUE.equals(o.getDelivery().getDeliveryAddress().getClarified())
                        && entrance.equals(o.getDelivery().getDeliveryAddress().getEntrance())
                        && floor.equals(o.getDelivery().getDeliveryAddress().getFloor())
                        && apartment.equals(o.getDelivery().getDeliveryAddress().getApartment())
                        && entryPhone.equals(o.getDelivery().getDeliveryAddress().getEntryPhone()));

    }

    @Test
    public void testRescheduleConfirmationNotice() {
        configurationServiceAdapter.insertValue(
            ConfigurationProperties.TRIGGERS_PLATFORM_COMMUNICATION_ORDER_RESCHEDULED_CONFIRM_ENABLED,
            true);

        testUserHelper.rescheduleNextDayMultiOrder(
            tasks.get(0).getRoutePoint(),
            OrderDeliveryRescheduleReasonType.CLIENT_REQUEST
        );

        Assertions.assertThat(tasks.get(0).getStatus()).isEqualTo(OrderDeliveryTaskStatus.DELIVERY_FAILED);
        Assertions.assertThat(tasks.get(1).getStatus()).isEqualTo(OrderDeliveryTaskStatus.DELIVERY_FAILED);

        // проверяем, что отправляли 1 смс об отмене заказа + события для Ярд
        dbQueueTestUtil.assertQueueHasSize(QueueType.LOGBROKER_WRITER, 3);
    }
}
