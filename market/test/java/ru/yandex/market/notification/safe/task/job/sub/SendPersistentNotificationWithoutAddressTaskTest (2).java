package ru.yandex.market.notification.safe.task.job.sub;

import org.junit.jupiter.api.Test;

import ru.yandex.market.notification.safe.model.vo.SendResultInfo;

/**
 * Unit-тесты для {@link SendPersistentNotificationWithoutAddressTask}.
 *
 * @author Vladislav Bauer
 */
public class SendPersistentNotificationWithoutAddressTaskTest extends AbstractSendTaskTest {

    @Test
    public void testTask() {
        final SendPersistentNotificationWithoutAddressTask task =
            new SendPersistentNotificationWithoutAddressTask(createRegistry(), createFacade(), createNotification());

        final SendResultInfo resultInfo = task.call();
        checkErrorResultInfo(resultInfo);
    }

}
