package ru.yandex.market.notification.safe.task.job.sub;

import org.junit.Test;

import ru.yandex.market.notification.safe.model.vo.SendResultInfo;

/**
 * Unit-тесты для {@link SendPersistentNotificationToAddressTask}.
 *
 * @author Vladislav Bauer
 */
public class SendPersistentNotificationToAddressTaskTest extends AbstractSendTaskTest {

    @Test
    public void testTaskNegative() {
        final SendPersistentNotificationToAddressTask task = new SendPersistentNotificationToAddressTask(
            createRegistry(), createFacade(), createNotification(), createAddress()
        );

        final SendResultInfo resultInfo = task.call();
        checkErrorResultInfo(resultInfo);
    }

}
