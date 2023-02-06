package ru.yandex.json.xpath;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.json.parser.ExceptionHandlingContentHandler;
import ru.yandex.json.parser.JsonException;
import ru.yandex.json.parser.JsonParser;
import ru.yandex.json.parser.MalformedJsonException;
import ru.yandex.json.parser.SparseStringCollectorsFactory;

public class XPathContentHandlerTest {
    private static final String TEXT = "text";
    private static final String HELLO = "hello";

    @Test
    public void test() throws Exception {
        final StringBuilder sb = new StringBuilder();
        new JsonParser(
            new XPathContentHandler(
                new PrimitiveHandler() {
                    @Override
                    public void handle(
                        final List<PathComponent> path,
                        final Object value)
                    {
                        Assert.assertEquals(HELLO, value);
                        Assert.assertEquals(1, path.size());
                        Assert.assertEquals(TEXT, path.get(0).name());
                        sb.append(TEXT);
                    }
                },
                SparseStringCollectorsFactory.INSTANCE.apply(-1L)))
            .parse("{\"text\":\"hello\"}");
        Assert.assertEquals(TEXT, sb.toString());
        sb.setLength(0);

        new JsonParser(
            new XPathContentHandler(
                new CompositePrimitiveHandler(
                    Collections.singletonList(
                        new ListHandlerResolver<>(
                            new PrimitiveHandler() {
                                @Override
                                public void handle(
                                    final List<PathComponent> path,
                                    final Object value)
                                {
                                    Assert.assertEquals(2L, value);
                                    Assert.assertEquals(1, path.size());
                                    Assert.assertNull(path.get(0).name());
                                    Assert.assertEquals(
                                        2,
                                        path.get(0).index());
                                    sb.append(TEXT);
                                }
                            },
                            new IndexMatcher(2)))),
                SparseStringCollectorsFactory.INSTANCE.apply(-1L)))
            .parse("[0,{},2]");
        Assert.assertEquals(TEXT, sb.toString());
    }

    @Test
    public void testCompositeHandler() throws Exception {
        final StringBuilder sb = new StringBuilder();
        JsonParser parser =
            new JsonParser(
                new XPathContentHandler(
                    new CompositePrimitiveHandler(
                        Arrays.asList(
                            new ListHandlerResolver<>(
                                new PrimitiveHandler() {
                                    @Override
                                    public void handle(
                                        final List<PathComponent> path,
                                        final Object value)
                                    {
                                        sb.append("\nfirst: " + value);
                                    }
                                },
                                AnyIndexMatcher.INSTANCE,
                                new NameMatcher(TEXT),
                                new IndexMatcher(2),
                                new NameMatcher(HELLO)),
                            new ListHandlerResolver<>(
                                new PrimitiveHandler() {
                                    @Override
                                    public void handle(
                                        final List<PathComponent> path,
                                        final Object value)
                                    {
                                        sb.append("\nsecond: " + value);
                                    }
                                },
                                new IndexMatcher(2),
                                new NameMatcher(HELLO)),
                            new ListHandlerResolver<>(
                                new PrimitiveHandler() {
                                    @Override
                                    public void handle(
                                        final List<PathComponent> path,
                                        final Object value)
                                    {
                                        sb.append("\nthird: " + value);
                                    }
                                },
                                new IndexMatcher(2 + 1)),
                            new ListHandlerResolver<>(
                                new PrimitiveHandler() {
                                    @Override
                                    public void handle(
                                        final List<PathComponent> path,
                                        final Object value)
                                    {
                                        sb.append("\nforth: " + value);
                                    }
                                },
                                new IndexMatcher(2 + 2)))),
                    SparseStringCollectorsFactory.INSTANCE.apply(-1L)));
        parser.process("[\"hello\",1,{\"text".toCharArray());
        parser.process("\":[2,[],{\"hello\":\"text\"}],\"hel".toCharArray());
        parser.process("lo\":\"again\"},4, 0.5]".toCharArray());
        parser.eof();
        Assert.assertEquals(
            "\nfirst: text\nsecond: again\nthird: 4\nforth: 0.5",
            sb.toString());
    }

