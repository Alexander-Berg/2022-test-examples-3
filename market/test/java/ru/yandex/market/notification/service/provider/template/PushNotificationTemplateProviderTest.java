package ru.yandex.market.notification.service.provider.template;

import org.junit.jupiter.api.Test;

import ru.yandex.market.notification.exception.template.TemplateIOException;
import ru.yandex.market.notification.simple.model.type.NotificationTransport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PushNotificationTemplateProviderTest extends BaseTemplateProviderTest {
    private static final NotificationTransport TRANSPORT = NotificationTransport.MOBILE_PUSH;

    /**
     * Все шаблоны на месте, должен сгенерироваться валидный результат обычного текстового шаблона.
     */
    @Test
    public void provideTextContent() {
        var templateService = prepareTemplateServiceMock(TEMPLATE, GENERAL_HTML_TEMPLATE, COMMON_TEMPLATE, TRANSPORT);
        var provider = new PushNotificationTemplateProvider(templateService);
        var mbiTemplate = checkAndCastTemplate(provider.provide(getContext(TRANSPORT)));

        assertThat(mbiTemplate.getId(), equalTo(TEMPLATE.getId()));
        assertThat(mbiTemplate.getName(), equalTo(TEMPLATE.getName()));
        assertThat(mbiTemplate.getContentType(), equalTo(TEMPLATE.getContentType()));
        assertThat(mbiTemplate.getXsl(), equalTo(TEMPLATE_TEXT_XSL_WITH_COMMON));
    }

    /**
     * Все шаблоны на месте, должен сгенерироваться валидный результат HTML шаблона.
     */
    @Test
    public void provideHtmlContent() {
        // Подготовить мок для DAO шаблонов.
        var templateDao = prepareTemplateServiceMock(HTML_TEMPLATE, GENERAL_HTML_TEMPLATE, COMMON_TEMPLATE, TRANSPORT);
        var provider = new PushNotificationTemplateProvider(templateDao);
        var mbiTemplate = checkAndCastTemplate(provider.provide(getContext(TRANSPORT)));

        assertThat(mbiTemplate.getId(), equalTo(HTML_TEMPLATE.getId()));
        assertThat(mbiTemplate.getName(), equalTo(HTML_TEMPLATE.getName()));
        assertThat(mbiTemplate.getContentType(), equalTo(HTML_TEMPLATE.getContentType()));
        assertThat(mbiTemplate.getXsl(), equalTo(TEMPLATE_XSL_WITH_HTML_EMPTY_WRAPPER));
    }

    /**
     * Шаблон не найден, должно быть выброшено TemplateIOException исключение.
     */
    @Test
    public void provideWhenTemplateNotFound() {
        var templateDao = prepareTemplateServiceMock(null, null, COMMON_TEMPLATE, TRANSPORT);
        var provider = new PushNotificationTemplateProvider(templateDao);

        assertThrows(TemplateIOException.class, () -> provider.provide(getContext(TRANSPORT)));
    }

    /**
     * Шаблон является самодостаточным, к нему ничего не добавляется, живет сам по себе.
     */
    @Test
    public void provideSelfSufficientTemplate() {
        var templateService =
                prepareTemplateServiceMock(SELF_SUFFICIENT_TEMPLATE, GENERAL_HTML_TEMPLATE, COMMON_TEMPLATE, TRANSPORT);
        var provider = new PushNotificationTemplateProvider(templateService);
        var mbiTemplate = checkAndCastTemplate(provider.provide(getContext(TRANSPORT)));

        assertThat(mbiTemplate.getId(), equalTo(SELF_SUFFICIENT_TEMPLATE.getId()));
        assertThat(mbiTemplate.getName(), equalTo(SELF_SUFFICIENT_TEMPLATE.getName()));
        assertThat(mbiTemplate.getContentType(), equalTo(SELF_SUFFICIENT_TEMPLATE.getContentType()));
        assertThat(mbiTemplate.getXsl(), equalTo(SELF_SUFFICIENT_XSL));
    }
}
