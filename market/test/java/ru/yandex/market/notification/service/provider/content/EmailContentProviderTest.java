package ru.yandex.market.notification.service.provider.content;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import ru.yandex.market.notification.exception.NotificationException;
import ru.yandex.market.notification.mail.model.EmailContent;
import ru.yandex.market.notification.model.context.NotificationContentProviderContext;
import ru.yandex.market.notification.model.data.NotificationContent;
import ru.yandex.market.notification.service.provider.template.AbstractMbiTemplateProvider;
import ru.yandex.market.partner.notification.service.email.MbiEmailAttachmentProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Тест для {@link ru.yandex.market.notification.service.provider.content.WebContentProvider}.
 *
 * @author avetokhin 22/07/16.
 */
class EmailContentProviderTest extends BaseContentProviderTest {

    private final EmailContentProvider contentProvider = new EmailContentProvider(createComposer(),
            new MbiEmailAttachmentProvider(), new ContentDataProvider());

    /**
     * Отрисовать тестовый шаблон и проверить, что в контент попали нужные данные при условии существования шаблона.
     */
    @Test
    void testContentRenderingWhenTemplateExists() throws IOException {
        checkRendering(null, SUBJECT, BODY, null, false, true);
    }

    /**
     * Отрисовать тестовый шаблон HTML и проверить, что в контент попали нужные данные
     * при условии существования шаблона.
     */
    @Test
    void testContentRenderingWithContentTypeHtmlWhenTemplateExists() throws IOException {
        checkRendering(AbstractMbiTemplateProvider.HTML_TYPE, SUBJECT, BODY, HTML_BODY, false, true);
    }

    /**
     * Сформировать контекст с явно указанными subject и body и проверить, что в контент сформировался из существующего
     * шаблона, а не взят из явно указанных subject и body.
     */
    @Test
    void testContentWithExplicitBodyWhenTemplateExists() throws IOException {
        checkRendering(null, SUBJECT, BODY, null, true, true);
    }

    /**
     * Сформировать контекст с явно указанными subject и body и проверить, что в контент сформировался из существующего
     * шаблона, а не взят из явно указанных subject и body.
     */
    @Test
    void testContentWithExplicitBodyWithContentTypeHtmlWhenTemplateExists() throws IOException {
        checkRendering(AbstractMbiTemplateProvider.HTML_TYPE, SUBJECT, BODY, HTML_BODY, true, true);
    }

    /**
     * Сформировать контекст с явно указанными subject и body и проверить, что в контент попали нужные данные
     * при условии отсутствия шаблона.
     */
    @Test
    void testContentWithExplicitBodyWhenTemplateDoesNotExist() throws IOException {
        checkRendering(null, EXPLICIT_SUBJECT, EXPLICIT_BODY, null, true, false);
    }

    /**
     * При формировании контента при отсутствии шаблона и явно указанного тела должно выбрасываться иключение.
     */
    @Test
    void testContentWithoutTemplateAndExplicitBody() {
        assertThrows(
                NotificationException.class,
                () -> checkRendering(null, SUBJECT, BODY, null, false, false)
        );
    }


    private void checkRendering(
            String contentType, String subject, String body, String htmlBody,
            boolean useExplicitBody, boolean templateExists
    ) throws IOException {
        NotificationContentProviderContext context =
                getContentProviderContext(contentType, useExplicitBody, templateExists, null);
        NotificationContent content = contentProvider.provide(context);

        assertThat(content, notNullValue());

        EmailContent emailContent = content.cast(EmailContent.class);

        assertThat(emailContent.getSubject(), equalTo(subject));
        assertThat(emailContent.getBody(), equalTo(body));
        assertThat(emailContent.getFormattedBody(), equalTo(htmlBody));
    }

}
