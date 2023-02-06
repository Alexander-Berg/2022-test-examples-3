package ru.yandex.market.notification.safe.task.job.sub;

import java.util.Collections;

import com.google.common.util.concurrent.MoreExecutors;
import org.junit.jupiter.api.Test;

import ru.yandex.market.notification.safe.model.vo.SendResultInfo;

/**
 * Unit-тесты для {@link SendPersistentNotificationToAddressesTask}.
 *
 * @author Vladislav Bauer
 */
public class SendPersistentNotificationToAddressesTaskTest extends AbstractSendTaskTest {

    @Test
    public void testTaskNegative() {
        final SendPersistentNotificationToAddressesTask task = new SendPersistentNotificationToAddressesTask(
            createRegistry(), createFacade(), createNotification(), Collections.singleton(createAddress()),
            MoreExecutors.newDirectExecutorService()
        );

        final SendResultInfo resultInfo = task.call();
        checkErrorResultInfo(resultInfo);
    }

}
