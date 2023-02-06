package ru.yandex.market.tsum.pipe.engine.runtime.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.GenericApplicationContext;
import ru.yandex.market.tsum.pipe.engine.runtime.di.ResourceInjector;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.JobTester;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.TestResourceInjector;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 16.01.18
 */
@Configuration
@PropertySource("classpath:test.properties")
public class JobTesterConfig {
    @Autowired
    private GenericApplicationContext applicationContext;

    @Bean
    public ResourceInjector resourceInjector() {
        return new TestResourceInjector();
    }

    @Bean
    public JobTester jobTester(ResourceInjector resourceInjector) {
        return new JobTester(resourceInjector, applicationContext);
    }
}
