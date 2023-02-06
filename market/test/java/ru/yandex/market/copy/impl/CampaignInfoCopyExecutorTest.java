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

public class CampaignInfoCopyExecutorTest extends FunctionalTest {

    @Autowired
    CopyExecutor campaignInfoCopyExecutor;

    @Autowired
    EnvironmentService environmentService;

    @BeforeEach
    public void beforeEach() {
        environmentService.setValue(String.format(ENV_COPY_ENABLED, "campaign_info"), "true");
    }

    @Test
    @DbUnitDataSet(
            before = "csv/CampaignInfoCopyExecutorTest.copy.before.csv",
            after = "csv/CampaignInfoCopyExecutorTest.copy.after.csv"
    )
    @DisplayName("Проверка копирования таблицы campaign_info из шопсового PG в Oracle")
    void copy() {
        campaignInfoCopyExecutor.doJob(null);
    }

    @Test
    @DbUnitDataSet(
            before = "csv/CampaignInfoCopyExecutorTest.copy.before.csv",
            after = "csv/CampaignInfoCopyExecutorTest.copy.after.csv"
    )
    @DisplayName("Проверка копирования таблицы campaign_info из шопсового PG в Oracle с разбивкой на чанки")
    void copyWithChunk() {
        environmentService.setValue(String.format(ENV_READ_CHUNK_SIZE, "campaign_info"), "2");
        environmentService.setValue(String.format(ENV_WRITE_CHUNK_SIZE, "campaign_info"), "2");
        campaignInfoCopyExecutor.doJob(null);
    }

    @Test
    @DbUnitDataSet(
            before = "csv/CampaignInfoCopyExecutorTest.copy.before.csv",
            after = "csv/CampaignInfoCopyExecutorTest.copy.before.csv"
    )
    @DisplayName("Копирование таблицы campaign_info из шопсового PG в Oracle не работает, когда enabled != true")
    void doesntWorkWhenDisabled() {
        environmentService.setValue(String.format(ENV_COPY_ENABLED, "campaign_info"), "false");
        campaignInfoCopyExecutor.doJob(null);
        environmentService.removeAllValues(String.format(ENV_COPY_ENABLED, "campaign_info"));
        campaignInfoCopyExecutor.doJob(null);
    }
}
