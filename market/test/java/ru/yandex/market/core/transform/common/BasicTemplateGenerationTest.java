package ru.yandex.market.core.transform.common;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.jdom.Element;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.core.language.model.Language;
import ru.yandex.market.core.notification.dao.NotificationTypeTransportTemplateLinkDao;
import ru.yandex.market.core.notification.data.CurrentDateTimeProvider;
import ru.yandex.market.core.notification.data.NotificationTypeDataProvider;
import ru.yandex.market.core.notification.data.RegionalDataProvider;
import ru.yandex.market.core.notification.data.SignatureDataProvider;
import ru.yandex.market.core.notification.data.TankerDataProvider;
import ru.yandex.market.core.notification.data.TransportTypeDataProvider;
import ru.yandex.market.core.notification.service.resolver.NotificationLanguageResolver;
import ru.yandex.market.core.tanker.dao.TankerDao;
import ru.yandex.market.core.tanker.model.MessageSet;
import ru.yandex.market.core.xml.impl.NamedContainer;
import ru.yandex.market.notification.model.context.NotificationContentProviderContext;
import ru.yandex.market.notification.model.context.NotificationContext;
import ru.yandex.market.notification.model.data.MbiNotificationData;
import ru.yandex.market.notification.model.template.MbiNotificationTemplate;
import ru.yandex.market.notification.model.template.NotificationTypeTransportTemplateLink;
import ru.yandex.market.notification.service.NotificationTypeTransportTemplateService;
import ru.yandex.market.notification.service.provider.content.ContentDataProvider;
import ru.yandex.market.notification.service.provider.template.AbstractMbiTemplateProvider;
import ru.yandex.market.notification.service.provider.template.NotificationTemplateProvider;
import ru.yandex.market.notification.simple.model.context.NotificationContextImpl;
import ru.yandex.market.notification.simple.model.data.ArrayListNotificationData;
import ru.yandex.market.notification.simple.model.type.CodeNotificationType;
import ru.yandex.market.notification.simple.model.type.NotificationPriority;
import ru.yandex.market.notification.simple.model.type.NotificationTransport;
import ru.yandex.market.notification.simple.service.provider.context.NotificationContentProviderContextImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.tanker.model.TankerKeySets.NOTIFICATION_TEMPLATES;
import static ru.yandex.market.core.transform.common.TemplateConstants.DELIVERY_GENERAL_HTML_TEMPLATE;
import static ru.yandex.market.core.transform.common.TemplateConstants.GENERAL_HTML_TEMPLATE_ID;
import static ru.yandex.market.core.transform.common.TemplateConstants.PICKUP_HTML_TEMPLATE;

/**
 * Базовый класс для тестов, которые проверяют генерацию содержимого шаблонов (с заточкой под транспорт).
 *
 * @author Vladislav Bauer
 */
public abstract class BasicTemplateGenerationTest extends AbstractTemplateGenerationTest {

    protected static final NotificationPriority DEFAULT_PRIORITY = NotificationPriority.HIGH;

    protected static final String PARAM_MARKET_URL = "http://market.yandex.ru";
    protected static final String PARAM_HELP_URL = "https://help.yandex.ru";
    protected static final String PARAM_PARTNER_URL = "http://partner.market.yandex.ru";
    protected static final String PARAM_PARTNER_SUPPORT_URL = "https://yandex.ru/support/partnermarket";
    protected static final String PARAM_PARTNER_DSBS_SUPPORT_URL = "https://yandex.ru/support/partnermarket-dsbs";
    protected static final String PARAM_MARKETPLACE_SUPPORT_URL = "https://yandex.ru/support/marketplace";
    protected static final String PARAM_BLUE_MARKET_SIGNATURE = "Команда Яндекс.Маркета";
    protected static final String CURRENT_DATE_TIME = "2016-12-03T10:15:30Z";

