package ru.yandex.chemodan.app.docviewer.copy.provider;

import org.junit.Test;

import ru.yandex.chemodan.app.docviewer.copy.DocumentSourceInfo;
import ru.yandex.misc.test.Assert;

/**
 * @author akirakozov
 */
public class SerpUrlProviderTest {

    @Test
    public void rewriteYaSerpUrl() {
        SerpUrlProvider provider = new SerpUrlProvider("serp/get", false);
        String result = provider.rewriteUrl(
                DocumentSourceInfo.builder().originalUrl("ya-serp://some.example.ru/a.pdf").build());
        Assert.equals("http://serp/get?url=http%3A%2F%2Fsome.example.ru%2Fa.pdf&noconv=1", result);
    }

    @Test
    public void rewriteYaSerpHttpsUrl() {
        SerpUrlProvider provider = new SerpUrlProvider("serp/get", true);
        String result = provider.rewriteUrl(
                DocumentSourceInfo.builder().originalUrl("ya-serps://some.example.ru/a.pdf").build());
        Assert.equals("http://serp/get?url=https%3A%2F%2Fsome.example.ru%2Fa.pdf&noconv=1", result);
    }

}
