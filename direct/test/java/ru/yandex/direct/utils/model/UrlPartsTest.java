package ru.yandex.direct.utils.model;

import org.junit.Assert;
import org.junit.Test;

public class UrlPartsTest {
    @Test
    public void testReplaceParamIfExistsWithParam() {
        UrlParts.Builder builder = UrlParts.fromUrl("https://example.com?param=value").toBuilder();

        builder.replaceParamIfExists("param", "changed_value");

        Assert.assertEquals(builder.build().toUrl(), "https://example.com?param=changed_value");
    }

    @Test
    public void testReplaceParamIfExistsWithoutParam() {
        UrlParts.Builder builder = UrlParts.fromUrl("https://example.com?another_param=value").toBuilder();

        builder.replaceParamIfExists("param", "changed_value");

        Assert.assertEquals(builder.build().toUrl(), "https://example.com?another_param=value");
    }

    @Test
    public void testReplaceOrAddParamWithParam() {
        UrlParts.Builder builder = UrlParts.fromUrl("https://example.com?param=value").toBuilder();

        builder.replaceOrAddParam("param", "changed_value");

        Assert.assertEquals(builder.build().toUrl(), "https://example.com?param=changed_value");
    }

    @Test
    public void testReplaceOrAddParamWithoutParam() {
        UrlParts.Builder builder = UrlParts.fromUrl("https://example.com?another_param=value").toBuilder();

        builder.replaceOrAddParam("param", "changed_value");

        Assert.assertEquals(builder.build().toUrl(), "https://example.com?another_param=value&param=changed_value");
    }

    @Test
    public void testAddParamIfNotExistsWithParam() {
        UrlParts.Builder builder = UrlParts.fromUrl("https://example.com?param=value").toBuilder();

        builder.addParamIfNotExists("param", "changed_value");

        Assert.assertEquals(builder.build().toUrl(), "https://example.com?param=value");
    }

    @Test
    public void testAddParamIfNotExistsWithoutParam() {
        UrlParts.Builder builder = UrlParts.fromUrl("https://example.com?another_param=value").toBuilder();

        builder.addParamIfNotExists("param", "changed_value");

        Assert.assertEquals(builder.build().toUrl(), "https://example.com?another_param=value&param=changed_value");
    }
}
