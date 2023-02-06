package ru.yandex.util.ip;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.io.DecodableByteArrayOutputStream;
import ru.yandex.test.util.TestBase;

public class CidrsMergerTest extends TestBase {
    private static String merge(final String input) throws Exception {
        DecodableByteArrayOutputStream outStream =
            new DecodableByteArrayOutputStream();
        InputStream in = System.in;
        PrintStream out = System.out;
        try {
            System.setIn(
                new ByteArrayInputStream(
                    input.getBytes(StandardCharsets.UTF_8)));
            System.setOut(new PrintStream(outStream));
            CidrsMerger.main();
        } finally {
            System.setIn(in);
            System.setOut(out);
        }
        return new String(outStream.toByteArray(), StandardCharsets.UTF_8);
    }

    @Test
    public void test() throws Exception {
        Assert.assertEquals(
            "8::/14\n2a::/16\n20f8::/13\n",
            merge("a::/15\n8::/16\n9::/16\n20fa:0000::0000/13\n2a::/16"));
    }

    @Test
    public void testDuplicates() throws Exception {
        Assert.assertEquals(
            "5.35.14.0/23\n"
            + "45.200.0.0/16\n"
            + "45.204.12.33\n"
            + "45.204.13.40\n"
            + "77.88.34.0/23\n",
            merge(
                "77.88.34.0/24\n77.88.35.0/24\n45.204.12.33\n45.204.13.40\n"
                + "45.200.0.0/16\n5.35.14.0/23\n5.35.15.44\n\n"));
    }
}

