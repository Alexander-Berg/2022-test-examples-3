package ru.yandex.market.jmf.lock;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.background.test.BackgroundTestConfiguration;
import ru.yandex.market.jmf.db.test.TestDefaultDataSourceConfiguration;
import ru.yandex.market.jmf.handshake.HandshakeServiceConfiguration;

@Configuration
@Import({
        TestDefaultDataSourceConfiguration.class,
        LockServiceConfiguration.class,
        HandshakeServiceConfiguration.class,
        BackgroundTestConfiguration.class,
})
@ComponentScan("ru.yandex.market.jmf.lock.test.impl")
public class LockServiceTestConfiguration {
}
