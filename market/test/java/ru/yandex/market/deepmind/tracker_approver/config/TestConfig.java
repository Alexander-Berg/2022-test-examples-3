package ru.yandex.market.deepmind.tracker_approver.config;

import java.util.concurrent.Executors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.deepmind.tracker_approver.repository.TrackerApproverDataHistoryRepository;
import ru.yandex.market.deepmind.tracker_approver.repository.TrackerApproverDataHistoryRepositoryImpl;
import ru.yandex.market.deepmind.tracker_approver.repository.TrackerApproverDataRepository;
import ru.yandex.market.deepmind.tracker_approver.repository.TrackerApproverDataRepositoryImpl;
import ru.yandex.market.deepmind.tracker_approver.repository.TrackerApproverTicketRepository;
import ru.yandex.market.deepmind.tracker_approver.repository.TrackerApproverTicketRepositoryImpl;
import ru.yandex.market.deepmind.tracker_approver.repository.TrackerApproverTicketStatusHistoryRepository;
import ru.yandex.market.deepmind.tracker_approver.repository.TrackerApproverTicketStatusHistoryRepositoryImpl;
import ru.yandex.market.mbo.storage.StorageKeyValueRepository;
import ru.yandex.market.mbo.storage.StorageKeyValueRepositoryImpl;
import ru.yandex.market.mbo.storage.StorageKeyValueServiceImpl;

@Configuration
@Import({
    DbConfig.class,
    HistoryContextSetterConfig.class
})
public class TestConfig {
    private final DbConfig dbConfig;

    @Value("${tracker_approver.schema}")
    private String schema;

    public TestConfig(DbConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    @Bean
    public ObjectMapper trackerApproverObjectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public TrackerApproverDataRepository trackerApproverDataRepository() {
        return new TrackerApproverDataRepositoryImpl(
            schema,
            dbConfig.namedParameterJdbcTemplate(),
            trackerApproverObjectMapper()
        );
    }

    @Bean
    public TrackerApproverTicketRepository trackerApproverTicketRepository() {
        return new TrackerApproverTicketRepositoryImpl(
            schema,
            dbConfig.namedParameterJdbcTemplate(),
            trackerApproverObjectMapper()
        );
    }

    @Bean
    public TrackerApproverTicketStatusHistoryRepository trackerApproverTicketStatusHistoryRepository() {
        return new TrackerApproverTicketStatusHistoryRepositoryImpl(
            schema,
            dbConfig.namedParameterJdbcTemplate(),
            trackerApproverObjectMapper()
        );
    }

    @Bean
    public TrackerApproverDataHistoryRepository trackerApproverDataHistoryRepository() {
        return new TrackerApproverDataHistoryRepositoryImpl(
            schema,
            dbConfig.namedParameterJdbcTemplate(),
            trackerApproverObjectMapper()
        );
    }

    @Bean
    public StorageKeyValueRepository deepmindStorageKeyValueRepository() {
        return new StorageKeyValueRepositoryImpl(
            dbConfig.namedParameterJdbcTemplate(),
            dbConfig.transactionTemplate(),
            schema + ".storage_key_value"
        );
    }

    @Bean
    public StorageKeyValueServiceImpl deepmindStorageKeyValueService() {
        var executorService = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("tracker-approver-key-value-cache-thread")
            .build());
        return new StorageKeyValueServiceImpl(deepmindStorageKeyValueRepository(), executorService);
    }
}
