package ru.yandex.market.core.framework;

import java.util.List;

import org.jdom.Element;
import org.junit.Test;

import ru.yandex.market.core.framework.converter.BeanElementConverter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Alexey Shevenkov ashevenkov@yandex-team.ru
 */

public class BeanElementConverterTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testConversion() {
        final BeanElementConverter converter = new BeanElementConverter();

        final MockBean mockBean = new MockBean();
        mockBean.setProperty("mock-value");
        mockBean.setAnotherProperty(512);

        final Element element = converter.convert(mockBean);
        assertEquals(element.getName(), "MockBean");

        final List<Element> children = element.getChildren();

        for (final Element child : children) {
            final String name = child.getName();
            final String text = child.getText();

            switch (name) {
                case "property":
                    assertEquals(text, "mock-value");
                    break;
                case "anotherProperty":
                    assertEquals(text, "512");
                    break;
                default:
                    fail();
                    break;
            }
        }
    }

}
