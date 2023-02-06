package ru.yandex.common.framework.core.servantletchecker.response;

import ru.yandex.common.framework.core.ServResponse;
import ru.yandex.common.framework.core.servantletchecker.util.FileUtil;
import ru.yandex.common.framework.core.servantletchecker.util.XmlUtil;

import static junit.framework.Assert.assertTrue;

/**
 * @author agorbunov @ Oct 25, 2010
 */
public class TestableResponse {
    private ServResponse original;

    public TestableResponse(ServResponse original) {
        this.original = original;
    }

    public void assertContainsFile(String fileName) {
        String expectedXml = FileUtil.readFile(fileName);
        assertContainsString(expectedXml);
    }

    public void assertContainsString(String expectedXml) {
        String actualXml = getXmlAsString();
        XmlContainmentMatcher matcher = new XmlContainmentMatcher(expectedXml, actualXml);
        String difference = matcher.getDifference();
        assertTrue(difference, difference.isEmpty());
    }

    public void print() {
        System.out.println("Response:");
        System.out.println(XmlUtil.format(getXmlAsString()));
    }

    private String getXmlAsString() {
        return new String(original.getXML());
    }
}
