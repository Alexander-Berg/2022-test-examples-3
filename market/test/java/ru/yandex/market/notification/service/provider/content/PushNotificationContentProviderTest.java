package ru.yandex.market.notification.service.provider.content;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.notification.exception.NotificationException;
import ru.yandex.market.notification.model.MobilePushNotificationContent;
import ru.yandex.market.notification.model.context.NotificationContentProviderContext;
import ru.yandex.market.notification.model.data.NotificationContent;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.notification.service.provider.template.AbstractMbiTemplateProvider.HTML_TYPE;

public class PushNotificationContentProviderTest extends BaseContentProviderTest {

    private final PushNotificationContentProvider contentProvider = new PushNotificationContentProvider(createComposer(), new ContentDataProvider());

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
        checkRendering(HTML_TYPE, BODY, false, true);
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
        checkRendering(HTML_TYPE, BODY, true, true);
    }

    /**
     * Для мобильных пушей должен быть шаблон определяющий правила формирования данных
     */
    @Test
    public void testContentWithExplicitBodyWhenTemplateDoesNotExist() throws IOException {
        assertThrows(
                NotificationException.class,
                () -> checkRendering(null, EXPLICIT_BODY, true, false)
        );
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
        String extraData = makeExtraData("/extraData/mobilePushBasic.json");
        NotificationContentProviderContext context =
                getContentProviderContext(contentType, useExplicitBody, templateExists, getDefaultTemplate(contentType, extraData));
        NotificationContent content = contentProvider.provide(context);

        assertThat(content, notNullValue());

        MobilePushNotificationContent textContent = content.cast(MobilePushNotificationContent.class);

        JsonTestUtil.assertEquals(textContent.getPayload(), expectedPayload());
        JsonTestUtil.assertEquals(textContent.getRepack(), expectedRepack());
        assertEquals("service", textContent.getService());
        List<String> tags = textContent.getTags();
        assertEquals(tags.size(), 1);
        assertEquals("tag", tags.get(0));
    }

    private String makeExtraData(String filename) throws IOException {
        InputStream is = getClass().getResourceAsStream(filename);
        return IOUtils.toString(is, StandardCharsets.UTF_8.name());
    }

    private static String expectedPayload() {
        return "{\n" +
                "        \"test1\": \"test-param-payload\"\n" +
                "}";
    }

    private static String expectedRepack() {
        return "{\n" +
                "        \"apns\": {\n" +
                "            \"aps\": {\n" +
                "                \"alert\": {\n" +
                "                    \"title\": \"Тестовый заголовок для заказа subject\",\n" +
                "                    \"body\": \"Изменение параметра body\"\n" +
                "                }\n" +
                "            },\n" +
                "            \"test-param\": \"test-param-repack\"\n" +
                "        },\n" +
                "        \"fcm\": {\n" +
                "            \"message\": {\n" +
                "                \"title\": \"Тестовый заголовок для заказа subject\",\n" +
                "                \"body\": \"Изменение параметра body\"\n" +
                "            }\n" +
                "        }\n" +
                "    }";
    }

}
