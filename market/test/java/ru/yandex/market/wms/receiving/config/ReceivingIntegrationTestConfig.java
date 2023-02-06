package ru.yandex.market.wms.receiving.config;

import java.sql.SQLException;
import java.time.Clock;

import javax.sql.DataSource;

import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.bean.DatabaseConfigBean;
import com.github.springtestdbunit.bean.DatabaseDataSourceConnectionFactoryBean;
import liquibase.integration.spring.SpringLiquibase;
import org.dbunit.database.DatabaseDataSourceConnection;
import org.dbunit.ext.h2.H2DataTypeFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.wms.common.model.enums.DatabaseSchema;
import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.common.service.ShelfLifeService;
import ru.yandex.market.wms.common.service.SkuImportValidationService;
import ru.yandex.market.wms.common.service.validation.dimensions.DimensionsValidationProcessor;
import ru.yandex.market.wms.common.spring.dao.implementation.AnomalyLotDao;
import ru.yandex.market.wms.common.spring.dao.implementation.BuildingDAO;
import ru.yandex.market.wms.common.spring.dao.implementation.CargoTypesBuildingsDAO;
import ru.yandex.market.wms.common.spring.dao.implementation.LotDao;
import ru.yandex.market.wms.common.spring.dao.implementation.ReceiptDao;
import ru.yandex.market.wms.common.spring.dao.implementation.ReceiptDetailDao;
import ru.yandex.market.wms.common.spring.dao.implementation.StorerDao;
import ru.yandex.market.wms.common.spring.dao.implementation.UserTaskDao;
import ru.yandex.market.wms.common.spring.helper.NullableColumnsDataSetLoader;
import ru.yandex.market.wms.common.spring.repository.sku.itemmaster.ItemMasterRepository;
import ru.yandex.market.wms.common.spring.service.GoldSkuService;
import ru.yandex.market.wms.common.spring.service.ReceiptAssortmentService;
import ru.yandex.market.wms.common.spring.service.ReceiptService;
import ru.yandex.market.wms.common.spring.service.SkuService;
import ru.yandex.market.wms.common.spring.service.component.sku.DefaultSkuBuilder;
import ru.yandex.market.wms.common.spring.service.component.sku.DefaultSkuPackBilderComponent;
import ru.yandex.market.wms.common.spring.service.identities.strategy.InboundIdentityCheckStrategy;
import ru.yandex.market.wms.common.spring.service.inbound.AdditionalDeliveryService;
import ru.yandex.market.wms.common.spring.service.inbound.InboundService;
import ru.yandex.market.wms.common.spring.service.inbound.ReceiptDetailsExtractor;
import ru.yandex.market.wms.common.spring.service.sku.CargoTypeService;
import ru.yandex.market.wms.common.spring.service.sku.InboundIdentityService;
import ru.yandex.market.wms.common.spring.service.sku.ItemMasterService;
import ru.yandex.market.wms.common.spring.service.sku.PackLoaderService;
import ru.yandex.market.wms.common.spring.service.sku.RegistryItemMasterService;
import ru.yandex.market.wms.common.spring.service.sku.SkuIdentityService;
import ru.yandex.market.wms.common.spring.service.sku.SkuLoaderService;
import ru.yandex.market.wms.common.spring.service.sku.SkuUrlLoaderService;
import ru.yandex.market.wms.common.spring.service.sku.SkusResultService;
import ru.yandex.market.wms.common.spring.service.validation.sku.AltSkuValidationService;
import ru.yandex.market.wms.common.spring.service.validation.sku.PackValidationService;
import ru.yandex.market.wms.common.spring.service.validation.sku.SkuDTOValidationService;
import ru.yandex.market.wms.common.spring.service.validation.sku.SkuUrlsValidationService;
import ru.yandex.market.wms.common.spring.servicebus.ServicebusClient;
import ru.yandex.market.wms.common.spring.servicebus.mapper.ManufacturerSkuMapper;
import ru.yandex.market.wms.common.spring.utils.FileContentUtils;
import ru.yandex.market.wms.receiving.service.receipt.ReceiptStateService;
import ru.yandex.market.wms.shared.libs.authorization.SecurityDataProvider;

@TestConfiguration
@DbUnitConfiguration(dataSetLoader = NullableColumnsDataSetLoader.class,
        databaseConnection = {"wmwhseConnection", "archiveWmwhseConnection"})
@SpringBootApplication(scanBasePackages = "ru.yandex.market.wms")
public class ReceivingIntegrationTestConfig {

    @Bean
    @ConfigurationProperties("spring.archivedatasource")
    DataSourceProperties archiveDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource archiveDataSource() throws SQLException {
        DataSource ds = archiveDataSourceProperties().initializeDataSourceBuilder().build();
        String initScriptContent = FileContentUtils.getFileContent("schema.sql");
        ds.getConnection().prepareStatement(initScriptContent).execute();
        return ds;
    }

    @Bean
    public LiquibaseProperties liquibaseProperties() {
        return new LiquibaseProperties();
    }

    @Bean
    public SpringLiquibase liquibase(@Qualifier("dataSource") DataSource dataSource, LiquibaseProperties properties) {
        return createLiquibase(dataSource, properties);
    }

    @Bean
    public SpringLiquibase archiveLiquibase(@Qualifier("archiveDataSource") DataSource archiveDataSource,
                                            LiquibaseProperties properties) {
        return createLiquibase(archiveDataSource, properties);
    }

    @Bean
    @Primary
    public DatabaseDataSourceConnection wmwhseConnection(DataSource dataSource) {
        return dbUnitDatabaseConnection(DatabaseSchema.WMWHSE1.getName(), dataSource);
    }

