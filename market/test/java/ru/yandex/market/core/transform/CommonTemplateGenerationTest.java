package ru.yandex.market.core.transform;

import java.io.StringReader;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang3.StringUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.transform.JDOMResult;
import org.jdom.transform.JDOMSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.framework.composer.Composer;
import ru.yandex.market.core.notification.dao.NotificationTypeTransportTemplateLinkDao;
import ru.yandex.market.core.notification.data.SignatureDataProvider;
import ru.yandex.market.core.notification.data.TankerDataProvider;
import ru.yandex.market.core.notification.data.TransportTypeDataProvider;
import ru.yandex.market.core.transform.common.AbstractTemplateGenerationTest;
import ru.yandex.market.core.transform.common.TemplateConstants;
import ru.yandex.market.core.transform.common.TemplateId;
import ru.yandex.market.core.xml.impl.NamedContainer;
import ru.yandex.market.notification.model.template.NotificationTypeTransportTemplateLink;
import ru.yandex.market.notification.service.provider.content.BaseXslContentProvider;
import ru.yandex.market.notification.service.provider.template.AbstractMbiTemplateProvider;
import ru.yandex.market.notification.simple.model.data.ArrayListNotificationData;
import ru.yandex.market.notification.simple.model.type.NotificationTransport;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.core.notification.data.TransportTypeDataProvider.createData;

/**
 * Unit-тест для общего шаблона.
 *
 * @author Vladislav Bauer
 */
class CommonTemplateGenerationTest extends AbstractTemplateGenerationTest {

    private static final String URL = "http://ya.ru";
    private static final String TITLE = "Yandex";
    private static final String TEST_SUBJECT = "Simple subject";
    private static final String TEST_BODY = "Simple body";

    private static final String TURN_OFF_HEADER_PARAM =
            "<xsl:variable name=\"template-param-hide-body-header\" select=\"1\"/>\n";
    private static final String TURN_OFF_FOOTER_PARAM =
            "<xsl:variable name=\"template-param-hide-body-footer\" select=\"1\"/>\n";

    private static final String HEADER = "Здравствуйте!";
    private static final String FOOTER = "Тестовая Команда Яндекса";

    @Autowired
    private NotificationTypeTransportTemplateLinkDao notificationTypeTransportTemplateLinkDao;

    @Test
    void testUrlWithoutTransport() throws Exception {
        assertThat(printUrl(URL, TITLE, Set.of())).isEqualTo(TITLE + " " + URL);
    }

    /**
     * Для каждого транспорта есть название.
     */
    @Test
    void testTransportNaming() {
        Set<NotificationTransport> transports = notificationTypeTransportTemplateLinkDao.findAll().stream()
                .map(NotificationTypeTransportTemplateLink::getTransport)
                .collect(Collectors.toSet());

        transports.forEach(e -> Assertions.assertTrue(TransportTypeDataProvider.TRANSPORT_NAME.containsKey(e)));
    }

    @Test
    void testUrlForTransportsWithTitle() throws Exception {
        for (final NotificationTransport transport : NotificationTransport.values()) {
            final Collection<Object> data = Set.of(createData(transport));
            switch (transport) {
                case MBI_WEB_UI:
                case TELEGRAM_BOT:
                    assertThat(printUrl(URL, TITLE, data)).isEqualTo(anchor(URL, TITLE));
                    break;
                default:
                    assertThat(printUrl(URL, TITLE, data)).isEqualTo(TITLE + " " + URL);
            }
        }
    }

    @Test
    void testUrlForTransportsWithoutTitle() throws Exception {
        for (final NotificationTransport transport : NotificationTransport.values()) {
            final Collection<Object> data = Set.of(createData(transport));
            switch (transport) {
                case MBI_WEB_UI:
                case TELEGRAM_BOT:
                    assertThat(printUrl(URL, null, data)).isEqualTo(anchor(URL, URL));
                    break;
                default:
                    assertThat(printUrl(URL, null, data)).isEqualTo(URL);
            }
        }
    }

    @Test
    void testSubject() throws Exception {
        final Map<String, String> result = render(generateSimpleXsl(false, false), generateTestData());
        assertThat(result).containsEntry(BaseXslContentProvider.SUBJECT_KEY, TEST_SUBJECT);
    }

    @Test
    void testBodyNoHeaderFooter() throws Exception {
        final Map<String, String> result = render(generateSimpleXsl(false, false), generateTestData());
        assertThat(result).containsEntry(BaseXslContentProvider.BODY_KEY, TEST_BODY);
    }

