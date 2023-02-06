package ru.yandex.market.hrms.core.config;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import ru.yandex.market.tpl.common.web.config.TplProfiles;
import ru.yandex.passport.tvmauth.TvmClient;

@Profile(TplProfiles.TESTS)
@Configuration
public class TestTvmConfig {

    @Bean
    public TvmClient tvmClient() {
        TvmClient mock = Mockito.mock(TvmClient.class);
        Mockito.when(mock.getServiceTicketFor(Mockito.anyInt())).thenReturn("SERVICE_TICKET");
        Mockito.when(mock.getServiceTicketFor(Mockito.anyString())).thenReturn("SERVICE_TICKET");
        return mock;
    }
}
