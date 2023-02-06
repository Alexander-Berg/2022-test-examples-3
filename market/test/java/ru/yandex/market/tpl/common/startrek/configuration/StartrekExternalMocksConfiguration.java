package ru.yandex.market.tpl.common.startrek.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import ru.yandex.market.tpl.common.startrek.domain.TestTicketStateFactory;
import ru.yandex.market.tpl.common.web.config.TplProfiles;
import ru.yandex.startrek.client.Session;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

/**
 * Мок для внешней конфигурации. Импортировать только в тесты!
 */
@Configuration
public class StartrekExternalMocksConfiguration {

    @Bean
    @Profile(TplProfiles.TESTS)
    public Session trackerSession() {
        return mock(Session.class, RETURNS_DEEP_STUBS);
    }

}
