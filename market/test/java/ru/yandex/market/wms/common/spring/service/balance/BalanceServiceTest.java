package ru.yandex.market.wms.common.spring.service.balance;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.config.BalanceServiceTestConfig;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ContextConfiguration(classes = BalanceServiceTestConfig.class)
public class BalanceServiceTest extends IntegrationTest {
    @Autowired
    private BalanceServiceImpl balanceService;

    @Test
    @DatabaseSetup(value =
            "/db/service/balance/avoid-moving-if-reached-destination/immutable-state.xml")
    @ExpectedDatabase(value =
            "/db/service/balance/avoid-moving-if-reached-destination/immutable-state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testMoveContainerSkipsMovingIfAlreadyInTheTargetCell() {
        ListAppender<ILoggingEvent> logAppender = super.attachLogListAppender(BalanceServiceImpl.class);

        balanceService.moveContainer("ID4", "FX-AA1",
                MovingCause.builder()
                        .key("1234567890")
                        .initiator("TEST")
                        .type("TO")
                        .build()
        );

        assertEquals(1, logAppender.list.stream().filter(f -> f.getMessage().contains(
                "Movement haven't been done. Balances won't be moved."
        )).count());
    }

    @Test
    @DatabaseSetup(value =
            "/db/service/balance/anomalylot-inconsistency/immutable-state.xml")
    @ExpectedDatabase(value =
            "/db/service/balance/anomalylot-inconsistency/immutable-state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testAnomalyLotLocationInconsistencyThrowsError() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> balanceService.moveContainer("ID4", "FX-AA2",
                        MovingCause.builder()
                                .key("1234567890")
                                .initiator("TEST")
                                .type("TO")
                                .build()
                ));
        assertEquals(exception.getMessage(),
                "Anomalies of the same container are in different locations.");
    }

    @Test
    @DatabaseSetup("/db/service/balance/move-with-lose-id/before.xml")
    @ExpectedDatabase(value = "/db/service/balance/move-with-lose-id/after.xml", assertionMode = NON_STRICT_UNORDERED)
    public void testMoveContainerToLocWithLoseId() {
        balanceService.moveContainer("ID1", "B1-01",
                MovingCause.builder()
                        .key("ID1")
                        .initiator("TEST")
                        .type("TO")
                        .build());
    }

    @Test
    @DatabaseSetup("/db/service/balance/move-with-lose-id/before.xml")
    public void testMoveContainerToLocWithLoseIdAndPickDetails() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> balanceService.moveContainer("ID2", "B1-01",
                        MovingCause.builder()
                                .key("ID2")
                                .initiator("TEST")
                                .type("TO")
                                .build()
                ));
        assertEquals(exception.getMessage(),
                "Container cannot be moved to requested location");
    }

    @Test
    @DatabaseSetup("/db/service/balance/move-drop-with-details/before.xml")
    public void testMoveDropToLocWithPickDetails() {
        assertions.assertThatThrownBy(() -> balanceService.moveContainer("DRP123", "B1-01",
                MovingCause.builder()
                        .key("DRP123")
                        .initiator("TEST")
                        .type("TO")
                        .build()
        )).hasMessage("400 BAD_REQUEST \"Drop DRP123 is not empty\"");
    }

    @Test
    @DatabaseSetup("/db/service/balance/update-mismatched-lotlocid-qty/before.xml")
    @ExpectedDatabase(value = "/db/service/balance/update-mismatched-lotlocid-qty/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testUpdateLotLocIdQtyInFavorSerialInventory() {
        balanceService.updateLotLocIdQtyInFavorSerialInventory("ID");
    }

    @Test
    @DatabaseSetup("/db/service/balance/update-mismatched-lotlocid-qty-multiple-locs/before.xml")
    @ExpectedDatabase(value = "/db/service/balance/update-mismatched-lotlocid-qty-multiple-locs/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testUpdateLotLocIdQtyInFavorSerialInventoryWithIdInMultipleLocs() {
        balanceService.updateLotLocIdQtyInFavorSerialInventory("ID");
    }

    @Test
    @DatabaseSetup("/db/service/balance/empty-tote/before.xml")
    @ExpectedDatabase(value = "/db/service/balance/empty-tote/after.xml", assertionMode = NON_STRICT_UNORDERED)
    public void testMoveEmptyTote() {
        balanceService.moveContainer("VS0123456789", "B1-01",
                MovingCause.builder()
                        .key("VS0123456789")
                        .initiator("TEST")
                        .type("TO")
                        .build());
    }

    @Test
    @DatabaseSetup("/db/service/balance/empty-tote-with-type/before.xml")
    @ExpectedDatabase(value = "/db/service/balance/empty-tote-with-type/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testMoveEmptyToteWithType() {
        balanceService.moveContainer("ID1", "B1-01",
                MovingCause.builder()
                        .key("ID1")
                        .initiator("TEST")
                        .type("TO")
                        .build());
    }

    @Test
    @DatabaseSetup("/db/service/balance/empty-non-tote/before.xml")
    @ExpectedDatabase(value = "/db/service/balance/empty-non-tote/after.xml", assertionMode = NON_STRICT_UNORDERED)
    public void testMoveEmptyNonTote() {
        balanceService.moveContainer("BL01234567", "B1-01",
                MovingCause.builder()
                        .key("BL01234567")
                        .initiator("TEST")
                        .type("TO")
                        .build());
    }
}
