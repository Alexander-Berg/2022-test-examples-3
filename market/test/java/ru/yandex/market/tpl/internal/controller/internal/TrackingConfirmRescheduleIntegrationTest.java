package ru.yandex.market.tpl.internal.controller.internal;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.tpl.api.model.order.partner.OrderEventType;
import ru.yandex.market.tpl.api.model.shift.UserShiftStatus;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryRescheduleReasonType;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.api.model.tracking.ConfirmDeliveryRescheduleDto;
import ru.yandex.market.tpl.api.model.tracking.TrackingDeliveryStatus;
import ru.yandex.market.tpl.api.model.tracking.TrackingDto;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderHistoryEvent;
import ru.yandex.market.tpl.core.domain.order.OrderHistoryEventRepository;
import ru.yandex.market.tpl.core.domain.order.difference.RescheduleConfirmationDifference;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.service.tracking.TrackingService;
import ru.yandex.market.tpl.internal.controller.BaseTplIntWebTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.tpl.core.mvc.ServiceTicketRequestHandler.SERVICE_TICKET_HEADER;

@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class TrackingConfirmRescheduleIntegrationTest extends BaseTplIntWebTest {

    private final MockMvc mockMvc;
    private final TestUserHelper testUserHelper;
    private final OrderGenerateService orderGenerateService;
    private final TrackingService trackingService;
    private final ObjectMapper tplObjectMapper;
    private final UserShiftCommandService userShiftCommandService;
    private final OrderHistoryEventRepository orderHistoryEventRepository;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final ConfigurationServiceAdapter configurationServiceAdapter;

    private String trackingId;
    private Order order;
    private UserShift shiftWithDeliveryTask;
    private OrderDeliveryTask deliveryTask;
    private User user;

    @BeforeEach
    void setUpThis() {

        order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .buyerYandexUid(1L)
                .deliveryPrice(new BigDecimal("56.7"))
                .items(
                        OrderGenerateService.OrderGenerateParam.Items.builder()
                                .itemsPrice(new BigDecimal("34.5"))
                                .build()
                )
                .build());

        user = testUserHelper.findOrCreateUser(UID);

        shiftWithDeliveryTask = testUserHelper.createShiftWithDeliveryTask(
                user,
                UserShiftStatus.SHIFT_OPEN,
                order
        );
        testUserHelper.finishPickupAtStartOfTheDay(shiftWithDeliveryTask);

        trackingId = trackingService.getTrackingLinkByOrder(order.getExternalOrderId()).orElseThrow();

        deliveryTask = shiftWithDeliveryTask.getDeliveryTaskForOrder(order.getId());
    }

    @Test
    void shouldShowRescheduleConfirmationNotice() {
        configurationServiceAdapter.insertValue(
                ConfigurationProperties.TRIGGERS_PLATFORM_COMMUNICATION_ORDER_RESCHEDULED_CONFIRM_ENABLED,
                true);

        TrackingDto trackingDto = getTrackingDto();

        assertThat(trackingDto.getDelivery().getStatus())
                .isEqualTo(TrackingDeliveryStatus.IN_PROGRESS);

        assertThat(trackingDto.getDelivery().getShowRescheduleConfirmationNotice()).isNull();
        assertThat(trackingDto.getOrders()).hasSize(1);
        assertThat(trackingDto.getOrders().get(0).getId()).isEqualTo(order.getExternalOrderId());


        testUserHelper.rescheduleNextDay(deliveryTask.getRoutePoint(), Source.COURIER,
                OrderDeliveryRescheduleReasonType.CLIENT_REQUEST
        );

        // проверяем, что отправляли смс об отмене заказа
        dbQueueTestUtil.assertQueueHasSize(QueueType.LOGBROKER_WRITER, 2 + 1); // 2 события в ярд + смс-ка

        trackingDto = getTrackingDto();

        assertThat(trackingDto.getDelivery().getStatus())
                .isEqualTo(TrackingDeliveryStatus.RESCHEDULED);

        assertThat(trackingDto.getDelivery().getShowRescheduleConfirmationNotice()).isEqualTo(true);
        assertThat(trackingDto.getOrders()).extracting("id").containsOnly(order.getExternalOrderId());
    }

    @Test
    void shouldntShowRescheduleConfirmationNoticeOnNotClientRequestReason() {
        testUserHelper.rescheduleNextDay(deliveryTask.getRoutePoint(), Source.COURIER,
                OrderDeliveryRescheduleReasonType.NO_PASSPORT
        );

        TrackingDto trackingDto = getTrackingDto();

        assertThat(trackingDto.getDelivery().getStatus())
                .isEqualTo(TrackingDeliveryStatus.RESCHEDULED);

        assertThat(trackingDto.getDelivery().getShowRescheduleConfirmationNotice()).isEqualTo(false);
    }

    @Test
    void shouldntShowRescheduleConfirmationNoticeOnClientReschedule() {
        testUserHelper.rescheduleNextDay(deliveryTask.getRoutePoint(), Source.CLIENT,
                OrderDeliveryRescheduleReasonType.CLIENT_REQUEST
        );

        TrackingDto trackingDto = getTrackingDto();

        assertThat(trackingDto.getDelivery().getStatus())
                .isEqualTo(TrackingDeliveryStatus.RESCHEDULED);

        assertThat(trackingDto.getDelivery().getShowRescheduleConfirmationNotice()).isEqualTo(false);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldSaveRescheduleConfirmationResult(boolean isConfirmed) {
        assertThat(deliveryTask.getRescheduleConfirmed()).isNull();

        testUserHelper.rescheduleNextDay(
                deliveryTask.getRoutePoint(), Source.COURIER,
                OrderDeliveryRescheduleReasonType.CLIENT_REQUEST
        );

        TrackingDto trackingDto = confirmReschedule(isConfirmed);
        assertThat(trackingDto.getDelivery().getShowRescheduleConfirmationNotice()).isEqualTo(false);

        assertThat(deliveryTask.getRescheduleConfirmed()).isEqualTo(isConfirmed);

        trackingDto = getTrackingDto();
        assertThat(trackingDto.getDelivery().getShowRescheduleConfirmationNotice()).isEqualTo(false);
    }

    @Test
    void shouldntConfirmRescheduleOnInProgress() throws Exception {
        confirmRescheduleWithActions(true)
                .andExpect(status().is4xxClientError());
    }

    @Test
    void shouldntConfirmRescheduleSecondTime() throws Exception {
        testUserHelper.rescheduleNextDay(deliveryTask.getRoutePoint(), Source.COURIER,
                OrderDeliveryRescheduleReasonType.CLIENT_REQUEST
        );

        confirmReschedule(true);

        confirmRescheduleWithActions(true)
                .andExpect(status().is4xxClientError());
    }

    @Test
    void shouldShowRescheduleNoticeOnSecondReschedule() {
        // 1й перенос
        testUserHelper.rescheduleNextDay(
                deliveryTask.getRoutePoint(), Source.COURIER,
                OrderDeliveryRescheduleReasonType.CLIENT_REQUEST
        );

        TrackingDto trackingDto = getTrackingDto();

        assertThat(trackingDto.getDelivery().getStatus())
                .isEqualTo(TrackingDeliveryStatus.RESCHEDULED);

        // клиент подтверждает перенос
        confirmReschedule(true);

        // закрываем смену с переносом
        userShiftCommandService.closeShift(new UserShiftCommand.Close(shiftWithDeliveryTask.getId()));

        // новый день
        Instant instant = Clock.system(ZoneId.systemDefault()).instant();
        instant.plus(1, ChronoUnit.DAYS);
        Clock.fixed(instant, ZoneId.systemDefault());

        // новая смена
        shiftWithDeliveryTask = testUserHelper.createShiftWithDeliveryTask(
                user,
                UserShiftStatus.SHIFT_OPEN,
                order
        );
        testUserHelper.finishPickupAtStartOfTheDay(shiftWithDeliveryTask);

        trackingDto = getTrackingDto();

        assertThat(trackingDto.getDelivery().getStatus())
                .isEqualTo(TrackingDeliveryStatus.IN_PROGRESS);

        // новая задача на доставку
        OrderDeliveryTask nextDeliveryTask = shiftWithDeliveryTask.getDeliveryTaskForOrder(order.getId());
        // новый перенос
        testUserHelper.rescheduleNextDay(
                nextDeliveryTask.getRoutePoint(), Source.COURIER,
                OrderDeliveryRescheduleReasonType.CLIENT_REQUEST
        );

        // подтверждения не было
        assertThat(nextDeliveryTask.getRescheduleConfirmed())
                .isEqualTo(null);

        trackingDto = getTrackingDto();
        // нужно показывать message box с подтверждением
        assertThat(trackingDto.getDelivery().getShowRescheduleConfirmationNotice())
                .isEqualTo(true);

        // старый ответ сохранен
        assertThat(deliveryTask.getRescheduleConfirmed()).isEqualTo(true);

    }

    @Test
    void shouldShowRescheduleNoticeOnSecondRescheduleAfterReopen() {
        // 1й перенос
        testUserHelper.rescheduleNextDay(
                deliveryTask.getRoutePoint(), Source.COURIER,
                OrderDeliveryRescheduleReasonType.CLIENT_REQUEST
        );

        {
            TrackingDto trackingDto = getTrackingDto();

            assertThat(trackingDto.getDelivery().getStatus())
                    .isEqualTo(TrackingDeliveryStatus.RESCHEDULED);
        }

        // клиент подтверждает перенос
        confirmReschedule(false);
        {
            OrderHistoryEvent lastEvent = getLastEvent();
            assertThat(lastEvent.getType()).isEqualTo(OrderEventType.ORDER_RESCHEDULE_CONFIRMATION);
            assertThat(((RescheduleConfirmationDifference) lastEvent.getDifference()).isConfirmed())
                    .isEqualTo(false);
        }

        testUserHelper.reopen(deliveryTask.getRoutePoint());

        testUserHelper.rescheduleNextDay(
                deliveryTask.getRoutePoint(), Source.COURIER,
                OrderDeliveryRescheduleReasonType.CLIENT_REQUEST
        );

        {
            TrackingDto trackingDto = getTrackingDto();

            assertThat(trackingDto.getDelivery().getStatus())
                    .isEqualTo(TrackingDeliveryStatus.RESCHEDULED);


            assertThat(trackingDto.getDelivery().getShowRescheduleConfirmationNotice()).isEqualTo(true);
        }

        confirmReschedule(true);
        {
            OrderHistoryEvent lastEvent = getLastEvent();
            assertThat(lastEvent.getType()).isEqualTo(OrderEventType.ORDER_RESCHEDULE_CONFIRMATION);
            assertThat(((RescheduleConfirmationDifference) lastEvent.getDifference()).isConfirmed())
                    .isEqualTo(true);
        }
    }

    private OrderHistoryEvent getLastEvent() {
        var events = orderHistoryEventRepository.findAllByOrderId(order.getId());
        var lastEvent = events.get(events.size() - 1);
        return lastEvent;
    }

    @SneakyThrows
    private TrackingDto getTrackingDto() {
        MvcResult result = mockMvc.perform(
                get("/internal/tracking/{trackingId}", trackingId)
                        .header(SERVICE_TICKET_HEADER, 100)
        )
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn();

        TrackingDto trackingDto = tplObjectMapper.readValue(result.getResponse().getContentAsString(),
                TrackingDto.class);
        return trackingDto;
    }

    @SneakyThrows
    private TrackingDto confirmReschedule(boolean confirmed) {
        MvcResult result = confirmRescheduleWithActions(confirmed)
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn();

        TrackingDto trackingDto = tplObjectMapper.readValue(result.getResponse().getContentAsString(),
                TrackingDto.class);
        return trackingDto;
    }

    @SneakyThrows
    private ResultActions confirmRescheduleWithActions(boolean confirmed) {
        ConfirmDeliveryRescheduleDto req = new ConfirmDeliveryRescheduleDto(confirmed);
        return mockMvc.perform(
                post("/internal/tracking/{trackingId}/confirmDeliveryReschedule", trackingId)
                        .content(tplObjectMapper.writeValueAsString(req))
                        .contentType(MediaType.APPLICATION_JSON_UTF8));
    }
}
