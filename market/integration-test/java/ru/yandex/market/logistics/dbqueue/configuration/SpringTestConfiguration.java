package ru.yandex.market.logistics.dbqueue.configuration;

import java.util.Collections;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.logistics.dbqueue.TestQueueMetaProvider;
import ru.yandex.market.logistics.dbqueue.controller.DbQueueController;
import ru.yandex.market.logistics.dbqueue.controller.DbQueueControllerExceptionHandler;
import ru.yandex.market.logistics.dbqueue.dao.DbQueueTaskDao;
import ru.yandex.market.logistics.dbqueue.dao.DbQueueTaskLogDaoImpl;
import ru.yandex.market.logistics.dbqueue.dao.QueueTableSchema;
import ru.yandex.market.logistics.dbqueue.domain.DbQueueTaskLogDao;
import ru.yandex.market.logistics.dbqueue.domain.QueueMetaProvider;
import ru.yandex.market.logistics.dbqueue.facade.DbQueueTaskFacade;
import ru.yandex.market.logistics.dbqueue.facade.DbQueueTaskFacadeImpl;
import ru.yandex.market.logistics.dbqueue.facade.DbQueueTaskLogFacade;
import ru.yandex.market.logistics.dbqueue.facade.DbQueueTaskLogFacadeImpl;
import ru.yandex.market.logistics.dbqueue.facade.DetailDtoConverter;
import ru.yandex.market.logistics.dbqueue.facade.GridDtoConverter;
import ru.yandex.market.logistics.dbqueue.service.DbQueueTaskLogService;
import ru.yandex.market.logistics.dbqueue.service.DbQueueTaskLogServiceImpl;
import ru.yandex.market.logistics.dbqueue.service.DbQueueTaskService;
import ru.yandex.market.logistics.dbqueue.service.DbQueueTaskServiceImpl;
import ru.yandex.market.logistics.dbqueue.service.TaskLogParameterConverter;
import ru.yandex.market.logistics.dbqueue.service.TaskParameterConverter;
import ru.yandex.market.logistics.test.integration.db.cleaner.config.DatabaseCleanerConfig;
import ru.yandex.market.logistics.test.integration.db.cleaner.config.DefaultSchemaCleanerConfigProvider;
import ru.yandex.market.logistics.test.integration.db.cleaner.config.base.CompoundDatabaseCleanerConfig;
import ru.yandex.market.logistics.test.integration.db.zonky.EnableZonkyEmbeddedPostgres;
import ru.yandex.money.common.dbqueue.dao.QueueDao;

@Configuration
@EnableZonkyEmbeddedPostgres
@Import({
    SecurityConfiguration.class
})
public class SpringTestConfiguration {

    public static final String TABLE_NAME = "dbqueue";
    public static final String LOG_TABLE_NAME = "task_log";

    @Bean
    public DatabaseCleanerConfig databaseCleanerConfig() {
        return new CompoundDatabaseCleanerConfig(Collections.singletonList(
            new DefaultSchemaCleanerConfigProvider("public")
        ));
    }

    @Bean
    public QueueMetaProvider queueMetaProvider() {
        return new TestQueueMetaProvider();
    }

    @Bean
    public QueueTableSchema queueTableSchema() {
        return QueueTableSchema.builder().build();
    }

    @Bean
    public DbQueueTaskDao dbQueueTaskDao(
        DataSource dataSource,
        QueueMetaProvider queueMetaProvider,
        QueueTableSchema queueTableSchema
    ) {
        return new DbQueueTaskDao(
            TABLE_NAME,
            new NamedParameterJdbcTemplate(dataSource),
            queueMetaProvider,
            queueTableSchema
        );
    }

    @Bean
    public DbQueueTaskLogDao dbQueueTaskLogDao(DataSource dataSource) {
        return new DbQueueTaskLogDaoImpl(
            LOG_TABLE_NAME,
            new NamedParameterJdbcTemplate(dataSource)
        );
    }

    @Bean
    public QueueDao queueDao(DataSource dataSource) {
        return new QueueDao(new JdbcTemplate(dataSource));
    }

    @Bean
    public DbQueueTaskService dbQueueTaskService(
        DbQueueTaskDao dao,
        QueueMetaProvider queueMetaProvider,
        QueueDao queueDao
    ) {
        return new DbQueueTaskServiceImpl(dao, queueMetaProvider, queueDao, TABLE_NAME);
    }

    @Bean
    @Primary
    public DbQueueTaskLogService dbQueueTaskLogService(DbQueueTaskLogDao dao) {
        return new DbQueueTaskLogServiceImpl(dao);
    }

    @Bean
    public DbQueueTaskFacade dbQueueTaskFacade(DbQueueTaskService dbQueueTaskService) {
        return new DbQueueTaskFacadeImpl(dbQueueTaskService);
    }

    @Bean
    public DbQueueTaskLogFacade dbQueueTaskLogFacade(DbQueueTaskLogService dbQueueTaskLogService) {
        return new DbQueueTaskLogFacadeImpl(dbQueueTaskLogService);
    }

    @Bean
    public DbQueueController dbQueueController(
        DbQueueTaskFacade taskFacade,
        DbQueueTaskLogFacade taskLogFacade
    ) {
        return new DbQueueController(taskFacade, taskLogFacade);
    }

    @Bean
    public DbQueueControllerExceptionHandler dbQueueControllerExceptionHandler() {
        return new DbQueueControllerExceptionHandler();
    }

    @Bean
    public TaskParameterConverter taskParameterConverter() {
        return new TaskParameterConverter();
    }

    @Bean
    public TaskLogParameterConverter taskLogParameterConverter() {
        return new TaskLogParameterConverter();
    }

    @Bean
    public GridDtoConverter gridDtoConverter() {
        return new GridDtoConverter();
    }

    @Bean
    public DetailDtoConverter detailDtoConverter() {
        return new DetailDtoConverter();
    }
}
