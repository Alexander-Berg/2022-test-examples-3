package ru.yandex.market.wms.scheduler.service;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.test.context.TestConstructor;

import ru.yandex.market.wms.scheduler.config.SchedulerIntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
@DatabaseSetup("/service/transfer/before-base.xml")
class TransferFinalizerServiceTest extends SchedulerIntegrationTest {
    private final TransferFinalizerService transferFinalizerService;

    @Test
    @DatabaseSetup("/service/transfer/before-single-hold.xml")
    @ExpectedDatabase(value = "/service/transfer/after-single-hold.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    void removeSingleCisQuar() {
        transferFinalizerService.finalizeAll();
    }

    @Test
    @DatabaseSetup("/service/transfer/before-multiple-holds.xml")
    @ExpectedDatabase(value = "/service/transfer/after-multiple-holds.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    void removeCisQuarAndKeepOther() {
        transferFinalizerService.finalizeAll();
    }

    @Test
    @DatabaseSetup("/service/transfer/before-new-lot.xml")
    @ExpectedDatabase(value = "/service/transfer/after-new-lot.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    void splitLotAndRemoveCisQuar() {
        transferFinalizerService.finalizeAll();
    }

    @Test
    @DatabaseSetup("/service/transfer/before-new-lot-multiple-holds.xml")
    @ExpectedDatabase(value = "/service/transfer/after-new-lot-multiple-holds.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    void splitLotWithHoldDelete() {
        transferFinalizerService.finalizeAll();
    }

    @Test
    @DatabaseSetup("/service/transfer/before-change-hold.xml")
    @ExpectedDatabase(value = "/service/transfer/after-change-hold.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    void changeHold() {
        transferFinalizerService.finalizeAll();
    }

    @Test
    @DatabaseSetup("/service/transfer/before-change-hold-new-lot.xml")
    @ExpectedDatabase(value = "/service/transfer/after-change-hold-new-lot.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    void splitLotWithHoldChange() {
        transferFinalizerService.finalizeAll();
    }
}
