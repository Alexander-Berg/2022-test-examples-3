package ru.yandex.market.core.fulfillment.report;

import java.io.IOException;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockWebServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import retrofit2.Retrofit;

import retrofit2.converter.gson.GsonConverterFactory;
import ru.yandex.market.core.fulfillment.report.oebs.OebsReportApi;

@Configuration
public class OebsReportTestConfig {

    @Bean
    public MockWebServer oebsReportApiMockWebServer() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start();
        return server;
    }

    @Bean
    public OebsReportApi oebsReportApi(MockWebServer oebsReportApiMockWebServer) {
        String url = oebsReportApiMockWebServer.url("/").toString();
        return new Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create(
                        new GsonBuilder()
                                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                                .setLenient()
                                .create()))
                .client(new OkHttpClient.Builder()
                        .build())
                .build()
                .create(OebsReportApi.class);
    }

}
