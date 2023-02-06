package ru.yandex.market.copy.impl;

import org.dbunit.database.DatabaseConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataBaseConfig;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.copy.CopyExecutor;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static ru.yandex.market.copy.AbstractCopyService.ENV_COPY_ENABLED;
import static ru.yandex.market.copy.AbstractCopyService.ENV_READ_CHUNK_SIZE;
import static ru.yandex.market.copy.AbstractCopyService.ENV_WRITE_CHUNK_SIZE;

@DbUnitDataBaseConfig({
        @DbUnitDataBaseConfig.Entry(name = DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS, value = "true")})
public class BusinessMetrikaCopyExecutorTest extends FunctionalTest {

    @Autowired
    CopyExecutor businessMetrikaCopyExecutor;

    @Autowired
    EnvironmentService environmentService;

    @BeforeEach
    public void beforeEach() {
        environmentService.setValue(String.format(ENV_COPY_ENABLED, "business_metrika_counter"), "true");
    }

    @Test
    @DbUnitDataSet(
            before = "csv/BusinessMetrikaCopyExecutorTest.copy.before.csv",
            after = "csv/BusinessMetrikaCopyExecutorTest.copy.after.csv"
    )
    @DisplayName("Проверка копирования таблицы business_metrika_counter из шопсового PG в Oracle")
    void copy() {
        businessMetrikaCopyExecutor.doJob(null);
    }

    @Test
    @DbUnitDataSet(
            before = "csv/BusinessMetrikaCopyExecutorTest.copy.before.csv",
            after = "csv/BusinessMetrikaCopyExecutorTest.copy.after.csv"
    )
    @DisplayName("Проверка копирования таблицы business_metrika_counter из шопсового PG в Oracle с разбивкой на чанки")
    void copyWithChunk() {
        environmentService.setValue(String.format(ENV_READ_CHUNK_SIZE, "business_metrika_counter"), "2");
        environmentService.setValue(String.format(ENV_WRITE_CHUNK_SIZE, "business_metrika_counter"), "2");
        businessMetrikaCopyExecutor.doJob(null);
    }

    @Test
    @DbUnitDataSet(
            before = "csv/BusinessMetrikaCopyExecutorTest.copy.before.csv",
            after = "csv/BusinessMetrikaCopyExecutorTest.copy.before.csv"
    )
    @DisplayName("Копирование таблицы business_metrika_counter из шопсового PG в Oracle не работает, когда enabled != true")
    void doesntWorkWhenDisabled() {
        environmentService.setValue(String.format(ENV_COPY_ENABLED, "business_metrika_counter"), "false");
        businessMetrikaCopyExecutor.doJob(null);
        environmentService.removeAllValues(String.format(ENV_COPY_ENABLED, "business_metrika_counter"));
        businessMetrikaCopyExecutor.doJob(null);
    }
}
