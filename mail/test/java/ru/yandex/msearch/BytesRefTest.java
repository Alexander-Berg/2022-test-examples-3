package ru.yandex.msearch;

import org.junit.Assert;
import org.junit.Test;

import org.apache.lucene.util.BytesRef;

import ru.yandex.test.util.TestBase;

public class BytesRefTest extends TestBase {
    @Test
    public void testSplit() throws Exception {
        BytesRef string = new BytesRef("qwe1,qwe2,qwe3,,qwe4,,");
        BytesRef[] cols = string.split((byte) ',');
        for (int i = 0; i < cols.length; i++) {
            System.out.println("cols[" + i + "]: " + cols[i].utf8ToString());
        }
        Assert.assertEquals(4, cols.length);
        Assert.assertEquals("qwe1", cols[0].utf8ToString());
        Assert.assertEquals("qwe2", cols[1].utf8ToString());
        Assert.assertEquals("qwe3", cols[2].utf8ToString());
        Assert.assertEquals("qwe4", cols[3].utf8ToString());

        string = new BytesRef(",,qwe1,qwe2,qwe3,,qwe4,,");
        cols = string.split((byte) ',');
        for (int i = 0; i < cols.length; i++) {
            System.out.println("cols[" + i + "]: " + cols[i].utf8ToString());
        }
        Assert.assertEquals(4, cols.length);
        Assert.assertEquals("qwe1", cols[0].utf8ToString());
        Assert.assertEquals("qwe2", cols[1].utf8ToString());
        Assert.assertEquals("qwe3", cols[2].utf8ToString());
        Assert.assertEquals("qwe4", cols[3].utf8ToString());

        string = new BytesRef(",,qwe1,qwe2,qwe3,,qwe4");
        cols = string.split((byte) ',');
        for (int i = 0; i < cols.length; i++) {
            System.out.println("cols[" + i + "]: " + cols[i].utf8ToString());
        }
        Assert.assertEquals(4, cols.length);
        Assert.assertEquals("qwe1", cols[0].utf8ToString());
        Assert.assertEquals("qwe2", cols[1].utf8ToString());
        Assert.assertEquals("qwe3", cols[2].utf8ToString());
        Assert.assertEquals("qwe4", cols[3].utf8ToString());

        string = new BytesRef("qwe1,qwe2,qwe3,,qwe4");
        cols = string.split((byte) ',');
        for (int i = 0; i < cols.length; i++) {
            System.out.println("cols[" + i + "]: " + cols[i].utf8ToString());
        }
        Assert.assertEquals(4, cols.length);
        Assert.assertEquals("qwe1", cols[0].utf8ToString());
        Assert.assertEquals("qwe2", cols[1].utf8ToString());
        Assert.assertEquals("qwe3", cols[2].utf8ToString());
        Assert.assertEquals("qwe4", cols[3].utf8ToString());

        string = new BytesRef(",,qwe1,,");
        cols = string.split((byte) ',');
        for (int i = 0; i < cols.length; i++) {
            System.out.println("cols[" + i + "]: " + cols[i].utf8ToString());
        }
        Assert.assertEquals(1, cols.length);
        Assert.assertEquals("qwe1", cols[0].utf8ToString());

        string = new BytesRef(",,qwe1");
        cols = string.split((byte) ',');
        for (int i = 0; i < cols.length; i++) {
            System.out.println("cols[" + i + "]: " + cols[i].utf8ToString());
        }
        Assert.assertEquals(1, cols.length);
        Assert.assertEquals("qwe1", cols[0].utf8ToString());

        string = new BytesRef("qwe1,,");
        cols = string.split((byte) ',');
        for (int i = 0; i < cols.length; i++) {
            System.out.println("cols[" + i + "]: " + cols[i].utf8ToString());
        }
        Assert.assertEquals(1, cols.length);
        Assert.assertEquals("qwe1", cols[0].utf8ToString());

        string = new BytesRef("qwe1");
        cols = string.split((byte) ',');
        for (int i = 0; i < cols.length; i++) {
            System.out.println("cols[" + i + "]: " + cols[i].utf8ToString());
        }
        Assert.assertEquals(1, cols.length);
        Assert.assertEquals("qwe1", cols[0].utf8ToString());

        string = new BytesRef("");
        cols = string.split((byte) ',');
        Assert.assertEquals(0, cols.length);

        string = new BytesRef(",");
        cols = string.split((byte) ',');
        Assert.assertEquals(0, cols.length);

        string = new BytesRef(",,,");
        cols = string.split((byte) ',');
        Assert.assertEquals(0, cols.length);
    }

