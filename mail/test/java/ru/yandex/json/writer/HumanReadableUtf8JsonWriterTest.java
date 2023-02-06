package ru.yandex.json.writer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.test.util.TestBase;

public class HumanReadableUtf8JsonWriterTest extends TestBase {
    public HumanReadableUtf8JsonWriterTest() {
        super(false, 0L);
    }

    // CSOFF: MagicNumber
    @Test
    public void test() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Utf8JsonWriter writer = new HumanReadableUtf8JsonWriter(out);
        writer.startArray();
        writer.value((Object) null);
        writer.startObject();
        writer.key("nil");
        writer.startArray();
        writer.value((Object) null);
        writer.value(-100500);
        writer.startArray();
        writer.value("hello");
        writer.startString();
        writer.write("world");
        writer.endString();
        writer.startObject();
        writer.key("testing");
        writer.startObject();
        writer.key("nested");
        writer.value("value");
        writer.endObject();
        writer.endObject();
        writer.endArray();
        writer.endArray();
        writer.key("test");
        writer.value(true);
        writer.key("small");
        writer.value(0.5d);
        writer.key("very-small");
        writer.value(-1e-40d);
        writer.key("big");
        writer.value(1e40d);
        writer.key("pretty");
        writer.value(1234567890.123456d);
        writer.endObject();
        writer.value(false);
        writer.endArray();
        Assert.assertEquals(
            "[\n"
            + "    null,\n"
            + "    {\n"
            + "        \"nil\": [\n"
            + "            null,\n"
            + "            -100500,\n"
            + "            [\n"
            + "                \"hello\",\n"
            + "                \"world\",\n"
            + "                {\n"
            + "                    \"testing\": {\n"
            + "                        \"nested\": \"value\"\n"
            + "                    }\n"
            + "                }\n"
            + "            ]\n"
            + "        ],\n"
            + "        \"test\": true,\n"
            + "        \"small\": 0.5,\n"
            + "        \"very-small\": -1.0E-40,\n"
            + "        \"big\": 1.0E40,\n"
            + "        \"pretty\": 1234567890.123456\n"
            + "    },\n"
            + "    false\n"
            + ']',
            new String(out.toByteArray(), StandardCharsets.UTF_8));
    }
    // CSON: MagicNumber

    @Test
    public void testRootNumber() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new HumanReadableUtf8JsonWriter(out).value(2);
        Assert.assertEquals(
            "2",
            new String(out.toByteArray(), StandardCharsets.UTF_8));
    }
}

