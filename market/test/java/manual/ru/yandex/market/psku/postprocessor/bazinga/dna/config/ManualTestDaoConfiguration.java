package manual.ru.yandex.market.psku.postprocessor.bazinga.dna.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.psku.postprocessor.common.db.dao.DeletedMappingModelsDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.DeletedMappingModelsQueueDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.DeletedMappingModelsResultDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.ExternalRequestResponseDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.RemovedNomappingModelsDao;

@Configuration
@Import(ManualTestDatabaseConfig.class)
public class ManualTestDaoConfiguration {
    private final org.jooq.Configuration jooqConfiguration;

    public ManualTestDaoConfiguration(
            @Qualifier("jooq.config.configuration.test") org.jooq.Configuration configuration
    ) {
        this.jooqConfiguration = configuration;
    }

    @Bean
    public DeletedMappingModelsQueueDao deletedMappingModelsQueueDao() {
        return new DeletedMappingModelsQueueDao(jooqConfiguration);
    }

    @Bean
    public DeletedMappingModelsDao deletedMappingModelsDao() {
        return new DeletedMappingModelsDao(jooqConfiguration);
    }

    @Bean
    public DeletedMappingModelsResultDao deletedMappingModelsResultDao() {
        return new DeletedMappingModelsResultDao(jooqConfiguration);
    }

    @Bean
    public ExternalRequestResponseDao externalRequestResponseDao() {
        return new ExternalRequestResponseDao(jooqConfiguration);
    }

    @Bean
    public RemovedNomappingModelsDao removedNomappingModelsDao() {
        return new RemovedNomappingModelsDao(jooqConfiguration);
    }
}
