package ru.yandex.market.jmf.blackbox.support.test;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.blackbox.support.BlackBoxSupportConfiguration;
import ru.yandex.market.jmf.blackbox.support.YandexBlackboxClient;
import ru.yandex.market.jmf.tvm.support.test.TvmSupportTestConfiguration;

@Configuration
@Import({
        BlackBoxSupportConfiguration.class,
        TvmSupportTestConfiguration.class,
})
public class BlackBoxSupportTestConfiguration {
    @Bean
    public YandexBlackboxClient mockYandexBlackboxClient() {
        return Mockito.mock(YandexBlackboxClient.class);
    }
}
