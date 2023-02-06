package ru.yandex.market.ff.configuration;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.ff.config.DeliveryTrackerClientConfig;
import ru.yandex.market.ff.config.LogisticGatewayConfig;
import ru.yandex.market.ff.config.LomClientConfig;
import ru.yandex.market.ff.health.service.implementation.TvmClientStatusCheckService;
import ru.yandex.market.ff.lgw.client.LogisticGatewayTvmTicketProvider;
import ru.yandex.market.ff.mvc.handler.RestTemplateResponseErrorHandler;
import ru.yandex.market.logistics.util.client.TvmTicketProvider;
import ru.yandex.market.logistics.util.client.tvm.TvmTicketChecker;
import ru.yandex.market.logistics.util.client.tvm.TvmTicketCheckerImpl;
import ru.yandex.market.logistics.util.client.tvm.client.TvmClientApi;
import ru.yandex.passport.tvmauth.TvmClient;

import static org.mockito.Mockito.mock;

@Configuration
@Import({LogisticGatewayConfig.class, LomClientConfig.class, DeliveryTrackerClientConfig.class})
public class TvmTestMockConfiguration extends MockConfiguration {

    @Bean
    public TvmClient tvmClient() {
        return Mockito.mock(TvmClient.class);
    }

    @Bean
    public TvmTicketProvider lgwTvmTicketProvider(TvmClient tvmClient,
                                                  @Value("${lgw.tvm.client.id}") int lgwTvmClientId) {
        return new LogisticGatewayTvmTicketProvider(tvmClient, lgwTvmClientId);
    }

    @Bean
    public RestTemplateResponseErrorHandler restTemplateResponseErrorHandler() {
        return Mockito.mock(RestTemplateResponseErrorHandler.class);
    }

    @Bean
    public TvmTicketChecker tvmTicketChecker() {
        TvmTicketCheckerImpl tvmTicketChecker = mock(TvmTicketCheckerImpl.class);
        return tvmTicketChecker;
    }

    @Bean
    public TvmClientApi tvmClientApi() {
        return mock(TvmClientApi.class);
    }

    @Bean
    public TvmClientStatusCheckService tvmClientStatusCheckService(
            TvmTicketChecker tvmTicketChecker,
            TvmClient tvmClient
    ) {
        return new TvmClientStatusCheckService(tvmTicketChecker, tvmClient);
    }
}