    @Test
    public void testParseInt() {
        BytesRef string = new BytesRef("0");
        Assert.assertEquals(0, string.parseInt());

        string = new BytesRef("0000");
        Assert.assertEquals(0, string.parseInt());

        string = new BytesRef("-0000");
        Assert.assertEquals(0, string.parseInt());

        string = new BytesRef("-1");
        Assert.assertEquals(-1, string.parseInt());

        string = new BytesRef("-1234567890");
        Assert.assertEquals(-1234567890, string.parseInt());

        string = new BytesRef("-01234567890");
        Assert.assertEquals(-1234567890, string.parseInt());

        string = new BytesRef(Integer.toString(Integer.MIN_VALUE));
        Assert.assertEquals(Integer.MIN_VALUE, string.parseInt());

        string = new BytesRef("-1.2");
        boolean nfe = false;
        try {
            string.parseInt();
        } catch (NumberFormatException e) {
            nfe = true;
        }
        Assert.assertEquals(true, nfe);

        string = new BytesRef("-");
        nfe = false;
        try {
            string.parseInt();
        } catch (NumberFormatException e) {
            nfe = true;
        }
        Assert.assertEquals(true, nfe);

        string = new BytesRef("+");
        nfe = false;
        try {
            string.parseInt();
        } catch (NumberFormatException e) {
            nfe = true;
        }
        Assert.assertEquals(true, nfe);

        string = new BytesRef("1");
        Assert.assertEquals(1, string.parseInt());

        string = new BytesRef("1234567890");
        Assert.assertEquals(1234567890, string.parseInt());

        string = new BytesRef("01234567890");
        Assert.assertEquals(1234567890, string.parseInt());

        string = new BytesRef(Integer.toString(Integer.MAX_VALUE));
        Assert.assertEquals(Integer.MAX_VALUE, string.parseInt());

        string = new BytesRef("0");
        Assert.assertEquals(0, string.parseInt(16));

        string = new BytesRef("012345");
        Assert.assertEquals(0x12345, string.parseInt(16));

        string = new BytesRef("6789a");
        Assert.assertEquals(0x6789a, string.parseInt(16));

        string = new BytesRef("abcdeff");
        Assert.assertEquals(0xabcdeff, string.parseInt(16));

        string = new BytesRef("abcdeff");
        nfe = false;
        try {
            string.parseInt();
        } catch (NumberFormatException e) {
            nfe = true;
        }
        Assert.assertEquals(true, nfe);

        string = new BytesRef("ffffffFF");
        nfe = false;
        try {
            string.parseInt(16);
        } catch (NumberFormatException e) {
            nfe = true;
        }
        Assert.assertEquals(true, nfe);

        string = new BytesRef("-ffffffFF");
        nfe = false;
        try {
            string.parseInt(16);
        } catch (NumberFormatException e) {
            nfe = true;
        }
        Assert.assertEquals(true, nfe);

        string = new BytesRef("-BFG");
        nfe = false;
        try {
            string.parseInt(16);
        } catch (NumberFormatException e) {
            nfe = true;
        }
        Assert.assertEquals(true, nfe);

        string = new BytesRef("7fFFffFF");
        Assert.assertEquals(Integer.MAX_VALUE, string.parseInt(16));

        string = new BytesRef("-80000000");
        Assert.assertEquals(Integer.MIN_VALUE, string.parseInt(16));

        string = new BytesRef("ABCDEF");
        Assert.assertEquals(0xabcdef, string.parseInt(16));

        string = new BytesRef("-ABCDEF");
        Assert.assertEquals(-0xabcdef, string.parseInt(16));
    }

