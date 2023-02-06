package ru.yandex.market.mboc.common.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.mbo.mdm.common.masterdata.repository.FromIrisItemRepositoryImpl;
import ru.yandex.market.mboc.common.categorygroups.CategoryGroupConfiguration;
import ru.yandex.market.mboc.common.config.JooqTestConfig;
import ru.yandex.market.mboc.common.config.KeyValueConfig;
import ru.yandex.market.mboc.common.config.ModelFormCacheConfig;
import ru.yandex.market.mboc.common.config.OfferServicesConfig;
import ru.yandex.market.mboc.common.config.SupplierConverterServiceConfiguration;
import ru.yandex.market.mboc.common.config.TestSqlDatasourceConfig;
import ru.yandex.market.mboc.common.config.TestYqlAutoClusterConfig;
import ru.yandex.market.mboc.common.config.TestYqlOverPgDatasourceConfig;
import ru.yandex.market.mboc.common.config.datacamp.DatacampImportConfig;
import ru.yandex.market.mboc.common.config.queue.MappedMskuChangesQueueConfig;
import ru.yandex.market.mboc.common.config.repo.JooqRepositoryConfig;
import ru.yandex.market.mboc.common.config.repo.OfferRepositoryObserverConfig;
import ru.yandex.market.mboc.common.config.repo.RepositoryConfig;
import ru.yandex.market.mboc.common.infrastructure.sql.SavePointTransactionHelper;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.logisticsparams.LogisticsParamRepositoryImpl;
import ru.yandex.market.mboc.common.masterdata.repository.CargoTypeRepositoryImpl;
import ru.yandex.market.mboc.common.masterdata.repository.MasterDataRepositoryImpl;
import ru.yandex.market.mboc.common.masterdata.repository.MskuSyncResultRepositoryImpl;
import ru.yandex.market.mboc.common.masterdata.repository.cutoff.OfferCutoffRepositoryImpl;
import ru.yandex.market.mboc.common.masterdata.repository.document.QualityDocumentRepositoryImpl;
import ru.yandex.market.mboc.common.offers.acceptance.rule.CategoryRuleConfig;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoCache;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoCacheImpl;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoRepository;
import ru.yandex.market.mboc.db.config.JooqConfig;
import ru.yandex.market.mboc.db.config.PostgresSQLBeanPostProcessor;

/**
 * Scan-free configuration for autowiring repositories only (and some related stuff).
 *
 * @author yuramalinov
 * @created 11.10.18
 */
@TestConfiguration
@PropertySource("classpath:db-test.properties")
@Import({
    PostgresSQLBeanPostProcessor.class,
    TestSqlDatasourceConfig.class,
    TestYqlOverPgDatasourceConfig.class,
    TestYqlAutoClusterConfig.class,
    RepositoryConfig.class,
    OfferRepositoryObserverConfig.class,
    MasterDataRepositoryImpl.class,
    QualityDocumentRepositoryImpl.class,
    KeyValueConfig.class,
    OfferCutoffRepositoryImpl.class,
    CargoTypeRepositoryImpl.class,
    LogisticsParamRepositoryImpl.class,
    JooqTestConfig.class,
    JooqRepositoryConfig.class,
    TestRemoteServicesConfig.class,
    FromIrisItemRepositoryImpl.class,
    MskuSyncResultRepositoryImpl.class,
    SupplierConverterServiceConfiguration.class,
    YtMockConfig.class,
    DatacampImportConfig.class,
    ModelFormCacheConfig.class,
    CategoryRuleConfig.class,
    MappedMskuChangesQueueConfig.class,
    OfferServicesConfig.class,
    DataCampTestConfig.class,
    CategoryGroupConfiguration.class
})
public class DbTestConfiguration {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private JooqConfig jooqConfig;

    @Bean
    @Primary
    public TransactionHelper commonTransactionHelper(PlatformTransactionManager transactionManager) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        return new SavePointTransactionHelper(transactionTemplate);
    }

    /**
     * Нужен для ситуаций, когда надо гарантировать новую транзакцию.
     * ВАЖНО! Создание транзакции через данный хэлпер никак не трекается JUnit,
     * поэтому commit или rollback транзакции нужно делать руками.
     * Если commit или rollback транзакции не сделать руками, то после окончания теста транзакция остается висеть
     * в состоянии completed == false и не дает соединению вернуться обратно в пул.
     */
    @Qualifier("newTransaction")
    @Bean
    public TransactionHelper newTransactionHelper(PlatformTransactionManager transactionManager) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return new SavePointTransactionHelper(transactionTemplate);
    }

    @Bean
    public CategoryInfoCache categoryInfoCache(CategoryInfoRepository repository) {
        return new CategoryInfoCacheImpl(repository);
    }

}
