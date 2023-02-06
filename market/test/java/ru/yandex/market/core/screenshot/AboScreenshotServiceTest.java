package ru.yandex.market.core.screenshot;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.cutoff.model.AboScreenshot;

/**
 * Тесты для {@link AboScreenshotService}.
 */
class AboScreenshotServiceTest extends FunctionalTest {
    @Autowired
    AboScreenshotService aboScreenshotService;
    @Autowired
    AboScreenshotDao aboScreenshotDao;

    private static final List<AboScreenshot> SCREENSHOTS = List.of(
            new AboScreenshot(17L, "hash4"),
            new AboScreenshot(19L, "hash5")
    );


    @DisplayName("" +
            "Просто сохраняем скриншоты без привязки к чему либо," +
            "тестируем дао потому что в сервисе такого метода быть не должно." +
            "Скриншоты сохраняем в транзакционной привязке к чему-нибудь.")
    @Test
    @DbUnitDataSet(after = "csv/AboScreenshotServiceTest.saveTest.after.csv")
    void saveScreenshotsTest() {
        aboScreenshotDao.save(SCREENSHOTS);
    }

    @DisplayName("Сохряняем скриншоты и привязываем их к нотификациям")
    @Test
    @DbUnitDataSet(after = "csv/AboScreenshotServiceTest.saveNotificationScreenshotsTest.after.csv")
    void saveNotificationScreenshotsTest() {
        aboScreenshotService.saveNotificationScreenshots(1, SCREENSHOTS);
    }

    @Test
    @DbUnitDataSet(
            before = "csv/AboScreenshotServiceTest.testCleanupExpiredScreenshots.before.csv",
            after = "csv/AboScreenshotServiceTest.testCleanupExpiredScreenshots.after.csv"
    )
    void testCleanupExpiredScreenshots() {
        aboScreenshotService.cleanupExpiredScreenshots(55);
    }

    @Test
    @DbUnitDataSet(
            before = "csv/AboScreenshotServiceTest.testCleanupExpiredScreenshotsNotification.before.csv",
            after = "csv/AboScreenshotServiceTest.testCleanupExpiredScreenshotsNotification.after.csv"
    )
    void testCleanupExpiredScreenshotsNotification() {
        aboScreenshotService.cleanupExpiredScreenshots(55);
    }
}