    @Bean
    public DatabaseDataSourceConnection archiveWmwhseConnection() throws SQLException {
        return dbUnitDatabaseConnection(DatabaseSchema.WMWHSE1.getName(), archiveDataSource());
    }

    //Copy from ru.yandex.market.wms.common.spring.config.IntegrationTestConfig
    public DatabaseDataSourceConnection dbUnitDatabaseConnection(String schemaName, DataSource dataSource) {
        DatabaseConfigBean dbConfig = new DatabaseConfigBean();
        dbConfig.setDatatypeFactory(new H2DataTypeFactory());
        dbConfig.setAllowEmptyFields(true);

        DatabaseDataSourceConnectionFactoryBean dbConnectionFactory =
                new DatabaseDataSourceConnectionFactoryBean(dataSource);
        dbConnectionFactory.setSchema(schemaName);
        dbConnectionFactory.setDatabaseConfig(dbConfig);

        try {
            return dbConnectionFactory.getObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private SpringLiquibase createLiquibase(DataSource dataSource, LiquibaseProperties properties) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog(properties.getChangeLog());
        liquibase.setShouldRun(properties.isEnabled());
        liquibase.setChangeLogParameters(properties.getParameters());
        liquibase.setContexts(properties.getContexts());
        liquibase.setDefaultSchema(properties.getDefaultSchema());
        liquibase.setDropFirst(properties.isDropFirst());
        liquibase.setLabels(properties.getLabels());
        liquibase.setRollbackFile(properties.getRollbackFile());
        return liquibase;
    }

    @Bean
    @SuppressWarnings("checkstyle:ParameterNumber")
    public ItemMasterService itemMasterService(DbConfigService configService,
                                               SkuDTOValidationService skuValidationService,
                                               PackValidationService packValidationService,
                                               AltSkuValidationService altSkuValidationService,
                                               SkuLoaderService skuUpdateInserterService,
                                               PackLoaderService packLoaderService,
                                               SkuUrlsValidationService skuUrlsValidationService,
                                               SkuUrlLoaderService skuUrlLoaderService,
                                               SkusResultService skusResultService,
                                               ServicebusClient servicebusClient,
                                               ManufacturerSkuMapper manufacturerSkuMapper,
                                               StorerDao storerDao,
                                               ItemMasterRepository itemMasterRepository,
                                               CargoTypeService cargoTypeService,
                                               SkuIdentityService skuIdentityService,
                                               GoldSkuService goldSkuService,
                                               Clock clock) {
        return new ItemMasterService(
                configService,
                skuValidationService,
                packValidationService,
                altSkuValidationService,
                skuUpdateInserterService,
                packLoaderService,
                skuUrlsValidationService,
                skuUrlLoaderService,
                skusResultService,
                servicebusClient,
                manufacturerSkuMapper,
                storerDao,
                itemMasterRepository,
                cargoTypeService,
                skuIdentityService,
                goldSkuService,
                clock);
    }

    @Bean
    public RegistryItemMasterService registryItemMasterService() {
        return new RegistryItemMasterService();
    }

    @Bean
    @SuppressWarnings("checkstyle:ParameterNumber")
    public InboundService inboundService(ReceiptDao receiptDao,
                                         ReceiptDetailDao receiptDetailDao,
                                         CargoTypesBuildingsDAO cargoTypesBuildingsDAO,
                                         BuildingDAO buildingDAO,
                                         ReceiptService receiptService,
                                         InboundIdentityService inboundIdentityService,
                                         RegistryItemMasterService registryItemMasterService,
                                         ReceiptDetailsExtractor receiptDetailsExtractor,
                                         ItemMasterService itemMasterService,
                                         AdditionalDeliveryService additionalDeliveryService,
                                         AnomalyLotDao anomalyLotDao,
                                         SecurityDataProvider userProvider,
                                         ReceiptAssortmentService receiptAssortmentService,
                                         ReceiptStateService receiptStateService,
                                         ApplicationEventPublisher publisher) {
        return new InboundService(
                receiptDao,
                receiptDetailDao,
                cargoTypesBuildingsDAO,
                buildingDAO,
                receiptService,
                inboundIdentityService,
                registryItemMasterService,
                receiptDetailsExtractor,
                itemMasterService,
                additionalDeliveryService,
                anomalyLotDao,
                userProvider,
                receiptAssortmentService,
                receiptStateService,
                publisher
        );
    }

    @Bean
    public InboundIdentityService inboundIdentityService(InboundIdentityCheckStrategy identityCheckStrategy) {
        return new InboundIdentityService(identityCheckStrategy);
    }

    @Bean
    public SkuDTOValidationService skuDTOValidationService(
            StorerDao storerDao,
            SkuImportValidationService skuImportValidationService,
            DefaultSkuBuilder defaultSkuBuilder,
            DefaultSkuPackBilderComponent defaultSkuPackBuilderComponent,
            LotDao lotDao,
            ShelfLifeService shelfLifeService,
            SkuService skuService) {
        return new SkuDTOValidationService(
                storerDao,
                skuImportValidationService,
                defaultSkuBuilder,
                defaultSkuPackBuilderComponent,
                lotDao,
                shelfLifeService,
                skuService);
    }

    @Bean
    public SkuImportValidationService skuValidationService(ShelfLifeService shelfLifeService,
                                                           DimensionsValidationProcessor processor) {
        return new SkuImportValidationService(shelfLifeService, processor);
    }

    @Bean
    public DimensionsValidationProcessor dimensionsValidationService(DbConfigService dbConfigService) {
        return new DimensionsValidationProcessor(dbConfigService);
    }

    @Bean
    public UserTaskDao userTaskDao(NamedParameterJdbcTemplate jdbcTemplate,
                                   Clock clock) {
        return new UserTaskDao(jdbcTemplate, clock);
    }
}
