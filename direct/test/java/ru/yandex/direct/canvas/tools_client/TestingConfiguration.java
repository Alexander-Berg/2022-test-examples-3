package ru.yandex.direct.canvas.tools_client;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.direct.asynchttp.FetcherSettings;
import ru.yandex.direct.asynchttp.ParallelFetcherFactory;
import ru.yandex.direct.http.smart.core.Smart;

@Configuration
public class TestingConfiguration {
    @Bean
    public AsyncHttpClient asyncHttpClient() {
        return new DefaultAsyncHttpClient();
    }

    @Bean
    public ParallelFetcherFactory fetcherFactory(AsyncHttpClient asyncHttpClient) {
        return new ParallelFetcherFactory(asyncHttpClient, new FetcherSettings());
    }

    @Bean
    public Smart.Builder smartBuilder(AsyncHttpClient asyncHttpClient) {
        return Smart.builder().withParallelFetcherFactory(fetcherFactory(asyncHttpClient)).withProfileName("test");
    }
}
