package ru.yandex.json.parser;

import java.io.StringReader;
import java.io.StringWriter;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.json.dom.CopyHandler;
import ru.yandex.json.writer.HumanReadableJsonWriter;
import ru.yandex.json.writer.JsonWriter;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class JsonParserTest extends TestBase {
    private static final String NULL = "null";
    private static final String INTEGER_UNDERFLOW_OCCURED =
        "Integer underflow occured";

    @Test
    public void test() throws Exception {
        StringWriter sw = new StringWriter();
        String str =
            "{\"hello\":\"world\", \"how\\\"are\\u0022\": "
            + "[\"you\", {\"a\\nd\": \"me\", \"for\":{\"ever\":\"here\"}},"
            + "\t[], 123,\r\n{}, {$key\0: $value\0\0here\0}],\n\"end\\f\":"
            + "\"here\"}";
        String expected =
            "{\n\t\"hello\": \"world\",\n"
            + "\t\"how\\\"are\\\"\": [\n\t\t\"you\",\n\t\t{\n\t\t\t"
            + "\"a\\nd\": \"me\",\n\t\t\t\"for\": {\n\t\t\t\t"
            + "\"ever\": \"here\"\n\t\t\t}\n\t\t},\n\t\t[\n\n\t\t],"
            + "\n\t\t123,\n\t\t{\n\n\t\t},\n\t\t{\n\t\t\t\"key\": "
            + "\"value\\u0000here\"\n\t\t}\n\t],\n\t\"end\\f\": "
            + "\"here\"\n}";
        String ident = "\t";
        try (JsonWriter writer = new HumanReadableJsonWriter(sw, ident)) {
            new JsonParser(new CopyHandler(writer)).parse(str);
            Assert.assertEquals(expected, sw.toString());
        }
        sw = new StringWriter();
        try (JsonWriter writer = new HumanReadableJsonWriter(sw, ident)) {
            new JsonParser(new CopyHandler(writer))
                .parse(new StringReader(str));
            Assert.assertEquals(expected, sw.toString());
        }
    }

    @Test
    public void testEdges() throws Exception {
        StringWriter sw = new StringWriter();
        try (JsonWriter writer = new JsonWriter(sw)) {
            JsonParser parser = new JsonParser(new CopyHandler(writer));
            parser.process("[$hei".toCharArray());
            parser.process("l\0\0o\0".toCharArray());
            parser.process(", \"wor\\".toCharArray());
            parser.process("td\",$h\0".toCharArray());
            parser.process("\0w\0,true,fal".toCharArray());
            parser.process("se,nu".toCharArray());
            parser.process("ll".toCharArray());
            parser.process(", \"\\u00".toCharArray());
            parser.process("2c\\u".toCharArray());
            parser.process("002".toCharArray());
            parser.process("c\",1".toCharArray());
            parser.process("12, \"l\\b\\rat".toCharArray());
            parser.process("e\",-".toCharArray());
            parser.process("12,1.".toCharArray());
            parser.process("000e".toCharArray());
            parser.process("+".toCharArray());
            parser.process("000".toCharArray());
            parser.process("    ".toCharArray());
            parser.process(",0".toCharArray());
            parser.process(",1e".toCharArray());
            parser.process("-2,-0.01,0e0".toCharArray());
            parser.process(",[[[[[[[[[[[[[[[[[[[[".toCharArray());
            parser.process("5]]]]]]]]]]]]]]]]]]]]]".toCharArray());
            parser.eof();
            Assert.assertEquals("[\"heil\\u0000o\",\"wor\\td\",\"h\\u0000w\","
                + "true,false,null,\",,\",112,\"l\\b\\rate\",-12,1,0,0.01,"
                + "-0.01,0,[[[[[[[[[[[[[[[[[[[[5]]]]]]]]]]]]]]]]]]]]]",
                sw.toString());
        }
    }

    @Test
    public void testRootValues() throws Exception {
        StringWriter sw;
        for (String example: new String[]{
            "123",
            "0",
            "1.0E128",
            "true",
            "false",
            NULL,
            "\"text\"",
            "\"te\\\"st\""})
        {
            System.out.println("Testing: " + example);
            sw = new StringWriter();
            try (JsonWriter writer = new JsonWriter(sw)) {
                new JsonParser(new CopyHandler(writer)).parse(example);
                Assert.assertEquals(example, sw.toString());
            }
        }
        sw = new StringWriter();
        try (JsonWriter writer = new JsonWriter(sw)) {
            new JsonParser(new CopyHandler(writer)).parse("$test\000");
            Assert.assertEquals("\"test\"", sw.toString());
        }
        sw = new StringWriter();
        try (JsonWriter writer = new JsonWriter(sw)) {
            new JsonParser(new CopyHandler(writer)).parse("$te\000\000st\000");
            Assert.assertEquals("\"te\\u0000st\"", sw.toString());
        }
    }

    @Test
    public void testNoInput() {
        JsonParser parser = new JsonParser(null);
        try {
            parser.eof();
            Assert.fail();
        } catch (JsonException e) {
            return;
        }
    }

    @Test
    public void testExceptionsBypassing() {
        StackContentHandler handler = new StackContentHandler();
        handler.push(new BadContentHandler());
        try {
            new JsonParser(
                new ExceptionHandlingContentHandler(handler, handler))
                .process("\"bad string\"".toCharArray());
            Assert.fail();
        } catch (JsonException e) {
            Assert.assertEquals(JsonException.class, e.getClass());
            Assert.assertNull(e.getCause());
            Assert.assertEquals(1, e.getSuppressed().length);
            YandexAssert.assertContains("'bad string'", e.getMessage());
        }
        try {
            new JsonParser(handler)
                .process("\"incomplete string".toCharArray());
            Assert.fail();
        } catch (JsonException e) {
            Assert.assertEquals(JsonException.class, e.getClass());
            Assert.assertNull(e.getCause());
            YandexAssert.assertContains(
                "'incomplete string…'",
                e.getMessage());
        }
        handler.pop();
        handler.push(
            new BadContentHandler() {
                @Override
                public void value(final long value) throws JsonException {
                    Assert.assertEquals(2L, value);
                    throw new MalformedJsonException(0, 0, 0);
                }
            });
        try {
            new JsonParser(
                new ExceptionHandlingContentHandler(handler, handler))
                .parse("2");
            Assert.fail();
        } catch (JsonException e) {
            Assert.assertEquals(MalformedJsonException.class, e.getClass());
            Assert.assertEquals(1, e.getSuppressed().length);
            YandexAssert.assertContains("Unexpected token", e.getMessage());
        }
    }

    // CSOFF: MethodLength
    @Test
    public void testParseDouble() throws Exception {
        // Test valid input parsing
        StringWriter sw = new StringWriter();
        try (JsonWriter writer = new JsonWriter(sw)) {
            JsonParser parser = new JsonParser(new CopyHandler(writer));
            parser.parse("1.250e0");
        }
        Assert.assertEquals("1.25", sw.toString());

        sw = new StringWriter();
        try (JsonWriter writer = new JsonWriter(sw)) {
            JsonParser parser = new JsonParser(new CopyHandler(writer));
            parser.parse("0.00625e1".toCharArray());
        }
        Assert.assertEquals("0.0625", sw.toString());

        sw = new StringWriter();
        try (JsonWriter writer = new JsonWriter(sw)) {
            JsonParser parser = new JsonParser(new CopyHandler(writer));
            parser.parse("0.03125000000000000000000001");
        }
        Assert.assertEquals("0.03125", sw.toString());

        sw = new StringWriter();
        try (JsonWriter writer = new JsonWriter(sw)) {
            JsonParser parser = new JsonParser(new CopyHandler(writer));
            parser.parse("0.010000000000000000000");
        }
        Assert.assertEquals("0.01", sw.toString());

        sw = new StringWriter();
        String value = "0.018446744073709551617";
        try (JsonWriter writer = new JsonWriter(sw)) {
            JsonParser parser = new JsonParser(new CopyHandler(writer));
            parser.parse(value);
        }
        Assert.assertEquals(
            Double.toString(Double.parseDouble(value)),
            sw.toString());

        sw = new StringWriter();
        try (JsonWriter writer = new JsonWriter(sw)) {
            JsonParser parser = new JsonParser(new CopyHandler(writer));
            parser.parse("10000000000000000000e2");
        }
        Assert.assertEquals("1.0E21", sw.toString());

        sw = new StringWriter();
        try (JsonWriter writer = new JsonWriter(sw)) {
            JsonParser parser = new JsonParser(new CopyHandler(writer));
            parser.parse(Long.toString(Long.MAX_VALUE));
        }
        Assert.assertEquals(Long.toString(Long.MAX_VALUE), sw.toString());

        sw = new StringWriter();
        try (JsonWriter writer = new JsonWriter(sw)) {
            JsonParser parser = new JsonParser(new CopyHandler(writer));
            parser.parse(Long.toString(Long.MIN_VALUE));
        }
        Assert.assertEquals(Long.toString(Long.MIN_VALUE), sw.toString());

        sw = new StringWriter();
        try (JsonWriter writer = new JsonWriter(sw)) {
            JsonParser parser = new JsonParser(new CopyHandler(writer));
            parser.parse(Long.toString(0L));
        }
        Assert.assertEquals(Long.toString(0L), sw.toString());

        sw = new StringWriter();
        try (JsonWriter writer = new JsonWriter(sw)) {
            JsonParser parser = new JsonParser(new CopyHandler(writer));
            parser.parse('-' + Long.toString(0L));
        }
        Assert.assertEquals(Long.toString(0L), sw.toString());

        sw = new StringWriter();
        try (JsonWriter writer = new JsonWriter(sw)) {
            JsonParser parser = new JsonParser(new CopyHandler(writer));
            parser.parse("0.0");
        }
        Assert.assertEquals(Long.toString(0L), sw.toString());

        sw = new StringWriter();
        try (JsonWriter writer = new JsonWriter(sw)) {
            JsonParser parser = new JsonParser(new CopyHandler(writer));
            parser.parse("-0.0");
        }
        Assert.assertEquals(Long.toString(0L), sw.toString());

        sw = new StringWriter();
        try (JsonWriter writer = new JsonWriter(sw)) {
            JsonParser parser = new JsonParser(new CopyHandler(writer));
            parser.parse("0.5e9223372036854775808");
        }
        // Infinite values replaced with nulls
        Assert.assertEquals(NULL, sw.toString());

        sw = new StringWriter();
        try (JsonWriter writer = new JsonWriter(sw)) {
            JsonParser parser = new JsonParser(new CopyHandler(writer));
            parser.parse("0.5e12223372036854775808");
        }
        Assert.assertEquals(NULL, sw.toString());

        sw = new StringWriter();
        try (JsonWriter writer = new JsonWriter(sw)) {
            JsonParser parser = new JsonParser(new CopyHandler(writer));
            parser.parse("0.5e18446744073709551617");
        }
        Assert.assertEquals(NULL, sw.toString());

        sw = new StringWriter();
        try (JsonWriter writer = new JsonWriter(sw)) {
            JsonParser parser = new JsonParser(new CopyHandler(writer));
            parser.parse("0.5e28446744073709551614");
        }
        Assert.assertEquals(NULL, sw.toString());

        // Test malformed input
        sw = new StringWriter();
        try (JsonWriter writer = new JsonWriter(sw)) {
            JsonParser parser = new JsonParser(new CopyHandler(writer));
            parser.parse("2.-8696743073709552364e0");
            Assert.fail();
        } catch (JsonException e) {
            Assert.assertEquals(MalformedJsonException.class, e.getClass());
        }

        // Test overflows
        sw = new StringWriter();
        value = "9223372036854775808";
        try (JsonWriter writer = new JsonWriter(sw)) {
            JsonParser parser = new JsonParser(new CopyHandler(writer));
            parser.parse(value);
        }
        Assert.assertEquals(
            '"' + Long.toUnsignedString(Long.parseUnsignedLong(value)) + '"',
            sw.toString());

        sw = new StringWriter();
        try (JsonWriter writer = new JsonWriter(sw)) {
            JsonParser parser = new JsonParser(new CopyHandler(writer));
            parser.parse("-9223372036854775809");
            Assert.fail();
        } catch (JsonException e) {
            Assert.assertEquals(MalformedJsonException.class, e.getClass());
            Assert.assertEquals(INTEGER_UNDERFLOW_OCCURED, e.getMessage());
        }

        sw = new StringWriter();
        try (JsonWriter writer = new JsonWriter(sw)) {
            JsonParser parser = new JsonParser(new CopyHandler(writer));
            parser.parse("100000000000000000000");
            Assert.fail();
        } catch (JsonException e) {
            Assert.assertEquals(MalformedJsonException.class, e.getClass());
            Assert.assertEquals(
                "Integer overflow at position 21",
                e.getMessage());
        }

        sw = new StringWriter();
        try (JsonWriter writer = new JsonWriter(sw)) {
            JsonParser parser = new JsonParser(new CopyHandler(writer));
            parser.parse("18446744073709551617");
            Assert.fail();
        } catch (JsonException e) {
            Assert.assertEquals(MalformedJsonException.class, e.getClass());
            Assert.assertEquals(
                "Integer overflow at position 20",
                e.getMessage());
        }
    }
    // CSON: MethodLength

    @Test
    public void testAutoReset() throws Exception {
        JsonParser parser = new JsonParser(null, true);
        parser.eof();

        StringWriter sw = new StringWriter();
        try (JsonWriter writer = new JsonWriter(sw)) {
            writer.startArray();
            new JsonParser(new CopyHandler(writer), true)
                .parse("1 2");
            writer.endArray();
            Assert.assertEquals("[1,2]", sw.toString());
        }

        sw = new StringWriter();
        try (JsonWriter writer = new JsonWriter(sw)) {
            writer.startArray();
            new JsonParser(new CopyHandler(writer), true)
                .parse("{\"key\":\"value\"}");
            writer.endArray();
            Assert.assertEquals("[{\"key\":\"value\"}]", sw.toString());
        }

        sw = new StringWriter();
        try (JsonWriter writer = new JsonWriter(sw)) {
            writer.startArray();
            new JsonParser(new CopyHandler(writer), true)
                .parse("{\"key\":\"value\"} {\"key\":\"value\"}");
            writer.endArray();
            Assert.assertEquals(
                "[{\"key\":\"value\"},{\"key\":\"value\"}]",
                sw.toString());
        }

        sw = new StringWriter();
        try (JsonWriter writer = new JsonWriter(sw)) {
            writer.startArray();
            new JsonParser(new CopyHandler(writer), true)
                .parse("\t{\"k\":\"v\"} {\"k\":\"v\"}\n\t"
                    + "{\"k1\":\"v1\", \"k2\":\"v2\"} ");
            writer.endArray();
            Assert.assertEquals(
                "[{\"k\":\"v\"},{\"k\":\"v\"},"
                    + "{\"k1\":\"v1\",\"k2\":\"v2\"}]",
                sw.toString());
        }
    }
}

