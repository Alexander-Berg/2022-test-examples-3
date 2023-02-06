package ru.yandex.market.notification.service.provider.content;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.notification.common.model.TelegramNotificationContent;
import ru.yandex.market.notification.exception.NotificationException;
import ru.yandex.market.notification.model.context.NotificationContentProviderContext;
import ru.yandex.market.notification.model.data.NotificationContent;
import ru.yandex.market.notification.service.provider.template.AbstractMbiTemplateProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TelegramBotContentProviderTest extends BaseContentProviderTest {

    private static final EnvironmentService environmentService = mock(EnvironmentService.class);

    private final TelegramBotContentProvider contentProvider = new TelegramBotContentProvider(
            createComposer(),
            new ContentDataProvider()
    );

    @BeforeEach
    void init() {
        when(environmentService.getValue(anyString(), anyString())).thenReturn("");
    }

    /**
     * Отрисовать тестовый шаблон и проверить, что в контент попали нужные данные при условии существования шаблона.
     */
    @Test
    public void testContentRenderingWhenTemplateExists() throws IOException {
        checkRendering(null, BODY, false, true);
    }

    /**
     * Отрисовать тестовый шаблон HTML и проверить, что в контент попали нужные данные
     * при условии существования шаблона.
     */
    @Test
    public void testContentRenderingWithContentTypeHtmlWhenTemplateExists() throws IOException {
        checkRendering(AbstractMbiTemplateProvider.HTML_TYPE, BODY, false, true);
    }

    /**
     * Сформировать контекст с явно указанными subject и body и проверить, что в контент сформировался из существующего
     * шаблона, а не взят из явно указанных subject и body.
     */
    @Test
    public void testContentWithExplicitBodyWhenTemplateExists() throws IOException {
        checkRendering(null, BODY, true, true);
    }

    /**
     * Сформировать контекст с явно указанными subject и body и проверить, что в контент сформировался из существующего
     * шаблона, а не взят из явно указанных subject и body.
     */
    @Test
    public void testContentWithExplicitBodyWhenTemplateExistsWithContentTypeHtml() throws IOException {
        checkRendering(AbstractMbiTemplateProvider.HTML_TYPE, BODY, true, true);
    }

    /**
     * Сформировать контекст с явно указанными subject и body и проверить, что в контент попали нужные данные
     * при условии отсутствия шаблона.
     */
    @Test
    public void testContentWithExplicitBodyWhenTemplateDoesNotExist() throws IOException {
        checkRendering(null, EXPLICIT_BODY, true, false);
    }

    /**
     * При формировании контента при отсутствии шаблона и явно указанного тела должно выбрасываться иключение.
     */
    @Test
    public void testContentWithoutTemplateAndExplicitBody() throws IOException {
        assertThrows(
                NotificationException.class,
                () -> checkRendering(null, BODY, false, false)
        );
    }

    private void checkRendering(
            String contentType, String body,
            boolean useExplicitBody, boolean templateExists
    ) throws IOException {
        NotificationContentProviderContext context =
                getContentProviderContext(contentType, useExplicitBody, templateExists, null);
        NotificationContent content = contentProvider.provide(context);

        assertThat(content, notNullValue());

        TelegramNotificationContent textContent = content.cast(TelegramNotificationContent.class);

        assertThat(textContent.getText(), equalTo(body));
    }

}
