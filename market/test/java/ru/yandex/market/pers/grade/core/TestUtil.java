package ru.yandex.market.pers.grade.core;

import org.junit.Assert;

import ru.yandex.common.util.xml.XmlConvertable;

/**
 * @author Vladimir Gorovoy vgorovoy@yandex-team.ru
 */
public class TestUtil {
    public static void printXmlConvertable(XmlConvertable xmlConvertable) {
        StringBuilder result = new StringBuilder();
        Assert.assertTrue(xmlConvertable != null);
        xmlConvertable.toXml(result);
        System.out.println(result);
    }
}
