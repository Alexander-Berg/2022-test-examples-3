package ru.yandex.market.notification.safe.dao.setter;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;

import org.junit.Test;

import ru.yandex.market.notification.safe.dao.setter.PersistentNotificationBatchSetter.Columns;
import ru.yandex.market.notification.safe.model.PersistentNotification;
import ru.yandex.market.notification.safe.model.data.PersistentBinaryData;
import ru.yandex.market.notification.safe.model.data.PersistentDeliveryData;
import ru.yandex.market.notification.simple.model.type.CodeNotificationType;
import ru.yandex.market.notification.test.util.ClassUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static ru.yandex.market.notification.safe.model.type.NotificationStatus.SENDING_ERROR;
import static ru.yandex.market.notification.simple.model.type.NotificationPriority.HIGH;
import static ru.yandex.market.notification.simple.model.type.NotificationTransport.EMAIL;

/**
 * Unit-тесты для {@link PersistentNotificationBatchSetter}.
 *
 * @author Vladislav Bauer
 */
public class PersistentNotificationBatchSetterTest extends AbstractBatchSetterTest {

    @Test
    public void testColumns() {
        ClassUtils.checkConstructor(Columns.class);
    }

    @Test
    public void testSetData() throws Exception {
        final PersistentNotificationBatchSetter setter = new PersistentNotificationBatchSetter(Collections.emptySet());
        final PreparedStatement statement = mock(PreparedStatement.class);

        final Instant now = Instant.now();
        final PersistentDeliveryData deliveryData = new PersistentDeliveryData(EMAIL, HIGH, now, now, 2);
        final PersistentBinaryData binaryData = createBinaryData();
        final CodeNotificationType type = new CodeNotificationType(3L);
        final PersistentNotification address = new PersistentNotification(
            1L, 2L, type, SENDING_ERROR, now, binaryData, deliveryData, "SPAM HASH"
        );

        setter.setData(statement, address);

        verify(statement, times(2)).setLong(anyInt(), any(Long.class));
        verify(statement, times(3)).setString(anyInt(), any(String.class));
        verify(statement, times(2)).setTimestamp(anyInt(), any(Timestamp.class));
        verify(statement, times(3)).setInt(anyInt(), any(Integer.class));
        verifyNoMoreInteractions(statement);
    }

}
