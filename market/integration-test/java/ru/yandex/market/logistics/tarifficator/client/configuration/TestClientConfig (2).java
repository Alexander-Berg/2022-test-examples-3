package ru.yandex.market.logistics.tarifficator.client.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import okhttp3.Call;
import org.asynchttpclient.Dsl;
import org.asynchttpclient.extras.retrofit.AsyncHttpClientCallFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.MimeTypeUtils;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import ru.yandex.market.common.retrofit.RetrofitUtils;
import ru.yandex.market.logistics.util.client.ClientUtilsFactory;
import ru.yandex.market.logistics.util.client.HttpTemplate;
import ru.yandex.market.logistics.util.client.TvmTicketProvider;

@Configuration
public class TestClientConfig {

    @Value("${tarifficator.wiremock.api.host}")
    private String wiremockHost;

    @Bean
    public TvmTicketProvider tvmTicketProvider() {
        return new TvmTicketProvider() {
            @Override
            public String provideServiceTicket() {
                return "test-service-ticket";
            }

            @Override
            public String provideUserTicket() {
                return "test-user-ticket";
            }
        };
    }

    @Bean
    public ObjectMapper objectMapper() {
        return ClientUtilsFactory.getObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Bean
    public Retrofit retrofit(
        TvmTicketProvider tvmTicketProvider,
        ObjectMapper objectMapper
    ) {
        Call.Factory callFactory = AsyncHttpClientCallFactory.builder()
            .httpClient(Dsl.asyncHttpClient())
            .build();

        return RetrofitUtils.build(
            callFactory,
            wiremockHost,
            (request, builder) -> builder
                .addHeader("Accept", MimeTypeUtils.APPLICATION_JSON_VALUE)
                .header(HttpTemplate.SERVICE_TICKET_HEADER, tvmTicketProvider.provideServiceTicket()),
            (b) -> b.addConverterFactory(JacksonConverterFactory.create(objectMapper))
        );
    }
}
