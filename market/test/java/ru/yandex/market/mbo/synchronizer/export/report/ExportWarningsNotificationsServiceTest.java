package ru.yandex.market.mbo.synchronizer.export.report;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import ru.yandex.market.mbo.export.ExportWarnings;
import ru.yandex.market.mbo.smtp.Message;
import ru.yandex.market.mbo.smtp.SmtpSender;

import javax.mail.MessagingException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ExportWarningsNotificationsServiceTest {

    @Mock
    private SmtpSender smtpSender;
    @Mock
    private AttachmentCreationService attachmentCreationService1;
    @Mock
    private AttachmentCreationService attachmentCreationService2;

    private ExportWarningsNotificationsService exportWarningsNotificationsService;

    private static final String RECIPIENT = "test-recipient";

    private static final String FIRST_ATTACHMENT_SERVICE_NAME = "first_name";
    private static final String SECOND_ATTACHMENT_SERVICE_NAME = "second_name";

    @Before
    public void setUp() {
        initMocks(this);
        exportWarningsNotificationsService = new ExportWarningsNotificationsService(
            smtpSender, RECIPIENT, Stream.of(attachmentCreationService1, attachmentCreationService2)
            .collect(Collectors.toList())
        );
    }

    @Test
    public void successNotifyTest() throws MessagingException, IOException, ExportWarningsNotificationsException {
        ExportWarnings exportWarnings = new ExportWarnings();

        final Path firstServicePath = File.createTempFile("temp1", ".tmp").toPath();
        final Path secondServicePath = File.createTempFile("temp2", ".tmp").toPath();

        when(attachmentCreationService1.createAttachment(exportWarnings)).thenReturn(firstServicePath);
        when(attachmentCreationService1.getAttachmentName()).thenReturn(FIRST_ATTACHMENT_SERVICE_NAME);

        when(attachmentCreationService2.createAttachment(exportWarnings)).thenReturn(secondServicePath);
        when(attachmentCreationService2.getAttachmentName()).thenReturn(SECOND_ATTACHMENT_SERVICE_NAME);

        final Message expectedMessage = createMessage();
        expectedMessage.addMessageAttachment(FIRST_ATTACHMENT_SERVICE_NAME, firstServicePath);
        expectedMessage.addMessageAttachment(SECOND_ATTACHMENT_SERVICE_NAME, secondServicePath);

        exportWarningsNotificationsService.notify(exportWarnings);

        verify(smtpSender).send(expectedMessage);
        verify(attachmentCreationService1).createAttachment(exportWarnings);
        verify(attachmentCreationService2).createAttachment(exportWarnings);
    }

    @Test(expected = ExportWarningsNotificationsException.class)
    public void failedWhileNotifyTest() throws Exception {
        ExportWarnings exportWarnings = new ExportWarnings();

        final Path firstServicePath = File.createTempFile("temp1", ".tmp").toPath();
        final Path secondServicePath = File.createTempFile("temp2", ".tmp").toPath();

        when(attachmentCreationService1.createAttachment(exportWarnings)).thenReturn(firstServicePath);
        when(attachmentCreationService1.getAttachmentName()).thenReturn(FIRST_ATTACHMENT_SERVICE_NAME);

        when(attachmentCreationService2.createAttachment(exportWarnings)).thenReturn(secondServicePath);
        when(attachmentCreationService2.getAttachmentName()).thenReturn(SECOND_ATTACHMENT_SERVICE_NAME);

        final Message expectedMessage = createMessage();
        expectedMessage.addMessageAttachment(FIRST_ATTACHMENT_SERVICE_NAME, firstServicePath);
        expectedMessage.addMessageAttachment(SECOND_ATTACHMENT_SERVICE_NAME, secondServicePath);

        doThrow(MessagingException.class).when(smtpSender).send(expectedMessage);

        exportWarningsNotificationsService.notify(exportWarnings);
    }

    @Test
    public void failedWhileCreateOneTemplateNotifyTest() throws Exception {
        ExportWarnings exportWarnings = new ExportWarnings();

        final Path firstServicePath = File.createTempFile("temp1", ".tmp").toPath();

        when(attachmentCreationService1.createAttachment(exportWarnings)).thenReturn(firstServicePath);
        when(attachmentCreationService1.getAttachmentName()).thenReturn(FIRST_ATTACHMENT_SERVICE_NAME);

        doThrow(TemplateCreationException.class).when(attachmentCreationService2).createAttachment(exportWarnings);

        final Message expectedMessage = createMessage();
        expectedMessage.addMessageAttachment(FIRST_ATTACHMENT_SERVICE_NAME, firstServicePath);

        exportWarningsNotificationsService.notify(exportWarnings);

        verify(smtpSender).send(expectedMessage);
        verify(attachmentCreationService1).createAttachment(exportWarnings);
        verify(attachmentCreationService2).createAttachment(exportWarnings);
    }

    private Message createMessage() {
        return new Message()
            .setSmtpTitle("Models warnings")
            .setRecipients(Collections.singletonList(RECIPIENT))
            .setMessage("Attached files contain models warnings");
    }
}
