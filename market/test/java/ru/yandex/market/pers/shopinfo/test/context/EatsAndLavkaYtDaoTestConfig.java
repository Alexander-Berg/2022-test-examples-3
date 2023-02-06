package ru.yandex.market.pers.shopinfo.test.context;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.pers.shopinfo.yt.YtTemplate;

@Configuration
public class EatsAndLavkaYtDaoTestConfig {
    @Bean
    public YtTemplate eatsAndLavkaPartnersYtTemplate() {
        return Mockito.mock(YtTemplate.class);
    }
}
