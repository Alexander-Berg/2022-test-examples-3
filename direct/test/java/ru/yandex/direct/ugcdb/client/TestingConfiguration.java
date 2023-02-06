package ru.yandex.direct.ugcdb.client;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.direct.asynchttp.FetcherSettings;
import ru.yandex.direct.asynchttp.ParallelFetcherFactory;

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
}
