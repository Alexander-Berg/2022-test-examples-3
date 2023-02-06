package ru.yandex.market.cutoff.cpc;

import java.util.Set;

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
import static ru.yandex.market.billing.config.CutoffJobsConfig.DEFAULT_NOTIFICATION_TEMPLATE_ID;
import static ru.yandex.market.billing.tasks.cutoff.CutoffMockTestUtils.checkClosingCutoff;
import static ru.yandex.market.billing.tasks.cutoff.CutoffMockTestUtils.checkOpeningCutoff;
import static ru.yandex.market.billing.tasks.cutoff.CutoffMockTestUtils.getActualCloseCutoffRequest;
import static ru.yandex.market.billing.tasks.cutoff.CutoffMockTestUtils.getActualOpenCutoffRequest;
import static ru.yandex.market.billing.tasks.cutoff.CutoffMockTestUtils.mockGetCutoffResponseWithCutoffs;

/**
 * Тест для {@link CpcTechnicalCutoffNeedInfoExecutor}.
 */
@DbUnitDataSet(before = "CpcTechnicalCutoffNeedInfoExecutorTest.csv")
public class CpcTechnicalCutoffNeedInfoExecutorTest extends FunctionalTest {

    private static final CutoffType CUTOFF_TYPE = CutoffType.TECHNICAL_NEED_INFO;

    @Autowired
    private MbiOpenApiClient mbiOpenApiClient;

    @Autowired
    private CutoffExecutor cpcTechnicalCutoffNeedInfoExecutor;

    @Test
    void testCpcTechnicalCutoffNeedInfoExecutor() {
        Set<Long> openCutoffsBefore = Set.of(102L, 103L);
        Long closingCutoff = 102L;
        Long openingCutoff = 104L;

        mockGetCutoffResponseWithCutoffs(mbiOpenApiClient, CUTOFF_TYPE, openCutoffsBefore);

        cpcTechnicalCutoffNeedInfoExecutor.doJob(null);

        verify(mbiOpenApiClient).getCutoffs(CUTOFF_TYPE.getId());

        CloseCutoffsRequest closeRequest = getActualCloseCutoffRequest(mbiOpenApiClient);
        checkClosingCutoff(closeRequest, CUTOFF_TYPE, closingCutoff);

        OpenCutoffsRequest openRequest = getActualOpenCutoffRequest(mbiOpenApiClient);
        checkOpeningCutoff(openRequest, CUTOFF_TYPE, openingCutoff, DEFAULT_NOTIFICATION_TEMPLATE_ID);
    }
}
