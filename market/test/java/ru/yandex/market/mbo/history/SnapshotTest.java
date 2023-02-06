package ru.yandex.market.mbo.history;

import org.junit.Test;
import ru.yandex.market.mbo.history.model.Snapshot;
import ru.yandex.market.mbo.history.model.ValueType;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Sergey Skrobotov, sskrobotov@yandex-team.ru
 */
@SuppressWarnings("checkstyle:lineLength")
public class SnapshotTest {

    private static final String EMPTY_SNAPSHOT_XML =
            "\n<snapshot>\n</snapshot>";

    private static final String EXPECTED_XML =
            "\n<snapshot>" +
                "\n<field>" +
                    "\n\t<key>some-key</key>" +
                    "\n\t<value-type>4</value-type>" +
                    "\n\t<value>x &lt; Y &amp;&amp; x &gt; 0</value>" +
                "\n</field>" +
            "\n</snapshot>";

    private static final String TEST_XML =
            "<snapshot>" +
                "<field>" +
                    "<key>some-key</key>" +
                    "<value>x &lt; Y &amp;&amp; x &gt; 0</value>" +
                "</field>\n" +
                "<field>" +
                    "<key>another-key</key>" +
                    "<value-type>1</value-type>" +
                    "<value>another\nvalue</value>" +
                "</field>" +
            "</snapshot>";

    private static final String ENUM_TEST_XML =
            "\n<snapshot>" +
                    "\n<field>\n\t<key>A</key>\n\t<value-type>0</value-type>\n\t<value>1234</value>\n</field>" +
                    "\n<field>\n\t<key>SOME_KEY</key>\n\t<value-type>0</value-type>\n\t<value>x &lt; Y &amp;&amp; x &gt; 0</value>\n</field>" +
            "\n</snapshot>";

    private static final String ENUM_EXPECTED_XML =
            "\n<snapshot>" +
                    "\n<field>\n\t<key>SOME_KEY</key>\n\t<value-type>5</value-type>\n\t<value>x &lt; Y &amp;&amp; x &gt; 0</value>\n</field>" +
            "\n</snapshot>";

    private static final String TEST_XML_A =
            "<snapshot>" +
                "<field>" +
                    "<key>some-key</key>" +
                    "<value-type>0</value-type>" +
                    "<value>x &lt; Y &amp;&amp; x &gt; 0</value>" +
                "</field>" +
                "<field>" +
                    "<key>another-key</key>" +
                    "<value-type>0</value-type>" +
                    "<value>another-value</value>" +
                "</field>" +
            "</snapshot>";

    private static final String TEST_XML_B =
            "<snapshot>" +
                 "<field>" +
                    "<key>another-key</key>" +
                    "<value-type>0</value-type>" +
                    "<value>another-value</value>" +
                 "</field>" +
                 "<field>" +
                    "<key>some-key</key>" +
                    "<value-type>0</value-type>" +
                    "<value>x &lt; Y &amp;&amp; x &gt; 0</value>" +
                 "</field>" +
             "</snapshot>";

    private static final String EMPTY_VALUE_TEST =
            "<snapshot><field><key>qqq</key><value-type>1</value-type><value></value></field></snapshot>";

    @Test
    public void testFromXml() throws Exception {
        Snapshot s = Snapshot.fromXml(TEST_XML);

        assertEquals("x < Y && x > 0", s.get("some-key").getValue());
        assertEquals("another\nvalue", s.get("another-key").getValue());

        assertEquals(ValueType.UNKNOWN, s.get("some-key").getValueType());
        assertEquals(ValueType.INTEGER, s.get("another-key").getValueType());
    }

    @Test
    public void testToXml() throws Exception {
        Snapshot f = new Snapshot();

        f.put("some-key", ValueType.STRING, "x < Y && x > 0");

        StringBuilder sb = new StringBuilder();

        f.toXml(sb);

        assertEquals(EXPECTED_XML, sb.toString());
    }

    @Test
    public void testEmptySnapshot() throws Exception {
        StringBuilder sb = new StringBuilder();

        Snapshot.EMPTY_SNAPSHOT.toXml(sb);

        assertEquals(EMPTY_SNAPSHOT_XML, sb.toString());
    }

    @Test
    public void testNullValue() throws Exception {
        Snapshot f = new Snapshot();

        f.put("some-key", ValueType.UNKNOWN, null);

        StringBuilder sb = new StringBuilder();

        f.toXml(sb);

        assertEquals(EMPTY_SNAPSHOT_XML, sb.toString());
    }

    @Test
    public void testEnumFromXml() throws Exception {
        Snapshot f = Snapshot.fromXml(ENUM_TEST_XML);

        assertEquals("1234", f.get(Keys.A).getValue());
        assertFalse(f.containsKey(Keys.B.name()));
        assertEquals(null, f.get(Keys.B));
        assertEquals("x < Y && x > 0", f.get(Keys.SOME_KEY).getValue());
    }

