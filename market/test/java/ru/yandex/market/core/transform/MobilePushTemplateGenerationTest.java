package ru.yandex.market.core.transform;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.core.framework.composer.Composer;
import ru.yandex.market.core.transform.common.BasicTemplateGenerationTest;
import ru.yandex.market.core.transform.common.TemplateId;
import ru.yandex.market.mbi.util.MbiAsserts;
import ru.yandex.market.notification.model.MobilePushNotificationContent;
import ru.yandex.market.notification.model.context.NotificationContext;
import ru.yandex.market.notification.service.NotificationTypeTransportTemplateService;
import ru.yandex.market.notification.service.provider.content.ContentDataProvider;
import ru.yandex.market.notification.service.provider.content.PushNotificationContentProvider;
import ru.yandex.market.notification.service.provider.template.NotificationTemplateProvider;
import ru.yandex.market.notification.service.provider.template.PushNotificationTemplateProvider;
import ru.yandex.market.notification.simple.model.type.NotificationTransport;

public class MobilePushTemplateGenerationTest extends BasicTemplateGenerationTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    MobilePushTemplateGenerationTest() {
        super("mobile_push");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("ru.yandex.market.core.transform.common.BasicTemplateGenerationTest#data")
    void testTemplateGenerationMobilePush(TemplateId templateId) {
        Assumptions.assumingThat(checkNecessity(templateId),
                () -> testMobilePushTemplates(templateId));
    }

    private void testMobilePushTemplates(TemplateId templateId) throws Exception {

        final NotificationTypeTransportTemplateService transportTemplateService =
                createMockedTypeTransportTemplateService(templateId);

        final NotificationTemplateProvider templateProvider = new PushNotificationTemplateProvider(transportTemplateService);
        final PushNotificationContentProvider contentProvider = createPushNotificationContentProvider();

        final Map<String, NotificationContext> originContexts =
                createNotificationContexts(NotificationTransport.MOBILE_PUSH, templateId);

        originContexts.forEach((fileName, ctx) -> {
            final var context = createContentProviderContext(templateProvider, ctx);
            final var content = contentProvider.provide(context).cast(MobilePushNotificationContent.class);
            var expectedFileName = "mobilePush/" + FilenameUtils.removeExtension(fileName) + ".json";
            checkContent(expectedFileName, content, templateId);
        });
    }

    private boolean checkNecessity(final TemplateId templateId) {
        var id = templateId.getId();
        var templateTransports = transports.get().getOrDefault(id, Set.of());
        return templateTransports.contains(NotificationTransport.MOBILE_PUSH);
    }

    private void checkContent(String expectedTemplateFile, MobilePushNotificationContent content, TemplateId templateId) {
        String actualText = null;
        try {
            actualText = MAPPER.writeValueAsString(content);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize notification content");
        }

        var expected = getExpectedText(expectedTemplateFile);
        Assertions.assertTrue(
                expected.isPresent(),
                () -> String.format("Sample mobile push template [classpath:/templates/%s] not found",
                        expectedTemplateFile)
        );

        MbiAsserts.assertJsonEquals(expected.get(), actualText);
    }

    private PushNotificationContentProvider createPushNotificationContentProvider() {
        final Composer composer = createComposer();
        final ContentDataProvider dataProvider = createContentDataProvider();

        return new PushNotificationContentProvider(
                composer,
                dataProvider
        );
    }
}
