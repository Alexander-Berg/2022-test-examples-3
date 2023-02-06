package ru.yandex.market.notification.safe.dao.setter;

import java.sql.PreparedStatement;
import java.util.Collections;

import org.junit.Test;

import ru.yandex.market.notification.safe.dao.setter.PersistentNotificationAddressBatchSetter.Columns;
import ru.yandex.market.notification.safe.model.PersistentNotificationAddress;
import ru.yandex.market.notification.safe.model.data.PersistentBinaryData;
import ru.yandex.market.notification.test.util.ClassUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static ru.yandex.market.notification.safe.model.type.NotificationAddressStatus.NEW;

/**
 * Unit-тесты для {@link PersistentNotificationAddressBatchSetter}.
 *
 * @author Vladislav Bauer
 */
public class PersistentNotificationAddressBatchSetterTest extends AbstractBatchSetterTest {

    @Test
    public void testColumns() {
        ClassUtils.checkConstructor(Columns.class);
    }

    @Test
    public void testSetData() throws Exception {
        final PersistentNotificationAddressBatchSetter setter =
            new PersistentNotificationAddressBatchSetter(Collections.emptySet());

        final PreparedStatement statement = mock(PreparedStatement.class);
        final PersistentBinaryData binaryData = createBinaryData();
        final PersistentNotificationAddress address = new PersistentNotificationAddress(1L, 2L, 3L, binaryData, NEW);

        setter.setData(statement, address);

        verify(statement, times(2)).setLong(anyInt(), any(Long.class));
        verify(statement, times(2)).setString(anyInt(), any(String.class));
        verify(statement, times(1)).setInt(anyInt(), any(Integer.class));
        verifyNoMoreInteractions(statement);
    }

}
