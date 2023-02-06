package ru.yandex.json.writer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.test.util.TestBase;

public class DollarUtf8JsonWriterTest extends TestBase {
    public DollarUtf8JsonWriterTest() {
        super(false, 0L);
    }

    @Test
    public void test() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Utf8JsonWriter writer = new DollarUtf8JsonWriter(out);
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
            new String(out.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    public void testRootString() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Utf8JsonWriter writer = new DollarUtf8JsonWriter(out);
        writer.value("test");
        Assert.assertEquals(
            "$test\000",
            new String(out.toByteArray(), StandardCharsets.UTF_8));
    }
}

