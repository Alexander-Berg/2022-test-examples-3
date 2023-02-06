package ru.yandex.market.notification.model.type;

import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Test;

import ru.yandex.market.notification.simple.model.type.CodeNotificationType;
import ru.yandex.market.notification.test.model.AbstractModelTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;

/**
 * Unit-тесты для {@link CodeNotificationType}.
 *
 * @author Vladislav Bauer
 */
public class CodeNotificationTypeTest extends AbstractModelTest {
    @Test
    public void testConstruction() {
        long id = generateId();
        CodeNotificationType type = new CodeNotificationType(id);

        assertThat(type.getId(), equalTo(id));
        assertThat(type.toString(), not(isEmptyOrNullString()));
    }

    @Test
    public void testBasicMethods() {
        long id = generateId();
        CodeNotificationType type = new CodeNotificationType(id);
        CodeNotificationType sameType = new CodeNotificationType(id);
        CodeNotificationType otherType = new CodeNotificationType(id + 1);

        checkBasicMethods(type, sameType, otherType);
    }

    private static long generateId() {
        return ThreadLocalRandom.current().nextLong();
    }
}
