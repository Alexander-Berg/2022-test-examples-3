package ru.yandex.market.notification.mail.service.provider.attachment;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;

import org.apache.commons.io.IOUtils;
import org.junit.Ignore;

import ru.yandex.market.notification.mail.model.EmailContent;
import ru.yandex.market.notification.mail.model.attachment.EmailAttachment;
import ru.yandex.market.notification.mail.model.context.EmailAttachmentProviderContext;

import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Базовый класс для тестов {@link EmailAttachmentContentProvider}.
 *
 * @author Vladislav Bauer
 */
@Ignore
abstract class BasicEmailAttachmentContentProviderTest {

    static final String BODY = "When nothing is going right, go left.";


    @SuppressWarnings("unchecked")
    <T extends EmailAttachment> String checkAttachmentProvider(
        final EmailAttachmentContentProvider provider, final EmailAttachmentProviderContext<T> context
    ) throws IOException {
        final InputStream stream = (InputStream) provider.provide(context);
        assertThat(stream, notNullValue());

        final String content = IOUtils.toString(stream, StandardCharsets.UTF_8);
        assertThat(content, not(isEmptyOrNullString()));

        return content;
    }

    @Nonnull
    EmailContent createEmailContent() {
        final EmailContent content = new EmailContent();
        content.setBody(BODY);
        return content;
    }

}