    protected static final String YANDEX_COPYRIGHT = "© 2016 «Яндекс.Маркет»";
    private static final String UTM_PARAMS = "utm_source=market_partner&utm_medium=%s&utm_campaign=msg_%s";
    private static final String MAILTO = "mailto:";
    private static final Map<Language, String> SALUTATION_MAP = ImmutableMap.<Language, String>builder()
            .put(Language.RUSSIAN, "Здравствуйте!")
            .put(Language.GERMAN, "Hallo!")
            .put(Language.ENGLISH, "Hello!")
            .put(Language.TURKISH, "Merhaba!")
            .put(Language.CHINESE_TRADITIONAL, "您好：")
            .put(Language.CHINESE_SIMPLIFIED, "您好!")
            .build();
    private static final Map<Language, String> RED_MARKET_SIGNATURE_MAP = ImmutableMap.<Language, String>builder()
            .put(Language.RUSSIAN, "Команда Bringly")
            .put(Language.GERMAN, "Das Bringly-Team")
            .put(Language.ENGLISH, "The Bringly team")
            .put(Language.TURKISH, "Bringly Ekibi")
            .put(Language.CHINESE_TRADITIONAL, "Bringly 團隊敬上")
            .put(Language.CHINESE_SIMPLIFIED, "Bringly 团队")
            .build();
    private static final Map<Language, String> ACCOUNT_MAP = ImmutableMap.<Language, String>builder()
            .put(Language.RUSSIAN, "Личный кабинет")
            .put(Language.ENGLISH, "Account")
            .build();
    /**
     * Сюда можно прописать id шаблонов для ручного запуска тестов для удобства отладки, например {@code 12345L}
     */
    private static final Set<Long> DEBUG_ONLY_TEMPLATE_IDS = Set.of(
    );

    private static final EmailValidator EMAIL_VALIDATOR = EmailValidator.getInstance();

    private final String utmMedium;

    @Autowired
    private NotificationTypeTransportTemplateLinkDao notificationTypeTransportTemplateLinkDao;

    protected final Supplier<Map<Long, Set<NotificationTransport>>> transports =
            Suppliers.memoize(this::loadTransports);

    public BasicTemplateGenerationTest(final String utmMedium) {
        this.utmMedium = utmMedium;
    }

    /**
     * Формирует параметры для тестов.
     * Параметром для тестов является идентификатор темплейта, содержащий непосредственно из айдишникв темплейта и
     * языка, на котором темплейт написан.
     *
     * @return параметры для тестов.
     */
    public static Stream<Arguments> data() {
        return getTemplates().keySet().stream()
                .filter(id -> !id.isCommonTemplate() && (
                        DEBUG_ONLY_TEMPLATE_IDS.isEmpty() || DEBUG_ONLY_TEMPLATE_IDS.contains(id.getId())
                ))
                .sorted(Comparator.comparing(TemplateId::getId))
                .map(Arguments::of);
    }

    /**
     * Создает поставщик данных для отрисовки контента.
     *
     * @return поставщик данных для отрисовки контента.
     */
    @Nonnull
    protected ContentDataProvider createContentDataProvider() {
        return new ContentDataProvider();
    }

    @Nonnull
    protected NotificationContentProviderContext createContentProviderContext(
            @Nonnull final NotificationTemplateProvider templateProvider,
            @Nonnull final NotificationContext context) {
        return new NotificationContentProviderContextImpl(context, templateProvider, Collections.emptySet());
    }

