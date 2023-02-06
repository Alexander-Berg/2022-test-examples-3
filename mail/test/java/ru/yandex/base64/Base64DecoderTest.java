package ru.yandex.base64;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.charset.Decoder;

public class Base64DecoderTest {
    private static final String CORNER_CASE = "@@>ng?";

    @Test
    public void test() throws IOException {
        Base64Decoder base64Decoder = new Base64Decoder();
        Decoder decoder = new Decoder(StandardCharsets.UTF_8);
        base64Decoder.process("TWFu".toCharArray());
        base64Decoder.processWith(decoder);
        Assert.assertEquals("Man", decoder.toString());
        base64Decoder.process(
            ("0J3QvtCy0YvQtSDRgtC10YHRgtGLINC+0YIgdGVzdHMuaG9"
                + "sbS5ydSA8dGVzdHNfc3Vic2NyaWJlQG1haWxsaXN0LnJ1Pg==")
                .toCharArray());
        base64Decoder.processWith(decoder);
        Assert.assertEquals("Новые тесты от tests.holm.ru "
            + "<tests_subscribe@maillist.ru>", decoder.toString());
        base64Decoder.process("QEA+bmc/".toCharArray());
        base64Decoder.processWith(decoder);
        Assert.assertEquals(CORNER_CASE, decoder.toString());
        base64Decoder.process("YW55IGNhcm5hbCBwbGVhc3VyZS4=".toCharArray());
        base64Decoder.processWith(decoder);
        Assert.assertEquals("any carnal pleasure.", decoder.toString());
    }

    @Test
    public void testUrl() throws IOException {
        Base64Decoder base64Decoder = new Base64Decoder(Base64.URL);
        Decoder decoder = new Decoder(StandardCharsets.UTF_8);
        base64Decoder.process("QEA-bmc_".toCharArray());
        base64Decoder.processWith(decoder);
        Assert.assertEquals(CORNER_CASE, decoder.toString());
        base64Decoder.process("YW55IGNhcm5hbCBwbGVhc3VyZQ".toCharArray());
        base64Decoder.processWith(decoder);
        Assert.assertEquals("any carnal pleasure", decoder.toString());
    }
}

