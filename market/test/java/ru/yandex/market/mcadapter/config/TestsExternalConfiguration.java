package ru.yandex.market.mcadapter.config;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import ru.yandex.passport.tvmauth.TvmClient;

/**
 * @author zagidullinri
 * @date 06.07.2022
 */
@Configuration
@Profile({"functionalTest", "functionalTestRecipe"})
public class TestsExternalConfiguration {

    @MockBean
    TvmClient tvmClient;
}
