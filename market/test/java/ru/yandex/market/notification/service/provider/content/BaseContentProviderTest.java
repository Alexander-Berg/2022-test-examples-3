package ru.yandex.market.notification.service.provider.content;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.io.IOUtils;
import org.jdom.Element;
import org.junit.jupiter.api.Disabled;

import ru.yandex.market.core.framework.ElementConverter;
import ru.yandex.market.core.framework.composer.JDOMComposer;
import ru.yandex.market.core.framework.composer.JDOMConverter;
import ru.yandex.market.core.framework.converter.NamedContainerConverter;
import ru.yandex.market.core.xml.impl.NamedContainer;
import ru.yandex.market.notification.common.model.destination.MbiDestination;
import ru.yandex.market.notification.exception.template.TemplateIOException;
import ru.yandex.market.notification.model.context.NotificationContentProviderContext;
import ru.yandex.market.notification.model.context.NotificationContext;
import ru.yandex.market.notification.model.data.MbiNotificationData;
import ru.yandex.market.notification.model.data.NotificationType;
import ru.yandex.market.notification.model.template.MbiNotificationTemplate;
import ru.yandex.market.notification.model.transport.NotificationDestination;
import ru.yandex.market.notification.service.provider.template.AbstractMbiTemplateProvider;
import ru.yandex.market.notification.service.provider.template.NotificationTemplateProvider;
import ru.yandex.market.notification.simple.model.context.NotificationContextImpl;
import ru.yandex.market.notification.simple.model.data.ArrayListNotificationData;
import ru.yandex.market.notification.simple.model.data.TextWithTitleData;
import ru.yandex.market.notification.simple.model.type.CodeNotificationType;
import ru.yandex.market.notification.simple.model.type.NotificationPriority;
import ru.yandex.market.notification.simple.model.type.NotificationTransport;
import ru.yandex.market.notification.simple.service.provider.context.NotificationContentProviderContextImpl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Базовый класс для тестирования поставщика контента.
 *
 * @author avetokhin 25/07/16.
 */
@Disabled
abstract class BaseContentProviderTest {
    static final Long SHOP_ID = 10L;
    static final Long USER_ID = 20L;
    static final Long NOTIFICATION_TYPE = 30L;
    static final String SUBJECT = "Тестовый заголовок для заказа subject";
    static final String BODY = "Изменение параметра body";
    static final String HTML_BODY = "При проверке магазина Изменение параметра body";

    static final String EXPLICIT_SUBJECT = "Явно указанный subject";
    static final String EXPLICIT_BODY = "Явно указанный body";

    NotificationContentProviderContext getContentProviderContext(
            NotificationTransport transport,
            String templateContentType,
            boolean useExplicitBody,
            boolean templateExists,
            MbiNotificationTemplate template
    ) throws IOException {
        NotificationContext context = createNotificationContext(transport, useExplicitBody);

        NotificationTemplateProvider templateProvider =
                createNotificationTemplateProvider(
                        templateExists,
                        templateContentType,
                        template
                );

        return new NotificationContentProviderContextImpl(context, templateProvider, Collections.emptyList());
    }

    NotificationContentProviderContext getContentProviderContext(
            String templateContentType,
            boolean useExplicitBody,
            boolean templateExists,
            MbiNotificationTemplate template
    ) throws IOException {
        return getContentProviderContext(
                NotificationTransport.EMAIL, templateContentType, useExplicitBody, templateExists, template
        );
    }

    private NotificationTemplateProvider createNotificationTemplateProvider(
            boolean templateExists, String templateContentType, MbiNotificationTemplate template
    ) throws IOException {
        if (template == null) {
            template = getDefaultTemplate(templateContentType, null);
        }
        NotificationTemplateProvider templateProvider = mock(NotificationTemplateProvider.class);
        if (templateExists) {
            when(templateProvider.provide(any())).thenReturn(template);
        } else {
            when(templateProvider.provide(any())).thenThrow(new TemplateIOException(""));
        }
        return templateProvider;
    }

    protected MbiNotificationTemplate getDefaultTemplate(String templateContentType, String extraData) throws IOException {
        String templateFileName = resolveTemplateFileName(templateContentType);
        InputStream is = getClass().getResourceAsStream(templateFileName);
        String xsl = IOUtils.toString(is, StandardCharsets.UTF_8.name());
        return new MbiNotificationTemplate(1L, "test", templateContentType, xsl, null, null, extraData);
    }

    private static NotificationContext createNotificationContext(
            NotificationTransport transport, boolean useExplicitBody) {
        NotificationType notificationType = new CodeNotificationType(NOTIFICATION_TYPE);

        Collection<NotificationDestination> destinations =
                Collections.singletonList(MbiDestination.create(SHOP_ID, USER_ID, null));

        ArrayListNotificationData<NamedContainer> templateData = createTemplateData();

        TextWithTitleData expBody =
                useExplicitBody
                        ? new TextWithTitleData(EXPLICIT_SUBJECT, EXPLICIT_BODY)
                        : null;

        MbiNotificationData data = new MbiNotificationData(templateData, expBody, null);

        return new NotificationContextImpl(notificationType, NotificationPriority.NORMAL,
                transport, destinations, Instant.now(), data, null, false);
    }

    private static ArrayListNotificationData<NamedContainer> createTemplateData() {
        ArrayListNotificationData<NamedContainer> templateData = new ArrayListNotificationData<>();
        templateData.add(new NamedContainer("test-param-subject", "subject"));
        templateData.add(new NamedContainer("test-param-body", "body"));
        templateData.add(new NamedContainer("test-param-payload", "test-param-payload"));
        templateData.add(new NamedContainer("test-param-repack", "test-param-repack"));
        templateData.add(new NamedContainer("subscription.service", "service"));
        return templateData;
    }

    protected static JDOMComposer createComposer() {
        JDOMConverter jdomConverter = new JDOMConverter();
        jdomConverter.setInnerConverters(Map.of(
                "java.lang.String", new StringElementConverter(),
                "ru.yandex.market.core.xml.impl.NamedContainer", new NamedContainerConverter()
        ));

        JDOMComposer composer = new JDOMComposer();
        composer.setElementConverter(jdomConverter);
        composer.setServantName("mbi-billing");
        return composer;
    }

    protected static String resolveTemplateFileName(String contentType) {
        boolean html = Objects.equals(AbstractMbiTemplateProvider.HTML_TYPE, contentType);
        return html
                ? "/xml/test-template-html.xml"
                : "/xml/test-template.xml";
    }

    private static class StringElementConverter implements ElementConverter<Object> {
        public Element convert(Object model) {
            Element root = new Element("value");
            root.setText(model.toString());
            return root;
        }
    }
}
