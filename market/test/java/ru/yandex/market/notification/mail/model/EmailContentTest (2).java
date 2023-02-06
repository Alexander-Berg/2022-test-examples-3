package ru.yandex.market.notification.mail.model;

import org.junit.jupiter.api.Test;

import ru.yandex.market.notification.mail.model.attachment.EmailAttachment;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

/**
 * Unit-тесты для {@link EmailContent}.
 *
 * @author Vladislav Bauer
 */
public class EmailContentTest {

    @Test
    public void testImmutableAttachments() {
        assertThrows(UnsupportedOperationException.class, () -> {
            final EmailContent content = EmailContent.create("", "", "", null);
            final EmailAttachment attachment = mock(EmailAttachment.class);

            fail(String.valueOf(content.getAttachments().add(attachment)));
        });
    }

}
