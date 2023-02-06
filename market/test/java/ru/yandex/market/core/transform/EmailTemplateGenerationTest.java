package ru.yandex.market.core.transform;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.core.framework.composer.Composer;
import ru.yandex.market.core.transform.common.BasicTemplateGenerationTest;
import ru.yandex.market.core.transform.common.TemplateId;
import ru.yandex.market.notification.mail.model.EmailContent;
import ru.yandex.market.notification.mail.service.provider.attachment.EmailAttachmentProvider;
import ru.yandex.market.notification.model.context.NotificationContentProviderContext;
import ru.yandex.market.notification.model.context.NotificationContext;
import ru.yandex.market.notification.service.NotificationTypeTransportTemplateService;
import ru.yandex.market.notification.service.provider.content.ContentDataProvider;
import ru.yandex.market.notification.service.provider.content.EmailContentProvider;
import ru.yandex.market.notification.service.provider.template.EmailTemplateProvider;
import ru.yandex.market.notification.service.provider.template.NotificationTemplateProvider;
import ru.yandex.market.notification.simple.model.type.NotificationTransport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Тесты по генерации шаблонов для транспорта {@link NotificationTransport#EMAIL}.
 *
 * @author Vladislav Bauer
 */
class EmailTemplateGenerationTest extends BasicTemplateGenerationTest {

    EmailTemplateGenerationTest() {
        super("email");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("ru.yandex.market.core.transform.common.BasicTemplateGenerationTest#data")
    void testTemplateGenerationEmail(TemplateId templateId) throws Exception {
        final NotificationTypeTransportTemplateService transportTemplateService =
                createMockedTypeTransportTemplateService(templateId);

        final NotificationTemplateProvider templateProvider = new EmailTemplateProvider(transportTemplateService);

        final Map<String, NotificationContext> originContexts = createNotificationContexts(NotificationTransport.EMAIL,
                templateId);

        originContexts.forEach((id, ctx) -> {
            final NotificationContentProviderContext context = createContentProviderContext(templateProvider, ctx);
            final EmailContentProvider contentProvider = createEmailContentProvider();
            final EmailContent content = contentProvider.provide(context).cast(EmailContent.class);
            checkContent(content, templateId, id);
        });
    }

    private void checkContent(final EmailContent content, final TemplateId templateId, String filename) {
        assertThat(content.getSubject()).isNotNull();
        assertThat(content.getBody()).isNotNull();

        final String formattedBody = content.getFormattedBody();
        assertThat(formattedBody).isNotNull();

        if (!templateId.isSelfSufficiantTemplate() && templateId.isPickupTemplate()) {
            assertThat(formattedBody).contains(PARAM_MARKET_URL, YANDEX_COPYRIGHT);
        }

        if (!templateId.isSelfSufficiantTemplate()) {
            checkLinks(templateId, formattedBody);
        }

//        printContent(content);
//        exportData(content, templateId, getIdx(filename));
    }

    /**
     * XXX(avetokhin) вывод в консоль контента, полезно для тестирования.
     *
     * @param content контент
     */
    @SuppressWarnings("unused")
    private void printContent(final EmailContent content) {
        System.out.println();
        System.out.println(content.getSubject());
        System.out.println();
        System.out.println(content.getBody());
        System.out.println();
        System.out.println(content.getFormattedBody());
        System.out.println();
    }

    /**
     * XXX(vbauer) Экспорт данных, может быть полезен для проверки результатов.
     *
     * @param content контент
     */
    @SuppressWarnings("unused")
    private void exportData(final EmailContent content, final TemplateId templateId, final int testIdx) {
        try {
            final File tmpDir = SystemUtils.getUserHome();
            final File dir = new File(tmpDir, "templates");
            FileUtils.forceMkdir(dir);

            final int idx = testIdx + 1;
            final File fileHtmlBody = new File(dir, String.format("template-html-%s-%d.html", templateId, idx));
            final File fileTextBody = new File(dir, String.format("template-txt-%s-%d.txt", templateId, idx));
            final File fileSubject = new File(dir, String.format("subject-%s-%d.txt", templateId, idx));

            FileUtils.writeStringToFile(fileHtmlBody, content.getFormattedBody(), StandardCharsets.UTF_8);
            FileUtils.writeStringToFile(fileTextBody, content.getBody(), StandardCharsets.UTF_8);
            FileUtils.writeStringToFile(fileSubject, content.getSubject(), StandardCharsets.UTF_8);
        } catch (final Exception ex) {
            throw new RuntimeException("Could not save generated data on FS for template " + templateId, ex);
        }
    }


    private EmailContentProvider createEmailContentProvider() {
        final Composer composer = createComposer();
        final EmailAttachmentProvider attachmentProvider = mock(EmailAttachmentProvider.class);
        final ContentDataProvider dataProvider = createContentDataProvider();

        return new EmailContentProvider(composer, attachmentProvider, dataProvider);
    }

    private void checkLinks(TemplateId templateId, final String formattedBody) {
        final Document document = Jsoup.parse(formattedBody);

        final Elements links = document.select("a[href]");
        final Collection<String> urls = links.stream()
                .map(link -> link.attr("href"))
                .collect(Collectors.toList());

        urls.forEach(url -> checkLink(templateId, url));
    }

    @SuppressWarnings("unused")
    private int getIdx(String filename) {
        final int index = filename.lastIndexOf("_");
        final int endIndex = filename.lastIndexOf(".xml");
        if (index < 0 || endIndex < 0) {
            return 0;
        }
        return Integer.parseInt(filename.substring(index + 1, endIndex));
    }
}
