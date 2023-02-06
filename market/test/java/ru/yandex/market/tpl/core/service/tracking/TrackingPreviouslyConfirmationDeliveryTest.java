package ru.yandex.market.tpl.core.service.tracking;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.tpl.api.model.task.MultiOrderDeliveryTaskDto;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.sms.SmsClient;
import ru.yandex.market.tpl.common.web.exception.TplInvalidActionException;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.tracking.Tracking;
import ru.yandex.market.tpl.core.domain.usershift.tracking.TrackingRepository;
import ru.yandex.market.tpl.core.external.xiva.PushSendService;
import ru.yandex.market.tpl.core.query.usershift.UserShiftQueryService;
import ru.yandex.market.tpl.core.service.tracking.confirmation.previously.PreviouslyConfirmationService;
import ru.yandex.market.tpl.core.service.tracking.confirmation.previously.queue.PreviouslyConfirmationQueueService;
import ru.yandex.market.tpl.core.test.ClockUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.tpl.api.model.task.call.CallToRecipientTaskStatus.SUCCESS_CLIENT_CONFIRM;
import static ru.yandex.market.tpl.api.model.tracking.PreviouslyConfirmationDeliveryStatus.CONFIRMED;
import static ru.yandex.market.tpl.api.model.tracking.PreviouslyConfirmationDeliveryStatus.NO_CONFIRMATION_NEEDED;
import static ru.yandex.market.tpl.api.model.tracking.PreviouslyConfirmationDeliveryStatus.WAITING_CONFIRMATION;
import static ru.yandex.market.tpl.api.model.tracking.SmsStatus.SENT;

@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@CoreTest
public class TrackingPreviouslyConfirmationDeliveryTest {
    private static final long UID = 14324L;

    private static final long DELIVERY_SERVICE_ID = 239L;
    private static final long SORTING_CENTER_ID = 47819L;

    private static final LocalDateTime DEFAULT_INIT_DATE_TIME = LocalDateTime.of(1990, 1, 1, 0, 0, 0);

    private final TestUserHelper testUserHelper;
    private final OrderGenerateService orderGenerateService;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final PreviouslyConfirmationService previouslyConfirmationService;
    private final TrackingRepository trackingRepository;
    private final UserShiftQueryService userShiftQueryService;
    private final PreviouslyConfirmationQueueService previouslyConfirmationQueueService;
    private final PushSendService sendBeruXivaClient;

    @MockBean
    private Clock clock;
    @Autowired
    @Qualifier("yaSmsClient")
    private SmsClient yaSmsClient;

    private LocalDate now;
    private User user;
    private UserShift userShift;
    private Order order;
    private Tracking tracking;

    @Captor
    private ArgumentCaptor<String> smsTextCaptor;

    @BeforeEach
    void setUp() {
        Mockito.clearInvocations(yaSmsClient);
        ClockUtil.initFixed(clock, DEFAULT_INIT_DATE_TIME);
        configurationServiceAdapter.insertValue(ConfigurationProperties.MERGE_MULTI_ORDER_SMS_NOTIFICATIONS_ENABLED,
                true);
        configurationServiceAdapter.mergeValue(ConfigurationProperties.SMS_PHONE_WHITELIST,
                OrderGenerateService.DEFAULT_PHONE);

        now = LocalDate.now();

        order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("4353457985")
                .recipientPhone(OrderGenerateService.DEFAULT_PHONE)
                .deliveryServiceId(DELIVERY_SERVICE_ID)
                .buyerYandexUid(123213L)
                .build());

