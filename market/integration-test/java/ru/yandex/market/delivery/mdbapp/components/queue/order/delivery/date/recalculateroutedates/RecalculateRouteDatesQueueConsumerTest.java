package ru.yandex.market.delivery.mdbapp.components.queue.order.delivery.date.recalculateroutedates;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import steps.utils.TestableClock;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.event.HistoryEventReason;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.DeliveryEditRequest;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.checkouter.order.TimeInterval;
import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.delivery.mdbapp.AllMockContextualTest;
import ru.yandex.market.delivery.mdbapp.configuration.LatchTaskListenerConfig;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.ChangeOrderRequestDto;
import ru.yandex.market.logistics.lom.model.dto.ChangeOrderRequestPayloadDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestReason;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestStatus;
import ru.yandex.market.logistics.lom.model.enums.ChangeOrderRequestType;
import ru.yandex.market.logistics.lom.model.enums.OptionalOrderPart;
import ru.yandex.money.common.dbqueue.api.EnqueueParams;
import ru.yandex.money.common.dbqueue.api.QueueProducer;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;
import ru.yandex.money.common.dbqueue.api.TaskLifecycleListener;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ParametersAreNonnullByDefault
@Sql(value = "/data/search-orders-in-lom-master-enabled.sql",
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Sql(value = "/data/clean_internal_variable.sql",
    executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
@DisplayName("Тест обработки заявок из лома на перенос даты доставки")
class RecalculateRouteDatesQueueConsumerTest extends AllMockContextualTest {

    private static final Instant FIXED_TIME = Instant.parse("2021-08-17T10:00:00Z");
    private static final Instant DATE_FROM = Instant.parse("2021-08-18T12:00:00Z");
    private static final Instant DATE_TO = Instant.parse("2021-08-19T14:00:00Z");
    private static final LocalDateTime RECALCULATED_LOCAL_DATE_FROM = LocalDateTime.ofInstant(
        DATE_FROM,
        DateTimeUtils.MOSCOW_ZONE
    );
    private static final LocalDateTime RECALCULATED_LOCAL_DATE_TO = LocalDateTime.ofInstant(
        DATE_TO,
        DateTimeUtils.MOSCOW_ZONE
    );
    private static final LocalTime RECALCULATED_TIME_FROM = RECALCULATED_LOCAL_DATE_FROM.toLocalTime();
    private static final LocalTime RECALCULATED_TIME_TO = RECALCULATED_LOCAL_DATE_TO.toLocalTime();

    private static final long CHECKOUTER_ORDER_ID = 123;
    private static final long CHANGE_REQUEST_ID = 1;
    private static final EnqueueParams<RecalculateRouteDatesDto> ENQUEUE_PARAMS = EnqueueParams.create(
        new RecalculateRouteDatesDto(CHANGE_REQUEST_ID, CHECKOUTER_ORDER_ID)
    );

    private static final Set<OptionalOrderPart> OPTIONAL_ORDER_PARTS = EnumSet.of(OptionalOrderPart.CHANGE_REQUESTS);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private QueueProducer<RecalculateRouteDatesDto> queueProducer;

    @Autowired
    private CheckouterAPI checkouterAPI;

    @Autowired
    private LomClient lomClient;

    @Autowired
    private TaskLifecycleListener taskListener;

    @Autowired
    private TestableClock clock;

    private LatchTaskListenerConfig.TaskListener mockedTaskListener;

    private CountDownLatch countDownLatch;

    @BeforeEach
    void beforeTest() {
        clock.setFixed(FIXED_TIME, DateTimeUtils.MOSCOW_ZONE);
        countDownLatch = new CountDownLatch(1);
        mockedTaskListener = (LatchTaskListenerConfig.TaskListener) taskListener;
        mockedTaskListener.setFinishedLatch(countDownLatch);
    }

    @AfterEach
    void afterTest() {
        verify(lomClient).getOrder(CHECKOUTER_ORDER_ID, OPTIONAL_ORDER_PARTS, true);
        verifyNoMoreInteractions(lomClient);
    }

    @MethodSource
    @SneakyThrows
    @ParameterizedTest(name = "[{index}] {0}")
    @DisplayName("У заказа в ломе заявки не в статусе REQUIRED_SEGMENT_SUCCESS")
    void changeRequestsInSkippedStatuses(ChangeOrderRequestStatus status) {
        mockGetLomOrder(lomOrder(status));

        queueProducer.enqueue(ENQUEUE_PARAMS);
        countDownLatch.await(2, TimeUnit.SECONDS);
        softly.assertThat(mockedTaskListener.getLastResults()).containsExactly(TaskExecutionResult.finish());

        verifyNoGetOrderCheckouterCalling();
        verifyNoEditOrderCheckouterCalling();
    }

    @Nonnull
    private static Stream<Arguments> changeRequestsInSkippedStatuses() {
        return Arrays.stream(ChangeOrderRequestStatus.values())
            .filter(
                status -> ChangeOrderRequestStatus.REQUIRED_SEGMENT_SUCCESS != status
                    && ChangeOrderRequestStatus.SUCCESS != status
            )
            .map(Arguments::of);
    }

    @ParameterizedTest(name = "[{index}]{0}")
    @SneakyThrows
    @MethodSource
    @DisplayName("Посчитана новая дата, дата в чекаутере обновляется")
    void successRecalculateRouteDate(
        String displayName,
        Date fromDate,
        Date toDate,
        LocalTime fromTime,
        LocalTime toTime
    ) {
        mockGetLomOrder(lomOrder(ChangeOrderRequestStatus.REQUIRED_SEGMENT_SUCCESS));
        mockGetCheckouterOrder(checkouterOrder(fromDate, toDate, fromTime, toTime));

        queueProducer.enqueue(ENQUEUE_PARAMS);
        countDownLatch.await(2, TimeUnit.SECONDS);
        softly.assertThat(mockedTaskListener.getLastResults()).containsExactly(TaskExecutionResult.finish());

        verify(checkouterAPI).getOrder(CHECKOUTER_ORDER_ID, ClientRole.SYSTEM, null);
        verify(checkouterAPI).editOrder(
            CHECKOUTER_ORDER_ID,
            ClientRole.SYSTEM,
            null,
            List.of(Color.BLUE),
            expectedUpdateRequest()
        );
    }

    @Nonnull
    private static Stream<Arguments> successRecalculateRouteDate() {
        return Stream.of(
            Arguments.of(
                "Отличается дата начала доставки",
                Date.from(DATE_FROM.minus(1, ChronoUnit.DAYS)),
                Date.from(DATE_TO),
                RECALCULATED_TIME_FROM,
                RECALCULATED_TIME_TO
            ),
            Arguments.of(
                "Отличается дата окончания доставки",
                Date.from(DATE_FROM),
                Date.from(DATE_TO.plus(2, ChronoUnit.DAYS)),
                RECALCULATED_TIME_FROM,
                RECALCULATED_TIME_TO
            ),
            Arguments.of(
                "Отличается время начала доставки",
                Date.from(DATE_FROM),
                Date.from(DATE_TO),
                RECALCULATED_TIME_FROM.plus(1, ChronoUnit.HOURS),
                RECALCULATED_TIME_TO
            ),
            Arguments.of(
                "Отличается время окончания доставки",
                Date.from(DATE_FROM),
                Date.from(DATE_TO),
                RECALCULATED_TIME_FROM,
                RECALCULATED_TIME_TO.minus(1, ChronoUnit.HOURS)
            )
        );
    }

    @Test
    @SneakyThrows
    @DisplayName("Посчитана новая дата, дата в чекаутере обновляется, дата перенесена по вине магазина")
    void successRecalculateRouteDateShippingDelayed() {
        mockGetLomOrder(
            lomOrder(
                ChangeOrderRequestStatus.REQUIRED_SEGMENT_SUCCESS,
                null,
                ChangeOrderRequestReason.PROCESSING_DELAYED_BY_PARTNER
            ));
        mockGetCheckouterOrder(checkouterOrder(
            Date.from(DATE_FROM.minus(1, ChronoUnit.DAYS)),
            Date.from(DATE_TO),
            RECALCULATED_TIME_FROM,
            RECALCULATED_TIME_TO
        ));

        queueProducer.enqueue(ENQUEUE_PARAMS);
        countDownLatch.await(2, TimeUnit.SECONDS);
        softly.assertThat(mockedTaskListener.getLastResults()).containsExactly(TaskExecutionResult.finish());

        verify(checkouterAPI).getOrder(CHECKOUTER_ORDER_ID, ClientRole.SYSTEM, null);
        verify(checkouterAPI).editOrder(
            CHECKOUTER_ORDER_ID,
            ClientRole.SYSTEM,
            null,
            List.of(Color.BLUE),
            expectedUpdateRequest(HistoryEventReason.DELAYED_DUE_EXTERNAL_CONDITIONS)
        );
    }

    @Test
    @SneakyThrows
    @DisplayName("Посчитана такая же дата, дата в чекаутере не обновляется")
    void datesAreSame() {
        mockGetLomOrder(lomOrder(ChangeOrderRequestStatus.REQUIRED_SEGMENT_SUCCESS));
        mockGetCheckouterOrder(checkouterOrder(
            Date.from(DATE_FROM),
            Date.from(DATE_TO),
            RECALCULATED_TIME_FROM,
            RECALCULATED_TIME_TO
        ));

        queueProducer.enqueue(ENQUEUE_PARAMS);
        countDownLatch.await(2, TimeUnit.SECONDS);
        softly.assertThat(mockedTaskListener.getLastResults()).containsExactly(TaskExecutionResult.finish());

        verify(checkouterAPI).getOrder(CHECKOUTER_ORDER_ID, ClientRole.SYSTEM, null);
        verifyNoEditOrderCheckouterCalling();
    }

    @Test
    @SneakyThrows
    @DisplayName("Отличается дата начала доставки, но из-за notifyUser = false дата в чекаутере не обновляется")
    void NotUpdateCheckouterDatesIfNotifyUserIsFalse() {
        mockGetLomOrder(lomOrder(ChangeOrderRequestStatus.REQUIRED_SEGMENT_SUCCESS, false));
        mockGetCheckouterOrder(checkouterOrder(
            Date.from(DATE_FROM.minus(1, ChronoUnit.DAYS)),
            Date.from(DATE_TO),
            RECALCULATED_TIME_FROM,
            RECALCULATED_TIME_TO
        ));

        queueProducer.enqueue(ENQUEUE_PARAMS);
        countDownLatch.await(2, TimeUnit.SECONDS);
        softly.assertThat(mockedTaskListener.getLastResults()).containsExactly(TaskExecutionResult.finish());

        verifyNoEditOrderCheckouterCalling();
    }

    @Test
    @SneakyThrows
    @DisplayName("Отличается дата начала доставки, при notifyUser = null дата в чекаутере обновляется")
    void UpdateCheckouterDatesIfNotifyUserIsNull() {
        mockGetLomOrder(lomOrder(ChangeOrderRequestStatus.REQUIRED_SEGMENT_SUCCESS, null));
        mockGetCheckouterOrder(checkouterOrder(
            Date.from(DATE_FROM.minus(1, ChronoUnit.DAYS)),
            Date.from(DATE_TO),
            RECALCULATED_TIME_FROM,
            RECALCULATED_TIME_TO
        ));

        queueProducer.enqueue(ENQUEUE_PARAMS);
        countDownLatch.await(2, TimeUnit.SECONDS);
        softly.assertThat(mockedTaskListener.getLastResults()).containsExactly(TaskExecutionResult.finish());

        verify(checkouterAPI).getOrder(CHECKOUTER_ORDER_ID, ClientRole.SYSTEM, null);
        verify(checkouterAPI).editOrder(
            CHECKOUTER_ORDER_ID,
            ClientRole.SYSTEM,
            null,
            List.of(Color.BLUE),
            expectedUpdateRequest()
        );
    }

    @Test
    @SneakyThrows
    @DisplayName("Не найден заказ в ЛОМ")
    void noOrderInLom() {
        mockGetLomOrder(null);

        queueProducer.enqueue(ENQUEUE_PARAMS);
        countDownLatch.await(2, TimeUnit.SECONDS);
        softly.assertThat(mockedTaskListener.getLastResults()).hasSize(0);

        verifyNoGetOrderCheckouterCalling();
        verifyNoEditOrderCheckouterCalling();
    }

    @Test
    @SneakyThrows
    @DisplayName("Не найден changeRequest с переданным id заказа из ЛОМа")
    void noChangeRequestWithIdInLomOrder() {
        mockGetLomOrder(lomOrder(ChangeOrderRequestStatus.REQUIRED_SEGMENT_SUCCESS).setChangeOrderRequests(List.of()));

        queueProducer.enqueue(ENQUEUE_PARAMS);
        countDownLatch.await(2, TimeUnit.SECONDS);
        softly.assertThat(mockedTaskListener.getLastResults()).hasSize(0);

        verifyNoGetOrderCheckouterCalling();
        verifyNoEditOrderCheckouterCalling();
    }

    @Test
    @SneakyThrows
    @DisplayName("Не найден заказ в чекаутере")
    void orderNotFoundInCheckouter() {
        mockGetLomOrder(lomOrder(ChangeOrderRequestStatus.REQUIRED_SEGMENT_SUCCESS));
        mockGetCheckouterOrder(null);

        queueProducer.enqueue(ENQUEUE_PARAMS);
        countDownLatch.await(2, TimeUnit.SECONDS);
        softly.assertThat(mockedTaskListener.getLastResults()).hasSize(0);

        verify(checkouterAPI).getOrder(CHECKOUTER_ORDER_ID, ClientRole.SYSTEM, null);
        verifyNoEditOrderCheckouterCalling();
    }

    @Test
    @SneakyThrows
    @DisplayName("При вызове метода обновления даты заказа в чекаутере произошла ошибка")
    void updateDeliveryDateInCheckouterFailed() {
        mockGetLomOrder(lomOrder(ChangeOrderRequestStatus.REQUIRED_SEGMENT_SUCCESS));
        mockGetCheckouterOrder(checkouterOrder(
            Date.from(DATE_FROM),
            Date.from(DATE_TO),
            RECALCULATED_TIME_FROM.plus(1, ChronoUnit.HOURS),
            RECALCULATED_TIME_TO
        ));
        doThrow(new RuntimeException())
            .when(checkouterAPI).editOrder(anyLong(), any(), anyLong(), any(), any());

        queueProducer.enqueue(ENQUEUE_PARAMS);
        countDownLatch.await(2, TimeUnit.SECONDS);
        softly.assertThat(mockedTaskListener.getLastResults()).containsExactly(TaskExecutionResult.fail());

        verify(checkouterAPI).editOrder(
            CHECKOUTER_ORDER_ID,
            ClientRole.SYSTEM,
            null,
            List.of(Color.BLUE),
            expectedUpdateRequest()
        );
    }

    private void mockGetLomOrder(@Nullable OrderDto orderDto) {
        doReturn(Optional.ofNullable(orderDto))
            .when(lomClient).getOrder(CHECKOUTER_ORDER_ID, OPTIONAL_ORDER_PARTS, true);

        doThrow(new RuntimeException())
            .when(lomClient).getOrder(CHECKOUTER_ORDER_ID, OPTIONAL_ORDER_PARTS, false);
    }

    private void mockGetCheckouterOrder(@Nullable Order order) {
        doReturn(order)
            .when(checkouterAPI).getOrder(CHECKOUTER_ORDER_ID, ClientRole.SYSTEM, null);
    }

    private void verifyNoGetOrderCheckouterCalling() {
        verify(checkouterAPI, never()).getOrder(anyLong(), any(), any());
    }

    private void verifyNoEditOrderCheckouterCalling() {
        verify(checkouterAPI, never()).editOrder(anyLong(), any(), anyLong(), any(), any());
    }

    @Nonnull
    private Order checkouterOrder(Date fromDate, Date toDate, LocalTime fromTime, LocalTime toTime) {
        Order order = new Order();
        Delivery delivery = new Delivery();
        DeliveryDates deliveryDates = new DeliveryDates(fromDate, toDate, fromTime, toTime);
        delivery.setDeliveryDates(deliveryDates);
        order.setDelivery(delivery);
        return order;
    }

    @Nonnull
    private OrderDto lomOrder(ChangeOrderRequestStatus changeOrderRequestStatus) {
        return lomOrder(changeOrderRequestStatus, true);
    }

    @Nonnull
    private OrderDto lomOrder(ChangeOrderRequestStatus changeOrderRequestStatus, @Nullable Boolean notifyUser) {
        return lomOrder(changeOrderRequestStatus, notifyUser, null);
    }

    @Nonnull
    private OrderDto lomOrder(
        ChangeOrderRequestStatus changeOrderRequestStatus,
        @Nullable Boolean notifyUser,
        @Nullable ChangeOrderRequestReason reason
    ) {
        return new OrderDto()
            .setExternalId(String.valueOf(CHECKOUTER_ORDER_ID))
            .setBarcode(String.valueOf(CHECKOUTER_ORDER_ID))
            .setChangeOrderRequests(List.of(
                    recalculateRouteDatesRequestDto(notifyUser)
                        .status(changeOrderRequestStatus)
                        .reason(reason).build()
                )
            );
    }

    @Nonnull
    private OrderEditRequest expectedUpdateRequest() {
        return expectedUpdateRequest(HistoryEventReason.ROUTE_RECALCULATION);
    }

    @Nonnull
    private OrderEditRequest expectedUpdateRequest(HistoryEventReason reason) {
        OrderEditRequest orderEditRequest = new OrderEditRequest();
        DeliveryEditRequest deliveryEditRequest = DeliveryEditRequest.newDeliveryEditRequest()
            .shipmentDate(null)
            .reason(reason)
            .fromDate(RECALCULATED_LOCAL_DATE_FROM.toLocalDate())
            .toDate(RECALCULATED_LOCAL_DATE_TO.toLocalDate())
            .timeInterval(new TimeInterval(RECALCULATED_TIME_FROM, RECALCULATED_TIME_TO))
            .build();
        orderEditRequest.setDeliveryEditRequest(deliveryEditRequest);
        return orderEditRequest;
    }

    @Nonnull
    private ChangeOrderRequestDto.ChangeOrderRequestDtoBuilder recalculateRouteDatesRequestDto(
        @Nullable Boolean notifyUser
    ) {
        return ChangeOrderRequestDto.builder()
            .id(CHANGE_REQUEST_ID)
            .requestType(ChangeOrderRequestType.RECALCULATE_ROUTE_DATES)
            .payloads(recalculateRouteDatesRequestPayloads(notifyUser));
    }

    @Nonnull
    @SneakyThrows
    private Set<ChangeOrderRequestPayloadDto> recalculateRouteDatesRequestPayloads(@Nullable Boolean notifyUser) {
        ChangeOrderRequestPayloadDto recalculatedPayload = ChangeOrderRequestPayloadDto.builder()
            .status(ChangeOrderRequestStatus.INFO_RECEIVED)
            .payload(objectMapper.readValue(
                getBody("/data/queue/order/delivery/date/recalculateroutedates/change_request_payload.json"),
                JsonNode.class
            ))
            .build();

        ObjectNode jsonPayload = objectMapper.createObjectNode();
        if (notifyUser != null) {
            jsonPayload.put("notifyUser", notifyUser);
        }
        ChangeOrderRequestPayloadDto recalculateDdPayload = ChangeOrderRequestPayloadDto.builder()
            .status(ChangeOrderRequestStatus.CREATED)
            .payload(jsonPayload)
            .build();

        return Set.of(recalculatedPayload, recalculateDdPayload);
    }

    @Nonnull
    private String getBody(@Nonnull String filePath) throws IOException {
        InputStream inputStream = this.getClass().getResourceAsStream(filePath);
        return IOUtils.toString(inputStream, UTF_8);
    }
}
