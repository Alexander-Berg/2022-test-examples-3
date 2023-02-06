package ru.yandex.market.copy.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.copy.CopyExecutor;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static ru.yandex.market.copy.AbstractCopyService.ENV_COPY_ENABLED;
import static ru.yandex.market.copy.AbstractCopyService.ENV_READ_CHUNK_SIZE;
import static ru.yandex.market.copy.AbstractCopyService.ENV_WRITE_CHUNK_SIZE;

public class BusinessCopyExecutorTest extends FunctionalTest {

    @Autowired
    CopyExecutor businessCopyExecutor;

    @Autowired
    EnvironmentService environmentService;

    @BeforeEach
    public void beforeEach() {
        environmentService.setValue(String.format(ENV_COPY_ENABLED, "business"), "true");
    }

    @Test
    @DbUnitDataSet(
            before = "csv/BusinessCopyExecutorTest.copy.before.csv",
            after = "csv/BusinessCopyExecutorTest.copy.after.csv"
    )
    @DisplayName("Проверка копирования таблицы business из шопсового PG в Oracle")
    void copy() {
        businessCopyExecutor.doJob(null);
    }

    @Test
    @DbUnitDataSet(
            before = "csv/BusinessCopyExecutorTest.copy.before.csv",
            after = "csv/BusinessCopyExecutorTest.copy.after.csv"
    )
    @DisplayName("Проверка копирования таблицы business из шопсового PG в Oracle с разбивкой на чанки")
    void copyWithChunk() {
        environmentService.setValue(String.format(ENV_READ_CHUNK_SIZE, "business"), "2");
        environmentService.setValue(String.format(ENV_WRITE_CHUNK_SIZE, "business"), "2");
        businessCopyExecutor.doJob(null);
    }

    @Test
    @DbUnitDataSet(
            before = "csv/BusinessCopyExecutorTest.copy.before.csv",
            after = "csv/BusinessCopyExecutorTest.copy.before.csv"
    )
    @DisplayName("Копирование таблицы business из шопсового PG в Oracle не работает, когда enabled != true")
    void doesntWorkWhenDisabled() {
        environmentService.setValue(String.format(ENV_COPY_ENABLED, "business"), "false");
        businessCopyExecutor.doJob(null);
        environmentService.removeAllValues(String.format(ENV_COPY_ENABLED, "business"));
        businessCopyExecutor.doJob(null);
    }
}
