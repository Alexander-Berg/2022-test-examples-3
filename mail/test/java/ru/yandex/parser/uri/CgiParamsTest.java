package ru.yandex.parser.uri;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.base64.Base64Decoder;
import ru.yandex.charset.Decoder;

public class CgiParamsTest {
    @Test
    public void testBase64() throws Exception {
        Base64Decoder base64Decoder = new Base64Decoder();
        Decoder decoder = new Decoder(StandardCharsets.UTF_8);
        CgiParams params = new CgiParams("ng=QEA%2Bbmc/&hi=SGkgdGhlcmU=&1&&"
            + "long=0J3QvtCy0YvQtSDRgtC10YHRgtGLINC%2B0YIgdGVzdHMuaG9sbS5ydSA8"
            + "dGVzdHNfc3Vic2NyaWJlQG1haWxsaXN0LnJ1Pg==&&");
        base64Decoder.process(params.getString("ng").toCharArray());
        base64Decoder.processWith(decoder);
        Assert.assertEquals("@@>ng?", decoder.toString());
        base64Decoder.process(params.getString("hi").toCharArray());
        base64Decoder.processWith(decoder);
        Assert.assertEquals("Hi there", decoder.toString());
        Assert.assertEquals(true, params.getBoolean("1"));
        base64Decoder.process(params.getString("long").toCharArray());
        base64Decoder.processWith(decoder);
        Assert.assertEquals("Новые тесты от tests.holm.ru "
            + "<tests_subscribe@maillist.ru>", decoder.toString());
    }

    @Test
    public void testEmpty() {
        Assert.assertEquals("abc", new CgiParams("=abc").getString("", null));
    }

    @Test
    public void testLastParam() throws Exception {
        String param = "deadline";
        String query = param + "=1&slow&" + param + "=0&" + param + "=2";
        for (CgiParamsBase params: new CgiParamsBase[] {
            new CgiParams(query),
            new ScanningCgiParams(query)})
        {
            Assert.assertEquals(
                Long.valueOf(1L),
                params.get(param, Long::valueOf));
            Assert.assertEquals(
                Long.valueOf(2L),
                params.getLast(param, Long::valueOf));
            Assert.assertEquals(
                Arrays.asList("1", "0", "2"),
                params.getAll(param, x -> x, new ArrayList<>()));
        }
    }
}

