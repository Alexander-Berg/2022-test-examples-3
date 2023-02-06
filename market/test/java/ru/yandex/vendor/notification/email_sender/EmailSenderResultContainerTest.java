package ru.yandex.vendor.notification.email_sender;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class EmailSenderResultContainerTest {

    @Test
    public void ok_factory_creates_ok_result() throws Exception {
        EmailSendResult sendResult = new EmailSendResult();
        sendResult.setTaskId("qwe");
        EmailSenderResultContainer<EmailSendResult> result = EmailSenderResultContainer.ok(sendResult);
        assertEquals(EmailSenderResultContainer.Status.OK, result.getStatus());
        assertEquals("qwe", result.getValue().getTaskId());
        assertNull(result.getError());
    }

    @Test
    @SuppressWarnings("ThrowableNotThrown")
    public void error_factory_creates_error_result() throws Exception {
        Exception error = new RuntimeException();
        EmailSenderResultContainer<EmailSendResult> result = EmailSenderResultContainer.error(error);
        assertEquals(EmailSenderResultContainer.Status.ERROR, result.getStatus());
        assertEquals(error, result.getError());
        assertNull(result.getValue());
    }
}
