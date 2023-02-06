package ru.yandex.market.notification.sample.template;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import ru.yandex.market.notification.mail.model.EmailContent;
import ru.yandex.market.notification.mail.model.attachment.EmailAttachment;
import ru.yandex.market.notification.mail.model.attachment.impl.ByteArrayEmailAttachment;
import ru.yandex.market.notification.mail.service.provider.attachment.EmailAttachmentProvider;
import ru.yandex.market.notification.model.context.NotificationContentProviderContext;
import ru.yandex.market.notification.model.data.NotificationContent;
import ru.yandex.market.notification.sample.service.provider.DefaultFreemarkerContentProvider;
import ru.yandex.market.notification.sample.service.provider.EmailFreemarkerContentProvider;
import ru.yandex.market.notification.service.content.NotificationTemplateModelComposer;
import ru.yandex.market.notification.service.provider.template.NotificationTemplateProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Тест для проверки корректной работы {@link DefaultFreemarkerContentProvider}.
 *
 * @author avetokhin 16/06/16.
 */
public class EmailFreemarkerContentProviderTest extends AbstractBaseFreemarkerContentProviderTest {

    private static final String SUBJECT = "Subject: key1:value1, key2:25";
    private static final String BODY = "Body: key1:value1, key2:25";

    private final static Map<String, Object> MODEL = ImmutableMap.<String, Object>builder()
            .put("key1", "value1")
            .put("key2", 25)
            .build();

    private static final Collection<EmailAttachment> ATTACHMENTS = ImmutableList.<EmailAttachment>builder()
            .add(ByteArrayEmailAttachment.create("attach1", new byte[]{1, 2}))
            .add(ByteArrayEmailAttachment.create("attach2", new byte[]{3, 4}))
            .build();

    /**
     * {@link DefaultFreemarkerContentProvider} должен отрисовать шаблон и вернуть корректный контент.
     *
     * @throws IOException если произошла ошибка IO
     */
    @Test
    public void provideValidContentTest() throws IOException {
        // Init model composer mock.
        final NotificationTemplateModelComposer modelComposer = mock(NotificationTemplateModelComposer.class);
        when(modelComposer.compose(any())).thenReturn(MODEL);

        // Init attach extractor mock.
        final EmailAttachmentProvider attachmentProvider = mock(EmailAttachmentProvider.class);
        when(attachmentProvider.provide(any())).thenReturn(ATTACHMENTS);

        // Init template provider mock.
        final NotificationTemplateProvider templateProvider = mock(NotificationTemplateProvider.class);
        when(templateProvider.provide(any()))
            .thenReturn(new EmailFreemarkerTemplate(
                getTemplate("email-freemarker-content-provider-test-subject.ftl"),
                getTemplate("email-freemarker-content-provider-test-body.ftl")
            ));


        // Init context mock.
        final NotificationContentProviderContext context = mock(NotificationContentProviderContext.class);
        when(context.getTemplateProvider()).thenReturn(templateProvider);

        final EmailFreemarkerContentProvider provider =
            new EmailFreemarkerContentProvider(modelComposer, attachmentProvider);

        final NotificationContent content = provider.provide(context);
        assertThat(content, instanceOf(EmailContent.class));

        final EmailContent emailContent = (EmailContent) content;
        assertThat(emailContent.getSubject(), equalTo(SUBJECT));
        assertThat(emailContent.getBody(), equalTo(BODY));
        assertThat(emailContent.getFormattedBody(), nullValue());

        assertThat(
            emailContent.getAttachments(),
            containsInAnyOrder(ATTACHMENTS.toArray(new EmailAttachment[0]))
        );
    }

}
