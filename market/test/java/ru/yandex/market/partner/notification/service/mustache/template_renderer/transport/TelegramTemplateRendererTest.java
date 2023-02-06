package ru.yandex.market.partner.notification.service.mustache.template_renderer.transport;

import java.util.List;

import com.fasterxml.jackson.databind.node.IntNode;
import org.junit.jupiter.api.Test;

import ru.yandex.market.partner.notification.service.mustache.model.ContentType;
import ru.yandex.market.partner.notification.service.mustache.model.RenderedMessage;
import ru.yandex.market.partner.notification.service.mustache.model.Template;
import ru.yandex.market.partner.notification.service.mustache.model.TransportType;
import ru.yandex.market.partner.notification.service.mustache.template_renderer.MustacheRenderer;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TelegramTemplateRendererTest {

    @Test
    void getTransport() {
        var renderer = new TelegramTemplateRenderer(mock(MustacheRenderer.class));

        assertThat(renderer.getTransport(), equalTo(TransportType.TELEGRAM));
    }

    @Test
    void render() {
        var mustacheMock = mock(MustacheRenderer.class);
        var telegramRenderer = new TelegramTemplateRenderer(mustacheMock);

        var params = new IntNode(1);
        String contentTemplate = "content template";

        when(mustacheMock.render(contentTemplate, params))
                .thenReturn("content");

        Template template = Template.builder()
                .setTransports(List.of(TransportType.TELEGRAM))
                .setSubjectTemplate("subject template")
                .setContentTemplate(contentTemplate)
                .build();

        assertThat(telegramRenderer.render(template, params),
                equalTo(RenderedMessage.builder()
                        .setTransportType(TransportType.TELEGRAM)
                        .setContentType(ContentType.MARKDOWN)
                        .setContent("content")
                        .build())
        );

        verify(mustacheMock).render(eq(contentTemplate), eq(params));
    }
}
