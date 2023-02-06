package ru.yandex.market.notification.mail.model;

import org.junit.Test;

import ru.yandex.market.notification.mail.model.attachment.EmailAttachment;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

/**
 * Unit-тесты для {@link EmailContent}.
 *
 * @author Vladislav Bauer
 */
public class EmailContentTest {

    @Test(expected = UnsupportedOperationException.class)
    public void testImmutableAttachments() {
        final EmailContent content = EmailContent.create("", "", "", null);
        final EmailAttachment attachment = mock(EmailAttachment.class);

        fail(String.valueOf(content.getAttachments().add(attachment)));
    }

}