    @Test
    public void testEnumToXml() throws Exception {
        Snapshot f = new Snapshot();

        f.put(Keys.SOME_KEY, ValueType.MULTILINE_STRING, "x < Y && x > 0");

        StringBuilder sb = new StringBuilder();

        f.toXml(sb);

        assertEquals(ENUM_EXPECTED_XML, sb.toString());
    }

    @Test
    public void testSingleValueSnapshot() throws Exception {
        StringBuilder sb = new StringBuilder();

        new Snapshot("key", ValueType.UNKNOWN, "value").toXml(sb);

        Snapshot q = Snapshot.fromXml(sb.toString());

        assertEquals("value", q.get("key").getValue());
        assertEquals(ValueType.UNKNOWN, q.get("key").getValueType());
    }

    @Test
    public void testEqualsTypeAware() throws Exception {
        Snapshot a = new Snapshot();
        Snapshot b = new Snapshot();
        Snapshot c = new Snapshot();

        a.put("key", ValueType.FLOAT, "1");
        b.put("key", ValueType.FLOAT, "1.0");
        c.put("key", ValueType.FLOAT, "1.1");
        assertTrue(Snapshot.equals(a, b));
        assertFalse(Snapshot.equals(c, b));

        a.put("key", ValueType.BOOLEAN, "TRUE");
        b.put("key", ValueType.BOOLEAN, "true");
        c.put("key", ValueType.BOOLEAN, "FALSE");

        assertTrue(Snapshot.equals(a, b));
        assertFalse(Snapshot.equals(c, b));

        a.put("key", ValueType.INTEGER, "123");
        b.put("key", ValueType.INTEGER, " 123");
        c.put("key", ValueType.INTEGER, "-123");

        assertTrue(Snapshot.equals(a, b));
        assertFalse(Snapshot.equals(c, b));

        a.put("key", ValueType.STRING, "as df");
        b.put("key", ValueType.STRING, "as df");
        c.put("key", ValueType.STRING, "as");

        assertTrue(Snapshot.equals(a, b));
        assertFalse(Snapshot.equals(c, b));

        a.put("key", ValueType.MULTILINE_STRING, "qqq\nwww\naaa");
        b.put("key", ValueType.MULTILINE_STRING, "www\n\raaa\nqqq");
        c.put("key", ValueType.MULTILINE_STRING, "qqq\nwww\naaa\nbbb");

        assertTrue(Snapshot.equals(a, b));
        assertFalse(Snapshot.equals(c, b));

        a.put("key1", ValueType.FLOAT, "122");
        b.put("key1", ValueType.FLOAT, "122.0");
        assertTrue(Snapshot.equals(a, b));
    }

    @Test
    public void testEquals() throws Exception {
        // a == b == c
        Snapshot a = Snapshot.fromXml(TEST_XML_A);
        Snapshot b = Snapshot.fromXml(TEST_XML_B);
        Snapshot c = new Snapshot();
        c.put("some-key", ValueType.UNKNOWN, "x < Y && x > 0");
        c.put("another-key", ValueType.UNKNOWN, "another-value");

        // d != a, e != a, e != d
        Snapshot d = Snapshot.fromXml(ENUM_TEST_XML);
        Snapshot e = new Snapshot();
        e.put("another-key", ValueType.UNKNOWN, "another-value");

        assertTrue(Snapshot.equals(a, b));
        assertTrue(Snapshot.equals(a, c));
        assertTrue(Snapshot.equals(c, b));

        assertFalse(Snapshot.equals(d, a));
        assertFalse(Snapshot.equals(d, b));
        assertFalse(Snapshot.equals(d, c));
        assertFalse(Snapshot.equals(e, d));
        assertFalse(Snapshot.equals(e, c));

        Snapshot empty1 = new Snapshot();
        Snapshot empty2 = new Snapshot();

        assertTrue(Snapshot.equals(empty1, empty2));
    }

    @Test
    public void testEmptyValue() throws Exception {
        Snapshot s = Snapshot.fromXml(EMPTY_VALUE_TEST);

        assertEquals("", s.get("qqq").getValue());
        assertEquals(ValueType.INTEGER, s.get("qqq").getValueType());
    }

    @Test
    public void testGetMultistringDiff() throws Exception {
        Snapshot oldValue = new Snapshot(
                "key",
                ValueType.MULTILINE_STRING,
                "qqq\nwww\nwww\neee"
        );
        Snapshot newValue = new Snapshot(
                "key",
                ValueType.MULTILINE_STRING,
                "eee\nwww\naaa\nbbb\naaa"
        );

        List<String> expectedAdded = Arrays.asList("aaa", "bbb");
        List<String> expectedRemoved = Arrays.asList("qqq");
        List<String> added = new LinkedList<String>();
        List<String> removed = new LinkedList<String>();

        Snapshot.getMultistringDiff(oldValue, newValue, "key", added, removed);

        assertEquals(expectedAdded, added);
        assertEquals(expectedRemoved, removed);
    }

    enum Keys {
        A,
        B,
        SOME_KEY
    }
}
