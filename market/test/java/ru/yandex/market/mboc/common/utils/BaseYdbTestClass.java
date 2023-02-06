package ru.yandex.market.mboc.common.utils;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.yandex.ydb.table.values.PrimitiveType;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mbo.ydb.YdbContainerContextInitializer;
import ru.yandex.market.mbo.ydb.client.YdbClient;
import ru.yandex.market.mbo.ydb.schema.Column;
import ru.yandex.market.mbo.ydb.schema.PrimaryKey;
import ru.yandex.market.mbo.ydb.schema.Primitive;
import ru.yandex.market.mboc.common.config.repo.OfferRepositoryObserverConfig;
import ru.yandex.market.mboc.common.config.ydb.YdbRepositoryConfig;
import ru.yandex.market.mboc.common.config.ydb.YdbServiceConfig;

@ContextConfiguration(
    classes = {
        TestYdbConfig.class,
        DbTestConfiguration.class,
        OfferRepositoryObserverConfig.class,
        YdbServiceConfig.class,
    },
    initializers = {
        YdbContainerContextInitializer.class,
        PGaaSZonkyInitializer.class,
    }
)
@TestPropertySource(properties = {
    "ydb.database=local",
    "ydbTestProfile=containered"
})
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public abstract class BaseYdbTestClass {

    @Autowired
    protected YdbClient ydbClient;

    @Autowired
    protected StorageKeyValueService storageKeyValueService;

    @Autowired
    private TestYdbConfig testYdbConfig;

    @Value("${ydb.database}")
    private String ydbDatabase;

    @Bean
    @Primary
    public YdbRepositoryConfig ydbRepositoryConfig() {
        return new YdbRepositoryConfig(testYdbConfig, storageKeyValueService);
    }

    public <T> void createTableFor(Class<T> type, String tableName) {
        var createTableQuery =  new StringBuilder();
        createTableQuery.append("Create Table `").append(tableName).append("` (\n");

        Stream.of(type.getDeclaredFields())
            .filter(it -> it.isAnnotationPresent(Column.class))
            .filter(it -> it.isAnnotationPresent(Primitive.class)
                || it.getType().equals(LocalDate.class)
                || it.getType().equals(LocalDateTime.class)
                || it.getType().equals(Instant.class)
                || it.getType().equals(Timestamp.class)
                || it.getType().equals(int.class)
                || it.getType().equals(long.class)
                || it.getType().equals(String.class)
            )
            .forEach(it -> {
                var column = it.getAnnotation(Column.class).value();
                var primitive = it.getAnnotation(Primitive.class);
                var hasPrimitive = primitive != null;
                var isTimestamp = it.getType().equals(LocalDate.class)
                    || it.getType().equals(LocalDateTime.class)
                    || it.getType().equals(Instant.class)
                    || it.getType().equals(Timestamp.class);
                if (isTimestamp) {
                    createTableQuery.append("\t").append(column).append("\tTimestamp,\n");
                } else if (it.getType().equals(int.class) && !hasPrimitive){
                    createTableQuery.append("\t").append(column).append("\tInt32,\n");
                } else if (it.getType().equals(long.class) && !hasPrimitive){
                    createTableQuery.append("\t").append(column).append("\tInt64,\n");
                } else if (it.getType().equals(String.class) && !hasPrimitive){
                    createTableQuery.append("\t").append(column).append("\tUtf8,\n");
                } else {
                    var primitiveType = PrimitiveType.of(primitive.value());
                    var typeAsString = primitiveType == PrimitiveType.float64() ? "Double" : primitiveType.toString();

                    createTableQuery.append("\t").append(column).append("\t").append(typeAsString).append(",\n");
                }
            });

        var primaryKey = Stream.of(type.getDeclaredFields())
            .filter(it -> it.isAnnotationPresent(Column.class))
            .filter(it -> it.isAnnotationPresent(PrimaryKey.class))
            .map(it -> it.getAnnotation(Column.class))
            .map(Column::value)
            .collect(Collectors.joining(",", "(", ")"));

        createTableQuery.append("\t PRIMARY KEY ").append(primaryKey).append("\n);");

        ydbClient.executeAsyncWithStatus(session -> session.executeSchemeQuery(createTableQuery.toString())).join();
    }

    public boolean tableExists(String tableName) {
        try {
            var tableDescription = ydbClient.describeTable(ydbDatabase + "/" + tableName);
            return tableDescription != null;
        } catch (Exception e) {
            return false;
        }
    }

    public Set<String> getDefinedColumnsFor(Class<?> type) {
        return Stream.of(type.getDeclaredFields())
            .filter(it -> it.isAnnotationPresent(Column.class))
            .map(it -> it.getAnnotation(Column.class).value())
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    public Set<String> getCompositeFields(Class<?> type) {
        return Stream.of(type.getDeclaredFields())
            .filter(it -> it.isAnnotationPresent(Column.class))
            .filter(it -> it.isAnnotationPresent(Primitive.class))
            .filter(it -> it.getAnnotation(Primitive.class).value().equals(PrimitiveType.Id.Json))
            .map(it -> it.getAnnotation(Column.class).value())
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }
}
