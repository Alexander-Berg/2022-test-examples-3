package ru.yandex.market.fulfillment.stockstorage.service.warehouse;

import java.util.Arrays;
import java.util.Collections;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.fulfillment.stockstorage.AbstractContextualTest;
import ru.yandex.market.fulfillment.stockstorage.domain.exception.JobFailedException;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.StockSyncSwitchReason;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class WarehouseStockSyncSwitchServiceTest extends AbstractContextualTest {

    @Autowired
    private WarehouseStockSyncSwitchService service;

    @Autowired
    private WarehouseUnfreezeJobSwitchService unfreezeService;

    @AfterEach
    @Override
    public void resetMocks() {
        verifyNoMoreInteractions(lmsClient);
        super.resetMocks();
    }

    @Test
    @DatabaseSetup("classpath:database/states/unfreeze_jobs/queued/all_jobs_ok.xml")
    @ExpectedDatabase(
            value = "classpath:database/states/unfreeze_jobs/queued/all_jobs_ok.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    public void noFailsTest() {
        prepareLmsMock(true, true);
        service.trigger();
    }

    @Test
    @DatabaseSetup("classpath:database/states/execution_queue/18.xml")
    @ExpectedDatabase(
            value = "classpath:database/states/execution_queue/25.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    public void fullSyncFailed() {
        prepareLmsMock(true, true);
        service.trigger();
        verify(lmsClient).changeStockSync(100500L, false, StockSyncSwitchReason.AUTO_CHANGED_AFTER_FAIL);
    }

    @Test
    @DatabaseSetup("classpath:database/states/execution_queue/19.xml")
    @ExpectedDatabase(
            value = "classpath:database/states/execution_queue/25.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    public void korobyteSyncFailed() {
        prepareLmsMock(true, true);
        service.trigger();
        verify(lmsClient).changeStockSync(100500L, false, StockSyncSwitchReason.AUTO_CHANGED_AFTER_FAIL);
    }

    @Test
    @DatabaseSetup("classpath:database/states/execution_queue/24.xml")
    @ExpectedDatabase(
            value = "classpath:database/states/execution_queue/25.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    public void korobyteAndFullSyncFailed() {
        prepareLmsMock(true, true);
        service.trigger();
        verify(lmsClient).changeStockSync(100500L, false, StockSyncSwitchReason.AUTO_CHANGED_AFTER_FAIL);
    }

    @Test
    @DatabaseSetup("classpath:database/states/execution_queue/19.xml")
    @ExpectedDatabase(
            value = "classpath:database/states/execution_queue/19.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    public void stockSyncAlreadyDisabled() {
        prepareLmsMock(false, true);
        service.trigger();
    }

    @Test
    @DatabaseSetup("classpath:database/states/execution_queue/19.xml")
    @ExpectedDatabase(
            value = "classpath:database/states/execution_queue/19.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    public void autoSwitchStockSyncDisabled() {
        prepareLmsMock(true, false);
        service.trigger();
    }

    @Test
    @DatabaseSetup("classpath:database/states/execution_queue/19.xml")
    @ExpectedDatabase(
            value = "classpath:database/states/execution_queue/19.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    public void autoSwitchStockSyncAndStockSyncDisabled() {
        prepareLmsMock(false, false);
        service.trigger();
    }

    @Test
    @DatabaseSetup("classpath:database/states/unfreeze_jobs/queued/unfreeze_jobs_fail.xml")
    @ExpectedDatabase(
            value = "classpath:database/states/unfreeze_jobs/queued/unfreeze_jobs_fail.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    public void unfreezeJobsFailedAutoSwitchOff() {
        prepareLmsMock(true, false);
        unfreezeService.trigger();
    }

    @Test
    @DatabaseSetup("classpath:database/states/unfreeze_jobs/queued/unfreeze_jobs_fail.xml")
    @ExpectedDatabase(
            value = "classpath:database/states/execution_queue/25.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    public void unfreezeJobsFailedAutoSwitchOn() {
        prepareLmsMock(true, true);
        unfreezeService.trigger();
        verify(lmsClient).changeStockSync(100500L, false, StockSyncSwitchReason.AUTO_CHANGED_AFTER_FAIL);
    }

    @Test
    @DatabaseSetup("classpath:database/states/execution_queue/19.xml")
    @ExpectedDatabase(
            value = "classpath:database/states/execution_queue/19.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    public void lmsReturnedEmptyList() throws JobFailedException {
        mockSearchPartners(Collections.emptyList());
        warehouseSyncService.recomputeCache();

        try {
            service.trigger();
            fail("Expected an JobFailedException to be thrown");
        } catch (JobFailedException e) {
            softly.assertThat(e.getMessage()).isEqualTo("Synced warehouses are not present in LMS: [100500]");
        }

        verify(fulfillmentLmsClient, times(2)).searchPartners(any(SearchPartnerFilter.class));
    }

    @Test
    @DatabaseSetup("classpath:database/states/execution_queue/19.xml")
    @ExpectedDatabase(
            value = "classpath:database/states/execution_queue/25.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    public void lmsReturnedWhThatWasNotInCache() throws JobFailedException {
        mockSearchPartners(Collections.emptyList());
        prepareLmsMock(true, true);
        warehouseSyncService.recomputeCache();

        service.trigger();

        verify(fulfillmentLmsClient, times(2)).searchPartners(any(SearchPartnerFilter.class));
        verify(lmsClient).changeStockSync(100500L, false, StockSyncSwitchReason.AUTO_CHANGED_AFTER_FAIL);
    }


    private void prepareLmsMock(Boolean sync, Boolean autoSwitch) {
        mockSearchPartners(Arrays.asList(PartnerResponse.newBuilder()
                .id(100500L)
                .partnerType(PartnerType.FULFILLMENT)
                .name("Warehouse Name")
                .status(PartnerStatus.ACTIVE)
                .stockSyncEnabled(sync)
                .autoSwitchStockSyncEnabled(autoSwitch)
                .build())
        );

        warehouseSyncService.recomputeCache();
        verify(fulfillmentLmsClient).searchPartners(any(SearchPartnerFilter.class));
    }
}
