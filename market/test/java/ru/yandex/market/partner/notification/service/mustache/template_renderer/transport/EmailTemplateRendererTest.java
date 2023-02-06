package ru.yandex.market.partner.notification.service.mustache.template_renderer.transport;

import java.util.List;

import com.fasterxml.jackson.databind.node.IntNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.partner.notification.service.mustache.CachedMustacheTemplateService;
import ru.yandex.market.partner.notification.service.mustache.model.ContentType;
import ru.yandex.market.partner.notification.service.mustache.model.RenderedMessage;
import ru.yandex.market.partner.notification.service.mustache.model.Template;
import ru.yandex.market.partner.notification.service.mustache.model.TransportType;
import ru.yandex.market.partner.notification.service.mustache.template_renderer.MarkdownToHtmlConverter;
import ru.yandex.market.partner.notification.service.mustache.template_renderer.MustacheRenderer;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Disabled
class EmailTemplateRendererTest {

    private EmailTemplateRenderer emailRenderer;
    private MustacheRenderer mustacheMock;

    @BeforeEach
    void setUp() {
        mustacheMock = mock(MustacheRenderer.class);
        MarkdownToHtmlConverter markdownToHtmlConverter = new MarkdownToHtmlConverter();
        emailRenderer = new EmailTemplateRenderer(mustacheMock,
                markdownToHtmlConverter,
                Mockito.mock(CachedMustacheTemplateService.class));
    }

    @Test
    void getTransport() {
        assertThat(emailRenderer.getTransport(), equalTo(TransportType.EMAIL));
    }

    @Test
    void render() {
        var params = new IntNode(1);
        String subjectTemplate = "subject template";
        String contentTemplate = "content template";

        when(mustacheMock.render(subjectTemplate, params))
                .thenReturn("email subject");
        when(mustacheMock.render(contentTemplate, params))
                .thenReturn("html email content");

        Template template = Template.builder()
                .setTransports(List.of(TransportType.EMAIL))
                .setSubjectTemplate(subjectTemplate)
                .setContentTemplate(contentTemplate)
                .build();

        assertThat(emailRenderer.render(template, params),
                equalTo(RenderedMessage.builder()
                        .setTransportType(TransportType.EMAIL)
                        .setSubject("email subject")
                        .setContent("html email content\n\n")
                        .setContentType(ContentType.HTML)
                        .build())
        );

        verify(mustacheMock).render(eq(subjectTemplate), eq(params));
        verify(mustacheMock).render(eq(contentTemplate), eq(params));
    }
}
