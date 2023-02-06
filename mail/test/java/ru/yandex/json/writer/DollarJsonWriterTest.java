package ru.yandex.json.writer;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.test.util.TestBase;

public class DollarJsonWriterTest extends TestBase {
    public DollarJsonWriterTest() {
        super(false, 0L);
    }

    @Test
    public void test() throws IOException {
        StringWriter sw = new StringWriter();
        JsonWriter writer = new DollarJsonWriter(sw);
        writer.startArray();
        writer.startObject();
        writer.key("hel\"lo");
        writer.value("world\000\n\032");
        writer.endObject();
        writer.startString();
        writer.write("hello\000 again\000!\000\000");
        writer.write('\000');
        writer.endString();
        writer.value('\000');
        writer.value(Arrays.asList(Arrays.asList(0, 1, 2).iterator()));
        writer.endArray();
        Assert.assertEquals(
            "[{$hel\"lo\000:$world\000\000\n\032\000},"
            + "$hello\000\000 again\000\000!\000\000\000\000\000\000\000,"
            + "$\000\000\000,[[0,1,2]]]",
            sw.toString());
    }

    @Test
    public void testRootString() throws IOException {
        StringWriter sw = new StringWriter();
        new DollarJsonWriter(sw).value("test");
        Assert.assertEquals("$test\000", sw.toString());
    }
}

