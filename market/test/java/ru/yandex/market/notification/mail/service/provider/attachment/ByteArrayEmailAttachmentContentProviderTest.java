package ru.yandex.market.notification.mail.service.provider.attachment;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

import ru.yandex.market.notification.mail.model.EmailContent;
import ru.yandex.market.notification.mail.model.attachment.impl.ByteArrayEmailAttachment;
import ru.yandex.market.notification.mail.model.context.EmailAttachmentProviderContext;
import ru.yandex.market.notification.mail.service.provider.attachment.impl.ByteArrayEmailAttachmentContentProvider;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Unit-тесты для {@link ByteArrayEmailAttachmentContentProvider}.
 *
 * @author Vladislav Bauer
 */
public class ByteArrayEmailAttachmentContentProviderTest extends BasicEmailAttachmentContentProviderTest {

    @Test
    public void testProvider() throws Exception {
        final ByteArrayEmailAttachmentContentProvider provider = new ByteArrayEmailAttachmentContentProvider();
        final EmailAttachmentProviderContext<ByteArrayEmailAttachment> context = createContext();

        final String attachment = checkAttachmentProvider(provider, context);
        assertThat(attachment, equalTo(BODY));
    }


    private EmailAttachmentProviderContext<ByteArrayEmailAttachment> createContext() {
        final EmailContent content = createEmailContent();
        final ByteArrayEmailAttachment attachment = createEmailAttachment();

        return new EmailAttachmentProviderContext<>(content, attachment);
    }

    private ByteArrayEmailAttachment createEmailAttachment() {
        final ByteArrayEmailAttachment attachment = new ByteArrayEmailAttachment();
        attachment.setContent(BODY.getBytes(StandardCharsets.UTF_8));
        return attachment;
    }

}
