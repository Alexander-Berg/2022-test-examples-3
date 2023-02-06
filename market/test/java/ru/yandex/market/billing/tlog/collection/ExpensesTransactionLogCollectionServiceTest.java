package ru.yandex.market.billing.tlog.collection;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.core.billing.fulfillment.promo.SupplierPromoTariffService;
import ru.yandex.market.billing.tlog.config.ExpensesTransactionLogConfig;
import ru.yandex.market.billing.tlog.model.ExpensesTransactionLogItem;
import ru.yandex.market.billing.tlog.model.ExportServiceType;
import ru.yandex.market.billing.tlog.yt.ExpensesTransactionLogDao;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.partner.contract.PartnerContractDao;
import ru.yandex.market.core.supplier.SupplierService;

class ExpensesTransactionLogCollectionServiceTest extends FunctionalTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2020-12-30T10:00:00Z"),
            ZoneOffset.systemDefault());

    @Autowired
    private ExpensesTransactionLogDao expensesTransactionLogDao;
    @Autowired
    private ExpensesTransactionLogCollectionDao expensesTransactionLogCollectionDao;
    @Autowired
    private PartnerContractDao supplierContractDao;
    @Autowired
    private SupplierService supplierService;
    @Autowired
    private SupplierPromoTariffService supplierPromoTariffService;
    @Autowired
    private TransactionTemplate transactionTemplate;

    private TransactionLogCollectionService<ExpensesTransactionLogItem> transactionLogCollectionService;

    @BeforeEach
    void init() {
        transactionLogCollectionService = new TransactionLogCollectionService<>(
                CLOCK,
                ExpensesTransactionLogConfig.getTransactionLogConfig(),
                expensesTransactionLogDao,
                expensesTransactionLogCollectionDao,
                supplierContractDao,
                supplierService,
                supplierPromoTariffService,
                transactionTemplate
        );
    }

    @Test
    @DbUnitDataSet(
            before = "ExpensesTransactionLogCollectionServiceTest.testLogisticPartnerOutgoingTx.before.csv",
            after = "ExpensesTransactionLogCollectionServiceTest.testLogisticPartnerOutgoingTx.after.csv"
    )
    void testLogisticPartnerOutgoingTx() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Collections.singleton(ExportServiceType.LOGISTIC_PARTNER_OUTGOING_TX)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "ExpensesTransactionLogCollectionServiceTest.testMultiplePaymentsAndRefunds.before.csv",
            after = "ExpensesTransactionLogCollectionServiceTest.testMultiplePaymentsAndRefunds.after.csv"
    )
    void testMultiplePaymentsAndRefunds() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Collections.singleton(ExportServiceType.LOGISTIC_PARTNER_OUTGOING_TX)
        );
    }
}
