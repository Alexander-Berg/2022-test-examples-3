package ru.yandex.market.partner.notification.service.mustache;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.partner.notification.service.mustache.model.RenderedMessage;
import ru.yandex.market.partner.notification.service.mustache.model.Template;
import ru.yandex.market.partner.notification.service.mustache.model.TransportType;
import ru.yandex.market.partner.notification.service.mustache.template_renderer.TemplateRenderer;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TemplateRendererServiceTest {

    private TemplateRendererService templateRendererService;
    private TemplateProvider templateProvider;
    private TemplateRenderer templateRenderer;

    @BeforeEach
    void setUp() {
        templateRenderer = mock(TemplateRenderer.class);
        templateProvider = mock(TemplateProvider.class);
        templateRendererService = new TemplateRendererService(templateProvider, templateRenderer);
    }

    @Test
    void renderTemplateOneTransport() {
        Template templateMock = mock(Template.class);
        when(templateMock.getTransports()).thenReturn(List.of(TransportType.TELEGRAM));
        when(templateProvider.getTemplate(777L)).thenReturn(templateMock);

        RenderedMessage renderedDummy = mock(RenderedMessage.class);
        JsonNode paramsDummy = mock(JsonNode.class);
        when(templateRenderer.render(TransportType.TELEGRAM, templateMock, paramsDummy)).thenReturn(renderedDummy);

        assertThat(templateRendererService.renderTemplate(777L, paramsDummy),
                equalTo(List.of(renderedDummy)));
    }


    @Test
    void renderTemplateTwoTransports() {
        Template templateMock = mock(Template.class);
        when(templateMock.getTransports()).thenReturn(List.of(TransportType.TELEGRAM, TransportType.EMAIL));
        when(templateProvider.getTemplate(888L)).thenReturn(templateMock);

        JsonNode paramsDummy = mock(JsonNode.class);
        RenderedMessage telegramRenderedDummy = mock(RenderedMessage.class);
        when(templateRenderer.render(TransportType.TELEGRAM, templateMock, paramsDummy))
                .thenReturn(telegramRenderedDummy);
        RenderedMessage emailRenderedDummy = mock(RenderedMessage.class);
        when(templateRenderer.render(TransportType.EMAIL, templateMock, paramsDummy))
                .thenReturn(emailRenderedDummy);

        assertThat(templateRendererService.renderTemplate(888L, paramsDummy),
                containsInAnyOrder(telegramRenderedDummy, emailRenderedDummy));
    }
}
