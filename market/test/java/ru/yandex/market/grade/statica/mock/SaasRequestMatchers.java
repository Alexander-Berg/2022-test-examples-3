package ru.yandex.market.grade.statica.mock;

import org.apache.http.client.methods.HttpUriRequest;
import org.mockito.ArgumentMatcher;

import ru.yandex.market.pers.test.http.HttpClientMockUtils;
import ru.yandex.market.pers.test.http.QueryRegexMatcher;

public class SaasRequestMatchers {
    private SaasRequestMatchers() {

    }

    public static ArgumentMatcher<HttpUriRequest> withSearchAttribute(String name, String value) {
        return withQueryParam(".*text=.*\\(" + name + ":" + value + "\\).*");
    }

    public static ArgumentMatcher<HttpUriRequest> withNoCache() {
        return withQueryParam("nocache=1");
    }

    public static ArgumentMatcher<HttpUriRequest> withDescSort(String sort) {
        return HttpClientMockUtils.and(
            new QueryRegexMatcher("(?!.*asc=1.*).*"),
            withQueryParam("how=" + sort));
    }

    public static ArgumentMatcher<HttpUriRequest> withAscSort(String sort) {
        return withQueryParam("how=" + sort + "&asc=1");
    }

    public static ArgumentMatcher<HttpUriRequest> withQueryParam(String nameAndValue) {
        return HttpClientMockUtils.withQueryParam(nameAndValue);
    }

    public static ArgumentMatcher<HttpUriRequest> withQueryRegexp(String regexp) {
        return new QueryRegexMatcher(regexp);
    }

}
