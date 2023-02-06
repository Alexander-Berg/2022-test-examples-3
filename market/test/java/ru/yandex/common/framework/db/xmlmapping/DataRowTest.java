package ru.yandex.common.framework.db.xmlmapping;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.common.framework.db.DataRow;

/**
 * @author korolyov
 * 31.07.17
 */
public class DataRowTest {
    @Test
    public void dataRowXmlEscapeTest() {
        DataRow dataRow = new DataRow("test");
        StringBuilder result = new StringBuilder();
        dataRow.writeElement("tag", "\u0010", result);
        Assert.assertEquals("<tag></tag>", result.toString());
    }
}
