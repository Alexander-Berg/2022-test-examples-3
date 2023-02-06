package ru.yandex.direct.dbqueue.configuration;

import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.direct.common.testing.CommonTestingConfiguration;
import ru.yandex.direct.dbqueue.repository.DbQueueRepository;
import ru.yandex.direct.dbqueue.repository.DbQueueTypeMap;
import ru.yandex.direct.dbqueue.service.DbQueueService;
import ru.yandex.direct.dbqueue.steps.DbQueueSteps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

@ParametersAreNonnullByDefault
@Configuration
@Import({CommonTestingConfiguration.class})
public class DbQueueTestingConfiguration {
    @Bean
    public DbQueueTypeMap dbQueueTypeMap(DslContextProvider dslContextProvider) {
        return new DbQueueTypeMap(dslContextProvider);
    }

    @Bean
    public DbQueueRepository dbQueueRepository(DslContextProvider dslContextProvider,
                                               ShardHelper shardHelper, DbQueueTypeMap typeMap) {
        return new DbQueueRepository(dslContextProvider, shardHelper, typeMap);
    }

    @Bean
    public DbQueueService dbQueueService(DbQueueRepository dbQueueRepository) {
        return new DbQueueService(dbQueueRepository);
    }

    @Bean
    public DbQueueSteps dbQueueSteps(ShardHelper shardHelper,
                                     DslContextProvider dslContextProvider,
                                     DbQueueTypeMap typeMap) {
        return new DbQueueSteps(shardHelper, dslContextProvider, typeMap);
    }
}
