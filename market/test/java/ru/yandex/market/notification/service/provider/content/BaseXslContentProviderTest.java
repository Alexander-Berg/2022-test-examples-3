package ru.yandex.market.notification.service.provider.content;

import java.io.StringReader;
import java.util.Collections;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.xml.transform.stream.StreamSource;

import org.hamcrest.MatcherAssert;
import org.jdom.Element;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.framework.composer.Composer;
import ru.yandex.market.core.framework.composer.JDOMComposer;
import ru.yandex.market.core.framework.composer.JDOMConverter;
import ru.yandex.market.notification.exception.template.TemplateRenderingException;
import ru.yandex.market.notification.model.context.NotificationContentProviderContext;
import ru.yandex.market.notification.model.context.NotificationContext;
import ru.yandex.market.notification.model.data.NotificationContent;
import ru.yandex.market.notification.simple.model.data.ArrayListNotificationData;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты для {@link BaseXslContentProvider}.
 *
 * @author Vladislav Bauer
 */
class BaseXslContentProviderTest {

    @Test
    void testWarningAsError() {
        TestXslContentProvider contentProvider = createContentProvider();
        Element element = new Element("data");

        assertThrows(
                TemplateRenderingException.class,
                () -> contentProvider.renderData(element, new StreamSource(new StringReader(
                        "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.1\">\n" +
                                "    <xsl:variable name=\"partnerUrl\" select=\"math:(/data/i-am-error)\"/>\n" +
                                "</xsl:stylesheet>"
                )))
        );
    }

    @Test
    void testProvide() {
        TestXslContentProvider contentProvider = createContentProvider();
        NotificationContentProviderContext providerContext = createProviderContext();

        // Проверить что повторная компоновка данных не вызывает проблем.
        for (int i = 0; i < 3; i++) {
            XmlContent content = contentProvider.provide(providerContext).cast(XmlContent.class);

            assertThat(content, notNullValue());
            assertThat(content.getElement(), notNullValue());
        }
    }


    private static NotificationContentProviderContext createProviderContext() {
        Element element = new Element("test");
        ArrayListNotificationData<Element> elements =
                new ArrayListNotificationData<>(Collections.singleton(element));

        NotificationContext notificationContext = mock(NotificationContext.class);
        when(notificationContext.getData()).thenReturn(Optional.empty());

        NotificationContentProviderContext providerContext = mock(NotificationContentProviderContext.class);
        when(providerContext.getParent()).thenReturn(notificationContext);
        when(providerContext.getExtraData()).thenReturn(Collections.singleton(elements));

        return providerContext;
    }

    private static TestXslContentProvider createContentProvider() {
        JDOMComposer composer = new JDOMComposer();
        composer.setElementConverter(new JDOMConverter());

        ContentDataProvider contentDataProvider = new ContentDataProvider();
        return new TestXslContentProvider(composer, contentDataProvider);
    }

    private static class TestXslContentProvider extends BaseXslContentProvider<XmlContent> {
        TestXslContentProvider(
                @Nonnull Composer composer,
                @Nonnull ContentDataProvider contentDataProvider
        ) {
            super(composer, contentDataProvider);
        }

        @Override
        protected XmlContent provideContent(
                NotificationContentProviderContext context, Element xmlDataRoot
        ) {
            return new XmlContent(xmlDataRoot);
        }

        @Override
        protected void validateContent(@Nonnull XmlContent content) {
        }
    }

    private static class XmlContent implements NotificationContent {

        private final Element element;

        XmlContent(Element element) {
            this.element = element;
        }

        Element getElement() {
            return element;
        }
    }
}
