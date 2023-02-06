package ru.yandex.market.adv.http.interceptor;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import ru.yandex.market.mbi.web.solomon.pull.SolomonUtils;
import ru.yandex.market.request.trace.Module;
import ru.yandex.monlib.metrics.labels.Labels;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

/**
 * Date: 07.12.2021
 * Project: arcadia-market_adv_adv-shop
 *
 * @author alexminakov
 */
class MonitoringInterceptorTest {

    private final MonitoringInterceptor monitoringInterceptor = new MonitoringInterceptor(Module.MBO_AUDIT, 399);

    @DisplayName("Проверяем, что интерцептор корректно отработал и нужное количество раз увеличил мониторинг.")
    @ParameterizedTest(name = "{1} - {2}")
    @CsvSource({
            "200,2xx,1",
            "300,3xx,1",
            "400,4xx,2",
            "500,5xx,2"
    })
    void intercept_mock_monitoringResult(int code, String label, int count) throws IOException {
        Interceptor.Chain chain = Mockito.mock(Interceptor.Chain.class);
        doReturn(
                new Response.Builder()
                        .request(
                                new Request.Builder()
                                        .url("https://localhost:80/ping")
                                        .build()
                        )
                        .protocol(Protocol.HTTP_1_1)
                        .message("OK")
                        .code(code)
                        .build()
        )
                .when(chain)
                .proceed(any());

        monitoringInterceptor.intercept(chain);

        Assertions.assertThat(
                        SolomonUtils.getMetricRegistry()
                                .counter("market_mbo-audit_response_code", Labels.of("code", label))
                                .inc()
                )
                .isEqualTo(count);
    }
}
