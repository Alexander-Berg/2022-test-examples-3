package ru.yandex.market.core.transform;

import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.core.framework.composer.Composer;
import ru.yandex.market.core.transform.common.BasicTemplateGenerationTest;
import ru.yandex.market.core.transform.common.TemplateId;
import ru.yandex.market.notification.model.WebContent;
import ru.yandex.market.notification.model.context.NotificationContext;
import ru.yandex.market.notification.service.NotificationTypeTransportTemplateService;
import ru.yandex.market.notification.service.provider.content.ContentDataProvider;
import ru.yandex.market.notification.service.provider.content.WebContentProvider;
import ru.yandex.market.notification.service.provider.template.NotificationTemplateProvider;
import ru.yandex.market.notification.service.provider.template.WebTemplateProvider;
import ru.yandex.market.notification.simple.model.type.NotificationTransport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Тесты по генерации шаблонов для транспорта {@link NotificationTransport#MBI_WEB_UI}.
 *
 * @author Vladislav Bauer
 */
class WebTemplateGenerationTest extends BasicTemplateGenerationTest {

    /**
     * RegExpr для проверки ссылок как в Markdown.
     */
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[([^\\]]+)\\]\\(([^)]+)\\)", Pattern.MULTILINE);
    private static final long LAST_TEMPLATE_ID_WITHOUT_REQUIRED_WEB = 1613647796;
    /**
     * Уведомления, которым не нужен транспорт {@link NotificationTransport#MBI_WEB_UI}.
     * Например, внутренние уведомления в коммерческий департамент
     */
    private static final Set<Long> NO_WEB_TEMPLATES = Set.of(1639240019L);

    WebTemplateGenerationTest() {
        super("ui");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("ru.yandex.market.core.transform.common.BasicTemplateGenerationTest#data")
    void testTemplateGenerationWeb(TemplateId templateId) throws Exception {
        var templateTransports = transports.get().getOrDefault(templateId.getId(), Set.of());
        if (!templateTransports.contains(NotificationTransport.MBI_WEB_UI)) {
            if (templateId.getId() > LAST_TEMPLATE_ID_WITHOUT_REQUIRED_WEB
                    && templateTransports.contains(NotificationTransport.EMAIL)
                    && !NO_WEB_TEMPLATES.contains(templateId.getId())
            ) {
                fail(String.format(
                        "Новые шаблоны должны поддерживать web в дополнение к email, шаблон [%s] не поддерживает",
                        templateId
                ));
            }
            return;
        }

        final NotificationTypeTransportTemplateService transportTemplateService =
                createMockedTypeTransportTemplateService(templateId);

        final NotificationTemplateProvider templateProvider = new WebTemplateProvider(transportTemplateService);

        final Map<String, NotificationContext> originContexts =
                createNotificationContexts(NotificationTransport.MBI_WEB_UI, templateId);

        originContexts.forEach((id, ctx) -> {
            final var context = createContentProviderContext(templateProvider, ctx);
            final WebContentProvider contentProvider = createWebContentProvider();
            final WebContent content = contentProvider.provide(context).cast(WebContent.class);
            checkContent(content, templateId);
            //printContent(content);
        });
    }

    /**
     * XXX(avetokhin) вывод в консоль контента, полезно для тестирования.
     *
     * @param content контент
     */
    @SuppressWarnings("unused")
    private void printContent(final WebContent content) {
        System.out.println();
        System.out.println(content.getSubject());
        System.out.println();
        System.out.println(content.getBody());
        System.out.println();
    }

    private void checkContent(final WebContent content, TemplateId templateId) {
        String subject = content.getSubject();
        String body = content.getBody();

        assertThat(subject).isNotNull().doesNotContain("*");
        assertThat(body).isNotNull();
        assertThat(content.getPriority()).isEqualTo(DEFAULT_PRIORITY.getId());
        assertThat(content.getNotificationTypeId()).isEqualTo(templateId.getId());

        checkLinks(templateId, body);
    }

    private WebContentProvider createWebContentProvider() {
        final Composer composer = createComposer();
        final ContentDataProvider dataProvider = createContentDataProvider();

        return new WebContentProvider(composer, dataProvider);
    }

    private void checkLinks(TemplateId templateId, final String body) {
        final Matcher matcher = LINK_PATTERN.matcher(body);

        while (matcher.find()) {
            final String link = matcher.group(2);
            checkLink(templateId, link);
        }
    }

}
