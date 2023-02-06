package ru.yandex.market.tpl.api.confg;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.tpl.core.external.avatarnica.AvatarnicaClient;
import ru.yandex.market.tpl.core.external.avatarnica.AvatarnicaProperties;

import static org.mockito.Mockito.mock;

@TestConfiguration
@ComponentScan(basePackages = {
        "ru.yandex.market.tpl.api.service"
})
@Import(AvatarnicaProperties.class)
public class TestTplApiConfiguration {

    @Primary
    @Bean
    public AvatarnicaClient mockedAvatarnicaClient() {
        return mock(AvatarnicaClient.class);
    }
}
