package ru.yandex.market.billing.tasks.cutoff;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.cutoff.model.CutoffType;
import ru.yandex.market.cutoff.CutoffExecutor;
import ru.yandex.market.mbi.open.api.client.MbiOpenApiClient;
import ru.yandex.market.mbi.open.api.client.model.CloseCutoffsRequest;
import ru.yandex.market.mbi.open.api.client.model.OpenCutoffsRequest;

import static org.mockito.Mockito.verify;
import static ru.yandex.market.billing.tasks.cutoff.CutoffMockTestUtils.checkClosingCutoff;
import static ru.yandex.market.billing.tasks.cutoff.CutoffMockTestUtils.checkOpeningCutoff;
import static ru.yandex.market.billing.tasks.cutoff.CutoffMockTestUtils.getActualCloseCutoffRequest;
import static ru.yandex.market.billing.tasks.cutoff.CutoffMockTestUtils.getActualOpenCutoffRequest;
import static ru.yandex.market.billing.tasks.cutoff.CutoffMockTestUtils.mockGetCutoffResponseWithCutoff;
import static ru.yandex.market.cutoff.CutoffJobConfig.DEFAULT_NOTIFICATION_TEMPLATE_ID;

class FinanceLimitCutoffTest extends FunctionalTest {

    private static final CutoffType CUTOFF_TYPE = CutoffType.CPC_FINANCE_LIMIT;
    private static final Long OPEN_CUTOFF_BEFORE = 45L;
    private static final Long OPEN_CUTOFF_AFTER = 42L;

    @Autowired
    private MbiOpenApiClient mbiOpenApiClient;

    @Autowired
    private CutoffExecutor financeDailyLimitCutoffExecutor;

    @Test
    @DbUnitDataSet(before = "financeLimitCutoffExecutorTest.before.csv")
    void checkOpenCutoff() {
        mockGetCutoffResponseWithCutoff(mbiOpenApiClient, CUTOFF_TYPE, OPEN_CUTOFF_BEFORE);

        financeDailyLimitCutoffExecutor.doJob(null);

        verify(mbiOpenApiClient).getCutoffs(CUTOFF_TYPE.getId());

        CloseCutoffsRequest closeRequest = getActualCloseCutoffRequest(mbiOpenApiClient);
        checkClosingCutoff(closeRequest, CUTOFF_TYPE, OPEN_CUTOFF_BEFORE);

        OpenCutoffsRequest openRequest = getActualOpenCutoffRequest(mbiOpenApiClient);
        checkOpeningCutoff(openRequest, CUTOFF_TYPE, OPEN_CUTOFF_AFTER, DEFAULT_NOTIFICATION_TEMPLATE_ID);
    }
}
