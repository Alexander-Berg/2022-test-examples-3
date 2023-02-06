package ru.yandex.market.api.internal.report;

import java.net.URISyntaxException;
import java.util.HashMap;

import javax.inject.Inject;

import org.apache.http.client.utils.URIBuilder;
import org.junit.Test;

import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithMocks;
import ru.yandex.market.api.util.Urls;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;

@WithMocks
public class ReportFilterQueryBuilderTest extends BaseTest {

    @Inject
    private ReportFilterQueryBuilder builder;

    @Test
    public void testApplyParameterWithCPA() throws URISyntaxException {
        URIBuilder uriBuilder = Urls.builder("test.yandex.ru").setPath("/yandsearch");
        HashMap<String, String> filterParams = new HashMap<>();
        filterParams.put("-7", "1");
        builder.apply(uriBuilder, filterParams);
        String actual = uriBuilder.build().toString();
        assertThat(actual, containsString("cpa=real"));
    }

    @Test
    public void testApplyParameterWithoutCPA() throws URISyntaxException {
        URIBuilder uriBuilder = Urls.builder("test.yandex.ru").setPath("/yandsearch");
        HashMap<String, String> filterParams = new HashMap<>();
        filterParams.put("-7", "");
        builder.apply(uriBuilder, filterParams);
        String actual = uriBuilder.build().toString();
        assertThat(actual, !actual.contains("cpa=real"));
    }

    @Test
    public void testApplyParameterWithCPAEqualZero() throws URISyntaxException {
        URIBuilder uriBuilder = Urls.builder("test.yandex.ru").setPath("/yandsearch");
        HashMap<String, String> filterParams = new HashMap<>();
        filterParams.put("-7", "0");
        builder.apply(uriBuilder, filterParams);
        String actual = uriBuilder.build().toString();
        assertThat(actual, !actual.contains("cpa="));
    }
}
