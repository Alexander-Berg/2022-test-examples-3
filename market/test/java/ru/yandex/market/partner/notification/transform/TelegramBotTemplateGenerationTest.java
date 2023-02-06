package ru.yandex.market.partner.notification.transform;

import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentest4j.AssertionFailedError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.core.framework.composer.Composer;
import ru.yandex.market.notification.common.model.TelegramNotificationContent;
import ru.yandex.market.notification.model.context.NotificationContext;
import ru.yandex.market.notification.service.NotificationTypeTransportTemplateService;
import ru.yandex.market.notification.service.provider.content.ContentDataProvider;
import ru.yandex.market.notification.service.provider.content.TelegramBotContentProvider;
import ru.yandex.market.notification.service.provider.template.NotificationTemplateProvider;
import ru.yandex.market.notification.service.provider.template.TelegramBotTemplateProvider;
import ru.yandex.market.notification.simple.model.type.NotificationTransport;
import ru.yandex.market.partner.notification.transform.common.BasicTemplateGenerationTest;
import ru.yandex.market.partner.notification.transform.common.TemplateId;

import static org.assertj.core.api.Assertions.assertThat;


class TelegramBotTemplateGenerationTest extends BasicTemplateGenerationTest {

    private static final Logger log = LoggerFactory.getLogger(TelegramBotTemplateGenerationTest.class);

    /**
     * RegExpr для проверки ссылок как в Markdown.
     */
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[([^]]+)]\\(([^)]+)\\)", Pattern.MULTILINE);

    /**
     * Эти типы уведомлений не нужны в телеграме (отправляются только менеджерам Яндекса, либо скоро будут удалены)
     */
    private static final Set<Long> TEMPORARY_EXCLUDES = Set.of(
            27L,
            40L,
            97L,
            1456478860L,
            1509423731L,
            1517412062L,
            1537796735L,
            1544638139L,
            1549300852L,
            1579690352L,
            1581398790L,
            1581398791L,
            1581398792L,
            1581398793L,
            1581398794L,
            1581398795L,
            1581398796L,
            1590582727L,
            1602083820L,
            1602083848L,
            1606180000L,
            1618813719L,
            1639240019L,
            100200300400500L
    );

    @Autowired
    @Qualifier("valuesEscapingExceptions")
    private Set<Long> valuesEscapingExceptions;

    @Autowired
    @Qualifier("attributesEscapingExceptions")
    private Set<Long> attributesEscapingExceptions;

    TelegramBotTemplateGenerationTest() {
        super("telegram");
    }

    /**
     * Все шаблоны переведены на TelegramMarkdownV2.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("ru.yandex.market.partner.notification.transform.common.BasicTemplateGenerationTest#data")
    void testTemplateGenerationTelegramMarkdownV2(TemplateId templateId) throws Exception {
        testTelegramTemplates(templateId);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("temporaryExcludesData")
    void testExcludeTemplates(final long templateId) {
        final TemplateId id = TemplateId.builder().buildForId(templateId);
        final String template = getTemplate(id);

        // Нужно удалить ID уведомления из TEMPORARY_EXCLUDES
        assertThat(template).isNotNull();
    }

    private static Stream<Arguments> temporaryExcludesData() {
        return TEMPORARY_EXCLUDES.stream().map(Arguments::of);
    }


    private void testTelegramTemplates(TemplateId templateId) throws Exception {
        if (!checkNecessity(templateId)) {
            return;
        }

        final NotificationTypeTransportTemplateService transportTemplateService =
                createMockedTypeTransportTemplateService(templateId);

        final NotificationTemplateProvider templateProvider = new TelegramBotTemplateProvider(transportTemplateService);
        final TelegramBotContentProvider contentProvider = createTelegramBotContentProvider();

        final Map<String, NotificationContext> originContexts =
                createNotificationContexts(NotificationTransport.TELEGRAM_BOT, templateId);

        originContexts.forEach((fileName, ctx) -> {
            final var context = createContentProviderContext(templateProvider, ctx);
            final var content = contentProvider.provide(context).cast(TelegramNotificationContent.class);
            var expectedFileName = "telegram/" + FilenameUtils.removeExtension(fileName) + ".txt";
            checkContent(expectedFileName, content, templateId);
        });
    }

    /**
     * Проверяет, нужна ли проверка рендеринга шаблона для ТГ.
     *
     * @return {@code true} если нужно проверить шаблон, {@code false в противном случае}
     * @throws AssertionFailedError если проверить шаблон нужно, но тестовый *.txt файл отсутствует
     */
    private boolean checkNecessity(final TemplateId templateId) {
        var id = templateId.getId();
        //var templateTransports = transports.get().getOrDefault(id, Set.of());

        if (TEMPORARY_EXCLUDES.contains(id)) {
            return false;
        }

//        if (templateTransports.contains(NotificationTransport.EMAIL)
//                && !templateTransports.contains(NotificationTransport.TELEGRAM_BOT)) {
//            fail(String.format(
//                    "Новые шаблоны должны поддерживать tg в дополнение к email, шаблон [%s] не поддерживает",
//                    templateId
//            ));
//        }

        return true;
    }

    private void checkContent(String expectedTemplateFile, TelegramNotificationContent content, TemplateId templateId) {
        String actualText = content.getText();
        assertThat(actualText).isNotEmpty();
        checkLinks(templateId, actualText);

        var expected = getExpectedText(expectedTemplateFile);
        Assertions.assertTrue(
                expected.isPresent(),
                () -> String.format("Sample telegram template [/templates/%s] not found",
                        expectedTemplateFile)
        );

        log.info("Checking file {}", expectedTemplateFile);
        log.info("Expected content: {}", expected.get());
        log.info("Actual content: {}", actualText);
        /*
            Некоторые редакторы обрезают trailing whitespaces при сохранении, но в сообщениях они могут быть значимыми.
            Для удобства тестирования игнорируем эту разницу.
         */
        assertThat(actualText.lines().map(String::stripTrailing))
                .containsExactly(expected.get().lines().map(String::stripTrailing).toArray(String[]::new));
    }

    private TelegramBotContentProvider createTelegramBotContentProvider() {
        final Composer composer = createComposer();
        final ContentDataProvider dataProvider = createContentDataProvider();

        return new TelegramBotContentProvider(
                composer,
                dataProvider,
                attributesEscapingExceptions,
                valuesEscapingExceptions
        );
    }

    private void checkLinks(TemplateId templateId, final String body) {
        final Matcher matcher = LINK_PATTERN.matcher(body);
        while (matcher.find()) {
            final String link = matcher.group(2);
            checkLink(templateId, link);
        }
    }
}
