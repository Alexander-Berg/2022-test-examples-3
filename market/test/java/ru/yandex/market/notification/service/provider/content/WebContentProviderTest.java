package ru.yandex.market.notification.service.provider.content;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import ru.yandex.market.notification.exception.NotificationException;
import ru.yandex.market.notification.model.WebContent;
import ru.yandex.market.notification.model.context.NotificationContentProviderContext;
import ru.yandex.market.notification.model.data.NotificationContent;
import ru.yandex.market.notification.service.provider.template.EmailTemplateProvider;
import ru.yandex.market.notification.simple.model.type.NotificationPriority;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Тест для {@link WebContentProvider}.
 *
 * @author avetokhin 22/07/16.
 */
public class WebContentProviderTest extends BaseContentProviderTest {

    private final WebContentProvider contentProvider = new WebContentProvider(createComposer(), new ContentDataProvider());

    /**
     * Отрисовать тестовый шаблон и проверить, что в контент попали нужные данные при условии существования шаблона.
     */
    @Test
    public void testContentRenderingWhenTemplateExists() throws IOException {
        checkRendering(null, SUBJECT, BODY, false, true);
    }

    /**
     * Отрисовать тестовый шаблон HTML и проверить, что в контент попали нужные данные
     * при условии существования шаблона.
     */
    @Test
    public void testContentRenderingWithContentTypeHtmlWhenTemplateExists() throws IOException {
        checkRendering(EmailTemplateProvider.HTML_TYPE, SUBJECT, BODY, false, true);
    }

    /**
     * Сформировать контекст с явно указанными subject и body и проверить, что в контент сформировался из существующего
     * шаблона, а не взят из явно указанных subject и body.
     */
    @Test
    public void testContentWithExplicitBodyWhenTemplateExists() throws IOException {
        checkRendering(null, SUBJECT, BODY, true, true);
    }

    /**
     * Сформировать контекст с явно указанными subject и body и проверить, что в контент сформировался из существующего
     * шаблона, а не взят из явно указанных subject и body.
     */
    @Test
    public void testContentWithExplicitBodyWhenTemplateExistsWithContentTypeHtml() throws IOException {
        checkRendering(EmailTemplateProvider.HTML_TYPE, SUBJECT, BODY, true, true);
    }


    /**
     * Сформировать контекст с явно указанными subject и body и проверить, что в контент попали нужные данные
     * при условии отсутствия шаблона.
     */
    @Test
    public void testContentWithExplicitBodyWhenTemplateDoesNotExist() throws IOException {
        checkRendering(null, EXPLICIT_SUBJECT, EXPLICIT_BODY, true, false);
    }

    /**
     * При формировании контента при отсутствии шаблона и явно указанного тела должно выбрасываться иключение.
     */
    @Test
    public void testContentWithoutTemplateAndExplicitBody() throws IOException {
        assertThrows(
                NotificationException.class,
                () -> checkRendering(null, SUBJECT, BODY, false, false)
        );
    }

    private void checkRendering(
            String contentType, String subject, String body,
            boolean useExplicitBody, boolean templateExists
    ) throws IOException {
        NotificationContentProviderContext context =
                getContentProviderContext(contentType, useExplicitBody, templateExists, null);
        NotificationContent content = contentProvider.provide(context);

        assertThat(content, notNullValue());

        WebContent webContent = content.cast(WebContent.class);

        assertThat(webContent.getSubject(), equalTo(subject));
        assertThat(webContent.getBody(), equalTo(body));
        assertThat(webContent.getNotificationTypeId(), equalTo(NOTIFICATION_TYPE));
        assertThat(webContent.getPriority(), equalTo(NotificationPriority.NORMAL.getId()));
        assertThat(webContent.getShopId(), equalTo(SHOP_ID));
        assertThat(webContent.getUserId(), equalTo(USER_ID));
    }

}
