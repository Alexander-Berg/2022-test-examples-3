package ru.yandex.market.core.framework.converter;

import java.util.List;

import org.jdom.Element;
import org.junit.Test;

import ru.yandex.market.core.framework.MockBean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Alexey Shevenkov ashevenkov@yandex-team.ru
 */
public class StandartBeanElementConverterTest {

    @Test
    public void testConverting() {
        MockBean mockBean = new MockBean("value", -1);
        StandartBeanElementConverter converter = new StandartBeanElementConverter();
        Element root = converter.convert(mockBean);
        assertEquals(root.getName(), "mock-bean");
        List children = root.getChildren();
        assertEquals(children.size(), 2);
        Element anotherPropertyElement = root.getChild("another-property");
        assertNotNull(anotherPropertyElement);
        assertEquals(anotherPropertyElement.getText(), "-1");
    }
}
