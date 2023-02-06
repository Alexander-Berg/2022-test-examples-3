package ru.yandex.market.notification.safe.task.job.sub.factory;

import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import org.junit.jupiter.api.Test;

import ru.yandex.market.notification.safe.model.PersistentNotification;
import ru.yandex.market.notification.safe.model.PersistentNotificationAddress;
import ru.yandex.market.notification.safe.model.data.PersistentDeliveryData;
import ru.yandex.market.notification.safe.model.vo.SendResultInfo;
import ru.yandex.market.notification.safe.service.facade.PersistentNotificationFacade;
import ru.yandex.market.notification.safe.task.job.sub.SendPersistentNotificationToAddressTask;
import ru.yandex.market.notification.safe.task.job.sub.SendPersistentNotificationToAddressesTask;
import ru.yandex.market.notification.safe.task.job.sub.SendPersistentNotificationWithoutAddressTask;
import ru.yandex.market.notification.service.registry.NotificationFacadeRegistry;
import ru.yandex.market.notification.simple.model.type.NotificationTransport;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.notification.simple.model.type.NotificationTransport.EMAIL;
import static ru.yandex.market.notification.simple.model.type.NotificationTransport.MBI_WEB_UI;

/**
 * Unit-тесты для {@link SendNotificationTaskFactoryImpl}.
 *
 * @author Vladislav Bauer
 */
public class SendNotificationTaskFactoryTest {

    @Test
    public void testCreateSendTaskWithoutAddresses() throws Exception {
        final SendNotificationTaskFactory taskFactory = createTaskFactory();

        assertThat(taskFactory.createSendTask(createNotification(MBI_WEB_UI), Collections.emptySet()), instanceOf(SendPersistentNotificationWithoutAddressTask.class));

        final Callable<SendResultInfo> task =
            taskFactory.createSendTask(createNotification(EMAIL), Collections.emptySet());

        assertThat(task.call(), notNullValue());
    }

    @Test
    public void testCreateSendTaskToAddress() throws Exception {
        final SendNotificationTaskFactory taskFactory = createTaskFactory();

        assertThat(taskFactory.createSendTask(createNotification(MBI_WEB_UI), Collections.singleton(createAddress())), instanceOf(SendPersistentNotificationToAddressTask.class));
    }

    @Test
    public void testCreateSendTaskToAddresses() throws Exception {
        final SendNotificationTaskFactory taskFactory = createTaskFactory();

        assertThat(taskFactory.createSendTask(createNotification(MBI_WEB_UI), asList(createAddress(), createAddress())), instanceOf(SendPersistentNotificationToAddressesTask.class));
    }


    private SendNotificationTaskFactoryImpl createTaskFactory() {
        return new SendNotificationTaskFactoryImpl(
            mock(NotificationFacadeRegistry.class),
            mock(PersistentNotificationFacade.class),
            mock(ExecutorService.class)
        );
    }

    private PersistentNotification createNotification(final NotificationTransport transport) {
        final PersistentNotification notification = mock(PersistentNotification.class);
        final PersistentDeliveryData deliveryData = mock(PersistentDeliveryData.class);

        when(deliveryData.getTransportType()).thenReturn(transport);
        when(notification.getDeliveryData()).thenReturn(deliveryData);

        return notification;
    }

    private PersistentNotificationAddress createAddress() {
        return mock(PersistentNotificationAddress.class);
    }

}
