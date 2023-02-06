package ru.yandex.chemodan.trust.client;

import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.HttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.chemodan.boot.value.OverridableValuePrefix;
import ru.yandex.chemodan.http.LoggingHttpInterceptor;
import ru.yandex.chemodan.util.http.HttpClientConfigurator;
import ru.yandex.inside.passport.tvm2.web.Tvm2BaseContextConfiguration;

@Configuration
@Import(Tvm2BaseContextConfiguration.class)
public class TrustClientConfiguration {
    @Bean
    @OverridableValuePrefix("trust")
    public HttpClientConfigurator trustHttpClientConfigurator() {
        return new HttpClientConfigurator();
    }

    @Bean
    public TrustClient trustClient(@Value("${trust.url}") String server) {
        LoggingHttpInterceptor httpInterceptor = new LoggingHttpInterceptor("trust", true);
        HttpClientConfigurator httpConfig = trustHttpClientConfigurator();
        httpConfig.setTvmDisabled(true);
        HttpClient httpClient = httpConfig.createBuilder()
                .multiThreaded()
                .withInterceptorLast((HttpRequestInterceptor) httpInterceptor)
                .withInterceptorFirst((HttpResponseInterceptor) httpInterceptor)
                .build();
        return new TrustClient(httpClient, server,
                Cf.map(690, "https://yav.yandex-team.ru/edit/secret/sec-01e6bpshy3aehd9ex83j9sp239/explore/versions"));
    }
}
