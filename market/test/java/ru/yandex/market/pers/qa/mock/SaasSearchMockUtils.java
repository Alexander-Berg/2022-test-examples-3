package ru.yandex.market.pers.qa.mock;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.http.client.methods.HttpUriRequest;
import org.mockito.ArgumentMatcher;

import ru.yandex.market.pers.qa.client.model.QuestionType;
import ru.yandex.market.pers.test.http.HttpClientMockUtils;
import ru.yandex.market.pers.test.http.QueryRegexMatcher;

import static ru.yandex.market.pers.qa.model.saas.SaasQaSearchAttribute.ENTITY_ID;
import static ru.yandex.market.pers.qa.model.saas.SaasQaSearchAttribute.ENTITY_TYPE;

public class SaasSearchMockUtils {
    private SaasSearchMockUtils() {
    }

    public static ArgumentMatcher<HttpUriRequest> pageFilter(long pageNum, long pageSize) {
        return HttpClientMockUtils.and(
            HttpClientMockUtils.withQueryParam("p", (pageNum - 1)),
            HttpClientMockUtils.withQueryParam("numdoc", pageSize)
        );
    }

    public static ArgumentMatcher<HttpUriRequest> entityFilter(QuestionType questionType, long entityId) {
        return HttpClientMockUtils.and(
            SaasSearchMockUtils
                .withSearchAttribute(ENTITY_TYPE.getName(), String.valueOf(questionType.getValue())),
            SaasSearchMockUtils.withSearchAttribute(ENTITY_ID.getName(), String.valueOf(entityId))
        );
    }

    public static ArgumentMatcher<HttpUriRequest> withSearchAttribute(String name, String... values) {
        if (values.length == 0) {
            throw new RuntimeException("no values");
        }
        String expectedQuery = Arrays.stream(values)
            .map(x -> name + ":" + x)
            .collect(Collectors.joining(" \\| "));
        return HttpClientMockUtils.withQueryParam(".*?text=.*?\\(?" + expectedQuery + "\\)?.*?");
    }

    public static ArgumentMatcher<HttpUriRequest> withSearchExcludeAttribute(String name, String value) {
        return HttpClientMockUtils.withQueryParam(".*?text=.*? ~~ \\(?" + name + ":" + value + "\\)?.*?");
    }

    public static ArgumentMatcher<HttpUriRequest> withNoCache() {
        return HttpClientMockUtils.withQueryParam("nocache", "1");
    }

    public static ArgumentMatcher<HttpUriRequest> withDescSort(String sort) {
        return HttpClientMockUtils.and(
            new QueryRegexMatcher("(?!.*asc=1.*).*"),
            HttpClientMockUtils.withQueryParam("how", sort)
        );
    }

    public static ArgumentMatcher<HttpUriRequest> withAscSort(String sort) {
        return HttpClientMockUtils.and(
            HttpClientMockUtils.withQueryParam("how", sort),
            HttpClientMockUtils.withQueryParam("asc", "1")
        );
    }
}
