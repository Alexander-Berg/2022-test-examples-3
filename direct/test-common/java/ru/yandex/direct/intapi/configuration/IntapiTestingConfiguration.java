package ru.yandex.direct.intapi.configuration;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.direct.common.lettuce.LettuceConnectionProvider;
import ru.yandex.direct.core.testing.configuration.CoreTestingConfiguration;
import ru.yandex.direct.core.testing.configuration.UacYdbTestingConfiguration;
import ru.yandex.direct.intapi.entity.balanceclient.model.NotifyPromocodeParameters;
import ru.yandex.direct.intapi.entity.balanceclient.service.NotifyPromocodeService;
import ru.yandex.direct.redislock.DistributedLock;
import ru.yandex.direct.redislock.StubDistributedLock;
import ru.yandex.direct.ytcore.entity.statistics.service.RecentStatisticsService;

import static org.mockito.Mockito.mock;
import static ru.yandex.direct.intapi.entity.balanceclient.service.NotifyPromocodeService.NOTIFY_PROMOCODE_LOCK_BUILDER;

@Configuration
@Import({WebAppConfiguration.class, CoreTestingConfiguration.class, UacYdbTestingConfiguration.class})
public class IntapiTestingConfiguration {

    @MockBean
    private RecentStatisticsService recentStatisticsService;

    @Bean(name = NOTIFY_PROMOCODE_LOCK_BUILDER)
    public NotifyPromocodeService.LockBuilder notifyPromocodeLockBuilder() {
        return new NotifyPromocodeService.LockBuilder(mock(LettuceConnectionProvider.class), "") {
            @Override
            public DistributedLock build(NotifyPromocodeParameters updateRequest) {
                return new StubDistributedLock();
            }
        };
    }
}