    @Test
    public void testPrefixExtract() throws Exception {
        final StringBuilder sb = new StringBuilder();
        new JsonParser(
            new XPathContentHandler(
                new CompositePrimitiveHandler(
                    ReportingPrimitiveHandler.INSTANCE,
                    Arrays.asList(
                        new ListHandlerResolver<>(
                            new PrimitiveHandler() {
                                @Override
                                public void handle(
                                    final List<PathComponent> path,
                                    final Object value)
                                {
                                    sb.append("\nprefix = ");
                                    sb.append(value);
                                }
                            },
                            new NameMatcher("prefix")),
                        new ListHandlerResolver<>(
                            new PrimitiveHandler() {
                                @Override
                                public void handle(
                                    final List<PathComponent> path,
                                    final Object value)
                                {
                                    sb.append("action = ");
                                    sb.append(value);
                                }
                            },
                            new NameMatcher("action")),
                        new ListHandlerResolver<>(
                            new PrimitiveHandler() {
                                @Override
                                public void handle(
                                    final List<PathComponent> path,
                                    final Object value)
                                {
                                    sb.append('\n');
                                    sb.append(path.get(2).name());
                                    sb.append(" = ");
                                    sb.append(value);
                                }
                            },
                            new NameMatcher("docs"),
                            AnyIndexMatcher.INSTANCE,
                            AnyNameMatcher.INSTANCE,
                            new NameMatcher("value")))),
                SparseStringCollectorsFactory.INSTANCE.apply(-1L)))
            .parse("{\"action\": \"add\",\"docs\": [{\"mimetype\": "
                + "{\"value\":\"text/plain\"},\n\"ctime\": {\"value\": "
                + "1360069777},\"version\":{\"value\":1}}],\n"
                + "\"prefix\": 89031628}");
        Assert.assertEquals("action = add\nmimetype = text/plain\n"
            + "ctime = 1360069777\nversion = 1\nprefix = 89031628",
            sb.toString());
    }

    @Test
    public void testDeepArray() throws Exception {
        final int depth = 20;
        final StringBuilder sb = new StringBuilder();
        new JsonParser(
            new XPathContentHandler(
                new PrimitiveHandler() {
                    @Override
                    public void handle(
                        final List<PathComponent> path,
                        final Object value)
                    {
                        Assert.assertEquals(depth, path.size());
                        sb.append(value);
                    }
                },
                SparseStringCollectorsFactory.INSTANCE.apply(-1L)))
            .parse("[[[[[[[[[[[[[[[[[[[[\"hi\"]]]]]]]]]]]]]]]]]]]]");
        Assert.assertEquals("hi", new String(sb));
    }

    @Test
    public void testEmptyObjectInArray() throws Exception {
        new JsonParser(
            new XPathContentHandler(
                new PrimitiveHandler() {
                    @Override
                    public void handle(
                        final List<PathComponent> path,
                        final Object value)
                    {
                        Assert.fail();
                    }
                },
                SparseStringCollectorsFactory.INSTANCE.apply(-1L)))
            .parse("[{}]");
    }

    @Test
    public void testRootPrimitive() throws Exception {
        final StringBuilder sb = new StringBuilder();
        new JsonParser(
            new XPathContentHandler(new PrimitiveHandler() {
                @Override
                public void handle(
                    final List<PathComponent> path,
                    final Object value)
                {
                    Assert.assertEquals(HELLO, value);
                    Assert.assertEquals(Collections.emptyList(), path);
                    sb.append(TEXT);
                }
            },
            SparseStringCollectorsFactory.INSTANCE.apply(-1L)))
            .parse("\"hello\"");
        Assert.assertEquals(TEXT, sb.toString());
        sb.setLength(0);

        new JsonParser(
            new XPathContentHandler(
                new PrimitiveHandler() {
                    @Override
                    public void handle(
                        final List<PathComponent> path,
                        final Object value)
                    {
                        Assert.assertEquals(true, value);
                        Assert.assertEquals(Collections.emptyList(), path);
                        sb.append(TEXT);
                    }
                },
                SparseStringCollectorsFactory.INSTANCE.apply(-1L)))
            .parse("true");
        Assert.assertEquals(TEXT, sb.toString());
    }

    @Test
    public void testExceptions() {
        XPathContentHandler handler =
            new XPathContentHandler(
                new PrimitiveHandler() {
                    @Override
                    public void handle(
                        final List<PathComponent> path,
                        final Object value)
                        throws JsonException
                    {
                        if (path.size() == 2) {
                            throw new JsonException();
                        } else {
                            throw new MalformedJsonException(0);
                        }
                    }
                },
                SparseStringCollectorsFactory.INSTANCE.apply(-1L));
        try {
            new JsonParser(
                new ExceptionHandlingContentHandler(handler, handler))
                .parse("{\"test\":[1]}");
            Assert.fail();
        } catch (JsonException e) {
            Assert.assertEquals(JsonException.class, e.getClass());
            Assert.assertNull(e.getCause());
            Assert.assertEquals(1, e.getSuppressed().length);
        }

        try {
            new JsonParser(
                new ExceptionHandlingContentHandler(handler, handler))
                .parse("[1]");
            Assert.fail();
        } catch (JsonException e) {
            Assert.assertEquals(MalformedJsonException.class, e.getClass());
        }

        try {
            new JsonParser(
                new XPathContentHandler(
                    ReportingPrimitiveHandler.INSTANCE,
                    SparseStringCollectorsFactory.INSTANCE.apply(-1L)))
                .parse("[5]");
            Assert.fail();
        } catch (JsonException e) {
            Assert.assertEquals(
                JsonUnexpectedTokenException.class,
                e.getClass());
        }

        try {
            new XPathContentHandler(
                null,
                SparseStringCollectorsFactory.INSTANCE.apply(-1L)).get(0);
            Assert.fail();
        } catch (IndexOutOfBoundsException e) {
            return;
        }
    }
}

