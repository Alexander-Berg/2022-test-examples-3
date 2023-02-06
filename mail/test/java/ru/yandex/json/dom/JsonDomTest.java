package ru.yandex.json.dom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.function.BasicGenericConsumer;
import ru.yandex.json.parser.ExceptionHandlingContentHandler;
import ru.yandex.json.parser.JsonException;
import ru.yandex.json.parser.JsonParser;
import ru.yandex.json.parser.SparseStringCollector;
import ru.yandex.json.parser.StackContentHandler;

public class JsonDomTest {
    private static final String TWO = "2";

    @Test
    public void testSimple() throws Exception {
        BasicGenericConsumer<Object, JsonException> consumer =
            new BasicGenericConsumer<>();
        StackContentHandler handler = new StackContentHandler();
        handler.push(
            new ValueContentHandler(
                consumer,
                new SparseStringCollector(),
                BasicContainerFactory.INSTANCE));
        JsonParser parser = new JsonParser(
            new ExceptionHandlingContentHandler(handler, handler));
        parser.process("[{}, {\"hel".toCharArray());
        parser.process("lo\": 1, \"world\": null, \"str\": \"h".toCharArray());
        Assert.assertNull(consumer.get());
        parser.process("ere\"}]".toCharArray());
        parser.eof();
        List<Object> list = new ArrayList<>();
        list.add(Collections.emptyMap());
        Map<String, Object> map = new HashMap<>();
        map.put("hello", 1L);
        map.put("world", null);
        map.put("str", "here");
        list.add(map);
        Assert.assertEquals(list, consumer.get());
    }

    @Test
    public void testComplex() throws Exception {
        BasicGenericConsumer<Object, JsonException> consumer =
            new BasicGenericConsumer<>();
        StackContentHandler handler = new StackContentHandler();
        handler.push(
            new ValueContentHandler(
                consumer,
                new SparseStringCollector(),
                BasicContainerFactory.INSTANCE));
        JsonParser parser = new JsonParser(
            new ExceptionHandlingContentHandler(handler, handler));
        parser.process(
            "{\"1\": [{},0.5], \"2\": {\"3\":0,\"5\":[]}, \"6\":true"
                .toCharArray());
        Assert.assertNull(consumer.get());
        parser.process(",\"7\":-9223372036854775808}".toCharArray());
        parser.eof();
        Map<String, Object> map = new HashMap<>();
        List<Object> list = new ArrayList<>();
        list.add(Collections.emptyMap());
        final double half = 0.5;
        list.add(half);
        map.put("1", list);
        Map<String, Object> submap = new HashMap<>();
        submap.put("3", 0L);
        submap.put("5", Collections.emptyList());
        map.put(TWO, submap);
        map.put("6", true);
        map.put("7", Long.MIN_VALUE);
        Assert.assertEquals(map, consumer.get());
    }

    @Test
    public void testRootPrimitives() throws Exception {
        Assert.assertEquals(Boolean.TRUE, ValueContentHandler.parse("true"));
        Assert.assertEquals(Boolean.FALSE, ValueContentHandler.parse("false"));
        Assert.assertNull(ValueContentHandler.parse("null"));
        Assert.assertEquals("hell", ValueContentHandler.parse("$hell\000"));
        Assert.assertEquals(2L, ValueContentHandler.parse(TWO));
        Assert.assertEquals(
            "test",
            ValueContentHandler.parse("\"t\\u0065st\""));
    }

    @Test
    public void testDeep() throws Exception {
        final int depth = 1000;
        List<Object> root = new ArrayList<>();
        List<Object> current = root;
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < depth; ++i) {
            sb.append('[');
            List<Object> next = new ArrayList<>();
            current.add(next);
            current = next;
        }
        for (int i = 0; i <= depth; ++i) {
            sb.append(']');
        }
        Assert.assertEquals(root, ValueContentHandler.parse(new String(sb)));
    }
}

