package ru.yandex.market.deliverycalculator.indexer.job;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.deliverycalculator.indexer.FunctionalTest;
import ru.yandex.market.deliverycalculator.storage.service.LogisticsStorageService;
import ru.yandex.market.deliverycalculator.storage.service.YaDeliveryTariffDbService;
import ru.yandex.market.deliverycalculator.storage.util.RetryTemplate;

class LogisticsPickupPointsImporterJobTest extends FunctionalTest {
    private static final String FILE_PATH = "LogisticsPickupPointsImporterJob/active_pickup_points.pbuf.sn";

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private LogisticsStorageService logisticsStorageService;

    @Autowired
    private YaDeliveryTariffDbService yaDeliveryTariffDbService;

    /**
     * Проверяет загрузку ПВЗ из прото файла. Новые точки должны добавиться в БД, неактуальные удалиться.
     */
    @Test
    @DbUnitDataSet(before = "logisticsPickupPointsImporterJob.before.csv",
            after = "logisticsPickupPointsImporterJob.after.csv")
    void logisticsPointsImport() throws IOException {
        final LogisticsPickupPointsImporterJob job = new LogisticsPickupPointsImporterJob(
                new ClassPathResource(FILE_PATH).getURL(),
                logisticsStorageService,
                transactionTemplate,
                RetryTemplate.createFixed(1, 1),
                yaDeliveryTariffDbService);

        job.doJob(null);
    }
}
