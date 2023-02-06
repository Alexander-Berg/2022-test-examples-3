package ru.yandex.market.wms.scheduler.service;

import java.util.Map;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.market.wms.common.spring.service.NamedCounterService;
import ru.yandex.market.wms.scheduler.config.SchedulerIntegrationTest;
import ru.yandex.market.wms.scheduler.service.holdlocks.LotSplitAndHoldService;

import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
public class LotSplitAndHoldServiceTest extends SchedulerIntegrationTest {

    @SpyBean
    @Autowired
    private LotSplitAndHoldService lotSplitAndHoldService;

    @SpyBean
    @Autowired
    private NamedCounterService namedCounterService;

    @BeforeEach
    public void reset() {
    }

    @Test
    @DatabaseSetup(value = "/service/lot-split-and-hold-service/1/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/service/lot-split-and-hold-service/1/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void checkDamageLockToHoldLot() {
        when(lotSplitAndHoldService.getNewLotKeys(anySet())).thenReturn(
                Map.of(
                        "LOT1", "LOT2"
                )
        );

        when(namedCounterService.getNextHoldTrnKey()).thenReturn("HOLDTRNKEY1");
        when(namedCounterService.getNextHoldTrnGroup()).thenReturn("HOLDTRNGROUP1");
        lotSplitAndHoldService.execute(10);
    }

    @Test
    @DatabaseSetup(value = "/service/lot-split-and-hold-service/2/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/service/lot-split-and-hold-service/2/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void checkSplittedLot() {
        Mockito.reset(lotSplitAndHoldService);
        when(namedCounterService.getNextHoldTrnKey()).thenReturn("HOLDTRNKEY1");
        when(namedCounterService.getNextHoldTrnGroup()).thenReturn("HOLDTRNGROUP1");
        when(namedCounterService.getNextLot()).thenReturn("LOT2");
        lotSplitAndHoldService.execute(10);
    }

    @Test
    @DatabaseSetup(value = "/service/lot-split-and-hold-service/3/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/service/lot-split-and-hold-service/3/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void checkHoldAndSplittedLots() {
        when(lotSplitAndHoldService.getNewLotKeys(anySet())).thenReturn(
                Map.of(
                        "LOT1", "LOT3",
                        "LOT2", "LOT4"
                )
        );

        lotSplitAndHoldService.execute(10);
    }

    @Test
    @DatabaseSetup(value = "/service/lot-split-and-hold-service/4/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/service/lot-split-and-hold-service/4/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void checkSeveralLocksOnHoldLot() {
        when(lotSplitAndHoldService.getNewLotKeys(anySet())).thenReturn(
                Map.of(
                        "LOT1", "LOT2"
                )
        );
        lotSplitAndHoldService.execute(10);
    }

    @Test
    @DatabaseSetup(value = "/service/lot-split-and-hold-service/5/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/service/lot-split-and-hold-service/5/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void checkLockIfNotExistsYet() {
        when(lotSplitAndHoldService.getNewLotKeys(anySet())).thenReturn(
                Map.of(
                        "LOT1", "LOT2"
                )
        );
        lotSplitAndHoldService.execute(10);
    }

    @Test
    @DatabaseSetup(value = "/service/lot-split-and-hold-service/6/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/service/lot-split-and-hold-service/6/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void checkPartlySplittedSeveralLots() {
        when(lotSplitAndHoldService.getNewLotKeys(anySet())).thenReturn(
                Map.of(
                       "LOT1", "LOT4",
                       "LOT2", "LOT5",
                       "LOT3", "LOT6"
                )
        );

        lotSplitAndHoldService.execute(10);
    }

    @Test
    @DatabaseSetup(value = "/service/lot-split-and-hold-service/7/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/service/lot-split-and-hold-service/7/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void checkSeveralLocksOnSplittedLot() {
        Mockito.reset(lotSplitAndHoldService);
        when(namedCounterService.getNextLot()).thenReturn("LOT2");
        lotSplitAndHoldService.execute(10);
    }

    @Test
    @DatabaseSetup(value = "/service/lot-split-and-hold-service/8/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/service/lot-split-and-hold-service/8/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void checkCopyLocksFromSourceLotToSplittedLot() {
        when(lotSplitAndHoldService.getNewLotKeys(anySet())).thenReturn(
                Map.of(
                        "LOT1", "LOT2"
                )
        );
        lotSplitAndHoldService.execute(10);
    }
}