    @Test
    public void testParseLong() {
        BytesRef string = new BytesRef("0");
        Assert.assertEquals(0, string.parseLong());

        string = new BytesRef("0000");
        Assert.assertEquals(0, string.parseLong());

        string = new BytesRef("-0000");
        Assert.assertEquals(0, string.parseLong());

        string = new BytesRef("-1");
        Assert.assertEquals(-1, string.parseLong());

        string = new BytesRef("-1234567890");
        Assert.assertEquals(-1234567890, string.parseLong());

        string = new BytesRef("-01234567890");
        Assert.assertEquals(-1234567890, string.parseLong());

        string = new BytesRef(Long.toString(Long.MIN_VALUE));
        Assert.assertEquals(Long.MIN_VALUE, string.parseLong());

        string = new BytesRef("-1.2");
        boolean nfe = false;
        try {
            string.parseLong();
        } catch (NumberFormatException e) {
            nfe = true;
        }
        Assert.assertEquals(true, nfe);

        string = new BytesRef("-");
        nfe = false;
        try {
            string.parseLong();
        } catch (NumberFormatException e) {
            nfe = true;
        }
        Assert.assertEquals(true, nfe);

        string = new BytesRef("+");
        nfe = false;
        try {
            string.parseLong();
        } catch (NumberFormatException e) {
            nfe = true;
        }
        Assert.assertEquals(true, nfe);

        string = new BytesRef("1");
        Assert.assertEquals(1, string.parseLong());

        string = new BytesRef("1234567890");
        Assert.assertEquals(1234567890, string.parseLong());

        string = new BytesRef("01234567890");
        Assert.assertEquals(1234567890, string.parseLong());

        string = new BytesRef(Long.toString(Long.MAX_VALUE));
        Assert.assertEquals(Long.MAX_VALUE, string.parseLong());

        string = new BytesRef("0");
        Assert.assertEquals(0, string.parseLong(16));

        string = new BytesRef("012345");
        Assert.assertEquals(0x12345, string.parseLong(16));

        string = new BytesRef("6789a");
        Assert.assertEquals(0x6789a, string.parseLong(16));

        string = new BytesRef("abcdeff");
        Assert.assertEquals(0xabcdeff, string.parseLong(16));

        string = new BytesRef("abcdeff");
        nfe = false;
        try {
            string.parseLong();
        } catch (NumberFormatException e) {
            nfe = true;
        }
        Assert.assertEquals(true, nfe);

        string = new BytesRef("ffffffFFffffffff");
        nfe = false;
        try {
            string.parseLong(16);
        } catch (NumberFormatException e) {
            nfe = true;
        }
        Assert.assertEquals(true, nfe);

        string = new BytesRef("-ffffffFFffffffff");
        nfe = false;
        try {
            string.parseLong(16);
        } catch (NumberFormatException e) {
            nfe = true;
        }
        Assert.assertEquals(true, nfe);

        string = new BytesRef("7fFFffFFffffffff");
        Assert.assertEquals(Long.MAX_VALUE, string.parseLong(16));

        string = new BytesRef("-8000000000000000");
        Assert.assertEquals(Long.MIN_VALUE, string.parseLong(16));

        string = new BytesRef("ABCDEF");
        Assert.assertEquals(0xabcdef, string.parseLong(16));

        string = new BytesRef("-ABCDEF");
        Assert.assertEquals(-0xabcdef, string.parseLong(16));
    }
}
