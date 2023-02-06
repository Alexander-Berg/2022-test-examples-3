package ru.yandex.market.jmf.trigger.test;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import ru.yandex.market.jmf.bcp.test.BcpTestConfiguration;
import ru.yandex.market.jmf.queue.retry.RetryServiceTestConfiguration;
import ru.yandex.market.jmf.trigger.TriggerConfiguration;

@Configuration
@Import({
        TriggerConfiguration.class,
        BcpTestConfiguration.class,
        RetryServiceTestConfiguration.class,
})
@PropertySource(name = "testTriggerConfiguration", value = "classpath:trigger_test.properties")
public class TriggerTestConfiguration {
}
