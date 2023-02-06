/**
 * <p>Copyright: Copyright (c) 2005</p>
 * <p>Company: Yandex</p>
 * <p>Date: 08.06.2006</p>
 * <p>Time: 18:25:08</p>
 */
package ru.yandex.common.framework.xml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import junit.framework.TestCase;

import ru.yandex.common.framework.core.ErrorInfo;
import ru.yandex.common.framework.core.SystemError;

/**
 * @author Nikolay Malevanny nmalevanny@yandex-team.ru
 */
public class StringXmlBuilderTest extends TestCase {
    private StringXmlBuilder xmlBuilder = new StringXmlBuilder();
    private List<Object> data;
    private List<ErrorInfo> errors;
    public static final String DATA_PREFIX = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<data servant=\"test\" version=\"-1\" host=\"localhost\">";
    public static final String DATA_SUFFIX = "</data>";

    public StringXmlBuilderTest() {
        xmlBuilder.setServantInfo(new MockServantInfo());
    }

    protected void setUp() throws Exception {
        data = new ArrayList<Object>();
        errors = new ArrayList<ErrorInfo>();
    }

    public void testEmpty() throws Exception {
        assertContent("");
    }

    public void testAttributes() throws Exception {
        HashMap additional = new HashMap();
        additional.put("additional", Arrays.asList(new Integer[]{1, 2}));
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<data servant=\"test\" version=\"-1\" host=\"localhost\" additional=\"[1, 2]\" >" + DATA_SUFFIX,
                xmlBuilder.build(data, errors, additional));
    }

    public void testPimitives() throws Exception {
        data.add("test");
        data.add(1L);
        data.add(1);
        data.add(1.0D);
        assertContent("<string>test</string><long>1</long><integer>1</integer><double>1.0</double>");
    }

    public void testEmptyCollection() throws Exception {
        data.add(new ArrayList());
        assertContent("<collection></collection>");
    }

    public void testMap() throws Exception {
        Map map = new TreeMap(new Comparator() {
            //"a" всегда меньше всего остального, а остальное пока считаем эквивалентным :) просто для гарантии
            // порядка в ЭТОМ тестовом случае
            public int compare(Object o1, Object o2) {
                return (o1.equals(o2)) ? 0
                        : ("a".equals(o1)) ? -1
                        : ("a".equals(o2)) ? 1 : 0;
            }
        });
        map.put("a", new Ttt("a", 1));
        map.put(new Ttt("a", 1), "a");
        data.add(map);
        assertContent("<map>" +
                "<entry><key><string>a</string></key><value><ttt b=\"1\"><a>a</a></ttt></value></entry>" +
                "<entry><key><ttt b=\"1\"><a>a</a></ttt></key><value><string>a</string></value></entry>" +
                "</map>");
    }

    public void testCollectionOfString() throws Exception {
        final ArrayList<String> al = new ArrayList<String>();
        al.add("test");
        al.add("test");
        data.add(al);
        assertContent("<collection><string>test</string><string>test</string></collection>");
    }

    public void testMock() throws Exception {
        data.add(new Ttt("test", 111));
        assertContent("<ttt b=\"111\"><a>test</a></ttt>");
    }

    public void testEscape() throws Exception {
        data.add("&<>");
        assertContent("<string>&amp;&lt;&gt;</string>");
    }

    public void testComplex() throws Exception {
        data.add(new Ccc(new Ttt("a", 1), 2));
        assertContent("<ccc a=\"2\"><ttt b=\"1\"><a>a</a></ttt></ccc>");
    }

    public void testEnum() throws Exception {
        data.add(new Sss("test", Eee.TEST));
        assertContent("<sss e=\"TEST\"><a>test</a></sss>");
    }

    public void testEmptyClass() throws Exception {
        data.add(new Empty());
        assertContent("<empty/>");
    }

    public void testRootEnum() throws Exception {
        errors.add(SystemError.FORBIDDEN);
        assertContent("<errors><system-error name=\"FORBIDDEN\"/></errors>");
    }

    private void assertContent(final String content) {
        assertEquals(DATA_PREFIX + content + DATA_SUFFIX, xmlBuilder.build(data, errors));
    }

    private static class Empty {
    }

    private static class Ttt {
        private String a;
        private int b;

        public Ttt(final String a, final int b) {
            this.a = a;
            this.b = b;
        }

        public String getA() {
            return a;
        }

        public int getB() {
            return b;
        }
    }

    private static class Ccc {
        private Ttt ttt;
        private int a;

        public Ccc(final Ttt ttt, final int a) {
            this.ttt = ttt;
            this.a = a;
        }

        public Ttt getTtt() {
            return ttt;
        }

        public int getA() {
            return a;
        }
    }

    private static class Sss {
        private String a;
        private Eee e;

        public Sss(final String a, final Eee e) {
            this.a = a;
            this.e = e;
        }

        public String getA() {
            return a;
        }

        public Eee getE() {
            return e;
        }
    }

    private static enum Eee {
        TEST,
        ENUM
    }
}
