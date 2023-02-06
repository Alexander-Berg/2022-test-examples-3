package ru.yandex.market.replenishment.autoorder.service;

import java.time.LocalDateTime;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.repository.pdb.PdbReplenishmentRepository;
import ru.yandex.market.replenishment.autoorder.repository.postgres.ReplenishmentResultRepository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PdbReplenishmentServiceTest extends FunctionalTest {

    private static final LocalDateTime MOCK_DATE = LocalDateTime.of(2020, 9, 1, 0, 0);
    @Autowired
    private PdbReplenishmentService pdbReplenishmentService;
    @Autowired
    private ReplenishmentResultRepository replenishmentResultRepository;
    @Autowired
    private PdbReplenishmentRepository pdbReplenishmentRepository;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Before
    public void mockTimeService() {
        setTestTime(MOCK_DATE);
    }

    @Test
    @DbUnitDataSet(dataSource = "pdbDataSource", before = "PdbReplenishmentServiceTest.exportToPdb.before.pdb.csv")
    @DbUnitDataSet(before = "PdbReplenishmentService.importOrderInfo.before.csv",
        after = "PdbReplenishmentService.importOrderInfo.after.csv")
    public void testImportOrderInfosFromPdb() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@NotNull TransactionStatus status) {
                pdbReplenishmentService.importOrderInfos();
            }
        });
    }

    @Test
    @DbUnitDataSet(dataSource = "pdbDataSource", before = "PdbReplenishmentServiceTest.exportToPdb.before.pdb.csv")
    @DbUnitDataSet(before = "PdbReplenishmentService.importOrderInfoWithSubSsku.before.csv",
        after = "PdbReplenishmentService.importOrderInfoWithSubSsku.after.csv")
    public void testImportOrderInfosFromPdbWithSubSsku() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@NotNull TransactionStatus status) {
                pdbReplenishmentService.importOrderInfos();
            }
        });
    }

    @Test
    @DbUnitDataSet(before = "PdbReplenishmentServiceTest.exportToPdb.before.csv")
    @DbUnitDataSet(dataSource = "pdbDataSource", after = "PdbReplenishmentServiceTest.exportToPdb.after.pdb.csv")
    public void testExportToPdb() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@NotNull TransactionStatus status) {
                pdbReplenishmentRepository.deleteAll();
                pdbReplenishmentService.exportToPdb();
            }
        });

        assertThat(replenishmentResultRepository.findByExportTimestampIsNull(), empty());
    }

    @Test
    @DbUnitDataSet(before = "PdbReplenishmentServiceTest.exportToPdbWithMonoXdoc.before.csv")
    @DbUnitDataSet(dataSource = "pdbDataSource",
        after = "PdbReplenishmentServiceTest.exportToPdbWithMonoXdoc.after.pdb.csv")
    public void testExportToPdbWithMonoXdoc() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@NotNull TransactionStatus status) {
                pdbReplenishmentRepository.deleteAll();
                pdbReplenishmentService.exportToPdb();
            }
        });

        assertThat(replenishmentResultRepository.findByExportTimestampIsNull(), empty());
    }

    @Test
    @DbUnitDataSet(before = "PdbReplenishmentServiceTest.exportToPdbWithMonoXdocOff.before.csv")
    @DbUnitDataSet(dataSource = "pdbDataSource",
        after = "PdbReplenishmentServiceTest.exportToPdbWithMonoXdocOff.after.pdb.csv")
    public void testExportToPdbWithMonoXdocOff() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@NotNull TransactionStatus status) {
                pdbReplenishmentRepository.deleteAll();
                pdbReplenishmentService.exportToPdb();
            }
        });

        assertThat(replenishmentResultRepository.findByExportTimestampIsNull(), empty());
    }

    @Test
    @DbUnitDataSet(before = "PdbReplenishmentServiceTest.exportToPdb.before.csv")
    @DbUnitDataSet(dataSource = "pdbDataSource",
        before = "PdbReplenishmentServiceTest.exportToPdb.withDuplicates.before.pdb.csv",
        after = "PdbReplenishmentServiceTest.exportToPdb.after.pdb.csv")
    public void testExportToPdbWithDuplicates() {
        pdbReplenishmentService.exportToPdb();

        assertThat(replenishmentResultRepository.findByExportTimestampIsNull(), empty());
    }

    @Test
    @DbUnitDataSet(before = "PdbReplenishmentServiceTest.notNotify.before.csv")
    public void testRetryToAX_NotNotified() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@NotNull TransactionStatus status) {
                assertFalse(pdbReplenishmentService.needRetryNotificationToAX());
            }
        });
    }

    @Test
    @DbUnitDataSet(before = "PdbReplenishmentServiceTest.notify.before.csv")
    public void testRetryToAX_Notified() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@NotNull TransactionStatus status) {
                assertTrue(pdbReplenishmentService.needRetryNotificationToAX());
            }
        });
    }

    @Test
    @DbUnitDataSet(before = "PdbReplenishmentServiceTest.AXExportDelayStatus_Exist.before.csv")
    public void testAXExportDelayStatus_Exist() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@NotNull TransactionStatus status) {
                assertTrue(pdbReplenishmentService.existTooLongExportingOrdersToAX());
            }
        });
    }

    @Test
    @DbUnitDataSet(before = "PdbReplenishmentServiceTest.AXExportDelayStatus_NotExist.before.csv")
    public void testAXExportDelayStatus_NotExist() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@NotNull TransactionStatus status) {
                assertFalse(pdbReplenishmentService.existTooLongExportingOrdersToAX());
            }
        });
    }

    @Test
    @DbUnitDataSet(before = "PdbReplenishmentServiceTest.exportToPdbWithEdi.before.csv")
    @DbUnitDataSet(dataSource = "pdbDataSource",
        before = "PdbReplenishmentServiceTest.exportToPdb.withEdi.before.pdb.csv",
        after = "PdbReplenishmentServiceTest.exportToPdb.withEdi.after.pdb.csv")
    public void testExportToPdbWithEdi() {
        pdbReplenishmentService.exportToPdb();

        assertThat(replenishmentResultRepository.findByExportTimestampIsNull(), empty());
    }

    @Test
    @DbUnitDataSet(before = "PdbReplenishmentServiceTest.exportToPdbWithEdi.before.csv")
    @DbUnitDataSet(dataSource = "pdbDataSource",
        before = "PdbReplenishmentServiceTest.exportToPdb.withEdi.before.pdb.csv",
        after = "PdbReplenishmentServiceTest.exportToPdb.withEdiWithoutTimeToSend.after.pdb.csv")
    public void testExportToPdbWithEdiAfterTime() {
        setTestTime(LocalDateTime.of(2020, 9, 1, 14, 0));
        pdbReplenishmentService.exportToPdb();

        assertThat(replenishmentResultRepository.findByExportTimestampIsNull(), empty());
        setTestTime(MOCK_DATE);
    }

    @Test
    @DbUnitDataSet(before = "PdbReplenishmentServiceTest.exportToPdbWithEdiTimeToSendDisabled.before.csv")
    @DbUnitDataSet(dataSource = "pdbDataSource",
        before = "PdbReplenishmentServiceTest.exportToPdb.withEdi.before.pdb.csv",
        after = "PdbReplenishmentServiceTest.exportToPdb.withEdiWithoutTimeToSend.after.pdb.csv")
    public void testExportToPdbWithoutTimeToSenEdiAfterTime() {
        pdbReplenishmentService.exportToPdb();

        assertThat(replenishmentResultRepository.findByExportTimestampIsNull(), empty());
    }
}
