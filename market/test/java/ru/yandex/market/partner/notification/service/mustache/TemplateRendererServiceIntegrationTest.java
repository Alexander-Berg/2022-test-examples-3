package ru.yandex.market.partner.notification.service.mustache;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.partner.notification.service.mustache.config.TemplateRendererServiceConfig;
import ru.yandex.market.partner.notification.service.mustache.model.ContentType;
import ru.yandex.market.partner.notification.service.mustache.model.RenderedMessage;
import ru.yandex.market.partner.notification.service.mustache.model.TransportType;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

/**
 * Шаблоны для уведомлений в {@link TemplateRendererServiceConfig#templateProvider}
 */
@JsonTest
@ExtendWith(SpringExtension.class)
@Disabled
@ContextConfiguration(classes = {TemplateRendererServiceConfig.class})
class TemplateRendererServiceIntegrationTest {

    @Autowired
    TemplateRendererService templateRendererService;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void renderTemplateWithOneTransports() throws IOException {
        var params = objectMapper.readValue("{\"partnerName\": \"ООО Ромашка\"}", JsonNode.class);

        var result = templateRendererService.renderTemplate(1234568L, params);

        assertThat(result.size(), equalTo(1));
        assertThat(result.get(0), equalTo(RenderedMessage.builder()
                .setTransportType(TransportType.TELEGRAM)
                .setContentType(ContentType.MARKDOWN)
                .setContent("Тестовое телеграмм-сообщение для магазина ООО Ромашка")
                .build())
        );
    }

    @Test
    void renderTemplateWithTwoTransports() throws IOException {
        var params = objectMapper.readValue("{\"partnerName\": \"ООО Ромашка\"}", JsonNode.class);

        var result = templateRendererService.renderTemplate(1234567L, params);

        assertThat(result.size(), equalTo(2));
        assertThat(result, containsInAnyOrder(
                RenderedMessage.builder()
                        .setTransportType(TransportType.TELEGRAM)
                        .setContentType(ContentType.MARKDOWN)
                        .setContent("Магазин ООО Ромашка успешно зарегистрирован на Я.Маркете!")
                        .build(),
                RenderedMessage.builder()
                        .setTransportType(TransportType.EMAIL)
                        .setSubject("Привет магазину ООО Ромашка!")
                        .setContentType(ContentType.HTML)
                        .setContent("Магазин ООО Ромашка успешно зарегистрирован на Я.Маркете!")
                        .build()
        ));
    }
}
