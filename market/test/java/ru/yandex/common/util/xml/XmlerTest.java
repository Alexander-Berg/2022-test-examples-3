package ru.yandex.common.util.xml;

import junit.framework.TestCase;
import ru.yandex.common.util.collections.CollectionFactory;
import ru.yandex.common.util.reflect.ReflectionUtils;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import java.io.PrintWriter;
import java.util.Collections;

import static ru.yandex.common.util.xml.Xmler.*;

/**
 * created by pulser at Jan 27, 2009 3:32:00 PM
 */
public class XmlerTest extends TestCase {

    public void testXmlerTagGenerationWithEmptyBody() {
        final StringBuilder sb = new StringBuilder();
        Xmler.tag("test", "").toXml(sb);
        final String value = sb.toString().trim();
        final String expected = "<test/>";
        assertEquals("Empty tag should render to single element " + expected, expected, value);
    }

    public void testXmlerTagGenerationWithEmptyBodyWhenNull() {
        final StringBuilder sb = new StringBuilder();
        Xmler.tag("test", (String) null).toXml(sb);
        final String value = sb.toString().trim();
        final String expected = "<test/>";
        assertEquals("Empty tag should render to single element " + expected, expected, value);
    }

    public void testXmlerTagSecondVersionGenerationWithEmptyBodyWhenNull() {
        final StringBuilder sb = new StringBuilder();
        Xmler.tag("test", null, new Xmler.Tag[]{}).toXml(sb);
        final String value = sb.toString();
        final String expected = "<test/>\n";
        assertEquals("Empty tag should render to single element " + expected, expected, value);
    }

    public void testXmlerAttributeAndOtherAttribute() throws Exception {
        final StringBuilder sb1 = new StringBuilder();
        Xmler.tag("test",
            Xmler.attribute("foo", "bar").and(Xmler.attribute("baz", "baz").and("ololo", "ololo"))
        ).toXml(sb1);
        assertEquals("<test foo=\"bar\" baz=\"baz\" ololo=\"ololo\"/>", sb1.toString().trim());

        final StringBuilder sb2 = new StringBuilder();
        Xmler.tag("test",
            Xmler.attribute("foo", "bar").
                and(Xmler.attribute("baz", null).
                    and(Xmler.attribute("ololo", "ololo")))
        ).toXml(sb2);
        assertEquals("<test foo=\"bar\" ololo=\"ololo\"/>", sb2.toString().trim());
    }

    public void testXmlerAttributesWithNullValue() {
        final StringBuilder sb = new StringBuilder();
        Xmler.tag("foo", Xmler.attribute("bar", null).and("baz", "baz").and("zoo", null)).toXml(sb);
        final String value = sb.toString();
        final String expected = "<foo baz=\"baz\"/>\n";
        assertEquals("Null attribute values should not produce empty attributes", expected, value);
    }

    public void testTagWithAttributesEmptyChildrenListCollapse() {
        final StringBuilder sb = new StringBuilder();
        tag("foo", attribute("bar", "baz"), Collections.<Tagable>emptyList()).toXml(sb);
        assertEquals(
            "<foo bar=\"baz\"/>",
            sb.toString().trim()
        );
        final StringBuilder sb1 = new StringBuilder();
        tag("foo", attribute("bar", "baz"), new Tagable[]{}).toXml(sb1);
        assertEquals(
            "<foo bar=\"baz\"/>",
            sb1.toString().trim()
        );
        final StringBuilder sb2 = new StringBuilder();
        tag("foo", attribute("bar", "baz"), new Tagable[]{EMPTY_TAG}).toXml(sb2);
        assertEquals(
            "<foo bar=\"baz\"/>",
            sb2.toString().trim()
        );
        final StringBuilder sb3 = new StringBuilder();
        tag("foo", attribute("bar", "baz"), new Tagable[]{EMPTY_TAG, tag("post", "bop")}).toXml(sb3);
        System.out.println(sb3.toString());
        assertEquals("<foo bar=\"baz\"><post>bop</post></foo>", sb3.toString().replaceAll("[\\n]", ""));
    }


