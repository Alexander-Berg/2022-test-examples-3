package ru.yandex.market.billing.tasks.cutoff;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.config.CutoffJobsConfig;
import ru.yandex.market.billing.overdraft.OverdraftControlExecutor;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.cutoff.model.CutoffType;
import ru.yandex.market.cutoff.CutoffExecutor;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.open.api.client.MbiOpenApiClient;
import ru.yandex.market.mbi.open.api.client.model.CloseCutoffsRequest;
import ru.yandex.market.mbi.open.api.client.model.OpenCutoffsRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.billing.config.CutoffJobsConfig.OVERDRAFT_CONOTROL_CUTOFF_NOTIFICATION_TEMPLATE_ID;
import static ru.yandex.market.billing.tasks.cutoff.CutoffMockTestUtils.checkClosingCutoff;
import static ru.yandex.market.billing.tasks.cutoff.CutoffMockTestUtils.checkClosingCutoffs;
import static ru.yandex.market.billing.tasks.cutoff.CutoffMockTestUtils.checkOpeningCutoffs;
import static ru.yandex.market.billing.tasks.cutoff.CutoffMockTestUtils.getActualCloseCutoffRequest;
import static ru.yandex.market.billing.tasks.cutoff.CutoffMockTestUtils.getActualOpenCutoffRequest;
import static ru.yandex.market.billing.tasks.cutoff.CutoffMockTestUtils.mockGetCutoffResponseWithCutoff;
import static ru.yandex.market.billing.tasks.cutoff.CutoffMockTestUtils.mockGetCutoffResponseWithCutoffs;

/**
 * Тесты для cutoff {@link CutoffType#CPC_OVERDRAFT_CONTROL}.
 * <p>
 * См {@link CutoffJobsConfig#overdraftControlCutoffExecutor()}.
 * См {@link OverdraftControlExecutor}.
 */
class OverdraftControlCutoffExecutorTest extends FunctionalTest {

    private static final CutoffType CUTOFF_TYPE = CutoffType.CPC_OVERDRAFT_CONTROL;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private MbiOpenApiClient mbiOpenApiClient;

    @Autowired
    private CutoffExecutor overdraftControlCutoffExecutor;

    @Test
    @DisplayName("Открытие и закрытие катоффов")
    @DbUnitDataSet(before = "db/OverdraftControlCutoffExecutorTest.before.csv")
    void test_checkCutoff() {
        Long openCutoffBefore = 555L;
        Set<Long> openCutoffsAfter = Set.of(111L, 112L, 222L, 333L, 444L);

        mockGetCutoffResponseWithCutoff(mbiOpenApiClient, CUTOFF_TYPE, openCutoffBefore);

        environmentService.setValue("mbi.overdraft_control.cutoff_enabled", "enabled");
        overdraftControlCutoffExecutor.doJob(null);

        verify(mbiOpenApiClient).getCutoffs(CUTOFF_TYPE.getId());

        CloseCutoffsRequest closeRequest = getActualCloseCutoffRequest(mbiOpenApiClient);
        checkClosingCutoff(closeRequest, CUTOFF_TYPE, openCutoffBefore);

        OpenCutoffsRequest openRequest = getActualOpenCutoffRequest(mbiOpenApiClient);
        checkOpeningCutoffs(openRequest, CUTOFF_TYPE, openCutoffsAfter,
                OVERDRAFT_CONOTROL_CUTOFF_NOTIFICATION_TEMPLATE_ID);
    }

    @Test
    @DisplayName("Учет исключений из админки")
    @DbUnitDataSet(before = "db/OverdraftControlCutoffExecutorTest.exclusions.before.csv")
    void test_checkCutoff_exclusions() {
        Set<Long> openCutoffsBefore = Set.of(222L, 333L);
        Set<Long> openCutoffsAfter = Set.of(111L, 444L);

        mockGetCutoffResponseWithCutoffs(mbiOpenApiClient, CUTOFF_TYPE, openCutoffsBefore);

        environmentService.setValue("mbi.overdraft_control.cutoff_enabled", "enabled");
        overdraftControlCutoffExecutor.doJob(null);

        verify(mbiOpenApiClient).getCutoffs(CUTOFF_TYPE.getId());

        CloseCutoffsRequest closeRequest = getActualCloseCutoffRequest(mbiOpenApiClient);
        checkClosingCutoffs(closeRequest, CUTOFF_TYPE, openCutoffsBefore);

        OpenCutoffsRequest openRequest = getActualOpenCutoffRequest(mbiOpenApiClient);
        checkOpeningCutoffs(openRequest, CUTOFF_TYPE, openCutoffsAfter,
                OVERDRAFT_CONOTROL_CUTOFF_NOTIFICATION_TEMPLATE_ID);
    }

    @Test
    @DisplayName("mbi.overdraft_control.cutoff_enabled != enabled")
    @DbUnitDataSet(before = "db/OverdraftControlCutoffExecutorTest.before.csv")
    void test_checkCutoff_when_disabledAll() {
        Long openCutoffBefore = 555L;

        mockGetCutoffResponseWithCutoff(mbiOpenApiClient, CUTOFF_TYPE, openCutoffBefore);

        environmentService.setValue("mbi.overdraft_control.cutoff_enabled", "some_irrelevant_value");
        overdraftControlCutoffExecutor.doJob(null);

        verify(mbiOpenApiClient).getCutoffs(CUTOFF_TYPE.getId());

        CloseCutoffsRequest closeRequest = getActualCloseCutoffRequest(mbiOpenApiClient);
        checkClosingCutoff(closeRequest, CUTOFF_TYPE, openCutoffBefore);

        verify(mbiOpenApiClient, times(0)).openCutoffs(any());
    }
}
