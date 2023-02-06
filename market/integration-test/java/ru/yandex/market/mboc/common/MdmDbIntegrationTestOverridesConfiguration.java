package ru.yandex.market.mboc.common;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.mbo.mdm.common.datacamp.MdmDatacampService;
import ru.yandex.market.mbo.mdm.common.infrastructure.MdmLogbrokerService;
import ru.yandex.market.mbo.mdm.common.infrastructure.MdmLogbrokerServiceMock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mbo.mdm.common.service.MdmParameterValueCachingService;
import ru.yandex.market.mbo.mdm.common.service.MdmParameterValueCachingServiceMock;
import ru.yandex.market.mbo.mdm.common.service.MdmSolomonPushService;
import ru.yandex.market.mbo.mdm.common.service.bmdm.MdmEntityStorageServiceMock;
import ru.yandex.market.mbo.mdm.common.utils.TestMdmSqlDatasourceConfig;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.masterdata.repository.MasterDataRepository;
import ru.yandex.market.mboc.common.masterdata.repository.MasterDataRepositoryImpl;
import ru.yandex.market.mboc.common.masterdata.repository.document.QualityDocumentRepository;
import ru.yandex.market.mboc.common.masterdata.services.SskuMasterDataStorageService;
import ru.yandex.market.mboc.common.offers.repository.MboMappingsServiceMock;
import ru.yandex.market.mboc.http.MboMappingsService;

/**
 * В некоторых особо эзотерических случаях в тестах хочется переопределить бины так, чтобы они не юзали основные
 * сервисы из прод-конфигов. Например, SupplierConverterService просто не заведётся в локальной постгре без КИшного
 * ликвибейза, которого в тестовой конфигурации МДМ нет.
 *
 * @author yuramalinov
 * @created 11.10.18
 */
@TestConfiguration
@Import({
    TestMdmSqlDatasourceConfig.class,
})
public class MdmDbIntegrationTestOverridesConfiguration {

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    private QualityDocumentRepository qualityDocumentRepository;

    @Autowired
    private StorageKeyValueService storageKeyValueService;

    @Bean
    @Primary
    public TransactionHelper commonTransactionHelper(PlatformTransactionManager transactionManager) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        return transactionTemplate::execute;
    }

    /**
     * Нужен для ситуаций, когда надо гарантировать новую транзакцию.
     */
    @Qualifier("newTransaction")
    @Bean
    public TransactionHelper newTransactionHelper(PlatformTransactionManager transactionManager) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transactionTemplate::execute;
    }

    @Bean
    @Primary
    public SupplierConverterService supplierConverterService() {
        return new SupplierConverterServiceMock();
    }

    @Bean
    @Primary
    public MasterDataRepository masterDataRepository(
        TransactionHelper transactionHelper,
        @Qualifier("newTransaction") TransactionHelper newTransactionHelper,
        TransactionTemplate transactionTemplate) {
        return sskuMasterDataStorageService(transactionHelper, transactionTemplate);
    }

    @Bean
    @Primary
    public MdmParameterValueCachingService parameterValueCachingService() {
        return new MdmParameterValueCachingServiceMock();
    }

    @Bean
    @Primary
    public MboMappingsService mboMappingsService() {
        return new MboMappingsServiceMock();
    }

    @Bean
    @Primary
    public MdmLogbrokerService logbrokerService() {
        return new MdmLogbrokerServiceMock();
    }

    private SskuMasterDataStorageService sskuMasterDataStorageService(TransactionHelper transactionHelper,
                                                                      TransactionTemplate transactionTemplate) {
        return new SskuMasterDataStorageService(
            new MasterDataRepositoryImpl(
                namedParameterJdbcTemplate,
                transactionHelper,
                transactionTemplate,
                qualityDocumentRepository
            ),
            qualityDocumentRepository,
            transactionHelper,
            supplierConverterService(),
            new ComplexMonitoring());
    }

    @Bean
    @Primary
    public MdmSolomonPushService mdmSolomonPushService() {
        return Mockito.mock(MdmSolomonPushService.class);
    }

    @Bean
    @Primary
    public MdmDatacampService mdmDatacampService() {
        return Mockito.mock(MdmDatacampService.class);
    }

    @Bean
    @Primary
    public MdmEntityStorageServiceMock mdmEntityStorageService() {
        return new MdmEntityStorageServiceMock();
    }
}
