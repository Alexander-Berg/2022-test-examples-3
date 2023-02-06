package ru.yandex.market.fps.module.payment.netting.test.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.fps.module.payment.netting.NettingAvailableJournalEntry;
import ru.yandex.market.fps.module.payment.netting.PaymentNetting;
import ru.yandex.market.fps.module.supplier1p.Supplier1p;
import ru.yandex.market.fps.module.supplier1p.test.impl.SupplierTestUtils;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.utils.Maps;

@Component
public class PaymentNettingTestUtils {
    private final SupplierTestUtils supplierTestUtils;
    private final BcpService bcpService;

    public PaymentNettingTestUtils(SupplierTestUtils supplierTestUtils, BcpService bcpService) {
        this.supplierTestUtils = supplierTestUtils;
        this.bcpService = bcpService;
    }

    public NettingAvailableJournalEntry createNettingAvailableJournalEntry(Supplier1p supplier) {
        return createNettingAvailableJournalEntry(Map.of(
                NettingAvailableJournalEntry.SUPPLIER, supplier
        ));
    }

    public NettingAvailableJournalEntry createNettingAvailableJournalEntry(Supplier1p supplier,
                                                                           Map<String, Object> attributes) {
        return createNettingAvailableJournalEntry(Maps.merge(attributes, Map.of(
                NettingAvailableJournalEntry.SUPPLIER, supplier
        )));
    }

    public NettingAvailableJournalEntry createNettingAvailableJournalEntry(Map<String, Object> attributes) {
        return bcpService.create(NettingAvailableJournalEntry.FQN, Maps.merge(Map.of(
                NettingAvailableJournalEntry.REC_ID, Randoms.positiveLongValue(),
                NettingAvailableJournalEntry.ISSUE_DATE, "2018-04-12T00:00:00+03",
                NettingAvailableJournalEntry.PLANNING_PAY_DATE, "2032-06-15T00:00:00+03",
                NettingAvailableJournalEntry.TITLE, Randoms.string(),
                NettingAvailableJournalEntry.INVOICE, Randoms.string(),
                NettingAvailableJournalEntry.DESCRIPTION, Randoms.string(),
                NettingAvailableJournalEntry.FULL_SUM, Randoms.positiveBigDecimal(5_000),
                NettingAvailableJournalEntry.REMAINING_SUM, Randoms.positiveBigDecimal(5_000),
                NettingAvailableJournalEntry.CURRENCY, "RUR"
        ), attributes));
    }

    public NettingAvailableJournalEntry createNettingAvailableJournalEntry() {
        return createNettingAvailableJournalEntry(supplierTestUtils.<Supplier1p>createSupplier(Map.of(
                Supplier1p.AX_RS_ID, Randoms.string()
        )));
    }

    public PaymentNetting createPaymentNetting(Supplier1p supplier,
                                               List<NettingAvailableJournalEntry> supplierJournalEntries,
                                               List<NettingAvailableJournalEntry> yandexJournalEntries) {
        return bcpService.create(PaymentNetting.FQN, Map.of(
                PaymentNetting.SUPPLIER, supplier,
                PaymentNetting.SUPPLIER_JOURNAL_ENTRIES, supplierJournalEntries,
                PaymentNetting.YANDEX_JOURNAL_ENTRIES, yandexJournalEntries
        ));
    }
}
