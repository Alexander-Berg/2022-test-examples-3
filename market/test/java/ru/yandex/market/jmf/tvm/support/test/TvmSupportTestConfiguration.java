package ru.yandex.market.jmf.tvm.support.test;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

import ru.yandex.market.jmf.http.test.HttpTestConfiguration;
import ru.yandex.market.jmf.tvm.support.TvmService;
import ru.yandex.market.jmf.tvm.support.TvmSupportConfiguration;

@Configuration
@Import({
        TvmSupportConfiguration.class,
        HttpTestConfiguration.class,
})
public class TvmSupportTestConfiguration {
    @Bean
    public TvmService mockTvmService() {
        return Mockito.mock(TvmService.class);
    }
}
