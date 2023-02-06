package ru.yandex.market.fps.module.payment.netting.test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import javax.annotation.Nonnull;

import com.google.common.collect.Iterators;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.fps.module.payment.netting.NettingAvailableJournalEntry;
import ru.yandex.market.fps.module.payment.netting.test.impl.PaymentNettingTestUtils;
import ru.yandex.market.fps.module.supplier1p.Supplier1p;
import ru.yandex.market.fps.module.supplier1p.test.impl.SupplierTestUtils;
import ru.yandex.market.jmf.dataimport.DataImportService;
import ru.yandex.market.jmf.dataimport.ImportRow;
import ru.yandex.market.jmf.dataimport.conf.datasource.Yt2DataSourceConf;
import ru.yandex.market.jmf.dataimport.datasource.DataSourceStrategy;
import ru.yandex.market.jmf.db.test.CleanDb;
import ru.yandex.market.jmf.entity.EntityStorageService;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.entity.test.assertions.EntityCollectionAssert;
import ru.yandex.market.jmf.time.Now;
import ru.yandex.market.jmf.tx.TxService;

@CleanDb
@SpringJUnitConfig(InternalModulePaymentNettingTestConfiguration.class)
public class JournalEntriesImportTest {
    private final DataSourceStrategy<Yt2DataSourceConf> yt2DataSourceStrategy;
    private final DataImportService dataImportService;
    private final EntityStorageService entityStorageService;
    private final SupplierTestUtils supplierTestUtils;
    private final TxService txService;
    private final PaymentNettingTestUtils paymentNettingTestUtils;

    public JournalEntriesImportTest(
            DataSourceStrategy<Yt2DataSourceConf> yt2DataSourceStrategy,
            DataImportService dataImportService,
            EntityStorageService entityStorageService,
            SupplierTestUtils supplierTestUtils,
            TxService txService,
            PaymentNettingTestUtils paymentNettingTestUtils) {
        this.yt2DataSourceStrategy = yt2DataSourceStrategy;
        this.dataImportService = dataImportService;
        this.entityStorageService = entityStorageService;
        this.supplierTestUtils = supplierTestUtils;
        this.txService = txService;
        this.paymentNettingTestUtils = paymentNettingTestUtils;
    }

    @Test
    public void testPositiveCaseOfNewImportWithoutOldData() {
        Supplier1p supplier = txService.doInTx(() -> supplierTestUtils.createSupplier(Map.of(
                Supplier1p.AX_RS_ID, "002344"
        )));

        Mockito.when(yt2DataSourceStrategy.iterator()).thenReturn(
                IntStream.range(0, 10).mapToObj(x -> randomImportRow(supplier.getAxRsId(), true)).iterator()
        );

        dataImportService.execute("module/payment/netting/imports/nettingAvailableJournalEntry.axYt.import.xml");

        txService.runInTx(() -> {
            List<NettingAvailableJournalEntry> journalEntries =
                    entityStorageService.list(Query.of(NettingAvailableJournalEntry.FQN));

            EntityCollectionAssert.assertThat(journalEntries)
                    .hasSize(10)
                    .allHasAttributes(NettingAvailableJournalEntry.SUPPLIER, supplier);
        });
    }

    @Test
    public void testPositiveCaseOfNewImportWithOldDataThatWillBeRemoved() {
        Supplier1p supplier = txService.doInTx(() -> {
            Supplier1p result = supplierTestUtils.createSupplier(Map.of(
                    Supplier1p.AX_RS_ID, "002344"
            ));

            paymentNettingTestUtils.createNettingAvailableJournalEntry(result);

            return result;
        });

        Mockito.when(yt2DataSourceStrategy.iterator()).thenReturn(
                IntStream.range(0, 10).mapToObj(x -> randomImportRow(supplier.getAxRsId(), true)).iterator()
        );

        Now.withOffset(Duration.ofSeconds(5), () ->
                dataImportService.execute("module/payment/netting/imports/nettingAvailableJournalEntry.axYt.import" +
                        ".xml"));
        txService.runInTx(() -> {
            List<NettingAvailableJournalEntry> journalEntries =
                    entityStorageService.list(Query.of(NettingAvailableJournalEntry.FQN));

            EntityCollectionAssert.assertThat(journalEntries)
                    .hasSize(10)
                    .allHasAttributes(NettingAvailableJournalEntry.SUPPLIER, supplier);
        });
    }

    @Test
    public void testNonApplicableJournalEntriesAreFilteredOut() {
        Supplier1p supplier = txService.doInTx(() -> supplierTestUtils.createSupplier(Map.of(
                Supplier1p.AX_RS_ID, "002344"
        )));

        Mockito.when(yt2DataSourceStrategy.iterator()).thenReturn(Iterators.concat(
                IntStream.range(0, 10).mapToObj(x -> randomImportRow(supplier.getAxRsId(), true)).iterator(),
                IntStream.range(0, 10).mapToObj(x -> randomImportRow(Randoms.string(), true)).iterator(),
                IntStream.range(0, 10).mapToObj(x -> randomImportRow(supplier.getAxRsId(), false)).iterator()
        ));

        dataImportService.execute("module/payment/netting/imports/nettingAvailableJournalEntry.axYt.import.xml");

        txService.runInTx(() -> {
            List<NettingAvailableJournalEntry> journalEntries =
                    entityStorageService.list(Query.of(NettingAvailableJournalEntry.FQN));

            EntityCollectionAssert.assertThat(journalEntries)
                    .hasSize(10)
                    .allHasAttributes(NettingAvailableJournalEntry.SUPPLIER, supplier);
        });
    }

    @Test
    public void testAx1900DateTreatedAsNull() {
        Supplier1p supplier = txService.doInTx(() -> supplierTestUtils.createSupplier(Map.of(
                Supplier1p.AX_RS_ID, "002344"
        )));

        Mockito.when(yt2DataSourceStrategy.iterator()).thenReturn(
                IntStream.range(0, 1).mapToObj(x -> randomImportRow(supplier.getAxRsId(), true))
                        .peek(r -> r.setProperty("planningPayDate", "01.01.1900 0:00:00")).iterator()
        );

        dataImportService.execute("module/payment/netting/imports/nettingAvailableJournalEntry.axYt.import.xml");

        txService.runInTx(() -> {
            List<NettingAvailableJournalEntry> journalEntries =
                    entityStorageService.list(Query.of(NettingAvailableJournalEntry.FQN));

            EntityCollectionAssert.assertThat(journalEntries).isEmpty();
        });
    }

    @Nonnull
    private ImportRow randomImportRow(String axRsId, boolean paymentNettingAvailable) {
        double fullSum = Randoms.moneyAsDouble();
        return new ImportRow(Map.of(
                "recId", Randoms.unsignedLongValue(),
                "rsId", axRsId,
                "issueDate", "12.04.2018 0:00:00",
                "planningPayDate", "15.06.2032 0:00:00",
                "invoice", Randoms.string(),
                "description", Randoms.string(),
                "fullSum", fullSum,
                "currency", "RUR",
                "remainingSum", Randoms.moneyAsDouble(fullSum / 2),
                "paymentNettingAvailable", paymentNettingAvailable
        ));
    }
}
