package ru.yandex.market.supplier;

import java.time.Clock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.db.SingleFileCsvProducer;
import ru.yandex.market.core.summary.ContractCorrectionSummaryService;
import ru.yandex.market.shop.FunctionalTest;
import ru.yandex.market.supplier.summary.ContractCorrectionSummaryExecutor;

import static org.mockito.Mockito.when;

class ContractCorrectionSummaryExecutorTest extends FunctionalTest {

    private ContractCorrectionSummaryExecutor executor;

    @Autowired
    private ContractCorrectionSummaryService contractCorrectionSummaryService;

    @Autowired
    Clock clock;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(SingleFileCsvProducer.Functions.truncedsysdate(0).toInstant());

        executor = new ContractCorrectionSummaryExecutor(contractCorrectionSummaryService);
    }

    @Test
    @DbUnitDataSet(
            before = "SupplierCorrectionSummaryExecutorTest.before.csv",
            after = "SupplierCorrectionSummaryExecutorTest.after.csv"
    )
    void doJob() {
        executor.doJob(null);
    }
}
