package ru.yandex.market.common.report;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.common.report.model.ModelInfoSearchRequest;
import ru.yandex.market.common.report.model.ModelInfoSearchRequestUrlBuilder;

import static org.hamcrest.CoreMatchers.is;
import static ru.yandex.common.util.currency.Currency.RUR;

/**
 * @author vbudnev
 */
public class GenericMarketReportServiceTest extends Assert {
    private final Long REGION_213 = 213L;
    private final List<Long> HYPER_ID_LIST_1_2 = Arrays.asList(1L, 2L);
    private final String EXAMPLE_URL = "http://www.example.com";

    private Map<Class, SearchRequestUrlBuilder> builders;

    @Before
    public void before() {
        builders = new HashMap<>();
    }

    @Test
    public void test_buildUrl_should_workWithoutPersistentOptions() throws Exception {
        ModelInfoSearchRequest req = new ModelInfoSearchRequest(REGION_213, RUR, HYPER_ID_LIST_1_2);

        builders.put(ModelInfoSearchRequest.class, new ModelInfoSearchRequestUrlBuilder(EXAMPLE_URL));
        GenericMarketReportService service = new GenericMarketReportService(builders);

        String expected = "http://www.example.com" +
                "?place=modelinfo&pp=18&rids=213&currency=RUR&hyperid=1&hyperid=2&use-virt-shop=0";
        String actual = service.buildUrl(req);
        assertThat(actual, is(expected));
    }

    @Test
    public void test_buildUrl_should_appendPersistent() throws Exception {
        ModelInfoSearchRequest req = new ModelInfoSearchRequest(REGION_213, RUR, HYPER_ID_LIST_1_2);

        builders.put(ModelInfoSearchRequest.class, new ModelInfoSearchRequestUrlBuilder(EXAMPLE_URL));

        GenericMarketReportService service = new GenericMarketReportService(
                builders,
                //для теста берем надежный порядок, иначе сравнивать строку будет сложнее
                new TreeMap<String, String>() {{
                    put("planet", "earth");
                    put("start", "sun");
                }}
        );

        String expected = "http://www.example.com" +
                "?place=modelinfo&pp=18&rids=213&currency=RUR&hyperid=1&hyperid=2" +
                "&use-virt-shop=0" +
                "&planet=earth&start=sun";
        String actual = service.buildUrl(req);
        assertThat(actual, is(expected));
    }

    @Test
    public void test_buildUrl_should_urlencodeKeysAndValues() throws Exception {
        ModelInfoSearchRequest req = new ModelInfoSearchRequest(REGION_213, RUR, HYPER_ID_LIST_1_2);

        builders.put(ModelInfoSearchRequest.class, new ModelInfoSearchRequestUrlBuilder(EXAMPLE_URL));

        GenericMarketReportService service = new GenericMarketReportService(
                builders,
                new HashMap<String, String>() {{
                    put("планеты&спутники", "earth&mars%ио");
                }}
        );

        String expected = "http://www.example.com" +
                "?place=modelinfo&pp=18&rids=213&currency=RUR&hyperid=1&hyperid=2&use-virt-shop=0" +
                "&%D0%BF%D0%BB%D0%B0%D0%BD%D0%B5%D1%82%D1%8B%26%D1%81%D0%BF%D1%83%D1%82%D0%BD%D0%B8%D0%BA%D0%B8=earth" +
                "%26mars%25%D0%B8%D0%BE";
        String actual = service.buildUrl(req);
        assertThat(actual, is(expected));
    }
}
