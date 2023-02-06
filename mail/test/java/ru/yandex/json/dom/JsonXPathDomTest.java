package ru.yandex.json.dom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.function.GenericConsumer;
import ru.yandex.json.parser.ExceptionHandlingContentHandler;
import ru.yandex.json.parser.JsonException;
import ru.yandex.json.parser.JsonParser;
import ru.yandex.json.parser.StackContentHandler;
import ru.yandex.json.parser.StringCollectorFactory;
import ru.yandex.json.xpath.AnyIndexMatcher;
import ru.yandex.json.xpath.AnyNameMatcher;
import ru.yandex.json.xpath.IndexMatcher;
import ru.yandex.json.xpath.ListHandlerResolver;
import ru.yandex.json.xpath.NameMatcher;
import ru.yandex.json.xpath.XPathStackContentHandler;

public class JsonXPathDomTest {
    @Test
    public void test() throws Exception {
        final List<Object> output = new ArrayList<>();
        GenericConsumer<Object, JsonException> consumer =
            new GenericConsumer<Object, JsonException>() {
                @Override
                public void accept(final Object object) {
                    output.add(object);
                }
            };
        PopValueContentHandler dataHandler =
            new PopValueContentHandler(consumer);
        StackContentHandler stackHandler = new StackContentHandler();
        dataHandler.stackContentHandler(stackHandler);
        XPathStackContentHandler xpathContentHandler =
            new XPathStackContentHandler(
                new ListHandlerResolver<>(
                    dataHandler,
                    AnyIndexMatcher.INSTANCE,
                    new NameMatcher("data")),
                stackHandler,
                StringCollectorFactory.INSTANCE.create());
        stackHandler.push(xpathContentHandler);
        JsonParser parser =
            new JsonParser(
                new ExceptionHandlingContentHandler(
                    new ExceptionHandlingContentHandler(
                        stackHandler,
                        stackHandler),
                    xpathContentHandler));
        parser.parse(
            "[{\"data\":\"hello\",\"ignored\":\"world\"},\"ignored_too\","
            + "{\"data\":2},{\"data\":{\"key1\":1,\"key2\":\"value\"}}]");
        List<Object> expected = new ArrayList<>();
        expected.add("hello");
        expected.add(2L);
        Map<String, Object> map = new HashMap<>();
        map.put("key1", 1L);
        map.put("key2", "value");
        expected.add(map);
        Assert.assertEquals(expected, output);
        output.clear();
        parser.parse("{\"data\":[\"test\"]}");
        Assert.assertEquals(Collections.emptyList(), output);

        xpathContentHandler =
            new XPathStackContentHandler(
                new ListHandlerResolver<>(
                    dataHandler,
                    new IndexMatcher(1),
                    AnyNameMatcher.INSTANCE),
                stackHandler,
                StringCollectorFactory.INSTANCE.create());
        stackHandler.push(xpathContentHandler);
        parser = new JsonParser(
            new ExceptionHandlingContentHandler(
                new ExceptionHandlingContentHandler(
                    stackHandler,
                    stackHandler),
                xpathContentHandler));
        parser.parse("[{\"data\":\"hello\",\"ignored\":[\"w\"]},[1],[2]]");
        Assert.assertEquals(Collections.emptyList(), output);
        parser.parse("{\"data\":[1,2,3]}");
        Assert.assertEquals(Collections.emptyList(), output);
        parser.parse("[\"value\",[1], null, 0.5, true]");
        Assert.assertEquals(Collections.emptyList(), output);

        parser.parse(
            "[{\"a\":\"\"},{\"c\":\"d\",\"e\":{},\"f\":[\"g\"],\"h\":null},"
            + "{\"i\":2}]");
        expected.clear();
        expected.add("d");
        expected.add(Collections.emptyMap());
        expected.add(Collections.singletonList("g"));
        expected.add(null);
        Assert.assertEquals(expected, output);
    }

    @Test
    public void testSplittedKey() throws Exception {
        final List<Object> output = new ArrayList<>();
        GenericConsumer<Object, JsonException> consumer =
            new GenericConsumer<Object, JsonException>() {
                @Override
                public void accept(final Object object) {
                    output.add(object);
                }
            };
        PopValueContentHandler dataHandler =
            new PopValueContentHandler(consumer);
        StackContentHandler stackHandler = new StackContentHandler();
        dataHandler.stackContentHandler(stackHandler);
        XPathStackContentHandler xpathContentHandler =
            new XPathStackContentHandler(
                new ListHandlerResolver<PopValueContentHandler>(
                    dataHandler,
                    new NameMatcher("key")),
                stackHandler,
                StringCollectorFactory.INSTANCE.create());
        stackHandler.push(xpathContentHandler);
        JsonParser parser =
            new JsonParser(
                new ExceptionHandlingContentHandler(
                    new ExceptionHandlingContentHandler(
                        stackHandler,
                        stackHandler),
                    xpathContentHandler));
        parser.process("{\"key\":\"value1\",\"ot".toCharArray());
        parser.process("h".toCharArray());
        parser.process("er\":\"value2\"}".toCharArray());
        parser.eof();
        Assert.assertEquals(Collections.singletonList("value1"), output);
    }

