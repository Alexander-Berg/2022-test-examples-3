package ru.yandex.market.core.moderation.qc.light;

import java.time.Clock;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.moderation.qc.result.LiteCheckResult;
import ru.yandex.market.core.moderation.qc.result.LiteCheckResultRequestImpl;
import ru.yandex.market.core.moderation.qc.result.Message;
import ru.yandex.market.core.protocol.model.ActionContext;
import ru.yandex.market.core.protocol.model.ActionContextBuilder;
import ru.yandex.market.core.testing.TestingType;

import static org.mockito.Mockito.when;
import static ru.yandex.market.core.protocol.model.ActionType.REGISTER_MODERATION_CHECK_RESULT;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
class LiteCheckResultEntryPointTest extends FunctionalTest {

    @Autowired
    private LiteCheckResultEntryPoint liteCheckResultEntryPoint;

    @Autowired
    private Clock clock;

    @BeforeEach
    public void setUp() throws Exception {
        when(clock.instant()).thenReturn(Clock.systemDefaultZone().instant());
    }

    @Test
    @DbUnitDataSet(before = "liteCheckResult.before.csv")
    void ok() {
        ActionContext action = ActionContextBuilder.system(REGISTER_MODERATION_CHECK_RESULT);

        liteCheckResultEntryPoint.accept(action, new LiteCheckResultRequestImpl(
                774, TestingType.CPC_LITE_CHECK, LiteCheckResult.Status.OK, null
        ));
    }

    @Test
    @DbUnitDataSet(before = "liteCheckResult.before.csv", after = "liteCheckResultFail.after.csv")
    void fail() {
        ActionContext action = ActionContextBuilder.system(REGISTER_MODERATION_CHECK_RESULT);

        LiteCheckResult result = new LiteCheckResultRequestImpl(
                774, TestingType.CPC_LITE_CHECK, LiteCheckResult.Status.FAILED,
                Message.of(54, "test", "test", Collections.emptyList())
        );

        liteCheckResultEntryPoint.accept(action, result);
    }

    @Test
    @DbUnitDataSet(before = "liteCheckDSBSResult.before.csv", after = "liteCheckDSBSResult.after.csv")
    void failDsbsCheck() {
        ActionContext action = ActionContextBuilder.system(REGISTER_MODERATION_CHECK_RESULT);

        LiteCheckResult result = new LiteCheckResultRequestImpl(
                774, TestingType.DSBS_LITE_CHECK, LiteCheckResult.Status.FAILED,
                Message.of(54, "test", "test", Collections.emptyList())
        );

        liteCheckResultEntryPoint.accept(action, result);
    }
}
