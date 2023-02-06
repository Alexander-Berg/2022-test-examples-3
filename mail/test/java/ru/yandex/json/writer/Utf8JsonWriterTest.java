package ru.yandex.json.writer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.test.util.TestBase;

public class Utf8JsonWriterTest extends TestBase {
    private static final String NIL = "nil";
    private static final String JOPA =
        "\"王\"";
    private static final String JOPA_ESCAPED =
        "\\\"王\\\"";
    private static final int INT = -100500;

    private static class IntWrapper implements Utf8JsonValue {
        private final int value;

        IntWrapper(final int value) {
            this.value = value;
        }

        @Override
        public void writeValue(final Utf8JsonWriter writer) throws IOException {
            writer.value(value);
        }
    }

    private static class ToStringable {
        @Override
        public String toString() {
            return "hi!";
        }
    }

    public Utf8JsonWriterTest() {
        super(false, 0L);
    }

    @Test
    public void test() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Utf8JsonWriter writer = new Utf8JsonWriter(out);
        writer.startArray();
        writer.value((Object) null);
        writer.value('a');
        writer.value(true);
        writer.value(INT);
        writer.value("王");
        writer.startObject();
        writer.key(NIL);
        writer.startObject();
        writer.key("val");
        writer.value((Object) null);
        writer.endObject();
        writer.key("hi");
        writer.startString();
        writer.write('t');
        writer.write("h\"ere");
        writer.endString();
        writer.endObject();
        writer.startString();
        writer.write("hello\000 again\000");
        writer.endString();
        writer.startObject();
        writer.key("hello\b\f");
        writer.value("world\000\n\032");
        writer.key("passport_display_name");
        writer.value("王");
        writer.endObject();
        writer.value(false);
        writer.endArray();
        Assert.assertEquals(
            "[null,\"a\",true,-100500,\"王\","
            + "{\"nil\":{\"val\":null},\"hi\":\"th\\\"ere\"},\"hello\\u0000 "
            + "again\\u0000\",{\"hello\\b\\f\":\"world\\u0000\\n\\u001A\""
            + ",\"passport_display_name\":\"王\"},"
            + "false]",
            new String(out.toByteArray(), StandardCharsets.UTF_8));
        out.reset();
        writer.reset();
        writer.startObject();
        writer.endObject();
        Assert.assertEquals(
            "{}",
            new String(out.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    public void testJopa() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Utf8JsonWriter writer = new Utf8JsonWriter(out);
        writer.startObject();
        writer.key("data");
        writer.value(JOPA);
        writer.endObject();
        Assert.assertEquals(
            "{\"data\":\"" + JOPA_ESCAPED + "\"}",
            new String(out.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    public void testComplex() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Utf8JsonWriter writer = new Utf8JsonWriter(out);
        Map<Object, Object> map = new TreeMap<>();
        map.put(
            INT,
            Arrays.asList(
                "hell",
                "worl",
                null,
                new ToStringable(),
                Arrays.asList("how", "are", "you", '?', new IntWrapper(2))));
        writer.startArray();
        writer.value((Object) null);
        writer.value(true);
        writer.value(map);
        map.clear();
        map.put("key1", "value1");
        map.put("key2", "value2");
        map.put("key3", Arrays.asList(0, 1, 2));
        writer.value(map);
        map = null;
        writer.value(map);
        writer.value((Integer) null);
        writer.value((Iterable) null);
        writer.value((Iterator) null);
        writer.value((JsonValue) null);
        writer.value((Object) null);
        writer.endArray();
        Assert.assertEquals("[null,true,{"
            + "\"-100500\":[\"hell\",\"worl\",null,\"hi!\","
            + "[\"how\",\"are\",\"you\",\"?\",2]]},"
            + "{\"key1\":\"value1\",\"key2\":\"value2\","
            + "\"key3\":[0,1,2]},null,null,null,null,null,null]",
            new String(out.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    public void testLucene() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Utf8JsonWriter writer = new Utf8JsonWriter(out);
        Map<Object, Object> map = new LinkedHashMap<>();
        map.put("prefix", INT);
        Map<Object, Object> doc = new LinkedHashMap<Object, Object>();
        doc.put(
            "sort",
            Arrays.asList(
                "hell2",
                "worl2",
                null,
                new ToStringable(),
                1,
                2,
                1 + 2));
        map.put("docs", Arrays.asList(doc));
        writer.value(map);
        Assert.assertEquals("{\"prefix\":-100500,\"docs\":[{\"sort\":"
            + "[\"hell2\",\"worl2\",null,\"hi!\",1,2,3]}]}",
            new String(out.toByteArray(), StandardCharsets.UTF_8));

        out.reset();
        writer.reset();
        writer.startObject();
        writer.key("prefix2");
        writer.value(INT);
        writer.key("docs2");
        writer.startArray();
        writer.startObject();
        writer.key("sort2");
        writer.startArray();
        writer.value(1, true);
        writer.value(new byte[]{(byte) '3'});
        writer.value(2, true);
        writer.value(1 + 2, true);
        writer.endArray();
        writer.endObject();
        writer.endArray();
        writer.endObject();
        Assert.assertEquals("{\"prefix2\":-100500,\"docs2\":[{\"sort2\":"
            + "[\"1\",\"3\",\"2\",\"3\"]}]}",
            new String(out.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    public void testDeepJson() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Utf8JsonWriter writer = new Utf8JsonWriter(out);
        final int depth = 9;
        for (int i = 0; i < depth; ++i) {
            writer.startObject();
            writer.key(new StringBuilder().append(i));
        }
        writer.startArray();
        writer.value("abc");
        writer.value("def");
        writer.endArray();
        for (int i = 0; i < depth; ++i) {
            writer.endObject();
        }
        Assert.assertEquals(
            "{\"0\":{\"1\":{\"2\":{\"3\":{\"4\":{\"5\":{\"6\":"
                + "{\"7\":{\"8\":[\"abc\",\"def\"]}}}}}}}}}",
            new String(out.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    public void testRootNull() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new HumanReadableUtf8JsonWriter(out).nullValue();
        Assert.assertEquals(
            "null",
            new String(out.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    public void testNaNs() throws IOException {
        float f1 = Float.NaN;
        float f2 = Float.POSITIVE_INFINITY;
        double d1 = Double.NaN;
        double d2 = Double.NEGATIVE_INFINITY;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Utf8JsonWriter writer = new Utf8JsonWriter(out);
        writer.value(
            Arrays.asList(
                Float.valueOf(f1),
                Float.valueOf(f2),
                Double.valueOf(d1),
                Double.valueOf(d2)));
        Assert.assertEquals(
            "[null,null,null,null]",
            new String(out.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test(expected = IllegalStateException.class)
    public void testMalformed1() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Utf8JsonWriter writer = new Utf8JsonWriter(out);
        writer.startArray();
        writer.startObject();
        writer.value('a');
    }

    @Test(expected = IllegalStateException.class)
    public void testMalformed2() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Utf8JsonWriter writer = new Utf8JsonWriter(out);
        writer.startArray();
        writer.key("a");
    }

    @Test(expected = IllegalStateException.class)
    public void testMalformed3() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Utf8JsonWriter writer = new Utf8JsonWriter(out);
        writer.startObject();
        writer.startArray();
    }

    @Test(expected = IllegalStateException.class)
    public void testMalformedClose() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Utf8JsonWriter writer = new Utf8JsonWriter(out)) {
            writer.startArray();
        }
    }

    @Test
    public void testJsonValue() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Utf8JsonWriter writer = new Utf8JsonWriter(out)) {
            writer.startObject();
            writer.key("data");
            writer.jsonValue("{\"key\":[\"value\"]}");
            writer.key("data2");
            writer.jsonValue("{\"key2\":\"value2\"}");
            writer.endObject();
        }
        Assert.assertEquals(
            "{\"data\":{\"key\":[\"value\"]},\"data2\":{\"key2\":\"value2\"}}",
            new String(out.toByteArray(), StandardCharsets.UTF_8));
    }
}

