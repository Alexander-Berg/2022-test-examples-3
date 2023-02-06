package ru.yandex.direct.common.testing;

import io.lettuce.core.cluster.RedisClusterClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.direct.common.configuration.CommonConfiguration;
import ru.yandex.direct.common.configuration.MetricsConfiguration;
import ru.yandex.direct.common.lettuce.LettuceConnectionProvider;
import ru.yandex.direct.dbutil.testing.DbUtilTestingConfiguration;

import static ru.yandex.direct.common.configuration.RedisConfiguration.LETTUCE;
import static ru.yandex.direct.common.configuration.RedisConfiguration.LETTUCE_CLIENT;

/**
 * Переопределяет некоторые бин-дефинишены из common
 * для подключения к тестовой базе
 */
@Configuration
@Import({DbUtilTestingConfiguration.class, CommonConfiguration.class})
public class CommonTestingConfiguration {
    /**
     * Пустой стаб
     */
    @MockBean(name = MetricsConfiguration.METRIC_COLLECTOR_BEAN_NAME)
    public MetricsConfiguration.CollectorWrapper metricCollectorWrapper;

    @MockBean(name = LETTUCE_CLIENT)
    public RedisClusterClient redisClusterClient;

    @MockBean(name = LETTUCE)
    public LettuceConnectionProvider lettuceConnectionProvider;
}
