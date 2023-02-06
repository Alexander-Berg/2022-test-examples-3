package ru.yandex.market.notification.model.transport.result;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import ru.yandex.market.notification.model.transport.NotificationAddress;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.notification.model.transport.result.NotificationResult.getFailedMessages;

/**
 * Unit-тесты для {@link NotificationResult}.
 *
 * @author Vladislav Bauer
 */
public class NotificationResultTest {

    @Test
    public void testGetFailedMessages() {
        assertThat(getFailedMessages(failed(emptyMap())), empty());

        final String err1 = "err1";
        final Map<NotificationAddress, NotificationResultErrorInfo> failed1 = singletonMap(address(), error(err1));
        assertThat(getFailedMessages(failed(failed1)), containsInAnyOrder(singleton(err1).toArray()));

        final String err2 = "err2";
        final Map<NotificationAddress, NotificationResultErrorInfo> failed2 = new HashMap<>();
        failed2.put(address(), error(err1));
        failed2.put(address(), error(err2));
        assertThat(getFailedMessages(failed(failed2)), containsInAnyOrder(asList(err1, err2).toArray()));
    }


    private NotificationAddress address() {
        return mock(NotificationAddress.class);
    }

    private NotificationResultErrorInfo error(final String message) {
        final NotificationResultErrorInfo errorInfo = mock(NotificationResultErrorInfo.class);
        when(errorInfo.getMessage()).thenReturn(message);
        return errorInfo;
    }

    private NotificationResult failed(final Map<NotificationAddress, NotificationResultErrorInfo> failed) {
        final NotificationResult result = mock(NotificationResult.class);
        when(result.getFailed()).thenReturn(failed);
        return result;
    }

}
