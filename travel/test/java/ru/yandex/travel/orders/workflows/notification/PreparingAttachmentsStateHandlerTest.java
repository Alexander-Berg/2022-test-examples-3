package ru.yandex.travel.orders.workflows.notification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.orders.entities.WellKnownWorkflowEntityType;
import ru.yandex.travel.orders.entities.notifications.Attachment;
import ru.yandex.travel.orders.entities.notifications.Notification;
import ru.yandex.travel.orders.repository.AttachmentRepository;
import ru.yandex.travel.orders.workflow.notification.proto.EAttachmentState;
import ru.yandex.travel.orders.workflow.notification.proto.ENotificationState;
import ru.yandex.travel.orders.workflow.notification.proto.TAttachmentFetched;
import ru.yandex.travel.orders.workflow.notification.proto.TPreparingExpired;
import ru.yandex.travel.orders.workflow.notification.proto.TSendingStart;
import ru.yandex.travel.orders.workflows.notification.handlers.PreparingAttachmentsStateHandler;
import ru.yandex.travel.workflow.EWorkflowState;
import ru.yandex.travel.workflow.TWorkflowCrashed;
import ru.yandex.travel.workflow.entities.Workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.travel.orders.workflows.WorkflowTestUtils.testMessagingContext;

public class PreparingAttachmentsStateHandlerTest {
    private PreparingAttachmentsStateHandler handler;
    private AttachmentRepository attachmentRepository;

    @Before
    public void setUp() {
        attachmentRepository = mock(AttachmentRepository.class);
        handler = new PreparingAttachmentsStateHandler(attachmentRepository);
    }

    @Test
    public void testWaitingAttachments() {
        Notification notification = this.createNotification(Instant.now().plusSeconds(600));
        Attachment attachment1 = this.createAttachment(notification, true, EAttachmentState.AS_FETCHED, EWorkflowState.WS_RUNNING);
        Attachment attachment2 = this.createAttachment(notification, false, EAttachmentState.AS_NEW, EWorkflowState.WS_RUNNING);
        var ctx = testMessagingContext(notification);

        handler.handleEvent(TAttachmentFetched.newBuilder().build(), ctx);

        assertThat(notification.getState()).isEqualTo(ENotificationState.NS_PREPARING_ATTACHMENTS);
        assertThat(ctx.getScheduledEvents().size()).isEqualTo(0);
    }

    @Test
    public void testWaitingRequiredAttachments() {
        Notification notification = this.createNotification(Instant.now().minusSeconds(1));
        Attachment attachment1 = this.createAttachment(notification, false, EAttachmentState.AS_FETCHED, EWorkflowState.WS_RUNNING);
        Attachment attachment2 = this.createAttachment(notification, true, EAttachmentState.AS_NEW, EWorkflowState.WS_RUNNING);
        var ctx = testMessagingContext(notification);

        handler.handleEvent(TPreparingExpired.newBuilder().build(), ctx);

        assertThat(notification.getState()).isEqualTo(ENotificationState.NS_PREPARING_REQUIRED_ATTACHMENTS);
        assertThat(ctx.getScheduledEvents().size()).isEqualTo(0);
    }

    @Test
    public void testSkipMinorAttachments() {
        Notification notification = this.createNotification(Instant.now().minusSeconds(1));
        Attachment attachment1 = this.createAttachment(notification, true, EAttachmentState.AS_FETCHED, EWorkflowState.WS_RUNNING);
        Attachment attachment2 = this.createAttachment(notification, false, EAttachmentState.AS_NEW, EWorkflowState.WS_RUNNING);
        Attachment attachment3 = this.createAttachment(notification, false, EAttachmentState.AS_NEW, EWorkflowState.WS_CRASHED);
        var ctx = testMessagingContext(notification);

        handler.handleEvent(TPreparingExpired.newBuilder().build(), ctx);

        assertThat(notification.getState()).isEqualTo(ENotificationState.NS_SENDING);
        assertThat(ctx.getScheduledEvents().get(0).getMessage()).isInstanceOf(TSendingStart.class);
    }

    @Test
    public void testCrashRequiredAttachments() {
        Notification notification = this.createNotification(Instant.now().plusSeconds(600));
        Attachment attachment1 = this.createAttachment(notification, true, EAttachmentState.AS_FETCHED, EWorkflowState.WS_RUNNING);
        Attachment attachment2 = this.createAttachment(notification, false, EAttachmentState.AS_NEW, EWorkflowState.WS_RUNNING);
        Attachment attachment3 = this.createAttachment(notification, true, EAttachmentState.AS_NEW, EWorkflowState.WS_CRASHED);
        when(attachmentRepository.getOne(attachment3.getId())).thenReturn(attachment3);
        var ctx = testMessagingContext(notification);

        assertThatExceptionOfType(RuntimeException.class).isThrownBy(
                () -> handler.handleEvent(TWorkflowCrashed.newBuilder()
                        .setEntityId(attachment3.getId().toString())
                        .setEntityType(WellKnownWorkflowEntityType.ATTACHMENT.getDiscriminatorValue())
                        .build(), ctx)
        );

        assertThat(notification.getState()).isEqualTo(ENotificationState.NS_PREPARING_ATTACHMENTS);
        assertThat(ctx.getScheduledEvents().size()).isEqualTo(0);
    }

    @Test
    public void testSkipCrashedMinorAttachments() {
        Notification notification = this.createNotification(Instant.now().plusSeconds(600));
        Attachment attachment1 = this.createAttachment(notification, true, EAttachmentState.AS_FETCHED, EWorkflowState.WS_RUNNING);
        Attachment attachment2 = this.createAttachment(notification, false, EAttachmentState.AS_NEW, EWorkflowState.WS_CRASHED);
        when(attachmentRepository.getOne(attachment2.getId())).thenReturn(attachment2);
        var ctx = testMessagingContext(notification);

        handler.handleEvent(TWorkflowCrashed.newBuilder()
                .setEntityId(attachment2.getId().toString())
                .setEntityType(WellKnownWorkflowEntityType.ATTACHMENT.getDiscriminatorValue())
                .build(), ctx);

        assertThat(notification.getState()).isEqualTo(ENotificationState.NS_SENDING);
        assertThat(ctx.getScheduledEvents().get(0).getMessage()).isInstanceOf(TSendingStart.class);
    }

    private Notification createNotification(Instant preparingAttachmentsTill) {
        var notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setAttachments(new ArrayList<>());
        notification.setPreparingAttachmentsTill(preparingAttachmentsTill);
        notification.setState(ENotificationState.NS_PREPARING_ATTACHMENTS);
        notification.setWorkflow(new Workflow());
        notification.getWorkflow().setState(EWorkflowState.WS_RUNNING);
        return notification;
    }

    private Attachment createAttachment(Notification notification, boolean required, EAttachmentState state,
                                        EWorkflowState wfState) {
        var attachment = Attachment.createUrlAttachment(notification, "fn", "application/pdf", required, "fake.url");
        attachment.setState(state);
        attachment.setWorkflow(new Workflow());
        attachment.getWorkflow().setState(wfState);
        return attachment;
    }
}
