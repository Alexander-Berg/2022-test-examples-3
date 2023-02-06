package ru.yandex.market.billing.imports.logistic;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.imports.logistic.dao.LogisticPartnerOutgoingTxDao;
import ru.yandex.market.billing.imports.logistic.dao.LogisticPartnerOutgoingTxYtImportDao;
import ru.yandex.market.billing.imports.logistic.model.LogisticPartnerOutgoingTx;
import ru.yandex.market.billing.service.environment.EnvironmentService;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.util.DateTimes;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class LogisticPartnerOutgoingTxImportServiceTest extends FunctionalTest {

    private static final LocalDate MAY_28_2020 = LocalDate.of(2020, 5, 28);

    @Autowired
    private LogisticPartnerOutgoingTxDao logisticPartnerOutgoingTxDao;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private EnvironmentService environmentService;

    private LogisticPartnerOutgoingTxImportService logisticPartnerOutgoingTxImportService;

    @BeforeEach
    void setup() {
        LogisticPartnerOutgoingTxYtImportDao logisticPartnerOutgoingTxYtImportDao =
                mock(LogisticPartnerOutgoingTxYtImportDao.class);

        doReturn(
                List.of(
                        LogisticPartnerOutgoingTx.builder()
                                .setBillingTransactionId(12)
                                .setYtTableDate(MAY_28_2020)
                                .setServiceTransactionId("service_transaction_id_3")
                                .setServiceEventTime(DateTimes.toInstantAtDefaultTz(
                                        LocalDateTime.of(2020, 5, 1, 17, 30, 0)
                                ))
                                .setBillingEventTime(DateTimes.toInstantAtDefaultTz(
                                        LocalDateTime.of(2020, 5, 26, 17, 30, 0)
                                ))
                                .setProductName("product_name_3")
                                .setClientId(1003)
                                .setServiceId(103)
                                .setAmount(650)
                                .setExportedToTlog(false)
                                .build(),
                        LogisticPartnerOutgoingTx.builder()
                                .setBillingTransactionId(14)
                                .setYtTableDate(MAY_28_2020)
                                .setServiceTransactionId("service_transaction_id_4")
                                .setServiceEventTime(DateTimes.toInstantAtDefaultTz(
                                        LocalDateTime.of(2020, 5, 1, 18, 30, 0)
                                ))
                                .setBillingEventTime(DateTimes.toInstantAtDefaultTz(
                                        LocalDateTime.of(2020, 5, 26, 18, 30, 0)
                                ))
                                .setProductName("product_name_4")
                                .setClientId(1004)
                                .setServiceId(104)
                                .setAmount(900)
                                .setExportedToTlog(false)
                                .build()
                )
        ).when(logisticPartnerOutgoingTxYtImportDao).getLogisticPartnerOutgoingTx(any(LocalDate.class));
        logisticPartnerOutgoingTxImportService = new LogisticPartnerOutgoingTxImportService(
                transactionTemplate,
                logisticPartnerOutgoingTxDao,
                logisticPartnerOutgoingTxYtImportDao,
                environmentService
        );
    }

    @Test
    @DbUnitDataSet(
            before = "LogisticPartnerOutgoingTxImportServiceTest.testImport.before.csv",
            after = "LogisticPartnerOutgoingTxImportServiceTest.testImport.after.csv"
    )
    void testImport() {
        logisticPartnerOutgoingTxImportService.process(MAY_28_2020);
    }

    @Test
    @DbUnitDataSet(
            before = "LogisticPartnerOutgoingTxImportServiceTest.testImportOnDuplicate.before.csv",
            after = "LogisticPartnerOutgoingTxImportServiceTest.testImportOnDuplicate.after.csv"
    )
    void testUpdateImportOnDuplicate() {
        logisticPartnerOutgoingTxImportService.process(MAY_28_2020);
    }
}
