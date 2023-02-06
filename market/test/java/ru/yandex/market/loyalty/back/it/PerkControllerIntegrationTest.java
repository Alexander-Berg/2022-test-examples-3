package ru.yandex.market.loyalty.back.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.loyalty.core.config.CoreConfigExternal;
import ru.yandex.market.loyalty.core.config.CoreConfigInternal;
import ru.yandex.market.loyalty.core.service.blackbox.BlackboxClient;
import ru.yandex.market.loyalty.core.test.LoyaltySpringTestRunner;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

@RunWith(LoyaltySpringTestRunner.class)
@ContextConfiguration(classes = {PerkControllerIntegrationTest.Config.class})
@Ignore
public class PerkControllerIntegrationTest {
    @Autowired
    private BlackboxClient blackboxClient;

    @Test
    public void test() {
        assertTrue(blackboxClient.getUserStat("8.8.8.8", 4003790491L).isYandexPlus());
        assertFalse(blackboxClient.getUserStat("8.8.8.8", 4003790491L).isYandexPlus());
        assertFalse(blackboxClient.getUserStat("8.8.8.8", 69238496L).isYandexPlus());
        assertFalse(blackboxClient.getUserStat("8.8.8.8", 69238496L).isYandexPlus());
    }

    @Configuration
    @Import(CoreConfigInternal.BlackboxConfig.class)
    @PropertySource("classpath:/perk-controller-it.properties")
    public static class Config {
        @Bean
        public ObjectMapper objectMapper() {
            return new Jackson2ObjectMapperBuilder()
                    .defaultViewInclusion(true)
                    .build();
        }
    }
}
