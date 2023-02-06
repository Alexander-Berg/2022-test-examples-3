package ru.yandex.market.notification.mail.service.provider.attachment;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import ru.yandex.market.notification.mail.model.attachment.impl.ByteArrayEmailAttachment;
import ru.yandex.market.notification.mail.model.context.EmailAttachmentProviderContext;
import ru.yandex.market.notification.mail.service.provider.attachment.impl.ByteArrayEmailAttachmentContentProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Unit-тесты для {@link ByteArrayEmailAttachmentContentProvider}.
 *
 * @author Vladislav Bauer
 */
public class ByteArrayEmailAttachmentContentProviderTest extends BasicEmailAttachmentContentProviderTest {

    @Test
    public void testProvider() throws Exception {
        var provider = new ByteArrayEmailAttachmentContentProvider();
        var context = createContext();

        var attachment = checkAttachmentProvider(provider, context);
        assertThat(attachment, equalTo(BODY));
    }


    private EmailAttachmentProviderContext<ByteArrayEmailAttachment> createContext() {
        var content = createEmailContent();
        var attachment = createEmailAttachment();

        return new EmailAttachmentProviderContext<>(content, attachment);
    }

    private ByteArrayEmailAttachment createEmailAttachment() {
        var attachment = new ByteArrayEmailAttachment();
        attachment.setContent(BODY.getBytes(StandardCharsets.UTF_8));
        return attachment;
    }

}
