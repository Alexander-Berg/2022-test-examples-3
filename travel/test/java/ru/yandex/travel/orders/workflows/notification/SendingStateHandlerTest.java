package ru.yandex.travel.orders.workflows.notification;

import java.util.ArrayList;
import java.util.UUID;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.orders.entities.notifications.Attachment;
import ru.yandex.travel.orders.entities.notifications.EmailChannelInfo;
import ru.yandex.travel.orders.entities.notifications.Notification;
import ru.yandex.travel.orders.entities.notifications.NotificationChannelType;
import ru.yandex.travel.orders.entities.notifications.NotificationFailureReason;
import ru.yandex.travel.orders.entities.notifications.SmsChannelInfo;
import ru.yandex.travel.orders.entities.notifications.TemplatedEmailChannelInfo;
import ru.yandex.travel.orders.management.StarTrekService;
import ru.yandex.travel.orders.services.MailSenderService;
import ru.yandex.travel.orders.services.YaSmsService;
import ru.yandex.travel.orders.services.YaSmsServiceException;
import ru.yandex.travel.orders.services.notifications.NotificationMeters;
import ru.yandex.travel.orders.services.notifications.TemplatedMailSenderService;
import ru.yandex.travel.orders.workflow.notification.proto.EAttachmentState;
import ru.yandex.travel.orders.workflow.notification.proto.ENotificationState;
import ru.yandex.travel.orders.workflow.notification.proto.TAttachmentFetched;
import ru.yandex.travel.orders.workflow.notification.proto.TSendingStart;
import ru.yandex.travel.orders.workflows.notification.handlers.SendingStateHandler;
import ru.yandex.travel.workflow.EWorkflowState;
import ru.yandex.travel.workflow.TWorkflowCrashed;
import ru.yandex.travel.workflow.entities.Workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.travel.orders.workflows.WorkflowTestUtils.testMessagingContext;

public class SendingStateHandlerTest {
    private MailSenderService mailSenderService;
    private TemplatedMailSenderService templatedMailSenderService;
    private YaSmsService smsService;
    private StarTrekService starTrekService;
    private SendingStateHandler handler;
    private NotificationMeters notificationMeters;

    @Before
    public void setUp() {
        mailSenderService = mock(MailSenderService.class);
        templatedMailSenderService = mock(TemplatedMailSenderService.class);
        smsService = mock(YaSmsService.class);
        starTrekService = mock(StarTrekService.class);
        notificationMeters = new NotificationMeters(new SimpleMeterRegistry());
        handler = new SendingStateHandler(mailSenderService, templatedMailSenderService, smsService, starTrekService,
                null, notificationMeters);
    }

    @Test
    public void testSendMail() {
        var notification = createEmailNotification(NotificationChannelType.EMAIL);
        var ctx = testMessagingContext(notification);
        double countSentBefore = notificationMeters.getNotificationsSent()
                .get(NotificationChannelType.EMAIL).count();

        handler.handleEvent(TSendingStart.newBuilder().build(), ctx);
        double countSentAfter = notificationMeters.getNotificationsSent()
                .get(NotificationChannelType.EMAIL).count();

        assertThat(countSentAfter).isEqualTo(countSentBefore + 1);
        verify(mailSenderService).sendEmailSync(any(), any(), any(), any(), any(), any(), any());
        assertThat(notification.getState()).isEqualTo(ENotificationState.NS_SENT);
        assertThat(ctx.getScheduledEvents().size()).isEqualTo(0);
    }

    @Test
    public void testSendTemplatedMail() {
        var notification = createEmailNotification(NotificationChannelType.TEMPLATED_EMAIL);
        var ctx = testMessagingContext(notification);

        handler.handleEvent(TSendingStart.newBuilder().build(), ctx);

        verify(templatedMailSenderService).sendEmailSync(any(), any(), any(), any());
        verify(mailSenderService, never()).sendEmailSync(any(), any(), any(), any(), any(), any(), any());
        assertThat(notification.getState()).isEqualTo(ENotificationState.NS_SENT);
        assertThat(ctx.getScheduledEvents().size()).isEqualTo(0);
    }

