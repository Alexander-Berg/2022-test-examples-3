package ru.yandex.market.wms.scheduler.service.blockexpiredlots;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.scheduler.config.SchedulerIntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BlockExpiredLotsTest extends SchedulerIntegrationTest {
    private static final String ROOT = "/service/blockexpiredlots";
    private static final String EXPIRE = ROOT + "/expire";
    private static final String UNEXPIRE = ROOT + "/unexpire";

    @Autowired
    private BlockExpiredLotsService blockExpiredLotsService;

    @Test
    @DatabaseSetup(value = EXPIRE + "/remove-disabled-holds/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = EXPIRE + "/remove-disabled-holds/after.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void removeDisabledHolds() throws InterruptedException {
        blockExpiredLotsService.execute();
    }

    @Test
    @DatabaseSetup(value = EXPIRE + "/without-holds/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = EXPIRE + "/without-holds/after.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void expireWithoutHolds() throws InterruptedException {
        blockExpiredLotsService.execute();
    }

    @Test
    @DatabaseSetup(value = EXPIRE + "/with-nonexp-holds/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/service/blockexpiredlots/expire/with-nonexp-holds/after.xml", connection =
            "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void expireWithNonexpirationHolds() throws InterruptedException {
        blockExpiredLotsService.execute();
    }

    @Test
    @DatabaseSetup(value = EXPIRE + "/with-exp-hold/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = EXPIRE + "/with-exp-hold/after.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void expireWithExpirationHold() throws InterruptedException {
        blockExpiredLotsService.execute();
    }

    @Test
    @DatabaseSetup(value = EXPIRE + "/with-disabled-exp-hold/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = EXPIRE + "/with-disabled-exp-hold/after.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void expireWithDisabledExpirationHold() throws InterruptedException {
        blockExpiredLotsService.execute();
    }

    @Test
    @DatabaseSetup(value = UNEXPIRE + "/with-nonexp-holds/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = UNEXPIRE + "/with-nonexp-holds/after.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void unexpireWithNonexpirationHold() throws InterruptedException {
        blockExpiredLotsService.execute();
    }

    @Test
    @DatabaseSetup(value = UNEXPIRE + "/with-exp-hold/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = UNEXPIRE + "/with-exp-hold/after.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void unexpireWithExpirationHold() throws InterruptedException {
        blockExpiredLotsService.execute();
    }

    @Test
    @DatabaseSetup(value = UNEXPIRE + "/with-exp-hold-in-hold-loc/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = UNEXPIRE + "/with-exp-hold-in-hold-loc/after.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void unexpireWithExpirationHoldInLocWithHold() throws InterruptedException {
        blockExpiredLotsService.execute();
    }

    @Test
    @DatabaseSetup(value = UNEXPIRE + "/with-two-holds/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = UNEXPIRE + "/with-two-holds/after.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void unexpireWithTwoHolds() throws InterruptedException {
        blockExpiredLotsService.execute();
    }

    @Test
    @DatabaseSetup(value = UNEXPIRE + "/with-exp-hold-in-hold-loc-part/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = UNEXPIRE + "/with-exp-hold-in-hold-loc-part/after.xml", connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    public void unexpireWithExpirationHoldInLocWithHoldPartial() throws InterruptedException {
        blockExpiredLotsService.execute();
    }
}
