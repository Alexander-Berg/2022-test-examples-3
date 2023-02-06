package ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.date.changerequest;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import steps.utils.TestableClock;

import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.event.HistoryEventReason;
import ru.yandex.market.checkout.checkouter.order.DeliveryOption;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditOptions;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.TimeInterval;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.DeliveryDatesChangeRequestPayload;
import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.delivery.mdbapp.AllMockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.date.UpdateOrderDeliveryDateDto;
import ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.date.changedbychangerequest.DeliveryDateChangeRequestDto;
import ru.yandex.market.delivery.mdbapp.components.service.LogisticsOrderService;
import ru.yandex.market.delivery.mdbapp.components.service.checkouter.client.CheckouterOrderService;
import ru.yandex.market.delivery.mdbapp.configuration.LatchTaskListenerConfig;
import ru.yandex.market.delivery.mdbapp.configuration.queue.DeliveryDateChangeRequestQueueConfiguration;
import ru.yandex.market.delivery.mdbapp.util.DeliveryDateUpdateReason;
import ru.yandex.market.logistics.lom.model.dto.ChangeOrderRequestDto;
import ru.yandex.market.logistics.lom.model.dto.ChangeOrderRequestPayloadDto;
import ru.yandex.market.logistics.lom.model.dto.DeliveryIntervalDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.UpdateOrderDeliveryDateRequestDto;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestReason;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestStatus;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestType;
import ru.yandex.market.logistics.lom.model.enums.OptionalOrderPart;
import ru.yandex.money.common.dbqueue.api.EnqueueParams;
import ru.yandex.money.common.dbqueue.api.QueueProducer;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;
import ru.yandex.money.common.dbqueue.api.TaskLifecycleListener;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

@DisplayName("Тест обработки заявок на изменение даты доставки в LOM")
public class DeliveryDateChangeRequestQueueConsumerTest extends AllMockContextualTest {
    private static final EnqueueParams<DeliveryDateChangeRequestDto> ENQUEUE_PARAMS = EnqueueParams.create(getDto());
    private static final Instant FIXED_TIME = LocalDate.of(2020, 10, 19).atStartOfDay().toInstant(ZoneOffset.UTC);
    private static final Long ORDER_ID = 234L;

    @Autowired
    private ObjectMapper objectMapper;

    @Qualifier(DeliveryDateChangeRequestQueueConfiguration.PRODUCER)
    @Autowired
    private QueueProducer<DeliveryDateChangeRequestDto> queueProducer;

    @MockBean
    private CheckouterOrderService checkouterOrderService;

    @MockBean
    private LogisticsOrderService logisticsOrderService;

    @Autowired
    private TaskLifecycleListener taskListener;

    @Autowired
    private TestableClock clock;

    @BeforeEach
    public void setUp() {
        clock.setFixed(FIXED_TIME, ZoneId.systemDefault());
    }

    @AfterEach
    public void tearDown() {
        verifyNoMoreInteractions(checkouterOrderService);
    }

    @ParameterizedTest
    @EnumSource(value = ChangeRequestStatus.class, names = {"REJECTED", "APPLIED"}, mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Успешное подтверждение существующей в чекауте заявки")
    public void testConfirmSuccessExisting(ChangeRequestStatus changeRequestStatus) throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        LatchTaskListenerConfig.TaskListener mockedTaskListener = (LatchTaskListenerConfig.TaskListener) taskListener;
        mockedTaskListener.setFinishedLatch(countDownLatch);
        doReturn(getLomOrder(ChangeOrderRequestStatus.SUCCESS))
            .when(logisticsOrderService)
            .getByIdOrThrow(eq(123L), eq(EnumSet.of(OptionalOrderPart.CHANGE_REQUESTS)));
        doReturn(getOrder(LocalDate.of(2021, 3, 1), null, changeRequestStatus, OrderStatus.DELIVERY))
            .when(checkouterOrderService)
            .getOrderWithChangeRequests(eq(ORDER_ID));

        queueProducer.enqueue(ENQUEUE_PARAMS);
        countDownLatch.await(2, TimeUnit.SECONDS);

        softly.assertThat(mockedTaskListener.getLastResults()).containsExactly(TaskExecutionResult.finish());
        verify(checkouterOrderService).applyChangeRequest(eq(ORDER_ID), eq(10001L));
        verify(checkouterOrderService).getOrderWithChangeRequests(eq(ORDER_ID));
    }

