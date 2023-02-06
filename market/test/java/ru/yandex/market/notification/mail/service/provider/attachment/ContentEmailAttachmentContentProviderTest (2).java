package ru.yandex.market.notification.mail.service.provider.attachment;

import org.junit.jupiter.api.Test;

import ru.yandex.market.notification.mail.model.attachment.impl.ContentEmailAttachment;
import ru.yandex.market.notification.mail.model.context.EmailAttachmentProviderContext;
import ru.yandex.market.notification.mail.service.provider.attachment.impl.ContentEmailAttachmentContentProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Unit-тесты для {@link ContentEmailAttachmentContentProvider}.
 *
 * @author Vladislav Bauer
 */
public class ContentEmailAttachmentContentProviderTest extends BasicEmailAttachmentContentProviderTest {

    @Test
    public void testProvider() throws Exception {
        var provider = new ContentEmailAttachmentContentProvider();
        var context = createContext();

        var attachment = checkAttachmentProvider(provider, context);
        assertThat(attachment, equalTo(BODY));
    }


    private EmailAttachmentProviderContext<ContentEmailAttachment> createContext() {
        var content = createEmailContent();
        var attachment = createEmailAttachment();

        return new EmailAttachmentProviderContext<>(content, attachment);
    }

    private ContentEmailAttachment createEmailAttachment() {
        return new ContentEmailAttachment();
    }

}
