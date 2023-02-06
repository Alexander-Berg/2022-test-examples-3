package ru.yandex.market.notification.simple.model.type;

import org.junit.Test;

import ru.yandex.common.util.id.HasId;
import ru.yandex.market.notification.test.model.AbstractModelTest;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Unit-тесты для {@link NotificationPriority}.
 *
 * @author Vladislav Bauer
 */
public class NotificationPriorityTest extends AbstractModelTest {

    @Test
    public void testCodes() {
        checkEnum(NotificationPriority.class, 3);

        assertThatCode(NotificationPriority.HIGH, 1);
        assertThatCode(NotificationPriority.NORMAL, 2);
        assertThatCode(NotificationPriority.LOW, 3);
    }

    @Test
    public void testGetByCode() {
        for (NotificationPriority expected : NotificationPriority.values()) {
            int id = expected.getId();
            NotificationPriority actual = HasId.getById(NotificationPriority.class, id);
            assertThat(actual, equalTo(expected));
        }
    }

}
