package ru.yandex.market.core.framework.converter;

import javax.annotation.ParametersAreNonnullByDefault;

import org.jdom.Element;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.core.xml.impl.NamedContainer;

@ParametersAreNonnullByDefault
public class NamedContainerConverterTest {
    @Test
    public void testNonnull() {
        Element element = new NamedContainerConverter().convert(new NamedContainer("test123", "testText"));
        Assert.assertEquals("test123", element.getName());
        Assert.assertEquals("testText", element.getText());
    }

    @Test
    public void testNull() {
        Element element = new NamedContainerConverter().convert(new NamedContainer("test321", null));
        Assert.assertEquals("test321", element.getName());
        Assert.assertEquals("", element.getText());
    }
}
