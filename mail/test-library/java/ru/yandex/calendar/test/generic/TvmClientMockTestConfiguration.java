package ru.yandex.calendar.test.generic;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.calendar.tvm.TvmClient;
import ru.yandex.calendar.tvm.TvmFirewall;
import ru.yandex.calendar.tvm.TvmManager;

@Configuration
public class TvmClientMockTestConfiguration {
    @Bean
    public TvmManager tvmManager() {
        return Mockito.mock(TvmManager.class);
    }

    @Bean
    public TvmClient tvmClient() {
        return Mockito.mock(TvmClient.class);
    }

    @Bean
    public TvmFirewall tvmFirewall() {
        return Mockito.mock(TvmFirewall.class);
    }
}
