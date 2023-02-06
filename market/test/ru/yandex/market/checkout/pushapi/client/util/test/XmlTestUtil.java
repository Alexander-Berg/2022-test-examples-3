package ru.yandex.market.checkout.pushapi.client.util.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xml.sax.Attributes;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.ElementSelectors;

import ru.yandex.common.util.xml.parser.StackableElementOrientedSAXHandler;
import ru.yandex.market.checkout.common.xml.AbstractXmlSerializer;
import ru.yandex.market.checkout.common.xml.SimpleXmlWriter;
import ru.yandex.market.checkout.common.xml.XmlSerializer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.xmlunit.matchers.CompareMatcher.isSimilarTo;

public class XmlTestUtil {

    public static Date date(String date) throws Exception {
        return new SimpleDateFormat("yyyy-MM-dd").parse(date);
    }

    public static <T> void assertSerializeResultAndString(
            XmlSerializer<T> serializer, T object, String expectedXml
    ) throws Exception {
        final StringWriter stringWriter = new StringWriter();
        if (serializer instanceof AbstractXmlSerializer) {
            final AbstractXmlSerializer abstractXmlSerializer = (AbstractXmlSerializer) serializer;
            abstractXmlSerializer.setEnumAsNumber(false);
            abstractXmlSerializer.setSerializeNulls(false);
        }
        serializer.serializeXml(object, new SimpleXmlWriter(stringWriter));

        System.out.println("actual: " + stringWriter.toString());
        assertThat(stringWriter.toString(), isSimilarTo(expectedXml)
                .ignoreWhitespace()
                .normalizeWhitespace()
                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndAttributes("id", "code"))));

    }

    public static <T> void initMockSerializer(XmlSerializer<T> mock, T object, final MockSerializer serializer)
            throws Exception {
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                final AbstractXmlSerializer.PrimitiveXmlWriter writer
                        = (AbstractXmlSerializer.PrimitiveXmlWriter) invocation.getArguments()[1];
                serializer.serialize(writer);
                return null;
            }
        }).when(mock).serializeXml(eq(object), any(AbstractXmlSerializer.PrimitiveXmlWriter.class));
    }

    public static <T> T deserialize(StackableElementOrientedSAXHandler<T> deserializer, String expected)
            throws Exception {
        deserializer.parseXmlStream(new ByteArrayInputStream(expected.getBytes()));
        return deserializer.getParsed();
    }

    public interface MockSerializer {

        void serialize(AbstractXmlSerializer.PrimitiveXmlWriter writer) throws IOException;
    }

    public static class SimpleMockSerializer implements MockSerializer {

        private String nodeName;
        private String nodeValue;

        public SimpleMockSerializer(String nodeName, String nodeValue) {
            this.nodeName = nodeName;
            this.nodeValue = nodeValue;
        }

        @Override
        public void serialize(AbstractXmlSerializer.PrimitiveXmlWriter writer) throws IOException {
            writer.addNode(nodeName, nodeValue);
        }
    }

    public static <T> String serialize(XmlSerializer<T> serializer, T object) throws Exception {
        final StringWriter stringWriter = new StringWriter();
        if (serializer instanceof AbstractXmlSerializer) {
            final AbstractXmlSerializer abstractXmlSerializer = (AbstractXmlSerializer) serializer;
            abstractXmlSerializer.setEnumAsNumber(false);
            abstractXmlSerializer.setSerializeNulls(false);
        }
        serializer.serializeXml(object, new SimpleXmlWriter(stringWriter));

        return stringWriter.toString();
    }

    public static <T extends StackableElementOrientedSAXHandler<F>, F> T createDeserializerMock(
            Class<T> clazz, final Map<String, F> map
    ) throws Exception {
        final T mock = spy(clazz);
        mock.clearListeners();
        final Answer<Object> answer = new Answer<Object>() {
            private StringBuilder currentXml = new StringBuilder();

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                final String name = invocation.getMethod().getName();
                final Object[] args = invocation.getArguments();
                if (name.equals("startElement")) {
                    final String qName = (String) args[2];
                    final Attributes attrs = (Attributes) args[3];

                    currentXml.append("<").append(qName);
                    for (int i = 0; i < attrs.getLength(); i++) {
                        currentXml.append(" ")
                                .append(attrs.getQName(i))
                                .append("='")
                                .append(attrs.getValue(i))
                                .append("'");
                    }
                    currentXml.append(">");
                } else if (name.equals("endElement")) {
                    final String qName = (String) args[2];

                    currentXml.append("</").append(qName).append(">");
                } else if (name.equals("characters")) {
                    final char[] ch = (char[]) args[0];
                    final int start = (int) args[1];
                    final int length = (int) args[2];

                    final char[] out = new char[length];
                    System.arraycopy(ch, start, out, 0, length);
                    currentXml.append(out);
                } else if (name.equals("getParsed")) {
                    for (String xml : map.keySet()) {
                        final org.xmlunit.diff.Diff diff = DiffBuilder.compare(xml)
                                .ignoreWhitespace()
                                .withTest(currentXml.toString())
                                .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byName))
                                .checkForSimilar()
                                .build();

                        if (!diff.hasDifferences()) {
                            currentXml = new StringBuilder();
                            return map.get(xml);
                        }
                    }
                }
                invocation.callRealMethod();
                return null;
            }
        };
        doAnswer(answer).when(mock).startElement(anyString(), anyString(), anyString(), any(Attributes.class));
        doAnswer(answer).when(mock).endElement(anyString(), anyString(), anyString());
        doAnswer(answer).when(mock).characters(any(char[].class), anyInt(), anyInt());
        when(mock.getParsed()).thenAnswer(answer);

        return mock;
    }

    public static Matcher<String> sameXmlAs(final String expected) {
        return new TypeSafeMatcher<String>() {
            org.xmlunit.diff.Diff compare;

            @Override
            public boolean matchesSafely(String o) {
                compare = DiffBuilder.compare(expected)
                        .ignoreWhitespace()
                        .withTest(o)
                        .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byName))
                        .checkForSimilar()
                        .build();

                return !compare.hasDifferences();
            }

            @Override
            public void describeTo(Description description) {
                description
                        .appendText(String.valueOf(compare))
                        .appendText("\n")
                        .appendText(expected);
            }
        };
    }


}
