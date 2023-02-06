package ru.yandex.market.delivery.mdbapp.configuration;

import org.junit.Ignore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import ru.yandex.market.logistics.util.client.tvm.client.MockTvmClient;
import ru.yandex.passport.tvmauth.TvmClient;

@Ignore
@Profile("integration-test")
@Configuration
public class TvmMockConfiguration {

    public static final String TEST_TVM_TICKET = "tvm-ticket";

    @Bean
    @Primary
    public TvmClient tvmClient() {
        return new MockTvmClient() {
            @Override
            public String getServiceTicketFor(int clientId) {
                return TEST_TVM_TICKET;
            }
        };
    }
}
