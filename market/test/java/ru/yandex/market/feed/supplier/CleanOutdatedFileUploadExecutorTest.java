package ru.yandex.market.feed.supplier;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneId;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.upload.FileUploadService;
import ru.yandex.market.shop.FunctionalTest;

@ParametersAreNonnullByDefault
class CleanOutdatedFileUploadExecutorTest extends FunctionalTest {

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private CleanOutdatedSupplierFeedUploadExecutor cleanOutdatedSupplierFeedUploadExecutor;

    @BeforeEach
    void before() {
        cleanOutdatedSupplierFeedUploadExecutor =
                new CleanOutdatedSupplierFeedUploadExecutor(
                        fileUploadService,
                        transactionTemplate,
                        Period.ofWeeks(2),
                        Clock.fixed(OffsetDateTime.parse("2018-01-14T00:00:00+03:00").toInstant(),
                                ZoneId.systemDefault()));
    }

    @Test
    @DbUnitDataSet(
            before = "CleanOutdatedSupplierFeedUploadExecutorTest.before.csv",
            after = "CleanOutdatedSupplierFeedUploadExecutorTest.after.csv")
    void testCleanup() {
        cleanOutdatedSupplierFeedUploadExecutor.doJob(null);
    }
}
