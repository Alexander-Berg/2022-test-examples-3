package ru.yandex.market.api.internal.report;

import java.net.URI;

import org.apache.http.client.utils.URIBuilder;
import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.URIMatcher;
import ru.yandex.market.api.domain.v2.redirect.parameters.SearchQuery;
import ru.yandex.market.api.search.SearchType;
import ru.yandex.market.api.util.Urls;

/**
 * @author dimkarp93
 */
public class CommonReportHandlersTest {

    @Test
    public void applyQueryNull() {
        doTest(null,
                URIMatcher.uri(
                        URIMatcher.hasNoQueryParams("text"),
                        URIMatcher.hasNoQueryParams("offer-url"),
                        URIMatcher.hasNoQueryParams("hid"),
                        URIMatcher.hasNoQueryParams("filter-warnings")
                )
        );
    }

    @Test
    public void applyQueryText() {
        doTest(SearchQuery.text("abc"),
                URIMatcher.uri(
                        URIMatcher.hasQueryParams("text", "abc"),
                        URIMatcher.hasNoQueryParams("offer-url"),
                        URIMatcher.hasNoQueryParams("hid"),
                        URIMatcher.hasNoQueryParams("filter-warnings")
                )
        );
        doTest(new SearchQuery("def", SearchType.TEXT, "yyy"),
                URIMatcher.uri(
                        URIMatcher.hasQueryParams("text", "def"),
                        URIMatcher.hasNoQueryParams("offer-url"),
                        URIMatcher.hasNoQueryParams("hid"),
                        URIMatcher.hasQueryParams("filter-warnings", "yyy")
                )
        );
    }

    @Test
    public void applyQueryBarcode() {
        doTest(new SearchQuery("abc", SearchType.BARCODE, null),
                URIMatcher.uri(
                        URIMatcher.hasQueryParams("text", "barcode:\"abc\""),
                        URIMatcher.hasNoQueryParams("offer-url"),
                        URIMatcher.hasNoQueryParams("hid"),
                        URIMatcher.hasNoQueryParams("filter-warnings")
                )
        );
        doTest(new SearchQuery("def", SearchType.BARCODE, "yyy"),
                URIMatcher.uri(
                        URIMatcher.hasQueryParams("text", "barcode:\"def\""),
                        URIMatcher.hasNoQueryParams("offer-url"),
                        URIMatcher.hasNoQueryParams("hid"),
                        URIMatcher.hasQueryParams("filter-warnings", "yyy")
                )
        );
    }

    @Test
    public void applyQueryOfferUrl() {
        doTest(new SearchQuery("abc", SearchType.URL, null),
                URIMatcher.uri(
                        URIMatcher.hasNoQueryParams("text"),
                        URIMatcher.hasQueryParams("offer-url", "abc"),
                        URIMatcher.hasNoQueryParams("hid"),
                        URIMatcher.hasNoQueryParams("filter-warnings")
                )
        );
        doTest(new SearchQuery("def", SearchType.URL, "yyy"),
                URIMatcher.uri(
                        URIMatcher.hasNoQueryParams("text"),
                        URIMatcher.hasQueryParams("offer-url", "def"),
                        URIMatcher.hasNoQueryParams("hid"),
                        URIMatcher.hasQueryParams("filter-warnings", "yyy")
                )
        );
    }

    @Test
    public void applyQueryIsbn() {
        doTest(new SearchQuery("abc", SearchType.ISBN, null),
                URIMatcher.uri(
                        URIMatcher.hasQueryParams("text", "isbn:\"abc\""),
                        URIMatcher.hasNoQueryParams("offer-url"),
                        URIMatcher.hasQueryParams("hid", "90829"),
                        URIMatcher.hasNoQueryParams("filter-warnings")
                )
        );
        doTest(new SearchQuery("def", SearchType.ISBN, "yyy"),
                URIMatcher.uri(
                        URIMatcher.hasQueryParams("text", "isbn:\"def\""),
                        URIMatcher.hasNoQueryParams("offer-url"),
                        URIMatcher.hasQueryParams("hid", "90829"),
                        URIMatcher.hasQueryParams("filter-warnings", "yyy")
                )
        );
        doTest(Urls.builder("https://exmaple.com?hid=5&hid=2&hid=7"),
                new SearchQuery("abc", SearchType.ISBN, null),
                URIMatcher.uri(
                        URIMatcher.hasQueryParams("text", "isbn:\"abc\""),
                        URIMatcher.hasNoQueryParams("offer-url"),
                        URIMatcher.hasQueryParams("hid", "5", "2", "7"),
                        URIMatcher.hasNoQueryParams("filter-warnings")
                )
        );
        doTest(Urls.builder("https://exmaple.com?hid=7&hid=90829"),
                new SearchQuery("def", SearchType.ISBN, "yyy"),
                URIMatcher.uri(
                        URIMatcher.hasQueryParams("text", "isbn:\"def\""),
                        URIMatcher.hasNoQueryParams("offer-url"),
                        URIMatcher.hasQueryParams("hid", "7", "90829"),
                        URIMatcher.hasQueryParams("filter-warnings", "yyy")
                )
        );
    }

    private void doTest(SearchQuery query, Matcher<URI> matcher) {
        URIBuilder builder = Urls.builder("https://example.com");
        doTest(builder, query, matcher);
    }

    private void doTest(URIBuilder builder, SearchQuery query, Matcher<URI> matcher) {
        CommonReportHandlers.applyQuery(builder, query);

        URI uri = Urls.toUri(builder);
        Assert.assertThat(uri, matcher);
    }
}
