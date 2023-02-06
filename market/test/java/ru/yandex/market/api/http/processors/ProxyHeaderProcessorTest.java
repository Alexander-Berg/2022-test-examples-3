package ru.yandex.market.api.http.processors;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.util.httpclient.processors.ProxyHeaderProcessor;

/**
 * @author dimkarp93
 */
public class ProxyHeaderProcessorTest extends UnitTestBase {
    @Test
    public void addParamNoExists() {
        ProxyHeaderProcessor processor = ProxyHeaderProcessor.ofContextAddParam("abc", ctx -> "def");
        HttpHeaders headers = new DefaultHttpHeaders();
        processor.process(headers);
        Assert.assertEquals("def", headers.get("abc"));
    }

    @Test
    public void addParamExists() {
        ProxyHeaderProcessor processor = ProxyHeaderProcessor.ofContextAddParam("abc", ctx -> "def");
        HttpHeaders headers = new DefaultHttpHeaders();
        headers.add("abc", "xyz");
        processor.process(headers);
        Assert.assertEquals("xyz", headers.get("abc"));
    }

    @Test
    public void overrideParamNoExists() {
        ProxyHeaderProcessor processor = ProxyHeaderProcessor.ofContextOverrideParam("abc", ctx -> "def");
        HttpHeaders headers = new DefaultHttpHeaders();
        processor.process(headers);
        Assert.assertEquals("def", headers.get("abc"));
    }

    @Test
    public void overrideParamExists() {
        ProxyHeaderProcessor processor = ProxyHeaderProcessor.ofContextOverrideParam("abc", ctx -> "def");
        HttpHeaders headers = new DefaultHttpHeaders();
        headers.add("abc", "xyz");
        processor.process(headers);
        Assert.assertEquals("def", headers.get("abc"));
    }
}
