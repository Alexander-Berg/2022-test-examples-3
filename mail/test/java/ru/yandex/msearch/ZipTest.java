package ru.yandex.msearch;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.compress.DeflaterOutputStream;
import ru.yandex.compress.InflaterInputStream;

import ru.yandex.test.util.TestBase;

public class ZipTest extends TestBase {

    @Test
    public void testSimple() throws Exception {
        final String testString = "TestString";
        ByteArrayOutputStream baos =
            new ByteArrayOutputStream();
        DeflaterOutputStream dos =
            new DeflaterOutputStream(baos, true);
        dos.write(testString.getBytes());
        dos.flush();

        //test jdk impl
        {
            java.util.zip.InflaterInputStream iis =
            new java.util.zip.InflaterInputStream(
                new ByteArrayInputStream(baos.toByteArray()));
            byte[] buf = new byte[testString.length() << 2];
            int red = iis.read(buf);
            String unpacked = new String(buf, 0, red);
            System.err.println(unpacked);
            Assert.assertEquals(testString, unpacked);
        }

        //test fast impl
        {
            InflaterInputStream iis =
                new InflaterInputStream(
                    new ByteArrayInputStream(baos.toByteArray()));
            byte[] buf = new byte[testString.length() << 2];
            int red = iis.read(buf);
            String unpacked = new String(buf, 0, red);
            System.err.println(unpacked);
            Assert.assertEquals(testString, unpacked);
        }

        dos.close();
    }
}

