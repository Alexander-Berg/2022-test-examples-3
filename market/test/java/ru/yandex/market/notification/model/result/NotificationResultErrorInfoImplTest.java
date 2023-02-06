package ru.yandex.market.notification.model.result;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import ru.yandex.market.notification.model.transport.result.NotificationResultErrorInfo;
import ru.yandex.market.notification.simple.model.result.NotificationResultErrorInfoImpl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;

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
