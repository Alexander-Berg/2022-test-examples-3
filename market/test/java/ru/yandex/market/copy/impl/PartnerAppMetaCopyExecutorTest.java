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

public class PartnerAppMetaCopyExecutorTest extends FunctionalTest {

    @Autowired
    CopyExecutor partnerAppMetaCopyExecutor;

    @Autowired
    EnvironmentService environmentService;

    @BeforeEach
    public void beforeEach() {
        environmentService.setValue(String.format(ENV_COPY_ENABLED, "partner_app_meta"), "true");
    }

    @Test
    @DbUnitDataSet(
            before = "csv/PartnerAppMetaCopyExecutorTest.copy.before.csv",
            after = "csv/PartnerAppMetaCopyExecutorTest.copy.after.csv"
    )
    @DisplayName("Проверка копирования таблицы shops_web.partner_app_meta из шопсового PG в Oracle")
    void copy() {
        partnerAppMetaCopyExecutor.doJob(null);
    }

    @Test
    @DbUnitDataSet(
            before = "csv/PartnerAppMetaCopyExecutorTest.copy.before.csv",
            after = "csv/PartnerAppMetaCopyExecutorTest.copy.after.csv"
    )
    @DisplayName("Проверка копирования таблицы shops_web.partner_app_meta из шопсового PG в Oracle с разбивкой на чанки")
    void copyWithChunk() {
        environmentService.setValue(String.format(ENV_READ_CHUNK_SIZE, "partner_app_meta"), "2");
        environmentService.setValue(String.format(ENV_WRITE_CHUNK_SIZE, "partner_app_meta"), "2");
        partnerAppMetaCopyExecutor.doJob(null);
    }

    @Test
    @DbUnitDataSet(
            before = "csv/PartnerAppMetaCopyExecutorTest.copy.before.csv",
            after = "csv/PartnerAppMetaCopyExecutorTest.copy.before.csv"
    )
    @DisplayName("Копирование таблицы shops_web.partner_app_meta из шопсового PG в Oracle не работает, когда enabled != true")
    void doesntWorkWhenDisabled() {
        environmentService.setValue(String.format(ENV_COPY_ENABLED, "partner_app_meta"), "false");
        partnerAppMetaCopyExecutor.doJob(null);
        environmentService.removeAllValues(String.format(ENV_COPY_ENABLED, "partner_app_meta"));
        partnerAppMetaCopyExecutor.doJob(null);
    }
}
