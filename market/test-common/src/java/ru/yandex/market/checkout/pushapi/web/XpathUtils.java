package ru.yandex.market.checkout.pushapi.web;

import java.util.Collections;

import javax.xml.xpath.XPathExpressionException;

import org.springframework.test.util.XpathExpectationsHelper;

public abstract class XpathUtils {

    private XpathUtils() {
        throw new UnsupportedOperationException();
    }

    public static XpathExpectationsHelper xpath(String expression) throws XPathExpressionException {
        return new XpathExpectationsHelper(expression, Collections.emptyMap());
    }
}