    @Test
    @DisplayName("Успешное подтверждение НЕ существующей в чекауте заявки")
    public void testConfirmSuccessNew() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        LatchTaskListenerConfig.TaskListener mockedTaskListener = (LatchTaskListenerConfig.TaskListener) taskListener;
        mockedTaskListener.setFinishedLatch(countDownLatch);
        doReturn(getLomOrder(ChangeOrderRequestStatus.SUCCESS))
            .when(logisticsOrderService)
            .getByIdOrThrow(eq(123L), eq(EnumSet.of(OptionalOrderPart.CHANGE_REQUESTS)));
        doReturn(getOrder(LocalDate.of(2021, 3, 2), true))
            .when(checkouterOrderService)
            .getOrderWithChangeRequests(eq(ORDER_ID));

        queueProducer.enqueue(ENQUEUE_PARAMS);
        countDownLatch.await(2, TimeUnit.SECONDS);

        softly.assertThat(mockedTaskListener.getLastResults()).containsExactly(TaskExecutionResult.finish());
        verify(checkouterOrderService).updateDeliveryDate(eq(createUpdateOrderDeliveryDateDto()));
        verify(checkouterOrderService).getOrderWithChangeRequests(eq(ORDER_ID));
    }

    @ParameterizedTest
    @EnumSource(value = ChangeRequestStatus.class, names = {"REJECTED", "APPLIED"})
    @DisplayName("Успешное подтверждение НЕ существующей в чекауте заявки,"
        + " если там уже есть заявка в финальном статусе")
    public void testConfirmSuccessNewWithRejected(ChangeRequestStatus changeRequestStatus) throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        LatchTaskListenerConfig.TaskListener mockedTaskListener = (LatchTaskListenerConfig.TaskListener) taskListener;
        mockedTaskListener.setFinishedLatch(countDownLatch);

        doReturn(getLomOrder(ChangeOrderRequestStatus.SUCCESS))
            .when(logisticsOrderService)
            .getByIdOrThrow(eq(123L), eq(EnumSet.of(OptionalOrderPart.CHANGE_REQUESTS)));

        doReturn(getOrder(LocalDate.of(2021, 3, 1), null, changeRequestStatus, OrderStatus.DELIVERY))
            .when(checkouterOrderService)
            .getOrderWithChangeRequests(eq(ORDER_ID));

        queueProducer.enqueue(ENQUEUE_PARAMS);
        countDownLatch.await(2, TimeUnit.SECONDS);

        softly.assertThat(mockedTaskListener.getLastResults()).containsExactly(TaskExecutionResult.finish());
        verify(checkouterOrderService).updateDeliveryDate(eq(createUpdateOrderDeliveryDateDto()));
        verify(checkouterOrderService).getOrderWithChangeRequests(eq(ORDER_ID));
    }

    @Test
    @DisplayName("Успешное подтверждение НЕ существующей в чекауте заявки: даты совпадают, интервалы - нет")
    public void testTimeIntervalChangedConfirmSuccess() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        LatchTaskListenerConfig.TaskListener mockedTaskListener = (LatchTaskListenerConfig.TaskListener) taskListener;
        mockedTaskListener.setFinishedLatch(countDownLatch);

        LocalTime startTime = LocalTime.of(14, 0);
        LocalTime endTime = LocalTime.of(15, 0);

        doReturn(getLomOrder(
            ChangeOrderRequestStatus.SUCCESS,
            ChangeOrderRequestReason.DELIVERY_DATE_UPDATED_BY_DELIVERY,
            changeOrderRequestPayloadBuilder()
                .startTime(startTime.plusMinutes(30))
                .endTime(endTime.plusMinutes(30))
                .build()
        ))
            .when(logisticsOrderService)
            .getByIdOrThrow(eq(123L), eq(EnumSet.of(OptionalOrderPart.CHANGE_REQUESTS)));

        doReturn(
            getOrder(
                LocalDate.of(2021, 3, 1),
                new TimeInterval(startTime, endTime),
                true,
                OrderStatus.DELIVERY
            )
        )
            .when(checkouterOrderService)
            .getOrderWithChangeRequests(eq(ORDER_ID));

        queueProducer.enqueue(ENQUEUE_PARAMS);
        countDownLatch.await(2, TimeUnit.SECONDS);

        softly.assertThat(mockedTaskListener.getLastResults()).containsExactly(TaskExecutionResult.finish());
        verify(checkouterOrderService).updateDeliveryDate(eq(createUpdateOrderDeliveryDateDto()));
        verify(checkouterOrderService).getOrderWithChangeRequests(eq(ORDER_ID));
    }

    @Test
    @DisplayName("Подтверждение НЕ существующей в чекауте заявки - дата доставки не обновляется, если нет даты в LOM")
    public void testConfirmNewDeliveryDateNotUpdatedSinceDeliveryIntervalNull() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        LatchTaskListenerConfig.TaskListener mockedTaskListener = (LatchTaskListenerConfig.TaskListener) taskListener;
        mockedTaskListener.setFinishedLatch(countDownLatch);
        doReturn(getLomOrder(ChangeOrderRequestStatus.SUCCESS).setDeliveryInterval(null))
            .when(logisticsOrderService)
            .getByIdOrThrow(eq(123L), eq(EnumSet.of(OptionalOrderPart.CHANGE_REQUESTS)));
        doReturn(getOrder(LocalDate.of(2021, 3, 2), true))
            .when(checkouterOrderService)
            .getOrderWithChangeRequests(eq(ORDER_ID));

        queueProducer.enqueue(ENQUEUE_PARAMS);
        countDownLatch.await(2, TimeUnit.SECONDS);

        softly.assertThat(mockedTaskListener.getLastResults()).containsExactly(TaskExecutionResult.finish());
        verify(checkouterOrderService).getOrderWithChangeRequests(eq(ORDER_ID));
    }

    @Test
    @DisplayName("Успешное отклонение существующей в чекауте заявки")
    public void testConfirmFail() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        LatchTaskListenerConfig.TaskListener mockedTaskListener = (LatchTaskListenerConfig.TaskListener) taskListener;
        mockedTaskListener.setFinishedLatch(countDownLatch);
        doReturn(getLomOrder(ChangeOrderRequestStatus.FAIL))
            .when(logisticsOrderService)
            .getByIdOrThrow(eq(123L), eq(EnumSet.of(OptionalOrderPart.CHANGE_REQUESTS)));
        doReturn(getOrder(LocalDate.of(2021, 3, 1), true))
            .when(checkouterOrderService)
            .getOrderWithChangeRequests(eq(ORDER_ID));

        queueProducer.enqueue(ENQUEUE_PARAMS);
        countDownLatch.await(2, TimeUnit.SECONDS);

        softly.assertThat(mockedTaskListener.getLastResults()).containsExactly(TaskExecutionResult.finish());
        verify(checkouterOrderService).rejectChangeRequest(eq(ORDER_ID), eq(10001L), isNull());
        verify(checkouterOrderService).getOrderWithChangeRequests(eq(ORDER_ID));
    }

    @Test
    @DisplayName("Успешная обработка новой заявки на обновление даты при задержке отгрузки")
    public void testNew() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        LatchTaskListenerConfig.TaskListener mockedTaskListener = (LatchTaskListenerConfig.TaskListener) taskListener;
        mockedTaskListener.setFinishedLatch(countDownLatch);
        doReturn(getLomOrder(
            ChangeOrderRequestStatus.CREATED,
            ChangeOrderRequestReason.SHIPPING_DELAYED,
            null
        ))
            .when(logisticsOrderService)
            .getByIdOrThrow(eq(123L), eq(EnumSet.of(OptionalOrderPart.CHANGE_REQUESTS)));
        doReturn(getOrder(LocalDate.of(2021, 3, 1), false))
            .when(checkouterOrderService)
            .getOrder(eq(ORDER_ID));
        doReturn(getOrderEditOptions())
            .when(checkouterOrderService)
            .getNewDeliveryOptions(
                eq(ORDER_ID),
                eq(LocalDate.of(2021, 3, 5))
            );

        queueProducer.enqueue(ENQUEUE_PARAMS);
        countDownLatch.await(2, TimeUnit.SECONDS);

        softly.assertThat(mockedTaskListener.getLastResults()).containsExactly(TaskExecutionResult.finish());
        verify(logisticsOrderService).processSaveUpdateDeliveryDatePayloadRequest(
            eq(1L),
            eq(UpdateOrderDeliveryDateRequestDto.builder()
                .barcode(ORDER_ID.toString())
                .dateMin(LocalDate.of(2021, 3, 10))
                .dateMax(LocalDate.of(2021, 3, 10))
                .reason(ChangeOrderRequestReason.SHIPPING_DELAYED)
                .build())
        );
        verify(checkouterOrderService).getOrder(eq(ORDER_ID));
        verify(checkouterOrderService).getNewDeliveryOptions(eq(ORDER_ID), eq(LocalDate.of(2021, 3, 5)));
    }

    @Test
    @DisplayName(
        "Неуспешная обработка новой заявки на обновление даты при задержке отгрузки: нет новой даты от чекаута"
    )
    public void testNewNoNewDeliveryDate() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        LatchTaskListenerConfig.TaskListener mockedTaskListener = (LatchTaskListenerConfig.TaskListener) taskListener;
        mockedTaskListener.setFinishedLatch(countDownLatch);
        doReturn(getLomOrder(
            ChangeOrderRequestStatus.CREATED,
            ChangeOrderRequestReason.SHIPPING_DELAYED,
            null
        ))
            .when(logisticsOrderService)
            .getByIdOrThrow(eq(123L), eq(EnumSet.of(OptionalOrderPart.CHANGE_REQUESTS)));
        doReturn(getOrder(LocalDate.of(2021, 3, 1), false))
            .when(checkouterOrderService)
            .getOrder(eq(ORDER_ID));
        OrderEditOptions orderEditOptions = getOrderEditOptions();
        orderEditOptions.setDeliveryOptions(Set.of());
        doReturn(orderEditOptions)
            .when(checkouterOrderService)
            .getNewDeliveryOptions(
                eq(ORDER_ID),
                eq(LocalDate.of(2021, 3, 5))
            );

        queueProducer.enqueue(ENQUEUE_PARAMS);
        countDownLatch.await(2, TimeUnit.SECONDS);

        softly.assertThat(mockedTaskListener.getLastResults()).containsExactly(TaskExecutionResult.finish());
        verify(logisticsOrderService).getByIdOrThrow(
            eq(123L),
            eq(EnumSet.of(OptionalOrderPart.CHANGE_REQUESTS))
        );
        verify(logisticsOrderService).processDenyDeliveryDateRequestRequest(eq(1L));
        verify(checkouterOrderService).getOrder(eq(ORDER_ID));
        verify(checkouterOrderService).getNewDeliveryOptions(eq(ORDER_ID), eq(LocalDate.of(2021, 3, 5)));
    }

    @Test
    @DisplayName("Неуспешная обработка новой заявки на обновление даты: неподходящая причина")
    public void testNewWithWrongReason() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        LatchTaskListenerConfig.TaskListener mockedTaskListener = (LatchTaskListenerConfig.TaskListener) taskListener;
        mockedTaskListener.setFinishedLatch(countDownLatch);
        doReturn(getLomOrder(
            ChangeOrderRequestStatus.CREATED,
            ChangeOrderRequestReason.DELIVERY_DATE_UPDATED_BY_DELIVERY,
            null
        ))
            .when(logisticsOrderService)
            .getByIdOrThrow(eq(123L), eq(EnumSet.of(OptionalOrderPart.CHANGE_REQUESTS)));

        queueProducer.enqueue(ENQUEUE_PARAMS);
        countDownLatch.await(2, TimeUnit.SECONDS);

        softly.assertThat(mockedTaskListener.getLastResults()).containsExactly(TaskExecutionResult.finish());
        verify(logisticsOrderService).getByIdOrThrow(
            eq(123L),
            eq(EnumSet.of(OptionalOrderPart.CHANGE_REQUESTS))
        );
        verifyNoMoreInteractions(logisticsOrderService);
        verifyZeroInteractions(checkouterOrderService);
    }

    @Test
    @DisplayName("Попытка подтверждения НЕ существующей в чекауте заявки для отменённого заказа")
    public void testConfirmSuccessNewOrderCancelled() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        LatchTaskListenerConfig.TaskListener mockedTaskListener = (LatchTaskListenerConfig.TaskListener) taskListener;
        mockedTaskListener.setFinishedLatch(countDownLatch);
        doReturn(getLomOrder(ChangeOrderRequestStatus.SUCCESS))
            .when(logisticsOrderService)
            .getByIdOrThrow(eq(123L), eq(EnumSet.of(OptionalOrderPart.CHANGE_REQUESTS)));
        doReturn(getOrder(LocalDate.of(2021, 3, 2), null, true, OrderStatus.CANCELLED))
            .when(checkouterOrderService)
            .getOrderWithChangeRequests(eq(ORDER_ID));

        queueProducer.enqueue(ENQUEUE_PARAMS);
        countDownLatch.await(2, TimeUnit.SECONDS);

        softly.assertThat(mockedTaskListener.getLastResults()).containsExactly(TaskExecutionResult.finish());
        verify(checkouterOrderService).getOrderWithChangeRequests(eq(ORDER_ID));
    }

    @Test
    @DisplayName("Успешное подтверждение НЕ существующей в чекауте заявки: проверка по externalId")
    public void testConfirmSuccessNewByExternalId() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        LatchTaskListenerConfig.TaskListener mockedTaskListener = (LatchTaskListenerConfig.TaskListener) taskListener;
        mockedTaskListener.setFinishedLatch(countDownLatch);

        doReturn(getLomOrder(
            ChangeOrderRequestStatus.SUCCESS,
            ChangeOrderRequestReason.DELIVERY_DATE_UPDATED_BY_DELIVERY,
            changeOrderRequestPayloadBuilder().changeRequestExternalId(10002L).build()
        ))
            .when(logisticsOrderService)
            .getByIdOrThrow(eq(123L), eq(EnumSet.of(OptionalOrderPart.CHANGE_REQUESTS)));

        doReturn(getOrder(LocalDate.of(2021, 3, 1), true))
            .when(checkouterOrderService)
            .getOrderWithChangeRequests(eq(ORDER_ID));

        queueProducer.enqueue(ENQUEUE_PARAMS);
        countDownLatch.await(2, TimeUnit.SECONDS);

        softly.assertThat(mockedTaskListener.getLastResults()).containsExactly(TaskExecutionResult.finish());
        verify(checkouterOrderService).updateDeliveryDate(eq(createUpdateOrderDeliveryDateDto()));
        verify(checkouterOrderService).getOrderWithChangeRequests(eq(ORDER_ID));
    }

    @Test
    @DisplayName("Успешное подтверждение существующей в чекауте заявки: проверка по externalId")
    public void testConfirmSuccessExistingByExternalId() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        LatchTaskListenerConfig.TaskListener mockedTaskListener = (LatchTaskListenerConfig.TaskListener) taskListener;
        mockedTaskListener.setFinishedLatch(countDownLatch);

        doReturn(getLomOrder(
            ChangeOrderRequestStatus.SUCCESS,
            ChangeOrderRequestReason.DELIVERY_DATE_UPDATED_BY_DELIVERY,
            changeOrderRequestPayloadBuilder().changeRequestExternalId(10001L).build()
        ))
            .when(logisticsOrderService)
            .getByIdOrThrow(eq(123L), eq(EnumSet.of(OptionalOrderPart.CHANGE_REQUESTS)));

        doReturn(getOrder(LocalDate.of(2021, 3, 1), true))
            .when(checkouterOrderService)
            .getOrderWithChangeRequests(eq(ORDER_ID));

        queueProducer.enqueue(ENQUEUE_PARAMS);
        countDownLatch.await(2, TimeUnit.SECONDS);

        softly.assertThat(mockedTaskListener.getLastResults()).containsExactly(TaskExecutionResult.finish());
        verify(checkouterOrderService).applyChangeRequest(eq(ORDER_ID), eq(10001L));
        verify(checkouterOrderService).getOrderWithChangeRequests(eq(ORDER_ID));
    }

    @Nonnull
    private OrderDto getLomOrder(ChangeOrderRequestStatus changeOrderRequestStatus) {
        return getLomOrder(
            changeOrderRequestStatus,
            ChangeOrderRequestReason.DELIVERY_DATE_UPDATED_BY_DELIVERY,
            changeOrderRequestPayloadBuilder().build()
        );
    }

    @Nonnull
    private UpdateOrderDeliveryDateRequestDto.UpdateOrderDeliveryDateRequestDtoBuilder
    changeOrderRequestPayloadBuilder() {
        return UpdateOrderDeliveryDateRequestDto.builder()
            .barcode("235")
            .dateMin(LocalDate.of(2021, 3, 1))
            .dateMax(LocalDate.of(2021, 3, 1));
    }

    @Nonnull
    @SneakyThrows
    private OrderDto getLomOrder(
        ChangeOrderRequestStatus changeOrderRequestStatus,
        ChangeOrderRequestReason reason,
        @Nullable UpdateOrderDeliveryDateRequestDto changeOrderRequestPayload
    ) {
        Set<ChangeOrderRequestPayloadDto> payloadDtos = Set.of();
        if (changeOrderRequestPayload != null) {
            payloadDtos = Set.of(
                ChangeOrderRequestPayloadDto.builder()
                    .status(ChangeOrderRequestStatus.INFO_RECEIVED)
                    .payload(objectMapper.valueToTree(changeOrderRequestPayload))
                    .build()
            );
        }
        return new OrderDto()
            .setId(123L)
            .setBarcode(ORDER_ID.toString())
            .setExternalId(ORDER_ID.toString())
            .setDeliveryInterval(
                DeliveryIntervalDto.builder()
                    .deliveryDateMin(LocalDate.of(2021, 3, 1))
                    .deliveryDateMax(LocalDate.of(2021, 3, 1))
                    .build()
            )
            .setChangeOrderRequests(
                List.of(
                    ChangeOrderRequestDto.builder()
                        .id(1L)
                        .requestType(ChangeOrderRequestType.DELIVERY_DATE)
                        .status(changeOrderRequestStatus)
                        .reason(reason)
                        .payloads(payloadDtos)
                        .build()
                )
            );
    }

    @Nonnull
    private static UpdateOrderDeliveryDateDto createUpdateOrderDeliveryDateDto() {
        return new UpdateOrderDeliveryDateDto(
            ORDER_ID,
            null,
            UpdateOrderDeliveryDateDto.FAKE_CHANGE_REQUEST_ID,
            null,
            LocalDate.of(2021, 3, 1).atStartOfDay(),
            LocalDate.of(2021, 3, 1).atStartOfDay(),
            null,
            null,
            null,
            null,
            DeliveryDateUpdateReason.DELIVERY_SERVICE_DELAYED
        );
    }

    @Nonnull
    private static ChangeRequest createChangeRequest(
        ChangeRequestStatus changeRequestStatus,
        LocalDate deliveryDate,
        @Nullable TimeInterval timeInterval
    ) {
        DeliveryDatesChangeRequestPayload payload = new DeliveryDatesChangeRequestPayload();
        payload.setFromDate(deliveryDate);
        payload.setToDate(deliveryDate);
        payload.setTimeInterval(timeInterval);
        payload.setReason(HistoryEventReason.DELIVERY_SERVICE_DELAYED);
        return new ChangeRequest(
            10001L,
            ORDER_ID,
            payload,
            changeRequestStatus,
            Instant.now(),
            "Any text",
            ClientRole.SYSTEM
        );
    }

    @Nonnull
    private static DeliveryDateChangeRequestDto getDto() {
        return new DeliveryDateChangeRequestDto(1L, 123L);
    }

    @Nonnull
    private Order getOrder(LocalDate deliveryDate, boolean withChangeRequests) {
        return getOrder(deliveryDate, null, withChangeRequests, OrderStatus.DELIVERY);
    }

    @Nonnull
    private Order getOrder(
        LocalDate deliveryDate,
        @Nullable TimeInterval timeInterval,
        boolean withChangeRequests,
        OrderStatus orderStatus
    ) {
        return getOrder(
            deliveryDate,
            timeInterval,
            withChangeRequests ? ChangeRequestStatus.PROCESSING : null,
            orderStatus
        );
    }

    @Nonnull
    private Order getOrder(
        LocalDate deliveryDate,
        @Nullable TimeInterval timeInterval,
        @Nullable ChangeRequestStatus changeRequestStatus,
        OrderStatus orderStatus
    ) {
        Track track = new Track();
        track.setDeliveryServiceType(DeliveryServiceType.FULFILLMENT);
        track.setDeliveryServiceId(123L);
        Parcel parcel = new Parcel();
        parcel.addTrack(track);
        parcel.setShipmentDate(LocalDate.of(2021, 3, 5));
        DeliveryDates deliveryDates = new DeliveryDates();
        Date deliveryDateDate = Date.from(deliveryDate.atStartOfDay().atZone(DateTimeUtils.MOSCOW_ZONE).toInstant());
        deliveryDates.setFromDate(deliveryDateDate);
        deliveryDates.setToDate(deliveryDateDate);
        Delivery delivery = new Delivery();
        delivery.addParcel(parcel);
        delivery.setDeliveryDates(deliveryDates);
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setDelivery(delivery);
        if (changeRequestStatus != null) {
            order.setChangeRequests(List.of(createChangeRequest(changeRequestStatus, deliveryDate, timeInterval)));
        }
        order.setStatus(orderStatus);
        return order;
    }

    @Nonnull
    private OrderEditOptions getOrderEditOptions() {
        DeliveryOption deliveryOption = new DeliveryOption();
        deliveryOption.setFromDate(LocalDate.of(2021, 3, 10));
        deliveryOption.setToDate(LocalDate.of(2021, 3, 10));
        OrderEditOptions orderEditOptions = new OrderEditOptions();
        orderEditOptions.setDeliveryOptions(Set.of(deliveryOption));
        return orderEditOptions;
    }
}
