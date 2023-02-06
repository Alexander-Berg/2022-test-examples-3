package ru.yandex.base64;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.function.WriterProcessorAdapter;

public class Base64EncoderTest {
    private static final String CORNER_CASE = "@@>ng?";

    @Test
    public void test() throws IOException {
        Base64Encoder encoder = new Base64Encoder();
        encoder.process("Man".getBytes(StandardCharsets.UTF_8));
        Assert.assertEquals("TWFu", encoder.toString());
        encoder.process(
            "Новые тесты от tests.holm.ru <tests_subscribe@maillist.ru>"
                .getBytes(StandardCharsets.UTF_8));
        StringWriter sw = new StringWriter();
        encoder.processWith(new WriterProcessorAdapter(sw));
        Assert.assertEquals(
            "0J3QvtCy0YvQtSDRgtC10YHRgtGLINC+0YIgdGVzdHMuaG9"
            + "sbS5ydSA8dGVzdHNfc3Vic2NyaWJlQG1haWxsaXN0LnJ1Pg==",
            sw.toString());
        encoder.process(CORNER_CASE.getBytes(StandardCharsets.UTF_8));
        Assert.assertEquals("QEA+bmc/", encoder.toString());
        encoder.process(
            "any carnal pleasure.".getBytes(StandardCharsets.UTF_8));
        Assert.assertEquals(
            "YW55IGNhcm5hbCBwbGVhc3VyZS4=",
            encoder.toString());
    }

    @Test
    public void testUrl() throws IOException {
        Base64Encoder encoder = new Base64Encoder(Base64.URL);
        encoder.process(CORNER_CASE.getBytes(StandardCharsets.UTF_8));
        Assert.assertEquals("QEA-bmc_", encoder.toString());
        encoder.process(
            "any carnal pleasure".getBytes(StandardCharsets.UTF_8));
        Assert.assertEquals("YW55IGNhcm5hbCBwbGVhc3VyZQ", encoder.toString());
        encoder.process("Hello".getBytes(StandardCharsets.UTF_8));
        Assert.assertEquals("SGVsbG8", encoder.toString());
    }
}

