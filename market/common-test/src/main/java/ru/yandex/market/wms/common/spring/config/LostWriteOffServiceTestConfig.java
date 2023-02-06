package ru.yandex.market.wms.common.spring.config;

import java.sql.SQLException;
import java.time.Clock;
import java.util.Arrays;

import javax.sql.DataSource;

import com.github.springtestdbunit.bean.DatabaseConfigBean;
import com.github.springtestdbunit.bean.DatabaseDataSourceConnectionFactoryBean;
import liquibase.integration.spring.SpringLiquibase;
import org.dbunit.database.DatabaseDataSourceConnection;
import org.dbunit.ext.h2.H2DataTypeFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import ru.yandex.market.wms.common.dao.LogisticUnitDAO;
import ru.yandex.market.wms.common.model.enums.DatabaseSchema;
import ru.yandex.market.wms.common.spring.dao.implementation.InstanceIdentityDAO;
import ru.yandex.market.wms.common.spring.dao.implementation.InventoryFixlostLogDao;
import ru.yandex.market.wms.common.spring.dao.implementation.LostLogDao;
import ru.yandex.market.wms.common.spring.dao.implementation.OrderDao;
import ru.yandex.market.wms.common.spring.dao.implementation.OrderDetailDao;
import ru.yandex.market.wms.common.spring.dao.implementation.OutboundRegisterDao;
import ru.yandex.market.wms.common.spring.service.CycleInventoryService;
import ru.yandex.market.wms.common.spring.service.LostWriteOffService;
import ru.yandex.market.wms.common.spring.service.SerialInventoryLostService;
import ru.yandex.market.wms.common.spring.service.SerialInventoryService;
import ru.yandex.market.wms.common.spring.utils.FileContentUtils;
import ru.yandex.market.wms.common.spring.utils.uuid.FixedListTestUuidGenerator;
import ru.yandex.market.wms.common.spring.utils.uuid.TimeBasedGenerator;
import ru.yandex.market.wms.common.spring.utils.uuid.UuidGenerator;

@TestConfiguration
public class LostWriteOffServiceTestConfig {


    public UuidGenerator getUuidGenerator() {
        return new FixedListTestUuidGenerator(
                Arrays.asList(
                        "6d809e60-d707-11ea-9550-a9553a7b0571",
                        "6d809e60-d707-11ea-9550-a9553a7b0572",
                        "6d809e60-d707-11ea-9550-a9553a7b0573"
                ));
    }

    @Bean
    InstanceIdentityDAO archiveInstanceIdentityDao(@Qualifier("archiveJdbcTemplate")
                                                           NamedParameterJdbcTemplate archiveTemplate) {
        return new InstanceIdentityDAO(archiveTemplate);
    }

    @Bean
    @ConfigurationProperties("spring.archivedatasource")
    DataSourceProperties archiveDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource archiveDataSource() throws SQLException {
        DataSource dataSource = archiveDataSourceProperties().initializeDataSourceBuilder().build();
        String initScriptContent = FileContentUtils.getFileContent("schema.sql");
        dataSource.getConnection().prepareStatement(initScriptContent).execute();
        return dataSource;
    }

    @Bean
    public NamedParameterJdbcTemplate archiveJdbcTemplate() throws SQLException {
        return new NamedParameterJdbcTemplate(archiveDataSource());
    }

    @Bean
    public LostWriteOffService lostWriteOffService(LostWriteOffSrvParOne lostWriteOffSrvParOne,
                                                   LostWriteOffSrvParTwo lostWriteOffSrvParTwo) {
        return new LostWriteOffService(
                lostWriteOffSrvParOne.getSerialInventoryLostService(),
                lostWriteOffSrvParOne.getSerialInventoryService(),
                lostWriteOffSrvParOne.getLostLogDao(),
                lostWriteOffSrvParOne.getLogisticUnitDAO(),
                lostWriteOffSrvParOne.getOutboundRegisterDao(),
                lostWriteOffSrvParTwo.getOrderDao(),
                lostWriteOffSrvParTwo.getClock(),
                getUuidGenerator(),
                lostWriteOffSrvParTwo.getPlatformTransactionManager(),
                lostWriteOffSrvParTwo.getInstanceIdentityDAO(),
                lostWriteOffSrvParTwo.getArchiveInstanceIdentityDAO());

    }

    @Bean
    public LostWriteOffSrvParOne lstWrOffSrvParOne(SerialInventoryLostService serialInventoryLostService,
                                                   SerialInventoryService serialInventoryService,
                                                   LostLogDao lostLogDao,
                                                   LogisticUnitDAO logisticUnitDAO,
                                                   OutboundRegisterDao outboundRegisterDao) {
        return new LostWriteOffSrvParOne(serialInventoryLostService,
                serialInventoryService, lostLogDao,
                logisticUnitDAO, outboundRegisterDao);
    }

    @Bean
    public LostWriteOffSrvParTwo lstWrOffSrvParTwo(OrderDao orderDao, Clock clock,
                                                   PlatformTransactionManager platformTransactionManager,
                                                   InstanceIdentityDAO instanceIdentityDAO,
                                                   @Qualifier("archiveInstanceIdentityDao")
                                                           InstanceIdentityDAO archiveInstanceIdentityDAO) {
        return new LostWriteOffSrvParTwo(orderDao, clock,
                platformTransactionManager,
                instanceIdentityDAO,
                archiveInstanceIdentityDAO);
    }

    @Bean
    public DatabaseDataSourceConnection archiveWmwhseConnection() throws SQLException {
        return dbUnitDatabaseConnection(DatabaseSchema.WMWHSE1.getName(), archiveDataSource());
    }


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
    public CycleInventoryService cycleInventoryService(NamedParameterJdbcTemplate jdbcTemplate,
                                                       SerialInventoryLostService lostService,
                                                       SerialInventoryService serialInventoryService,
                                                       LostLogDao lostLogDao,
                                                       LogisticUnitDAO logisticUnitDAO,
                                                       OutboundRegisterDao outboundRegisterDao,
                                                       InstanceIdentityDAO instanceIdentityDAO,
                                                       @Qualifier("archiveInstanceIdentityDao")
                                                       InstanceIdentityDAO archiveInstanceIdentityDAO,
                                                       PlatformTransactionManager platformTransactionManager,
                                                       Clock clock,
                                                       OrderDao orderDao,
                                                       OrderDetailDao orderDetailDao) {
        InventoryFixlostLogDao inventoryFixlostLog = new InventoryFixlostLogDao(jdbcTemplate);
        return new CycleInventoryService(
                lostService, inventoryFixlostLog, serialInventoryService, lostLogDao,
                logisticUnitDAO, new TimeBasedGenerator(), outboundRegisterDao, instanceIdentityDAO,
                archiveInstanceIdentityDAO, platformTransactionManager, orderDao, orderDetailDao, clock
        );
    }

}
