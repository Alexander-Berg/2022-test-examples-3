package ru.yandex.market.notification.service.provider.template;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Disabled;

import ru.yandex.market.notification.model.context.NotificationContext;
import ru.yandex.market.notification.model.context.NotificationTemplateProviderContext;
import ru.yandex.market.notification.model.data.NotificationTemplate;
import ru.yandex.market.notification.model.template.MbiNotificationTemplate;
import ru.yandex.market.notification.service.NotificationTypeTransportTemplateService;
import ru.yandex.market.notification.simple.model.context.NotificationContextImpl;
import ru.yandex.market.notification.simple.model.type.CodeNotificationType;
import ru.yandex.market.notification.simple.model.type.NotificationPriority;
import ru.yandex.market.notification.simple.model.type.NotificationTransport;
import ru.yandex.market.notification.simple.service.provider.context.NotificationTemplateProviderContextImpl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.notification.service.provider.template.AbstractMbiTemplateProvider.HTML_TYPE;

/**
 * Базовый класс для тестирование поставщиков шаблонов.
 *
 * @author avetokhin 11/08/16.
 */
@Disabled
abstract class BaseTemplateProviderTest {

    static final String SELF_SUFFICIENT_XSL = "<xsl:template match=\"/\">CUSTOM_HTML</xsl:template>";
    private static final String TEMPLATE_XSL = "<xsl:template match=\"/\">TEMPLATE</xsl:template>";
    protected static final String TEMPLATE_TEXT_XSL = wrapXsl(TEMPLATE_XSL);
    static final MbiNotificationTemplate TEMPLATE =
            new MbiNotificationTemplate(10L, "test_template", null, TEMPLATE_TEXT_XSL, null, null);
    static final MbiNotificationTemplate HTML_TEMPLATE =
            new MbiNotificationTemplate(20L, "test_html_template", HTML_TYPE, TEMPLATE_TEXT_XSL, null, null);
    static final MbiNotificationTemplate SELF_SUFFICIENT_TEMPLATE =
            new MbiNotificationTemplate(50L, "self_sufficient_test_template",
                    HTML_TYPE, SELF_SUFFICIENT_XSL, null, null, true, null);
    private static final Long NOTIFICATION_TYPE = 1L;
    private static final String GENERAL_HTML_XSL = "<xsl:template match=\"/\">HTML</xsl:template>";
    static final MbiNotificationTemplate GENERAL_HTML_TEMPLATE =
            new MbiNotificationTemplate(30L, "general_html", null, GENERAL_HTML_XSL, null, null);
    private static final String COMMON_XSL = "<xsl:template match=\"/\">COMMON</xsl:template>";
    static final MbiNotificationTemplate COMMON_TEMPLATE =
            new MbiNotificationTemplate(40L, "common_html", null, COMMON_XSL, null, null);

    protected static final String TEMPLATE_TEXT_XSL_WITH_COMMON = wrapXsl(COMMON_XSL + TEMPLATE_XSL);
    protected static final String TEMPLATE_TEXT_XSL_WITH_HTML_WRAPPER = wrapXsl(GENERAL_HTML_XSL + COMMON_XSL + TEMPLATE_XSL);
    protected static final String TEMPLATE_XSL_WITH_HTML_EMPTY_WRAPPER = wrapXsl(
            AbstractMbiTemplateProvider.EMPTY_HTML_WRAPPER + COMMON_XSL + TEMPLATE_XSL
    );

    private static String wrapXsl(String xsl) {
        return "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.1\">"
                + xsl + "</xsl:stylesheet>";
    }

    static MbiNotificationTemplate checkAndCastTemplate(NotificationTemplate template) {
        assertThat(template, notNullValue());
        assertThat(template, instanceOf(MbiNotificationTemplate.class));

        return template.cast(MbiNotificationTemplate.class);
    }

    static NotificationTypeTransportTemplateService prepareTemplateServiceMock(
            MbiNotificationTemplate byIdTemplate,
            MbiNotificationTemplate generalHtmlTemplate,
            MbiNotificationTemplate commonTemplate,
            NotificationTransport transport
    ) {
        NotificationTypeTransportTemplateService templateService = mock(NotificationTypeTransportTemplateService.class);

        when(templateService.findByNotificationTypeAndTransport(NOTIFICATION_TYPE, transport))
                .thenReturn(Optional.ofNullable(byIdTemplate));
        when(templateService.getGeneralHtmlTemplate())
                .thenReturn(Optional.ofNullable(generalHtmlTemplate));
        when(templateService.getCommonTemplate())
                .thenReturn(Optional.ofNullable(commonTemplate));

        return templateService;
    }

    static NotificationTemplateProviderContext getContext(NotificationTransport transport) {
        NotificationContext notificationContext = new NotificationContextImpl(
                new CodeNotificationType(NOTIFICATION_TYPE),
                NotificationPriority.NORMAL,
                transport,
                Collections.emptyList(),
                Instant.now(), null, null, false
        );
        return new NotificationTemplateProviderContextImpl(
                notificationContext, Collections.emptyList()
        );
    }

}
