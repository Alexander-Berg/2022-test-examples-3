package ru.yandex.calendar.tvm;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CalendarTvmTestConfiguration {

    @Bean
    public TvmClient tvmClient() {
        return Mockito.mock(TvmClient.class);
    }

    @Bean
    public TvmFirewall tvmFirewall() {
        return Mockito.mock(TvmFirewall.class);
    }

    @Bean
    public TvmManager tvmManager() {
        return new TvmManager();
    }
}
