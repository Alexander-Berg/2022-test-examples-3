package ru.yandex.market.core.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import ru.yandex.market.common.retrofit.RetryStrategy;
import ru.yandex.market.core.npd.client.IntegrationNpdRetrofitService;
import ru.yandex.market.core.npd.client.IntegrationNpdService;
import ru.yandex.market.integration.npd.client.api.ApplicationApi;
import ru.yandex.market.request.trace.Module;

@Configuration
public class IntegrationNpdRetrofitClientTestConfig {
    @Bean
    WireMockConfiguration integrationNpdWireMockConfiguration() {
        return WireMockConfiguration.wireMockConfig().dynamicPort();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    WireMockServer integrationNpdWireMockServer(WireMockConfiguration integrationNpdWireMockConfiguration) {
        return new WireMockServer(integrationNpdWireMockConfiguration);
    }

    @Bean
    public Retrofit integrationNpdRetrofit(WireMockServer integrationNpdWireMockServer) {
        OkHttpClient client = new OkHttpClient.Builder()
                .build();

        var retrofitBuilder = new Retrofit.Builder()
                .client(client)
                .baseUrl(integrationNpdWireMockServer.baseUrl())
                .validateEagerly(true)
                .addConverterFactory(JacksonConverterFactory.create(
                        new ObjectMapper()
                                .registerModule(new JavaTimeModule())
                                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                ));

        return retrofitBuilder.build();
    }

    @Bean
    public ApplicationApi integrationNpdApi(Retrofit integrationNpdRetrofit) {
        return integrationNpdRetrofit.create(ApplicationApi.class);
    }

    @Bean
    public IntegrationNpdRetrofitService integrationNpdRetrofitService(Retrofit integrationNpdRetrofit,
                                                                       Module sourceModule) {
        return new IntegrationNpdRetrofitService(integrationNpdRetrofit, RetryStrategy.NO_RETRY_STRATEGY, sourceModule);
    }

    @Bean
    public IntegrationNpdService integrationNpdService(
            IntegrationNpdRetrofitService integrationNpdRetrofitService
    ) {
        return new IntegrationNpdService(integrationNpdRetrofitService);
    }

}
