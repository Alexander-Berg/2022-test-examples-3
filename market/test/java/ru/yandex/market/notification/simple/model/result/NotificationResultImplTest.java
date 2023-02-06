package ru.yandex.market.notification.simple.model.result;

import java.util.Map;

import org.junit.Test;

import ru.yandex.market.notification.model.transport.NotificationAddress;
import ru.yandex.market.notification.model.transport.result.NotificationResult;
import ru.yandex.market.notification.model.transport.result.NotificationResultErrorInfo;
import ru.yandex.market.notification.model.transport.result.NotificationResultInfo;

import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

/**
 * Unit-тесты для {@link NotificationResultImpl}.
 *
 * @author Vladislav Bauer
 */
public class NotificationResultImplTest {

    @Test
    public void testConstructionEmpty() {
        final NotificationResult result = new NotificationResultImpl(null, null);

        assertThat(result.getFailed().isEmpty(), equalTo(true));
        assertThat(result.getSuccessful().isEmpty(), equalTo(true));
        assertThat(result.toString(), not(isEmptyOrNullString()));
    }

    @Test
    public void testConstruction() {
        final Map<NotificationAddress, NotificationResultInfo> successful =
            singletonMap(mock(NotificationAddress.class), mock(NotificationResultInfo.class));

        final Map<NotificationAddress, NotificationResultErrorInfo> failed =
            singletonMap(mock(NotificationAddress.class), mock(NotificationResultErrorInfo.class));

        final NotificationResult result = new NotificationResultImpl(successful, failed);

        assertThat(result.getFailed(), equalTo(failed));
        assertThat(result.getSuccessful(), equalTo(successful));
        assertThat(result.toString(), not(isEmptyOrNullString()));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testImmutabilitySuccess() {
        final NotificationResult result = new NotificationResultImpl(null, null);
        result.getSuccessful().put(mock(NotificationAddress.class), mock(NotificationResultInfo.class));

        fail("Successful map should not be mutable");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testImmutabilityFailed() {
        final NotificationResult result = new NotificationResultImpl(null, null);
        result.getFailed().put(mock(NotificationAddress.class), mock(NotificationResultErrorInfo.class));

        fail("Failed map should not be mutable");
    }

}
