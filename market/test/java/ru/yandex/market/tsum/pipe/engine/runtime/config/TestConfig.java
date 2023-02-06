package ru.yandex.market.tsum.pipe.engine.runtime.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 10.03.17
 */
@Configuration
@ComponentScan(value = {
    "ru.yandex.market.tsum.pipe.engine.runtime.test_data"
})
@Import(TestBeansConfig.class)
public class TestConfig {

}
