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

public class PartnerAppBusinessCopyExecutorTest extends FunctionalTest {

    @Autowired
    CopyExecutor partnerAppBusinessCopyExecutor;

    @Autowired
    EnvironmentService environmentService;

    @BeforeEach
    public void beforeEach() {
        environmentService.setValue(String.format(ENV_COPY_ENABLED, "partner_app_business"), "true");
    }

    @Test
    @DbUnitDataSet(
            before = "csv/PartnerAppBusinessCopyExecutorTest.copy.before.csv",
            after = "csv/PartnerAppBusinessCopyExecutorTest.copy.after.csv"
    )
    @DisplayName("Проверка копирования таблицы shops_web.partner_app_business из шопсового PG в Oracle")
    void copy() {
        partnerAppBusinessCopyExecutor.doJob(null);
    }

    @Test
    @DbUnitDataSet(
            before = "csv/PartnerAppBusinessCopyExecutorTest.copy.before.csv",
            after = "csv/PartnerAppBusinessCopyExecutorTest.copy.after.csv"
    )
    @DisplayName("Проверка копирования таблицы shops_web.partner_app_business из шопсового PG в Oracle с разбивкой на чанки")
    void copyWithChunk() {
        environmentService.setValue(String.format(ENV_READ_CHUNK_SIZE, "partner_app_business"), "3");
        environmentService.setValue(String.format(ENV_WRITE_CHUNK_SIZE, "partner_app_business"), "3");
        partnerAppBusinessCopyExecutor.doJob(null);
    }

    @Test
    @DbUnitDataSet(
            before = "csv/PartnerAppBusinessCopyExecutorTest.copy.before.csv",
            after = "csv/PartnerAppBusinessCopyExecutorTest.copy.before.csv"
    )
    @DisplayName("Копирование таблицы shops_web.partner_app_business из шопсового PG в Oracle не работает, когда enabled != true")
    void doesntWorkWhenDisabled() {
        environmentService.setValue(String.format(ENV_COPY_ENABLED, "partner_app_business"), "false");
        partnerAppBusinessCopyExecutor.doJob(null);
        environmentService.removeAllValues(String.format(ENV_COPY_ENABLED, "partner_app_business"));
        partnerAppBusinessCopyExecutor.doJob(null);
    }
}
