package ru.yandex.market.billing.calendar;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockWebServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import retrofit2.Retrofit;

import ru.yandex.market.core.calendar.api.YandexCalendarApi;

@Configuration
public class YandexCalendarApiTestConfig {

    @Bean
    public MockWebServer yandexCalendarApiMockWebServer() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start();
        return server;
    }

    @Bean
    public YandexCalendarApi yandexCalendarApi(Retrofit yandexCalendarApiRetrofitBuilder,
                                               MockWebServer yandexCalendarApiMockWebServer) {
        HttpUrl url = yandexCalendarApiMockWebServer.url("/");
        return yandexCalendarApiRetrofitBuilder.newBuilder()
                .baseUrl(url)
                .build()
                .create(YandexCalendarApi.class);
    }
}
