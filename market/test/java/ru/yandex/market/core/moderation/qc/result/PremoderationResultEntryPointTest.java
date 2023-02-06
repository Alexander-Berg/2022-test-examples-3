package ru.yandex.market.core.moderation.qc.result;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.feature.cutoff.ModerationEventsListener;
import ru.yandex.market.core.moderation.event.ModerationEvent;
import ru.yandex.market.core.moderation.sandbox.ModerationClock;
import ru.yandex.market.core.protocol.model.ActionContext;
import ru.yandex.market.core.protocol.model.ActionContextBuilder;
import ru.yandex.market.core.testing.TestingType;

import static org.mockito.Mockito.when;
import static ru.yandex.market.core.protocol.model.ActionType.REGISTER_MODERATION_CHECK_RESULT;

/**
 * @author zoom
 */
public class PremoderationResultEntryPointTest extends FunctionalTest {

    @Autowired
    private PremoderationResultEntryPoint moderationCheckResultEntryPoint;

    @Autowired
    private Clock clock;

    @Mock
    private ModerationClock moderationClock;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(clock.instant()).thenReturn(Clock.systemDefaultZone().instant());
    }

    /**
     * Принимаем результат полной премодерации и меняем магазину статус на
     * {@link ru.yandex.market.core.testing.TestingStatus#WAITING_FEED_LAST_LOAD}
     */
    @Test
    @DbUnitDataSet(before = "premodResult.before.csv", after = "premodResultCorrect.after.csv")
    public void premodResultCorrect() {
        ActionContext action = ActionContextBuilder.system(REGISTER_MODERATION_CHECK_RESULT);

        PremoderationResult result = PremoderationResult.of(
                774L, TestingType.CPC_PREMODERATION, PremoderationResult.Status.PASSED, PremoderationResult.Status.PASSED,
                PremoderationResult.Status.PASSED, PremoderationResult.Status.PASSED, null, null
        );
        moderationCheckResultEntryPoint.accept(action, result);
    }

    /**
     * Принимаем результат полной премодерации и меняем магазину статус на
     * {@link ru.yandex.market.core.testing.TestingStatus#READY_TO_FAIL}
     */
    @Test
    @DbUnitDataSet(
            before = {"premodResult.before.csv", "premodResultQualityFailed.before.csv"},
            after = "premodResultQualityFailed.after.csv"
    )
    public void premodResultQualityFailed() {
        when(moderationClock.now()).thenReturn(Date.from(LocalDateTime.of(2016, 1, 2, 0, 0).atZone(ZoneId.systemDefault()).toInstant()));
        ActionContext action = ActionContextBuilder.system(REGISTER_MODERATION_CHECK_RESULT);

        PremoderationResult result = PremoderationResult.of(
                774L, TestingType.CPC_PREMODERATION, PremoderationResult.Status.FAILED, PremoderationResult.Status.PASSED,
                PremoderationResult.Status.PASSED, PremoderationResult.Status.PASSED, Message.of(54, "test", "test", Collections.emptyList()), null
        );

        moderationCheckResultEntryPoint.accept(action, result);
    }

    /**
     * Проверяет правильность обработки статуса {@link ModerationEvent.ModerationEventType#FAIL}
     * в {@link ModerationEventsListener#onApplicationEvent(ModerationEvent)}
     */
    @Test
    @DbUnitDataSet(
            before = "premodResultQualityHalted.openQualityCutoff.before.csv",
            after = "premodResultQualityHalted.openQualityCutoff.after.csv"
    )
    @DisplayName("Проверка, что катоф QUALITY открывается, когда модерация завершилась неуспешно")
    public void premodResultQualityHaltedTestOpenQualityCutoff() {
        sendHaltedQualityResult(774);
        sendHaltedQualityResult(1000);
    }

    @Test
    @DbUnitDataSet(
            before = "premodResultQualityFailed.cutoffOpened.before.csv",
            after = "premodResultQualityFailed.cutoffOpened.after.csv"
    )
    @DisplayName("Проверка, что катоф QUALITY открывается, когда модерация завершилась неуспешно, если фича уже " +
            "отключена")
    public void failModerationWhenAlreadyOpenedCutoff() {
        when(moderationClock.now())
                .thenReturn(Date.from(LocalDateTime.of(2020, 1, 2, 0, 0).atZone(ZoneId.systemDefault()).toInstant()));
        ActionContext action = ActionContextBuilder.system(REGISTER_MODERATION_CHECK_RESULT);

        var result = PremoderationResult.of(
                774L, TestingType.CPA_PREMODERATION, PremoderationResult.Status.FAILED,
                PremoderationResult.Status.PASSED, PremoderationResult.Status.FAILED, PremoderationResult.Status.PASSED,
                Message.of(54, "test", "test", Collections.emptyList()), null
        );
        moderationCheckResultEntryPoint.accept(action, result);
    }

    private void sendHaltedQualityResult(long shopId) {
        when(moderationClock.now())
                .thenReturn(Date.from(LocalDateTime.of(2020, 1, 2, 0, 0).atZone(ZoneId.systemDefault()).toInstant()));
        ActionContext action = ActionContextBuilder.system(REGISTER_MODERATION_CHECK_RESULT);

        var result = PremoderationResult.of(
                shopId, TestingType.CPA_PREMODERATION, PremoderationResult.Status.HALTED,
                PremoderationResult.Status.PASSED, PremoderationResult.Status.PASSED, PremoderationResult.Status.PASSED,
                Message.of(54, "test", "test", Collections.emptyList()), null
        );
        moderationCheckResultEntryPoint.accept(action, result);
    }
}
