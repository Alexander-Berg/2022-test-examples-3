package ru.yandex.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.test.util.TestBase;

public class TrimmingWriterTest extends TestBase {
    private static final String HELLO = "Hello,";
    private static final String TEXT = "Hello, world\nNice to meet you";
    private static final int BUFFER_SIZE = 65536;

    @Test
    public void testGoodText() throws IOException {
        StringWriter sw = new StringWriter();
        try (Writer writer = new TrimmingWriter(sw)) {
            writer.write(TEXT);
        }
        Assert.assertEquals(TEXT, sw.toString());
    }

    @Test
    public void testSpacesText() throws IOException {
        StringWriter sw = new StringWriter();
        try (Writer writer = new TrimmingWriter(sw)) {
            writer.write("\n\tHello,  world \n\n \r\n Nice to meet\tyou\r\n");
        }
        Assert.assertEquals(TEXT, sw.toString());
    }

    @Test
    public void testWriteByPieces() throws IOException {
        StringWriter sw = new StringWriter();
        try (Writer writer = new TrimmingWriter(sw)) {
            writer.write("   Hello, world ");
            writer.write('\n');
            writer.write(' ');
            writer.write("\u00A0Nice to meet you  ");
        }
        Assert.assertEquals(TEXT, sw.toString());
    }

    @Test
    public void testFlush() throws IOException {
        StringWriter sw = new StringWriter();
        try (Writer writer =
            new TrimmingWriter(new BufferedWriter(sw, BUFFER_SIZE)))
        {
            writer.write(TEXT + '\t');
            Assert.assertEquals("", sw.toString());
            writer.flush();
            Assert.assertEquals(TEXT, sw.toString());
        }
    }

    @Test
    public void testClose() throws IOException {
        StringWriter sw = new StringWriter();
        try (Writer writer =
            new TrimmingWriter(new BufferedWriter(sw, BUFFER_SIZE)))
        {
            writer.write('\t' + TEXT + "  ");
            Assert.assertEquals("", sw.toString());
        }
        Assert.assertEquals(TEXT, sw.toString());
    }

    @Test
    public void testFlushInTheMiddle() throws IOException {
        StringWriter sw = new StringWriter();
        try (Writer writer =
            new TrimmingWriter(new BufferedWriter(sw, BUFFER_SIZE)))
        {
            writer.write("\tHello, ");
            Assert.assertEquals("", sw.toString());
            writer.flush();
            Assert.assertEquals(HELLO, sw.toString());
            writer.write(" world\r\n");
            Assert.assertEquals(HELLO, sw.toString());
        }
        Assert.assertEquals("Hello, world", sw.toString());
    }
}

