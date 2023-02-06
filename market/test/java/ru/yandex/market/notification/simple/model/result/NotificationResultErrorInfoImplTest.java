package ru.yandex.market.notification.simple.model.result;

import java.util.UUID;

import org.junit.Test;

import ru.yandex.market.notification.model.transport.result.NotificationResultErrorInfo;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

/**
 * Unit-тесты для {@link NotificationResultErrorInfoImpl}.
 *
 * @author Vladislav Bauer
 */
public class NotificationResultErrorInfoImplTest {

    @Test
    public void testConstruction() {
        final String message = UUID.randomUUID().toString();
        final NotificationResultErrorInfo info = new NotificationResultErrorInfoImpl(message);

        assertThat(info.getMessage(), equalTo(message));
        assertThat(info.toString(), not(isEmptyOrNullString()));
    }

}