    @Test
    public void testSplittedValue() throws Exception {
        final List<Object> output = new ArrayList<>();
        GenericConsumer<Object, JsonException> consumer =
            new GenericConsumer<Object, JsonException>() {
                @Override
                public void accept(final Object object) {
                    output.add(object);
                }
            };
        PopValueContentHandler dataHandler =
            new PopValueContentHandler(consumer);
        StackContentHandler stackHandler = new StackContentHandler();
        dataHandler.stackContentHandler(stackHandler);
        XPathStackContentHandler xpathContentHandler =
            new XPathStackContentHandler(
                new ListHandlerResolver<>(
                    dataHandler,
                    new IndexMatcher(2)),
                stackHandler,
                StringCollectorFactory.INSTANCE.create());
        stackHandler.push(xpathContentHandler);
        JsonParser parser =
            new JsonParser(
                new ExceptionHandlingContentHandler(
                    new ExceptionHandlingContentHandler(
                        stackHandler,
                        stackHandler),
                    xpathContentHandler));
        parser.process("[\"a".toCharArray());
        parser.process("bc\",\"def\",\"ghi\"]".toCharArray());
        parser.eof();
        Assert.assertEquals(Collections.singletonList("ghi"), output);
    }

    @Test
    public void testTypesafe() throws Exception {
        JsonList output = new JsonList(BasicContainerFactory.INSTANCE);
        TypesafePopValueContentHandler dataHandler =
            new TypesafePopValueContentHandler(output);
        StackContentHandler stackHandler = new StackContentHandler();
        dataHandler.stackContentHandler(stackHandler);
        XPathStackContentHandler xpathContentHandler =
            new XPathStackContentHandler(
                new ListHandlerResolver<>(
                    dataHandler,
                    new NameMatcher("docs"),
                    AnyIndexMatcher.INSTANCE),
                stackHandler,
                StringCollectorFactory.INSTANCE.create());
        stackHandler.push(xpathContentHandler);
        JsonParser parser =
            new JsonParser(
                new ExceptionHandlingContentHandler(
                    new ExceptionHandlingContentHandler(
                        stackHandler,
                        stackHandler),
                    xpathContentHandler));
        parser.parse(
            "{\"pocs\":[\"azaza\",123],"
            + "\"docs\":[2,{\"key2_1\":1,\"key2_2\":\"value2\","
            + "\"key2_3\":[null,\"value3\"],\"key2_4\":0.5},1,false,0.5]}");
        Map<String, Object> map = new HashMap<>();
        map.put("key2_1", 1L);
        map.put("key2_2", "value2");
        String key23 = "key2_3";
        map.put(key23, Arrays.asList(null, "value3"));
        String key24 = "key2_4";
        map.put(key24, 1f / 2);
        Assert.assertEquals(
            JsonObject.adapt(
                output.containerFactory(),
                Arrays.asList(2L, map, 1, false, 1d / 2)),
            output);

        // Test deep copy
        JsonList copy = output.deepCopy();
        Assert.assertEquals(output, copy);

        JsonObject outputMap = output.get(1);
        JsonObject copyMap = copy.get(1);
        Assert.assertEquals(outputMap, copyMap);
        Assert.assertNotSame(outputMap, copyMap);
        JsonObject outputList = outputMap.get(key23);
        JsonObject copyList = copyMap.get(key23);
        Assert.assertEquals(outputList, copyList);
        Assert.assertNotSame(outputList, copyList);
        Assert.assertSame(outputList.get(0), copyList.get(0));
        Assert.assertSame(outputList.get(1), copyList.get(1));

        // Test getDouble
        Assert.assertEquals(
            Double.valueOf(1d / 2),
            Double.valueOf(copyMap.asMap().getDouble(key24)));
    }

    @Test
    public void testFilter() throws Exception {
        JsonObject unfiltered =
            TypesafeValueContentHandler.parse(
                "[null, {\"key\":\"value\", \"key2\": null}, null, "
                + "{\"key\":{\"key\":null,\"key2\":\"value2\"}}]");
        Assert.assertEquals(
            TypesafeValueContentHandler.parse(
                "[null, {\"key\":\"value\"}, null, "
                + "{\"key\":{\"key2\":\"value2\"}}]"),
            unfiltered.filter(
                NullObjectFilter.INSTANCE,
                BasicContainerFactory.INSTANCE));
    }
}

