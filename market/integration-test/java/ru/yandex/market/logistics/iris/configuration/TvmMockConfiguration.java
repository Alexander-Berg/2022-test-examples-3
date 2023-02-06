package ru.yandex.market.logistics.iris.configuration;

import org.springframework.context.annotation.Bean;

import ru.yandex.passport.tvmauth.TvmClient;
import ru.yandex.passport.tvmauth.ClientStatus;
import ru.yandex.market.logistics.util.client.TvmTicketProvider;
import ru.yandex.market.logistics.util.client.tvm.TvmTicketChecker;
import ru.yandex.market.logistics.util.client.tvm.TvmTicketCheckerImpl;
import ru.yandex.market.logistics.util.client.tvm.client.TvmClientApi;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.iris.configuration.TvmConfiguration.LGW_TVM_TICKER_PROVIDER;
import static ru.yandex.market.logistics.iris.configuration.TvmConfiguration.LOG_BROKER_TVM_TICKER_PROVIDER;

public class TvmMockConfiguration {

    public static final String SERVICE_TICKET = "service_ticket";

    public static final String USER_TICKET = "user_ticket";

    @Bean(name = {LGW_TVM_TICKER_PROVIDER, LOG_BROKER_TVM_TICKER_PROVIDER})
    public TvmTicketProvider tvmTicketProvider() {
        return new TvmTicketProvider() {
            @Override
            public String provideServiceTicket() {
                return SERVICE_TICKET;
            }

            @Override
            public String provideUserTicket() {
                return USER_TICKET;
            }
        };
    }

    @Bean
    public TvmTicketChecker tvmTicketChecker() {
        return mock(TvmTicketCheckerImpl.class);
    }

    @Bean
    public TvmClientApi tvmClientApi() {
        return mock(TvmClientApi.class);
    }

    @Bean
    public TvmClient tvmClient() {
        TvmClient mock = mock(TvmClient.class);
        when(mock.getStatus()).thenReturn(new ClientStatus(ClientStatus.Code.OK, ""));
        return mock;
    }
}