    @Test
    void testBodyWithHeader() throws Exception {
        final Map<String, String> result = render(generateSimpleXsl(true, false), generateTestData());
        assertThat(result).containsEntry(BaseXslContentProvider.BODY_KEY, HEADER + "\n\n" + TEST_BODY);
    }

    @Test
    void testBodyWithFooter() throws Exception {
        final Map<String, String> result = render(generateSimpleXsl(false, true), generateTestData());
        assertThat(result).containsEntry(BaseXslContentProvider.BODY_KEY, TEST_BODY + "\n\n" + FOOTER);
    }

    @Test
    void testBodyWithHeaderAndFooter() throws Exception {
        final Map<String, String> result = render(generateSimpleXsl(true, true), generateTestData());
        assertThat(result).containsEntry(BaseXslContentProvider.BODY_KEY, HEADER + "\n\n" +
                TEST_BODY + "\n\n"
                + FOOTER);
    }

    private String anchor(final String url, final String title) {
        return String.format("[%s](%s)", title, url);
    }

    private String printUrl(final String url, final String title, final Collection<Object> data) throws Exception {
        final String xsl = generateXslForUrl(url, title);
        final Map<String, String> result = render(xsl, data);
        return result.get(BaseXslContentProvider.BODY_KEY);
    }

    private Collection<Object> generateTestData() {
        final ArrayListNotificationData<Object> data = new ArrayListNotificationData<>();

        data.add(new NamedContainer(SignatureDataProvider.KEY_MARKET_TEAM_SIGNATURE, FOOTER));
        data.add(new NamedContainer(TankerDataProvider.KEY_SALUTATION, HEADER));

        return data;
    }

    private Map<String, String> render(final String xsl, final Collection<Object> data) throws Exception {
        final Composer composer = createComposer();
        final Element roodXml = composer.compose(null, data, null);

        final Transformer transformer = createTransformer(xsl);
        final JDOMResult outputTarget = new JDOMResult();
        transformer.transform(new JDOMSource(roodXml), outputTarget);

        final Document document = outputTarget.getDocument();
        return extractContent(document);
    }

    private String generateSimpleXsl(final boolean withHeader, final boolean withFooter) {
        return wrapInXslDocument(
                getCommonXsl()
                        + (withHeader ? "" : TURN_OFF_HEADER_PARAM)
                        + (withFooter ? "" : TURN_OFF_FOOTER_PARAM)
                        + AbstractMbiTemplateProvider.EMPTY_HTML_WRAPPER
                        + generateXslSubject(wrapXslText(TEST_SUBJECT))
                        + generateXslBody(wrapXslText(TEST_BODY))
        );
    }

    private String generateXslForUrl(final String url, final String title) {
        return wrapInXslDocument(
                getCommonXsl()
                        + TURN_OFF_HEADER_PARAM
                        + TURN_OFF_FOOTER_PARAM
                        + AbstractMbiTemplateProvider.EMPTY_HTML_WRAPPER
                        + generateXslSubject("")
                        + generateXslBody(printLinkXsl(url, title))
        );
    }

    private String getCommonXsl() {
        return getTemplate(TemplateId.builder().buildForId(TemplateConstants.COMMON_TEMPLATE_ID));
    }

    private String generateXslSubject(final String subject) {
        return "<xsl:template name=\"subject-content\">" + subject + "</xsl:template>";
    }

    private String generateXslBody(final String body) {
        return "<xsl:template name=\"body-content\">" + body + "</xsl:template>";
    }

    private String wrapXslText(final String text) {
        return "<xsl:text>" + text + "</xsl:text>";
    }

    private String printLinkXsl(final String url, final String title) {
        final StringBuilder result = new StringBuilder("<xsl:call-template name=\"print-link\">");
        if (!StringUtils.isBlank(url)) {
            result.append("<xsl:with-param name=\"url\" select=\"'").append(url).append("'\" />");
        }
        if (!StringUtils.isBlank(title)) {
            result.append("<xsl:with-param name=\"title\" select=\"'").append(title).append("'\" />");
        }
        result.append("</xsl:call-template>");
        return result.toString();
    }

    private Transformer createTransformer(final String xsl) throws Exception {
        TransformerFactory transformerFactory = BaseXslContentProvider.createTransformerFactory();
        StreamSource streamSource = new StreamSource(new StringReader(xsl));
        return transformerFactory.newTransformer(streamSource);
    }

}
