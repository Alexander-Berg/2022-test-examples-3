package ru.yandex.market.notification.mail.service.provider.attachment;

import org.junit.Test;

import ru.yandex.market.notification.mail.model.EmailContent;
import ru.yandex.market.notification.mail.model.attachment.impl.ContentEmailAttachment;
import ru.yandex.market.notification.mail.model.context.EmailAttachmentProviderContext;
import ru.yandex.market.notification.mail.service.provider.attachment.impl.ContentEmailAttachmentContentProvider;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Unit-тесты для {@link ContentEmailAttachmentContentProvider}.
 *
 * @author Vladislav Bauer
 */
public class ContentEmailAttachmentContentProviderTest extends BasicEmailAttachmentContentProviderTest {

    @Test
    public void testProvider() throws Exception {
        final ContentEmailAttachmentContentProvider provider = new ContentEmailAttachmentContentProvider();
        final EmailAttachmentProviderContext<ContentEmailAttachment> context = createContext();

        final String attachment = checkAttachmentProvider(provider, context);
        assertThat(attachment, equalTo(BODY));
    }


    private EmailAttachmentProviderContext<ContentEmailAttachment> createContext() {
        final EmailContent content = createEmailContent();
        final ContentEmailAttachment attachment = createEmailAttachment();

        return new EmailAttachmentProviderContext<>(content, attachment);
    }

    private ContentEmailAttachment createEmailAttachment() {
        return new ContentEmailAttachment();
    }

}
