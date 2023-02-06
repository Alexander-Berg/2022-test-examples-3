package ru.yandex.market.mboc.common.notifications.model;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.api.Randomizer;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mboc.common.notifications.model.data.NotificationData;
import ru.yandex.market.mboc.common.notifications.model.data.catmans.NewOffersData;
import ru.yandex.market.mboc.common.notifications.model.data.suppliers.ProcessedOffersData;
import ru.yandex.market.mboc.common.notifications.repository.NotificationRepositoryMock;

/**
 * @author prediger
 */
public class NotificationEqualsTest {

    private static final long SEED = 1408;

    private final EnhancedRandom random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
        .seed(SEED)
        .randomize(NotificationData.class, (Randomizer<NotificationData>) this::getRandomData)
        .overrideDefaultInitialization(true)
        .build();

    private static void assertEquals(Notification notification1, Notification notification2) {
        // проверяем, что при копировании объекты равны друг другу
        Assertions.assertThat(notification2).isEqualToComparingFieldByField(notification1);
        // также проверяем, что тестовый компаратор тоже будет возвращать равенство
        Assertions.assertThat(notification2)
            .usingComparator(NotificationRepositoryMock.NOTIFICATION_COMPARATOR)
            .isEqualTo(notification1);
    }

    private static void assertNotEquals(Notification notification1, Notification notification2) {
        // проверяем, что тестовый компаратор НЕ будет возвращать равенство
        Assertions.assertThat(notification2)
            .usingComparator(NotificationRepositoryMock.NOTIFICATION_COMPARATOR)
            .isNotEqualTo(notification1);
    }

    @Test
    public void testCopyWorksCorrectly() {
        for (int i = 0; i < 100; i++) {
            Notification notification = getRandomValue();
            Notification copy = new Notification(notification);

            assertEquals(notification, copy);
        }
    }

    @Test
    public void testNotificationsComparatorWillCorrectlyCompareDifferentObjects() {
        Notification prevValue = getRandomValue();
        for (int i = 0; i < 100; i++) {
            Notification notification = getRandomValue();

            assertNotEquals(notification, prevValue);
            prevValue = notification;
        }
    }

    private Notification getRandomValue() {
        return random.nextObject(Notification.class);
    }

    private NotificationData getRandomData() {
        int dataType = random.nextInt(2);
        return dataType == 0 ? random.nextObject(ProcessedOffersData.class) : random.nextObject(NewOffersData.class);
    }
}