    @Test
    public void testIgnoreAttachmentEvents() {
        var notification = createEmailNotification(NotificationChannelType.EMAIL);
        var ctx = testMessagingContext(notification);

        handler.handleEvent(TAttachmentFetched.newBuilder().build(), ctx);
        handler.handleEvent(TWorkflowCrashed.newBuilder().build(), ctx);

        verify(mailSenderService, never()).sendEmailSync(any(), any(), any(), any(), any(), any(), any());
        assertThat(notification.getState()).isEqualTo(ENotificationState.NS_SENDING);
        assertThat(ctx.getScheduledEvents().size()).isEqualTo(0);
    }

    private Notification createEmailNotification(NotificationChannelType channelType) {
        var notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setAttachments(new ArrayList<>());
        notification.setState(ENotificationState.NS_SENDING);
        notification.setChannel(channelType);
        notification.setWorkflow(new Workflow());
        notification.getWorkflow().setState(EWorkflowState.WS_RUNNING);
        if (channelType == NotificationChannelType.EMAIL) {
            var channelInfo = new EmailChannelInfo();
            notification.setChannelInfo(channelInfo);
        } else if (channelType == NotificationChannelType.TEMPLATED_EMAIL) {
            var channelInfo = new TemplatedEmailChannelInfo();
            notification.setChannelInfo(channelInfo);
        }
        return notification;
    }

    private Notification createSmsNotification() {
        var notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setAttachments(new ArrayList<>());
        notification.setState(ENotificationState.NS_SENDING);
        notification.setChannel(NotificationChannelType.SMS);
        var channelInfo = new SmsChannelInfo();
        channelInfo.setPhone("+79000000000");
        channelInfo.setText("privet");
        notification.setChannelInfo(channelInfo);
        return notification;
    }

    @Test
    public void testSendSms() {
        var notification = createSmsNotification();
        var ctx = testMessagingContext(notification);

        handler.handleEvent(TSendingStart.newBuilder().build(), ctx);

        verify(smsService).sendSms(eq("privet"), eq("+79000000000"));
        assertThat(notification.getState()).isEqualTo(ENotificationState.NS_SENT);

        verify(starTrekService, never()).createIssueForTrustReceiptNotFetched(any(), any(), any(), any());
    }

    @Test
    public void testSmsBadPhoneErrorSilentlyFailed() {
        var notification = createSmsNotification();
        var ctx = testMessagingContext(notification);
        when(smsService.sendSms(any(), any())).thenThrow(new YaSmsServiceException("BADPHONE", ""));
        double countBadPhoneErrorsBefore = notificationMeters.getNotificationsFailed()
                .get(NotificationChannelType.SMS).get(NotificationFailureReason.BAD_PHONE).count();

        handler.handleEvent(TSendingStart.newBuilder().build(), ctx);

        double countBadPhoneErrorsAfter = notificationMeters.getNotificationsFailed()
                .get(NotificationChannelType.SMS).get(NotificationFailureReason.BAD_PHONE).count();
        assertThat(notification.getState()).isEqualTo(ENotificationState.NS_FAILED);
        verify(starTrekService, never()).createIssueForTrustReceiptNotFetched(any(), any(), any(), any());
        assertThat(countBadPhoneErrorsAfter).isEqualTo(countBadPhoneErrorsBefore + 1);
    }

    @Test
    public void testSendMailWithoutReceipt() {
        var notification = createEmailNotification(NotificationChannelType.EMAIL);
        notification.getAttachments().add(createAttachment(
                notification, false, EAttachmentState.AS_NEW, EWorkflowState.WS_RUNNING));

        var ctx = testMessagingContext(notification);

        handler.handleEvent(TSendingStart.newBuilder().build(), ctx);

        verify(mailSenderService).sendEmailSync(any(), any(), any(), any(), any(), any(), any());
        assertThat(notification.getState()).isEqualTo(ENotificationState.NS_SENT);
        assertThat(ctx.getScheduledEvents().size()).isEqualTo(0);

        verify(starTrekService).createIssueForTrustReceiptNotFetched(any(), any(), any(), any());
    }

    @SuppressWarnings("SameParameterValue")
    private Attachment createAttachment(Notification notification, boolean required, EAttachmentState state,
                                        EWorkflowState wfState) {
        var attachment = Attachment.createFiscalReceiptAttachment(
                notification, "receipt.pdf", "application/pdf", required, 555);
        attachment.setState(state);
        attachment.setWorkflow(new Workflow());
        attachment.getWorkflow().setState(wfState);
        return attachment;
    }
}