    public void testSaxing() {
        final PrintWriter pw = new PrintWriter(System.out);
        final StreamResult streamResult = new StreamResult(pw);
        final SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();

        try {

            final TransformerHandler xmlHandler = tf.newTransformerHandler();

            final Transformer serializer = xmlHandler.getTransformer();
            serializer.setOutputProperty(OutputKeys.ENCODING, "utf-8");
            serializer.setOutputProperty(OutputKeys.VERSION, "1.0");
            serializer.setOutputProperty(OutputKeys.INDENT, "yes");
            serializer.setErrorListener(new ErrorListener() {
                public void warning(final TransformerException exception) throws TransformerException {
                    throw new RuntimeException("not implemented");
                }

                public void error(final TransformerException exception) throws TransformerException {
                    throw new RuntimeException("not implemented");
                }

                public void fatalError(final TransformerException exception) throws TransformerException {
                    throw new RuntimeException("not implemented");
                }
            });

            xmlHandler.setResult(streamResult);
            tag(
                "test", attribute("foo", "bar"),
                tag("inner", "ROGA & KOPYTA -- Рога И Копыта"),
                tag("inner-with-attrs", attribute("foo", "bar").and("baz", "zag").and(Xmler.attribute("zoo", "zee"))),
                tag("inner-group",
                    tags("value", CollectionFactory.list("first", "second", "third"))
                )
            ).toXml(xmlHandler);


            streamResult.getWriter().flush();


        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public void testCDATA() {
        assertEquals("<a/>", getTagAsString(Xmler.tagWithCDATA("a", null), true));
        assertEquals("<a/>", getTagAsString(Xmler.tagWithCDATA("a", ""), true));
        assertEquals("<a><![CDATA[cdata]]></a>", getTagAsString(Xmler.tagWithCDATA("a", "cdata"), true));
        assertEquals("<a><![CDATA[<tipa>&]]></a>", getTagAsString(Xmler.tagWithCDATA("a", "<tipa>&"), true));
        assertEquals("<a><![CDATA[<tipa>&]]]]><![CDATA[>]]></a>", getTagAsString(Xmler.tagWithCDATA("a", "<tipa>&]]>"), true));

        assertEquals("<a attr=\"value\"/>", getTagAsString(Xmler.tagWithCDATA("a", Xmler.attribute("attr", "value"), null), true));
        assertEquals("<a attr=\"value\"/>", getTagAsString(Xmler.tagWithCDATA("a", Xmler.attribute("attr", "value"), ""), true));
        assertEquals("<a attr=\"value\"><![CDATA[cdata]]></a>", getTagAsString(Xmler.tagWithCDATA("a", Xmler.attribute("attr", "value"), "cdata"), true));
        assertEquals("<a attr=\"value\"><![CDATA[<tipa>&]]></a>", getTagAsString(Xmler.tagWithCDATA("a", Xmler.attribute("attr", "value"), "<tipa>&"), true));
        assertEquals("<a attr=\"value\"><![CDATA[<tipa>&]]]]><![CDATA[>]]></a>", getTagAsString(Xmler.tagWithCDATA("a", Xmler.attribute("attr", "value"), "<tipa>&]]>"), true));
    }

    public void testConsistency() throws Exception {
        innerConsistencyTest(true);
    }

    private void innerConsistencyTest(final boolean trimResult) {
        final String tagName = "sample-tag";

        final String emptyTag = getTagAsString(tag(tagName), trimResult);
        final Attribute nullAttr = null;
        final String tagWithNullAttr = getTagAsString(tag(tagName, nullAttr), trimResult);
        assertEquals(emptyTag, tagWithNullAttr);

        final String tagEmptyValue = getTagAsString(tag(tagName, ""), trimResult);
        assertEquals(tagWithNullAttr, tagEmptyValue);

        final String tagWithNullAttrEmptyValue = getTagAsString(tag(tagName, nullAttr, ""), trimResult);
        assertEquals(tagEmptyValue, tagWithNullAttrEmptyValue);

        final Attribute attr = attribute("attr", "attr-value");
        final String tagFilledAttr = getTagAsString(tag(tagName, attr), trimResult);
        final String tagFilledAttrEmptyValue = getTagAsString(tag(tagName, attr, ""), trimResult);
        assertEquals(tagFilledAttr, tagFilledAttrEmptyValue);

        final Tag innerTag = tag("inner-tag", attribute("inner-attr", "inner-attr-value"), "inner-value");
        final String tagWithInner = getTagAsString(tag(tagName, innerTag), trimResult);
        final String tagNullAttrWithInner = getTagAsString(tag(tagName, nullAttr, innerTag), trimResult);
        assertEquals(tagWithInner, tagNullAttrWithInner);
    }

    private String getTagAsString(final Tag tag, final boolean trim) {
        final StringBuilder sb = new StringBuilder();
        tag.toXml(sb);
        final String stringValue = sb.toString();
        return trim ? stringValue.trim() : stringValue;
    }


    public void testCreateEmptyAttr() throws Exception {
        Attribute attribute = EMPTY_ATTRS;
        attribute = attribute.and("x", "y");

        final StringBuilder storage = new StringBuilder();
        attribute.toXml(storage);
        assertEquals(" x=\"y\"", storage.toString());

        final StringBuilder storage1 = new StringBuilder();
        EMPTY_ATTRS.toXml(storage);
        assertNotSame(" x=\"y\"", storage1);
    }

    public void testState() throws Exception {
        Attribute attribute = EMPTY_ATTRS;
        attribute = attribute.and("x", "y");
        final Tag tag = tag("a", attribute, tag("b", "c"));
        tag.toXml(ReflectionUtils.newEmptyProxy(TransformerHandler.class));
        assertEquals(0, EMPTY_ATTRS.asAttributes().getLength());
    }

    public void testAndEmptyAttrs() throws Exception {
        final StringBuilder sb = new StringBuilder();
        tag("foo", EMPTY_ATTRS).toXml(sb);
        assertEquals("<foo/>", sb.toString().trim());
        final StringBuilder sb1 = new StringBuilder();
        tag("foo", EMPTY_ATTRS.and("a", "b")).toXml(sb1);
        assertEquals("<foo a=\"b\"/>", sb1.toString().trim());
        final StringBuilder sb2 = new StringBuilder();
        tag("foo", EMPTY_ATTRS).toXml(sb2);
        assertEquals("<foo/>", sb2.toString().trim());
    }
}
