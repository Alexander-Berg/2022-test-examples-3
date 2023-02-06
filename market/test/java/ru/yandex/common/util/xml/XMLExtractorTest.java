package ru.yandex.common.util.xml;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import static ru.yandex.common.util.collections.CollectionFactory.list;

/**
 * Created on 12:52:12 24.09.2008
 *
 * @author jkff
 */
public class XMLExtractorTest {
    @Test
    public void testFindAttribute() {
        String xml =
                "<a>\n" +
                "    <b>\n" +
                "        <c value=\"1\"/>\n" +
                "        <c value=\"2\"><d value=\"-1\"/></c>\n" +
                "        <x><y><c value=\"-2\"/></y></x>\n" +
                "    </b>\n" +
                "    <b>\n" +
                "        <c value=\"3\"/>\n" +
                "    </b>\n" +
                "    <b/>\n" +
                "    <k value=\"-3\">\n" +
                "        <c value=\"-4\"/>\n" +
                "    </k>\n" +
                "    <b>\n" +
                "        <c value=\"4\"/>\n" +
                "    </b>\n" +
                "</a>";
        assertEquals(list("1","2","3","4"), XMLExtractor.extractAttribute(xml, "a/b/c", "value"));
    }

    @Test
    public void testFindText() {
        String xml =
                "<a>\n" +
                "    <b>\n" +
                "        <c>1</c>\n" +
                "        <c>2<d>3<e>4</e><f>5</f>6</d>7</c>\n" +
                "        <x><y><c>-1<d>-2</d>-3<c>-4</c></c></y></x>\n" +
                "    </b>\n" +
                "    <b>\n" +
                "        <c>8</c>\n" +
                "        <c/>\n" +
                "        <c>9</c>\n" +
                "    </b>\n" +
                "    <b/>\n" +
                "    <k value=\"-3\">\n" +
                "        <c>-5</c>\n" +
                "        <d><c>-6</c></d>\n" +
                "    </k>\n" +
                "    <b>\n" +
                "        <c>10<d>11<e>12</e><f>13</f>14</d>15</c>\n" +
                "    </b>\n" +
                "    <c>-7</c>\n" +
                "</a>";
        assertEquals(list("1","234567","8","","9","101112131415"), XMLExtractor.extractText(xml, "a/b/c"));
    }
}
