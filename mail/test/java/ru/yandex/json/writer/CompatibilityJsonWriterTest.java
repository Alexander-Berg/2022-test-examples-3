package ru.yandex.json.writer;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.test.util.TestBase;

public class CompatibilityJsonWriterTest extends TestBase {
    public CompatibilityJsonWriterTest() {
        super(false, 0L);
    }

    @Test
    public void testRootNumber() throws IOException {
        StringWriter sw = new StringWriter();
        new CompatibilityJsonWriter(sw).value("Привет, \"Мир\"");
        Assert.assertEquals(
            "\"\\u041f\\u0440\\u0438\\u0432\\u0435\\u0442, "
            + "\\u0022\\u041c\\u0438\\u0440\\u0022\"",
            sw.toString());
    }
}