    /**
     * Создает мок провайдера темплейта.
     *
     * @return см. описание.
     */
    @Nonnull
    protected NotificationTypeTransportTemplateService createMockedTypeTransportTemplateService(TemplateId templateId) {
        final NotificationTypeTransportTemplateService transportTemplateService =
                mock(NotificationTypeTransportTemplateService.class);

        when(transportTemplateService.findByNotificationTypeAndTransport(anyLong(), any()))
                .thenReturn(createTemplate(AbstractMbiTemplateProvider.HTML_TYPE, templateId));
        when(transportTemplateService.getGeneralHtmlTemplate())
                .thenReturn(createTemplate(null, TemplateId.builder().buildForId(GENERAL_HTML_TEMPLATE_ID)));
        when(transportTemplateService.getById(eq(PICKUP_HTML_TEMPLATE)))
                .thenReturn(createTemplate(null, TemplateId.builder().buildForId(PICKUP_HTML_TEMPLATE)));
        when(transportTemplateService.getById(eq(DELIVERY_GENERAL_HTML_TEMPLATE)))
                .thenReturn(createTemplate(null,
                        TemplateId.builder().buildForId(DELIVERY_GENERAL_HTML_TEMPLATE)));
        when(transportTemplateService.getCommonTemplate()).thenReturn(createTemplate(null,
                TemplateId.builder().buildForId(TemplateConstants.COMMON_TEMPLATE_ID)));

        return transportTemplateService;
    }

    @Nonnull
    protected NotificationLanguageResolver createLanguageResolver(TemplateId templateId) {
        final NotificationLanguageResolver languageResolver = mock(NotificationLanguageResolver.class);

        when(languageResolver.resolve(anySet())).thenReturn(templateId.getLanguage());

        return languageResolver;
    }

    @Nonnull
    protected TankerDao createTankerDao(TemplateId templateId) {
        final TankerDao tankerDao = mock(TankerDao.class);
        final MessageSet notificationKeyset = mock(MessageSet.class);

        when(tankerDao.getMessageSet(eq(NOTIFICATION_TEMPLATES),
                eq(templateId.getLanguage()))).thenReturn(notificationKeyset);
        when(notificationKeyset.getMessage(eq(Long.toString(templateId.getId()))))
                .thenReturn(getTemplate(TemplateId.builder().buildForId(templateId.getId())));

        return tankerDao;
    }

    /**
     * Создать коллекцию контекстов уведомления. Создает минимум 1 контекст, если имеется несколько XML файлов с
     * дополнительными данными, то создает контекст для каждого из них.
     */
    @Nonnull
    protected Map<String, NotificationContext> createNotificationContexts(
            @Nonnull final NotificationTransport transportType,
            @Nonnull final TemplateId templateId) throws Exception {
        final Map<String, NotificationContext> contexts = new HashMap<>();
        final Map<String, List<Element>> extraData = TemplateExtraDataLoader.loadExtraData(templateId.getFileName());
        if (extraData.isEmpty()) {
            contexts.put(templateId.getFileName(), createContext(transportType, templateId, List.of()));
        } else {
            extraData.forEach((k, v) -> contexts.put(k, createContext(transportType, templateId, v)));
        }
        return contexts;
    }

    private NotificationContext createContext(NotificationTransport transportType,
                                              TemplateId templateId,
                                              Collection<Element> data) {
        return new NotificationContextImpl(
                new CodeNotificationType(templateId.getId()),
                DEFAULT_PRIORITY,
                transportType,
                List.of(),
                Instant.now(),
                createNotificationData(templateId, transportType, data),
                null,
                false
        );
    }

    protected void checkLink(@Nonnull TemplateId templateId, @Nonnull final String url) {
        boolean validLink;
        if (StringUtils.startsWith(url, MAILTO)) {
            final String email = StringUtils.substringAfter(url, MAILTO);
            validLink = EMAIL_VALIDATOR.isValid(email);
        } else {
            validLink = UrlValidator.getInstance().isValid(url);
        }

        if (!validLink) {
            fail(String.format("Incorrect link address \"%s\"", url));
        }

        URI uri = URI.create(url);
        String query = uri.getQuery();
        if (StringUtils.isNotBlank(query)) {
            Collection<NameValuePair> pairs = URLEncodedUtils.parse(query, StandardCharsets.UTF_8);
            assertThat(pairs).allSatisfy(pair -> {
                assertThat(pair.getName()).as(url).isNotEmpty();
                assertThat(pair.getValue()).as(url).isNotEmpty();
            });
        }

        String utm = String.format(UTM_PARAMS, utmMedium, templateId.getId());
        if (url.contains(PARAM_PARTNER_URL) && !url.contains(utm)) {
            fail(String.format("Partner url must contain utm, but it doesn't: \"%s\"", url));
        }
    }

