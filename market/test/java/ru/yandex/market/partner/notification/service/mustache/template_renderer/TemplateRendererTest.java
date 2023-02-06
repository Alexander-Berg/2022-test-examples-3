package ru.yandex.market.partner.notification.service.mustache.template_renderer;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.partner.notification.service.mustache.model.RenderedMessage;
import ru.yandex.market.partner.notification.service.mustache.model.Template;
import ru.yandex.market.partner.notification.service.mustache.model.TransportType;
import ru.yandex.market.partner.notification.service.mustache.template_renderer.transport.TransportTemplateRenderer;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TemplateRendererTest {

    private TemplateRenderer renderer;
    private TransportTemplateRenderer emailRendererMock;
    private TransportTemplateRenderer telegramRendererMock;

    @BeforeEach
    void setUp() {
        emailRendererMock = mock(TransportTemplateRenderer.class);
        when(emailRendererMock.getTransport()).thenReturn(TransportType.EMAIL);
        telegramRendererMock = mock(TransportTemplateRenderer.class);
        when(telegramRendererMock.getTransport()).thenReturn(TransportType.TELEGRAM);
        renderer = new TemplateRenderer(List.of(emailRendererMock, telegramRendererMock));
    }

    @Test
    void testRenderTelegram() {
        Template templateDummy = mock(Template.class);
        RenderedMessage renderedDummy = mock(RenderedMessage.class);
        JsonNode paramsDummy = mock(JsonNode.class);

        when(telegramRendererMock.render(templateDummy, paramsDummy))
                .thenReturn(renderedDummy);

        assertThat(renderer.render(TransportType.TELEGRAM, templateDummy, paramsDummy),
                equalTo(renderedDummy));

        verify(telegramRendererMock).render(templateDummy, paramsDummy);
        verify(emailRendererMock, never()).render(any(), any());
    }
}
