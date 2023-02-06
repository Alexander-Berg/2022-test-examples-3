package ru.yandex.market.moderation;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.feature.cutoff.ModerationEventsListener;
import ru.yandex.market.core.moderation.event.ModerationEvent;
import ru.yandex.market.core.shop.BulkShopHandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Тесты для {@link ModerationExecutor}.
 *
 * @author Vadim Lyalin
 */
public class ModerationExecutorTest extends FunctionalTest {
    @Autowired
    private ModerationExecutor moderationExecutor;

    /**
     * Проверяем, что собираем исключения и магазин продвигается по премодерационному пайплайну.
     */
    @Test
    @DbUnitDataSet(before = "ModerationExecutorTest.before.csv", after = "ModerationExecutorTest.after.csv")
    void test() {
        List<BulkShopHandler> tasks = new ArrayList<>(moderationExecutor.getTasks());
        tasks.add(4, buildErrorBulkShopHandler("2"));
        tasks.add(0, buildErrorBulkShopHandler("1"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> new ModerationExecutor(tasks).doJob(null));

        assertEquals("1", exception.getMessage());
        assertEquals(1, exception.getSuppressed().length);
        assertEquals("2", exception.getSuppressed()[0].getMessage());
    }

    /**
     * Проверяет правильность обработки статуса {@link ModerationEvent.ModerationEventType#PASS}
     * в {@link ModerationEventsListener#onApplicationEvent(ModerationEvent)}
     */
    @Test
    @DbUnitDataSet(
            before = {
                    "ModerationExecutorTest.closeTesting.before.csv",
                    "successMarketplaceSelfDeliveryFeature.selfTariffs.before.csv"
            },
            after = "ModerationExecutorTest.closeTesting.after.csv")
    @DisplayName("Проверка, что катоф TESTING закрывается, когда модерация завершилась")
    void testClosingTestingCutoff() {
        moderationExecutor.doJob(null);
    }

    /**
     * Проверяет правильность обработки статуса {@link ModerationEvent.ModerationEventType#PASS}
     * в {@link ModerationEventsListener#onApplicationEvent(ModerationEvent)}
     */
    @Test
    @DbUnitDataSet(
            before = {
                    "ModerationExecutorTest.testClosingPremoderationHoldCutoff.before.csv",
                    "successMarketplaceSelfDeliveryFeature.selfTariffs.before.csv"
            },
            after = "ModerationExecutorTest.testClosingPremoderationHoldCutoff.after.csv")
    @DisplayName("Проверка, что катоф MODERATION_NEED_INFO закрывается, когда модерация завершилась")
    void testClosingPremoderationHoldCutoff() {
        moderationExecutor.doJob(null);
    }

    private BulkShopHandler buildErrorBulkShopHandler(String errorMsg) {
        return new BulkShopHandler(
                () -> {
                    throw new RuntimeException(errorMsg);
                },
                null,
                null
        );
    }
}
