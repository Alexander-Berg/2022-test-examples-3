package ru.yandex.market.mbo.cms.tms.executors.collectlinks;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.mbo.cms.core.models.Node;
import ru.yandex.market.mbo.cms.core.models.NodeBlock;
import ru.yandex.market.mbo.cms.core.models.NodeType;

/**
 * @author ayratgdl
 * @since 14.09.18
 */
public class UrlsExtractorTest {
    @Test(expected = NullPointerException.class)
    public void whenWidgetIsNullThenThrowNPE() {
        UrlsExtractor.extract(null);
    }

    @Test
    public void extractUrlWithoutDomainAndWithProduct() {
        Set<String> actualUrls = UrlsExtractor.extract(createWidgetWithValue("param1", "/product/1234?etc"));
        Assert.assertEquals(buildSet("/product/1234?etc"), actualUrls);
    }

    @Test
    public void extractUrlsFromParamWithTwoValues() {
        Set<String> actualUrls = UrlsExtractor.extract(createWidgetWithValue("param1", "/product/1234?etc",
                "param1", "/product/5678?etc=val"));
        Assert.assertEquals(buildSet("/product/1234?etc", "/product/5678?etc=val"), actualUrls);
    }

    @Test
    public void extractUrlsFromTwoParams() {
        Set<String> actualUrls = UrlsExtractor.extract(createWidgetWithValue("param1", "/product/1234?etc",
                "param2", "/product/5678?etc=val"));
        Assert.assertEquals(buildSet("/product/1234?etc", "/product/5678?etc=val"), actualUrls);
    }

    @Test
    public void extractUrlWithHttpsAndDomainName() {
        Set<String> actualUrls = UrlsExtractor.extract(
                createWidgetWithValue("param", "https://market.yandex.ru/product/1234")
        );
        Assert.assertEquals(buildSet("/product/1234"), actualUrls);
    }

    @Test
    public void extractUrlWithHttpAndDomainName() {
        Set<String> actualUrls = UrlsExtractor.extract(
                createWidgetWithValue("param", "http://market.yandex.ru/product/1234")
        );
        Assert.assertEquals(buildSet("/product/1234"), actualUrls);
    }

    @Test
    public void extractUrlWithDomainName() {
        Set<String> actualUrls = UrlsExtractor.extract(
                createWidgetWithValue("param", "market.yandex.ru/product/1234")
        );
        Assert.assertEquals(buildSet("/product/1234"), actualUrls);
    }

    @Test
    public void extractUrlWithHttpsAndMobileDomainName() {
        Set<String> actualUrls = UrlsExtractor.extract(
                createWidgetWithValue("param", "https://m.market.yandex.ru/product/1234")
        );
        Assert.assertEquals(buildSet("/product/1234"), actualUrls);
    }

    @Test
    public void extractCatalogUrlWithoutFilter() {
        Set<String> actualUrls = UrlsExtractor.extract(
                createWidgetWithValue("param", "/catalog/1234")
        );
        Assert.assertEquals(Collections.emptySet(), actualUrls);
    }

    @Test
    public void extractCatalogUrlWithGFilter() {
        Set<String> actualUrls = UrlsExtractor.extract(
                createWidgetWithValue("param", "/catalog/1234?gfilter=1")
        );
        Assert.assertEquals(buildSet("/catalog/1234?gfilter=1"), actualUrls);
    }

    @Test
    public void extractCatalogUrlWithGLFilter() {
        Set<String> actualUrls = UrlsExtractor.extract(
                createWidgetWithValue("param", "/catalog/1234?glfilter=1")
        );
        Assert.assertEquals(buildSet("/catalog/1234?glfilter=1"), actualUrls);
    }

    @Test
    public void extractCatalogUrlWithText() {
        Set<String> actualUrls = UrlsExtractor.extract(
                createWidgetWithValue("param", "/catalog/1234?text=1")
        );
        Assert.assertEquals(buildSet("/catalog/1234?text=1"), actualUrls);
    }

    @Test
    public void extractSearchUrl() {
        Set<String> actualUrls = UrlsExtractor.extract(
                createWidgetWithValue("param", "/search?1234")
        );
        Assert.assertEquals(buildSet("/search?1234"), actualUrls);
    }

    @Test
    public void extractMultiSearchUrl() {
        Set<String> actualUrls = UrlsExtractor.extract(
                createWidgetWithValue("param", "/multisearch?1234")
        );
        Assert.assertEquals(buildSet("/multisearch?1234"), actualUrls);
    }

    private static Node createWidgetWithValue(String... paramsAndValues) {
        Node widget = new Node(new NodeType(), 0, 0);
        NodeBlock paramBlock = new NodeBlock();
        Iterator<String> iter = Arrays.asList(paramsAndValues).iterator();
        while (iter.hasNext()) {
            paramBlock.addParameterValue(iter.next(), iter.next());
        }
        widget.setParametersBlock(paramBlock);
        return widget;
    }

    private static <T> Set<T> buildSet(T... values) {
        return new HashSet<>(Arrays.asList(values));
    }
}