        user = testUserHelper.findOrCreateUser(UID);
        userShift = testUserHelper.createOpenedShift(user, order, now, SORTING_CENTER_ID);
        tracking = trackingRepository.findByOrderId(order.getId()).get();
    }

    @AfterEach
    void tearDown() {
        Mockito.clearInvocations(yaSmsClient);
    }

    @Test
    void shouldSendOnlyConfirmationDeliverySms() {
        setUpNewExpectedDeliveryTime(Duration.ofMinutes(30));

        testUserHelper.openShift(user, userShift.getId());
        testUserHelper.finishPickupAtStartOfTheDayWithoutFinishCallTasks(userShift);

        dbQueueTestUtil.executeAllQueueItems(QueueType.PREVIOUSLY_CONFIRMATION_SMS_SEND);
        assertThat(dbQueueTestUtil.isEmpty(QueueType.PREVIOUSLY_CONFIRMATION_SMS_SEND)).isTrue();

        dbQueueTestUtil.assertQueueHasSize(QueueType.SMS_NOTIFICATION_SEND, 1);
        dbQueueTestUtil.executeAllQueueItems(QueueType.SMS_NOTIFICATION_SEND);
        verify(yaSmsClient, Mockito.atLeastOnce()).send(Mockito.anyString(), smsTextCaptor.capture());

        Tracking tracking = trackingRepository
                .findByOrderId(userShift.streamOrderDeliveryTasks().findFirst().get().getOrderId())
                .get();
        assertThat(smsTextCaptor.getAllValues()).hasSize(1);
        assertThat(smsTextCaptor.getAllValues().get(0)).startsWith("Вам удобно встретить курьера ");
        assertThat(tracking.getPreviouslyConfirmationDelivery().getConfirmationSmsStatus()).isEqualTo(SENT);
        assertThat(tracking.getPreviouslyConfirmationDelivery().getConfirmationStatus()).isEqualTo(WAITING_CONFIRMATION);
        assertThat(tracking.getPreviouslyConfirmationDelivery().getConfirmationSmsSentTime()).isNotNull();
    }

    @Test
    void shouldSendUsuallyDeliverySms() {
        setUpNewExpectedDeliveryTime(Duration.ofMinutes(120));

        testUserHelper.openShift(user, userShift.getId());
        testUserHelper.finishPickupAtStartOfTheDay(userShift);

        dbQueueTestUtil.executeAllQueueItems(QueueType.PREVIOUSLY_CONFIRMATION_SMS_SEND);
        assertThat(dbQueueTestUtil.isEmpty(QueueType.PREVIOUSLY_CONFIRMATION_SMS_SEND)).isFalse();

        dbQueueTestUtil.assertQueueHasSize(QueueType.SMS_NOTIFICATION_SEND, 1);
        dbQueueTestUtil.executeAllQueueItems(QueueType.SMS_NOTIFICATION_SEND);

        verify(yaSmsClient, Mockito.atLeastOnce()).send(Mockito.anyString(), smsTextCaptor.capture());

        assertThat(smsTextCaptor.getAllValues()).hasSize(1);
        assertThat(smsTextCaptor.getAllValues().get(0)).doesNotStartWith("Вам удобно встретить курьера ");
    }

    @Test
    void shouldDelayBeforeSendConfirmationSms() {
        setUpNewExpectedDeliveryTime(Duration.ofMinutes(120));
        Optional<Duration> duration;

        mockDateTime(Duration.ofMinutes(15));
        duration = previouslyConfirmationService.needDelayBeforeSendingSms(tracking);
        assertThat(duration.isPresent()).isTrue();

        mockDateTime(duration.get());
        duration = previouslyConfirmationService.needDelayBeforeSendingSms(tracking);
        assertThat(duration.isPresent()).isTrue();

        mockDateTime(duration.get());
        duration = previouslyConfirmationService.needDelayBeforeSendingSms(tracking);
        assertThat(duration.isPresent()).isFalse();
    }

    @Test
    void shouldSendSms() {
        setUpNewExpectedDeliveryTime(Duration.ofMinutes(120));
        Optional<Duration> duration;

        mockDateTime(Duration.ofMinutes(30));

        duration = previouslyConfirmationService.needDelayBeforeSendingSms(tracking);
        assertThat(duration.isPresent()).isFalse();

        assertThat(previouslyConfirmationService.needSendPreviouslyConfirmationDeliverySmsAtDuringDay(tracking)).isTrue();
        previouslyConfirmationQueueService.sendConfirmationDeliveryPush(List.of(tracking));
        assertThat(previouslyConfirmationService.needSendPreviouslyConfirmationDeliverySmsAtDuringDay(tracking)).isFalse();

        verify(sendBeruXivaClient, times(1)).send(any());
    }

    @Test
    void shouldConfirmDeliveryAndCloseCallTasks() {
        setUpNewExpectedDeliveryTime(Duration.ofMinutes(30));

        testUserHelper.openShift(user, userShift.getId());
        testUserHelper.finishPickupAtStartOfTheDayWithoutFinishCallTasks(userShift);

        previouslyConfirmationService.confirmDelivery(tracking.getId());
        previouslyConfirmationService.confirmDelivery(tracking.getId());
        var actualTracking = trackingRepository.findByIdOrThrow(tracking.getId());

        assertThat(actualTracking.getPreviouslyConfirmationDelivery().getConfirmationStatus())
                .isEqualTo(CONFIRMED);
        MultiOrderDeliveryTaskDto multiOrderDeliveryTaskDto = userShiftQueryService.getMultiOrderDeliveryTaskDto(
                user, tracking.getOrderDeliveryTask().getCallToRecipientTask().getId().toString());

        assertThat(multiOrderDeliveryTaskDto.getTasks().get(0).getCallStatus()).isEqualTo(SUCCESS_CLIENT_CONFIRM);
    }

    @Test
    void shouldConfirmDeliveryAfterSuccessCall() {
        testUserHelper.openShift(user, userShift.getId());

        assertThat(tracking.getPreviouslyConfirmationDelivery().getConfirmationStatus()).isEqualTo(NO_CONFIRMATION_NEEDED);
        //Здесь звоним клиенту
        testUserHelper.finishPickupAtStartOfTheDay(userShift);

        assertThat(tracking.getPreviouslyConfirmationDelivery().getConfirmationStatus()).isEqualTo(CONFIRMED);
    }

    @Test
    void shouldThrowExceptionWhenClientConfirmDeliveryBeforeSendingSms() {
        setUpNewExpectedDeliveryTime(Duration.ofMinutes(120));

        thenThrownBy(() ->
                previouslyConfirmationService.confirmDelivery(tracking.getId()))
                .isInstanceOf(TplInvalidActionException.class);
    }

    private void setUpNewExpectedDeliveryTime(Duration duration) {
        OrderDeliveryTask orderDeliveryTask = userShift.streamOrderDeliveryTasks().findFirst().get();
        Instant nowPlusNMinutes = Instant.now(clock).plus(duration);
        orderDeliveryTask.setExpectedDeliveryTime(nowPlusNMinutes);
    }

    private void mockDateTime(Duration plus) {
        ClockUtil.initFixed(clock, LocalDateTime.now(clock).plus(plus));
    }

}
