package ru.yandex.market.hrms.core.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import ru.yandex.connect.client.YandexConnectClientFacade;
import ru.yandex.connect.client.YandexConnectClientFacadeMock;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.startrek.configuration.StartrekExternalMocksConfiguration;
import ru.yandex.market.tpl.common.startrek.configuration.StartrekListenerTestClassesConfiguration;
import ru.yandex.market.tpl.common.web.config.TplProfiles;

@Configuration
@Import({
        HrmsCoreConfig.class,
        StartrekExternalMocksConfiguration.class,
        StartrekListenerTestClassesConfiguration.class,
        DbQueueTestUtil.class
})
@ComponentScan({
        "ru.yandex.market.hrms.test.configurer"
})
@EnableAutoConfiguration
public class HrmsCoreTestConfig {
    @Bean
    public PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    @Profile(TplProfiles.TESTS)
    public YandexConnectClientFacade yandexConnectClientFacade() {
        return new YandexConnectClientFacadeMock().withEmailHost("@hrms-sc.ru");
    }
}
