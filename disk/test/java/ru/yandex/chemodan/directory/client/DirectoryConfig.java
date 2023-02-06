package ru.yandex.chemodan.directory.client;

import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.HttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import ru.yandex.chemodan.boot.value.OverridableValuePrefix;
import ru.yandex.chemodan.http.LoggingHttpInterceptor;
import ru.yandex.chemodan.util.http.HttpClientConfigurator;
import ru.yandex.inside.passport.tvm2.web.Tvm2BaseContextConfiguration;

@Configuration
@Import(Tvm2BaseContextConfiguration.class)
public class DirectoryConfig {
    @Bean
    @Qualifier("directory")
    @OverridableValuePrefix("directory")
    public HttpClientConfigurator directoryHttpClientConfigurator() {
        return new HttpClientConfigurator();
    }

    @Bean
    public DirectoryClient directoryClient(@Value("${directory.host}") String host) {
        LoggingHttpInterceptor httpInterceptor = new LoggingHttpInterceptor("directory", true);
        HttpClientConfigurator httpConfig = directoryHttpClientConfigurator();
        httpConfig.setTvmDisabled(true);
        HttpClient httpClient = httpConfig.createBuilder()
                .multiThreaded()
                .withInterceptorLast((HttpRequestInterceptor) httpInterceptor)
                .withInterceptorFirst((HttpResponseInterceptor) httpInterceptor)
                .build();
        RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
        return new DirectoryClient(host, restTemplate);
    }
}
