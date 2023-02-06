package ru.yandex.market.notification.model.result;

import org.junit.Test;

import ru.yandex.market.notification.model.transport.result.NotificationResultInfo;
import ru.yandex.market.notification.simple.model.result.NotificationResultInfoImpl;

import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

/**
 * Unit-тесты для {@link NotificationResultInfoImpl}.
 *
 * @author Vladislav Bauer
 */
public class NotificationResultInfoImplTest {

    @Test
    public void testConstruction() {
        final NotificationResultInfo info = new NotificationResultInfoImpl();

        assertThat(info.toString(), not(isEmptyOrNullString()));
    }

}
