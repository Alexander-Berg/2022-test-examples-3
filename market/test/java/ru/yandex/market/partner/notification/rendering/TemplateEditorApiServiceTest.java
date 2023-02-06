package ru.yandex.market.partner.notification.rendering;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.partner.notification.api.TemplateEditorApiService;
import ru.yandex.market.partner.notification.service.mustache.CachedMustacheTemplateService;
import ru.yandex.market.partner.notification.service.mustache.dao.MustacheTemplateDao;
import ru.yandex.market.partner.notification.service.mustache.dao.MustacheTemplateVersionDao;
import ru.yandex.market.partner.notification.service.mustache.template_renderer.MarkdownToHtmlConverter;
import ru.yandex.market.partner.notification.service.mustache.template_renderer.MustacheRenderer;
import ru.yandex.market.partner.notification.service.mustache.template_renderer.TemplateRenderer;
import ru.yandex.market.partner.notification.service.mustache.template_renderer.transport.EmailTemplateRenderer;
import ru.yandex.market.partner.notification.service.mustache.template_renderer.transport.TelegramTemplateRenderer;
import ru.yandex.market.partner.notification.service.providers.TemplateDataProvider;
import ru.yandex.mj.generated.server.model.RenderRequestDTO;
import ru.yandex.mj.generated.server.model.RenderResponseDTO;
import ru.yandex.mj.generated.server.model.TransportDTO;

public class TemplateEditorApiServiceTest {
    private final TemplateEditorApiService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TemplateEditorApiServiceTest() {
        MarkdownToHtmlConverter markdownToHtmlConverter = new MarkdownToHtmlConverter();
        MustacheRenderer mustacheRenderer = new MustacheRenderer();
        TemplateRenderer templateRenderer = new TemplateRenderer(
                List.of(new TelegramTemplateRenderer(mustacheRenderer),
                        new EmailTemplateRenderer(mustacheRenderer, markdownToHtmlConverter,
                                Mockito.mock(CachedMustacheTemplateService.class))));

        service = new TemplateEditorApiService(
                Mockito.mock(MustacheTemplateVersionDao.class),
                Mockito.mock(MustacheTemplateDao.class),
                templateRenderer,
                Mockito.mock(TemplateDataProvider.class));
    }

    @Test
    void render() {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("partnerName", "AnyName");
        node.put("shopName", "SuperShop");

        RenderRequestDTO request = new RenderRequestDTO()
                .templateBody("Тестовое почтовое-сообщение для магазина {{partnerName}}")
                .transport(TransportDTO.TELEGRAM)
                .parameters(node);

        RenderResponseDTO response = service.renderTemplate(request).getBody();

        Assertions.assertEquals(response.getBody(), "Тестовое почтовое-сообщение для магазина AnyName");
    }
}
