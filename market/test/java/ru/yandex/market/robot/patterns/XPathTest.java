package ru.yandex.market.robot.patterns;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.NodeList;
import ru.yandex.common.util.http.HttpGetLocation;
import ru.yandex.common.util.http.Page;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class XPathTest extends Assert {

    /**
     * Для корректной работы робота требуется реализация XPathPattern из org.apache.xpath.jaxp
     */

    @Test
    public void XpathTest() {
        XPathExpression xPathExpression = new XPathPattern(
            "//LI[contains(@class,\"header__nav-item\")][3]/A[contains(@class,\"header__nav-link\")]"
        ).pattern;

        ClassLoader classLoader = XPathTest.class.getClassLoader();

        try (InputStream inputStream = classLoader.getResourceAsStream("XPathTest.html")) {

            String result = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            Page page = new Page(new HttpGetLocation("XPathTest.ru"), result);

            NodeList nodeList = (NodeList) xPathExpression.evaluate(page.getDocument(), XPathConstants.NODESET);

            assertEquals(1, nodeList.getLength());
        } catch (IOException | XPathExpressionException e) {
            e.printStackTrace();
        }


    }
}
