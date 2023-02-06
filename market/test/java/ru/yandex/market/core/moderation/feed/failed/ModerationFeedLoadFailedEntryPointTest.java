package ru.yandex.market.core.moderation.feed.failed;

import java.time.Clock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.moderation.TestingShop;
import ru.yandex.market.core.protocol.model.ActionContext;
import ru.yandex.market.core.protocol.model.ActionContextBuilder;

import static org.mockito.Mockito.when;
import static ru.yandex.market.core.notification.service.PartnerNotificationApiServiceTest.verifySentNotificationType;
import static ru.yandex.market.core.protocol.model.ActionType.CANCEL_TESTING_PROCEDURE;

public class ModerationFeedLoadFailedEntryPointTest extends FunctionalTest {
    @Autowired
    private ModerationFeedLoadFailedEntryPoint tested;

    @Autowired
    private Clock clock;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(Clock.systemDefaultZone().instant());
    }

    @Test
    @DbUnitDataSet(
            before = "dsbsModeration.before.csv",
            after = "dsbsModeration.after.csv"
    )
    void testCancelDsbsModeration() {
        ActionContext action = ActionContextBuilder.system(CANCEL_TESTING_PROCEDURE);

        tested.rollback(action, new TestingShop(1L, 1L));

        verifySentNotificationType(partnerNotificationClient, 1, 55L);
    }

    @Test
    @DbUnitDataSet(
            before = "dsbsSelfCheck.before.csv",
            after = "dsbsSelfcheck.after.csv"
    )
    void testCancelDsbsSelfCheck() {
        ActionContext action = ActionContextBuilder.system(CANCEL_TESTING_PROCEDURE);

        tested.rollback(action, new TestingShop(1L, 1L));
    }
}
