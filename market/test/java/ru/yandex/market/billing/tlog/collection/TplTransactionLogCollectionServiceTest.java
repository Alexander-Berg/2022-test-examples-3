package ru.yandex.market.billing.tlog.collection;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.config.OldFirstPartySuppliersIds;
import ru.yandex.market.billing.fulfillment.promo.SupplierPromoTariffDao;
import ru.yandex.market.billing.model.tlog.ExportServiceType;
import ru.yandex.market.billing.model.tlog.TplTransactionLogItem;
import ru.yandex.market.billing.service.environment.EnvironmentService;
import ru.yandex.market.billing.tlog.config.TplTransactionLogConfig;
import ru.yandex.market.billing.tlog.dao.TplTransactionLogCollectionDao;
import ru.yandex.market.billing.tlog.dao.TplTransactionLogDao;
import ru.yandex.market.billing.tlog.service.TransactionLogCollectionService;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.partner.PartnerContractServiceFactory;
import ru.yandex.market.core.partner.PartnerDao;

class TplTransactionLogCollectionServiceTest extends FunctionalTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2020-12-30T10:00:00Z"),
            ZoneOffset.systemDefault());

    @Autowired
    private TplTransactionLogDao tplTransactionLogDao;
    @Autowired
    private TplTransactionLogCollectionDao tplTransactionLogCollectionDao;
    @Autowired
    private PartnerDao partnerDao;
    @Autowired
    private PartnerContractServiceFactory contractServiceFactory;
    @Autowired
    private SupplierPromoTariffDao supplierPromoTariffDao;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private EnvironmentService environmentService;
    @Autowired
    private OldFirstPartySuppliersIds oldFirstPartySuppliersIds;

    private TransactionLogCollectionService<TplTransactionLogItem> transactionLogCollectionService;

    @BeforeEach
    void init() {
        transactionLogCollectionService = new TransactionLogCollectionService<>(
                CLOCK,
                TplTransactionLogConfig.getTransactionLogConfig(),
                tplTransactionLogDao,
                tplTransactionLogCollectionDao,
                contractServiceFactory,
                partnerDao,
                supplierPromoTariffDao,
                transactionTemplate,
                environmentService,
                oldFirstPartySuppliersIds
        );
    }

    @Test
    @DbUnitDataSet(
            before = "TplTransactionLogCollectionServiceTest.testLogisticPartnerOutgoingTx.before.csv",
            after = "TplTransactionLogCollectionServiceTest.testLogisticPartnerOutgoingTx.after.csv"
    )
    void testLogisticPartnerOutgoingTx() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Collections.singleton(ExportServiceType.LOGISTIC_PARTNER_OUTGOING_TX)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "TplTransactionLogCollectionServiceTest.testMultiplePaymentsAndRefunds.before.csv",
            after = "TplTransactionLogCollectionServiceTest.testMultiplePaymentsAndRefunds.after.csv"
    )
    void testMultiplePaymentsAndRefunds() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Collections.singleton(ExportServiceType.LOGISTIC_PARTNER_OUTGOING_TX)
        );
    }
}