    protected Optional<String> getExpectedText(String templateFileName) {
        try {
            String content = StringTestUtil.getString(getClass(), "/templates/" + templateFileName);
            return Optional.ofNullable(StringUtils.trimToNull(content));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private MbiNotificationData createNotificationData(
            final TemplateId templateId,
            final NotificationTransport transport,
            final Collection<Element> extraData
    ) {
        final ArrayListNotificationData<Object> data = new ArrayListNotificationData<>();

        // Добавить данные о транспорте.
        data.addAll(TransportTypeDataProvider.createData(transport));

        // Добавить данные о типе уведомления.
        data.add(new NamedContainer(NotificationTypeDataProvider.KEY_NOTIFICATION_TYPE, templateId.getId()));

        // Добавить данные для ссылок.
        data.add(new NamedContainer(RegionalDataProvider.KEY_MARKET_URL, PARAM_MARKET_URL));
        data.add(new NamedContainer(RegionalDataProvider.KEY_HELP_URL, PARAM_HELP_URL));
        data.add(new NamedContainer(RegionalDataProvider.KEY_PARTNER_URL, PARAM_PARTNER_URL));
        data.add(new NamedContainer(RegionalDataProvider.KEY_PARTNER_SUPPORT_URL, PARAM_PARTNER_SUPPORT_URL));
        data.add(new NamedContainer(RegionalDataProvider.KEY_PARTNER_DSBS_SUPPORT_URL, PARAM_PARTNER_DSBS_SUPPORT_URL));
        data.add(new NamedContainer(RegionalDataProvider.KEY_MARKETPLACE_SUPPORT_URL, PARAM_MARKETPLACE_SUPPORT_URL));

        // добавить данные о подписях
        data.add(new NamedContainer(SignatureDataProvider.KEY_BLUE_MARKET_TEAM_SIGNATURE,
                PARAM_BLUE_MARKET_SIGNATURE));

        // добавить данные из танкерв
        data.add(new NamedContainer(TankerDataProvider.KEY_SALUTATION, SALUTATION_MAP.get(templateId.getLanguage())));
        data.add(new NamedContainer(TankerDataProvider.KEY_ACCOUNT, ACCOUNT_MAP.get(
                templateId.getLanguage() == Language.RUSSIAN ? Language.RUSSIAN : Language.ENGLISH)));

        // Добавить данные о текущей дате/времени.
        data.add(new NamedContainer(CurrentDateTimeProvider.CURRENT_DATE_TIME_PARAM_ID, CURRENT_DATE_TIME));

        data.addAll(extraData);

        return new MbiNotificationData(data, null, null);
    }


    private Optional<MbiNotificationTemplate> createTemplate(final String contentType, final TemplateId templateId) {
        return Optional.of(new MbiNotificationTemplate(templateId.getId(),
                StringUtils.EMPTY,
                contentType,
                getTemplate(templateId),
                null, /* mustache subject */
                null, /* mustache body */
                templateId.isSelfSufficiantTemplate(),
                resolveHtmlWrapper(templateId),
                getTemplateExtraData(templateId)
        ));
    }

    @Nullable
    private Long resolveHtmlWrapper(TemplateId templateId) {
        if (templateId.isPickupTemplate()) {
            return PICKUP_HTML_TEMPLATE;
        }
        if (templateId.isDeliveryTemplate()) {
            return DELIVERY_GENERAL_HTML_TEMPLATE;
        }
        return null;
    }

    private Map<Long, Set<NotificationTransport>> loadTransports() {
        return notificationTypeTransportTemplateLinkDao.findAll().stream()
                .filter(e -> e.getTemplateId().isPresent())
                .collect(Collectors.groupingBy(
                        e -> e.getTemplateId().orElseThrow(),
                        Collectors.mapping(
                                NotificationTypeTransportTemplateLink::getTransport,
                                Collectors.toSet()
                        )
                ));
    }
}
