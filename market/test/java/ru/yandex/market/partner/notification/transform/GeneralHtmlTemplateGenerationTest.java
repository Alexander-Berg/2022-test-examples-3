package ru.yandex.market.partner.notification.transform;

import java.io.StringReader;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.jdom.Element;
import org.jdom.transform.JDOMResult;
import org.jdom.transform.JDOMSource;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.framework.composer.JDOMComposer;
import ru.yandex.market.notification.service.provider.content.BaseXslContentProvider;
import ru.yandex.market.partner.notification.transform.common.AbstractTemplateGenerationTest;
import ru.yandex.market.partner.notification.transform.common.TemplateId;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.partner.notification.transform.common.TemplateConstants.GENERAL_HTML_TEMPLATE_ID;

/**
 * Unit-тесты для проверки обертки с HTML версткой.
 *
 * @author Vladislav Bauer
 */
class GeneralHtmlTemplateGenerationTest extends AbstractTemplateGenerationTest {

    @Test
    void testGeneralStyle() throws Exception {
        String wrappedXsl = createWrapperGeneralTemplate();
        Transformer transformer = createTransformer(wrappedXsl);

        Element source = new Element(JDOMComposer.DATA_ELEMENT);
        JDOMResult result = new JDOMResult();

        transformer.transform(new JDOMSource(source), result);

        assertThat(transformer).isNotNull();
        assertThat(result.getResult()).isEmpty();
    }


    private Transformer createTransformer(String xsl) throws Exception {
        TransformerFactory transformerFactory = BaseXslContentProvider.createTransformerFactory();
        StreamSource streamSource = new StreamSource(new StringReader(xsl));
        return transformerFactory.newTransformer(streamSource);
    }

    private static String createWrapperGeneralTemplate() {
        String htmlXsl = getTemplate(TemplateId.builder().buildForId(GENERAL_HTML_TEMPLATE_ID));
        String contentXsl = htmlXsl
                + "<xsl:template name=\"body-content\"/>"
                + "<xsl:variable name=\"red-market\"/>";

        return wrapInXslDocument(contentXsl);
    }

}
