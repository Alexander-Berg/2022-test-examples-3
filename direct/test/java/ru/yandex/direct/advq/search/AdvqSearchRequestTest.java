package ru.yandex.direct.advq.search;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;

import one.util.streamex.StreamEx;
import org.asynchttpclient.Param;
import org.asynchttpclient.Request;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.advq.Device;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static ru.yandex.direct.advq.AdvqClient.X_ADVQ_CUSTOMER_HEADER;
import static ru.yandex.direct.tracing.util.TraceUtil.X_YANDEX_TRACE;


public class AdvqSearchRequestTest {

    private static final String ADVQ_URL = "http://back-normal.advq.yandex.net";
    private static final String TEST_YANDEX_TRACE = "test-yandex-trace";
    private static final String ADVQ_CUSTOMER_NAME = "direct.unittest";

    private AdvqSearchRequest skeleton;

    @Before
    public void setUp() throws Exception {
        skeleton = new AdvqSearchRequest()
                .withId(1L)
                .withWords(singletonList("ADVQ"));
    }

    /**
     * Возвращает результат вызова {@link AdvqSearchRequest#formRequest(String, String, String)}
     * на переданном {@code request}
     */
    private static Request formRequest(AdvqSearchRequest request) {
        return request.formRequest(ADVQ_URL, TEST_YANDEX_TRACE, ADVQ_CUSTOMER_NAME);
    }

    /**
     * Так как {@code toString()} у {@link Param} не даёт полезной информации,
     * преобразуем набор параметров в {@link Map} для наглядности
     */
    private static Map<String, String> formParamsAsMap(Request request) {
        Collection<Param> params = request.getFormParams();
        return StreamEx.of(params)
                .mapToEntry(Param::getName, Param::getValue)
                .toMap();
    }

    @Test
    public void formRequest_success_minimalRequest() {
        Request actual = formRequest(skeleton);
        assertSoftly(softly -> {
            softly.assertThat(actual.getHeaders().get(X_YANDEX_TRACE)).isEqualTo(TEST_YANDEX_TRACE);
            softly.assertThat(actual.getHeaders().get(X_ADVQ_CUSTOMER_HEADER)).isEqualTo(ADVQ_CUSTOMER_NAME);
            //noinspection unchecked
            softly.assertThat(formParamsAsMap(actual))
                    .contains(entry("parser", "advq"),
                            entry("format", "json"),
                            entry("words", "ADVQ"));
        });
    }

    @Test
    public void formRequest_success_withTimeout() {
        Request actual = formRequest(skeleton.withTimeout(Duration.ofSeconds(20)));
        assertThat(formParamsAsMap(actual))
                .contains(entry("timeout", Double.toString(20)));
    }

    @Test
    public void formRequest_success_withRegions() {
        Request actual = formRequest(skeleton.withRegions(asList(1L, 2L)));
        assertThat(formParamsAsMap(actual))
                .contains(entry("regions", "1,2"));
    }

    @Test
    public void formRequest_success_withDevices() {
        Request actual = formRequest(skeleton.withDevices(
                asList(Device.ALL, Device.DESKTOP, Device.PHONE, Device.TABLET)));
        // ADVQ ожидает значение параметра devices в lowercase
        assertThat(formParamsAsMap(actual))
                .contains(entry("devices", "all,desktop,phone,tablet"));
    }

    @Test
    public void formRequest_success_withDbName() {
        Request actual = formRequest(skeleton.withDbName("rus"));
        // Кажется, что dbname должен быть enum'ом (rus/tur/...). Но пока что это строка
        assertThat(formParamsAsMap(actual))
                .contains(entry("dbname", "rus"));
    }

    @Test
    public void formRequest_success_withCalcTotalHist() {
        Request actual = formRequest(skeleton.withCalcTotalHits(true));
        assertThat(formParamsAsMap(actual))
                .contains(entry("calc_total_hits", "1"));
    }

    @Test
    public void formRequest_success_withAssocs() {
        Request actual = formRequest(skeleton.withAssocs(true));
        assertThat(formParamsAsMap(actual))
                .contains(entry("assocs", "1"));
    }

    @Test
    public void formRequest_success_withFastMode() {
        Request actual = formRequest(skeleton.withFastMode(true));
        assertThat(formParamsAsMap(actual))
                .contains(entry("fast_mode", "1"));
    }

    @Test
    public void formRequest_success_withPhPage() {
        Request actual = formRequest(skeleton.withPhPage(1));
        assertThat(formParamsAsMap(actual))
                .contains(entry("ph_page", "1"));
    }

    @Test
    public void formRequest_success_withPhPageSize() {
        Request actual = formRequest(skeleton.withPhPageSize(100));
        assertThat(formParamsAsMap(actual))
                .contains(entry("ph_page_size", "100"));
    }

}
