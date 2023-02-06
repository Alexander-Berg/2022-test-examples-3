package ru.yandex.market.notification.safe.dao.setter;

import java.sql.PreparedStatement;
import java.util.Collections;

import org.junit.Test;

import ru.yandex.market.notification.safe.dao.setter.PersistentNotificationDestinationBatchSetter.Columns;
import ru.yandex.market.notification.safe.model.PersistentNotificationDestination;
import ru.yandex.market.notification.safe.model.data.PersistentBinaryData;
import ru.yandex.market.notification.test.util.ClassUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Unit-тесты для {@link PersistentNotificationDestinationBatchSetter}.
 *
 * @author Vladislav Bauer
 */
public class PersistentNotificationDestinationBatchSetterTest extends AbstractBatchSetterTest {

    @Test
    public void testColumns() {
        ClassUtils.checkConstructor(Columns.class);
    }

    @Test
    public void testSetData() throws Exception {
        final PersistentNotificationDestinationBatchSetter setter =
            new PersistentNotificationDestinationBatchSetter(Collections.emptySet());

        final PreparedStatement statement = mock(PreparedStatement.class);
        final PersistentBinaryData binaryData = createBinaryData();
        final PersistentNotificationDestination destination = new PersistentNotificationDestination(1L, 2L, binaryData);

        setter.setData(statement, destination);

        verify(statement, times(1)).setLong(anyInt(), any(Long.class));
        verify(statement, times(2)).setString(anyInt(), any(String.class));
        verifyNoMoreInteractions(statement);
    }

}
