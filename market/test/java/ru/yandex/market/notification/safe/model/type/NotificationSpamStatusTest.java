package ru.yandex.market.notification.safe.model.type;

import org.junit.Test;

import static org.hamcrest.Matchers.arrayWithSize;
import static org.junit.Assert.assertThat;

/**
 * Unit-тесты для {@link NotificationSpamStatus}.
 *
 * @author Vladislav Bauer
 */
public class NotificationSpamStatusTest {

    /**
     * NB: Если тест упал, значит произошли изменения в спамоловилке и необходима ее доработка.
     */
    @Test
    public void testEnums() {
        assertThat(NotificationSpamStatus.values(), arrayWithSize(3));
    }

}
