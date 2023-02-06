package ru.yandex.market.partner.payment.util;

import org.jdom.Element;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.framework.composer.JDOMConverter;
import ru.yandex.market.core.framework.composer.JDOMUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Nikolay Malevanny nmalevanny@yandex-team.ru
 */
class JDOMConverterTest {
    private final JDOMConverter converter = new JDOMConverter();

    @Test
    void testConvertElement() {
        Element element = new Element("test");
        element.addContent("aaa");
        assertEquals("<test>aaa</test>", converter.convert(element));
    }

    @Test
    void testCreateValueElement() {
        Element element = JDOMUtil.createValueElement("id", "1");
        assertEquals("<id><value>1</value></id>", converter.convert(element));
    }

    @Test
    void testCreateErrorElement() {
        String errorCode = "code";
        Element element = JDOMUtil.createErrorElement(errorCode);
        assertEquals("<error>code</error>", converter.convert(element));
    }

}
