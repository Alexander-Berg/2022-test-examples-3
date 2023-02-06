package ru.yandex.market.logistics.nesu.client.configuration;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.extras.retrofit.AsyncHttpClientCallFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.MimeTypeUtils;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import ru.yandex.market.logistics.nesu.client.NesuClient;
import ru.yandex.market.logistics.nesu.client.NesuClientImpl;
import ru.yandex.market.logistics.util.client.ClientUtilsFactory;
import ru.yandex.market.logistics.util.client.ExternalServiceProperties;
import ru.yandex.market.logistics.util.client.HttpTemplate;
import ru.yandex.market.logistics.util.client.HttpTemplateBuilder;
import ru.yandex.market.logistics.util.client.StatelessTvmTicketProvider;
import ru.yandex.market.logistics.util.client.converter.InputStreamMessageConverter;
import ru.yandex.market.request.trace.Module;
import ru.yandex.market.request.trace.RequestContextHolder;

import static ru.yandex.market.logistics.util.client.HttpTemplate.SERVICE_TICKET_HEADER;
import static ru.yandex.market.request.trace.RequestTraceUtil.REQUEST_ID_HEADER;

@Slf4j
@Configuration
@MockBean(AsyncHttpClient.class)
public class TestClientConfig {

    @ParametersAreNonnullByDefault
    private static final StatelessTvmTicketProvider TICKET_PROVIDER = new StatelessTvmTicketProvider() {

        @Override
        public String provideServiceTicket(Integer tvmServiceId) {
            return "test-service-ticket";
        }

        @Override
        public String provideUserTicket(Integer tvmServiceId) {
            return "test-user-ticket";
        }
    };

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AsyncHttpClient asyncHttpClient;

    @Bean
    @ConfigurationProperties("nesu.api")
    public ExternalServiceProperties testNesuProperties() {
        return new ExternalServiceProperties();
    }

    @Bean
    public NesuClient internalClient(HttpTemplate httpTemplate) {
        return new NesuClientImpl(httpTemplate, objectMapper);
    }

    @Bean
    public HttpTemplate httpTemplate(@Qualifier("testNesuProperties") ExternalServiceProperties nesuProperties) {
        return HttpTemplateBuilder.create(nesuProperties, Module.NESU)
            .withTicketProvider(TICKET_PROVIDER)
            .withConverters(List.of(
                new StringHttpMessageConverter(StandardCharsets.UTF_8),
                new InputStreamMessageConverter(MediaType.TEXT_HTML),
                new ByteArrayHttpMessageConverter(),
                new MappingJackson2HttpMessageConverter(objectMapper)
            ))
            .build();
    }

    @Bean(name = "nesuRetrofit")
    public Retrofit nesuRetrofit(@Qualifier("testNesuProperties") ExternalServiceProperties nesuProperties) {
        Call.Factory callFactory = AsyncHttpClientCallFactory.builder()
            .httpClient(asyncHttpClient)
            .build();

        return new Retrofit.Builder()
            .baseUrl(nesuProperties.getUrl())
            .callFactory(request -> {
                var builder = request.newBuilder();
                builder
                    .addHeader("Accept", MimeTypeUtils.APPLICATION_JSON_VALUE)
                    .header(
                        SERVICE_TICKET_HEADER,
                        Objects.requireNonNull(TICKET_PROVIDER.provideServiceTicket(nesuProperties.getTvmServiceId()))
                    )
                    .header(REQUEST_ID_HEADER, RequestContextHolder.getContext().getNextSubReqId());
                return callFactory.newCall(builder.build());
            })
            .addConverterFactory(JacksonConverterFactory.create(ClientUtilsFactory.getObjectMapper()))
            .validateEagerly(true)
            .build();
    }
}
