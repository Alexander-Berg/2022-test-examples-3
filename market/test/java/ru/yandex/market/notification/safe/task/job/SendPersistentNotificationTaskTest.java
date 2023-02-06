package ru.yandex.market.notification.safe.task.job;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutorService;

import javax.annotation.Nonnull;

import com.google.common.util.concurrent.MoreExecutors;
import org.junit.Test;

import ru.yandex.market.notification.safe.dao.PersistentNotificationDao;
import ru.yandex.market.notification.safe.model.PersistentNotification;
import ru.yandex.market.notification.safe.model.PersistentNotificationAddress;
import ru.yandex.market.notification.safe.model.type.NotificationStatus;
import ru.yandex.market.notification.safe.model.vo.SendResultInfo;
import ru.yandex.market.notification.safe.service.facade.PersistentDaoFacade;
import ru.yandex.market.notification.safe.service.facade.PersistentNotificationFacade;
import ru.yandex.market.notification.safe.task.job.misc.QuadraticRetryTimeCalculator;
import ru.yandex.market.notification.safe.task.job.sub.factory.SendNotificationTaskFactory;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты для {@link SendPersistentNotificationTask}.
 *
 * @author Vladislav Bauer
 */
public class SendPersistentNotificationTaskTest extends AbstractNotificationTaskTest {

    private static final Collection<PersistentNotification> PREPARED_NNS = new ArrayList<>();

    @Test
    public void testContract() {
        final PersistentNotificationFacade facade = createFacade(createPersistentDaoFacade());
        final SendNotificationTaskFactory taskFactory = createTaskFactory(null);
        final ExecutorService executorService = mock(ExecutorService.class);

        final CustomSendPersistentNotificationTask task =
            new CustomSendPersistentNotificationTask(executorService, facade, taskFactory, 0, 0);

        assertThat(task.findPrepared(), equalTo(PREPARED_NNS));
        assertThat(task.send(mock(PersistentNotification.class), Collections.emptyList()), notNullValue());
    }

    @Test
    public void testNoNotification() throws Exception {
        final SendNotificationTaskFactory taskFactory = createTaskFactory(null);
        final PersistentDaoFacade daoFacade = createPersistentDaoFacade();
        final PersistentNotificationFacade facade = createFacade(daoFacade);
        final PersistentNotificationDao notificationDao = daoFacade.getNotificationDao();

        when(notificationDao.findPrepared(anyInt(), anyInt())).thenReturn(Collections.emptyList());

        executeTask(facade, taskFactory);

        verify(notificationDao, times(1)).findPrepared(anyInt(), anyInt());
        verify(notificationDao, never()).updateStatus(anyInt(), any());
    }

    @Test
    public void testSendingError() throws Exception {
        final SendResultInfo sendResultInfo = new SendResultInfo(Collections.emptySet(), Collections.singleton("err"));
        final SendNotificationTaskFactory taskFactory = createTaskFactory(sendResultInfo);
        final PersistentDaoFacade daoFacade = createPersistentDaoFacade();
        final PersistentNotificationFacade facade = createFacade(daoFacade);
        final PersistentNotificationDao notificationDao = daoFacade.getNotificationDao();

        final PersistentNotification notification = createNotification(NotificationStatus.PREPARED);

        when(notificationDao.findPrepared(anyInt(), anyInt())).thenReturn(Collections.singletonList(notification));

        executeTask(facade, taskFactory);

        verify(notificationDao, times(1)).findPrepared(anyInt(), anyInt());
        verify(notificationDao, times(1)).markAsError(anyMap(), eq(NotificationStatus.SENDING_ERROR));
        verify(notificationDao, never()).markAsSent(anyCollection());
    }

    @Test
    public void testSent() throws Exception {
        final SendResultInfo sendResultInfo = new SendResultInfo(Collections.emptySet(), Collections.emptySet());
        final SendNotificationTaskFactory taskFactory = createTaskFactory(sendResultInfo);
        final PersistentDaoFacade daoFacade = createPersistentDaoFacade();
        final PersistentNotificationFacade facade = createFacade(daoFacade);
        final PersistentNotificationDao notificationDao = daoFacade.getNotificationDao();

        final PersistentNotification notification = createNotification(NotificationStatus.PREPARED);
        final Long notificationId = notification.getId();

        when(notificationDao.findPrepared(anyInt(), anyInt())).thenReturn(Collections.singletonList(notification));

        executeTask(facade, taskFactory);

        verify(notificationDao, times(1)).findPrepared(anyInt(), anyInt());
        verify(notificationDao, times(1)).markAsSent(Collections.singletonList(notificationId));
        verify(notificationDao, never()).markAsError(anyMap(), eq(NotificationStatus.SENDING_ERROR));
    }


    private void executeTask(
        final PersistentNotificationFacade facade, final SendNotificationTaskFactory taskFactory
    ) throws Exception {
        final ExecutorService executorService = MoreExecutors.newDirectExecutorService();
        final SendPersistentNotificationTask task = new SendPersistentNotificationTask(
                executorService, facade, taskFactory, 0, new QuadraticRetryTimeCalculator(), 0);

        task.execute();
    }

    private SendNotificationTaskFactory createTaskFactory(final SendResultInfo sendResultInfo) {
        final SendNotificationTaskFactory taskFactory = mock(SendNotificationTaskFactory.class);

        when(taskFactory.createSendTask(any(), anyCollection()))
            .thenReturn(() -> sendResultInfo);

        return taskFactory;
    }


    /**
     * Класс, проверяющий контракт {@link SendPersistentNotificationTask}.
     *
     * @author Vladislav Bauer
     */
    private static class CustomSendPersistentNotificationTask extends SendPersistentNotificationTask {

        CustomSendPersistentNotificationTask(
            @Nonnull final ExecutorService executorService,
            @Nonnull final PersistentNotificationFacade facade,
            @Nonnull final SendNotificationTaskFactory taskFactory,
            final int maxRetryNum,
            final long delayTimeout
        ) {
            super(executorService, facade, taskFactory, maxRetryNum, new QuadraticRetryTimeCalculator(), delayTimeout);
        }

        /**
         * {@inheritDoc}
         */
        @Nonnull
        @Override
        protected Collection<PersistentNotification> findPrepared() {
            return PREPARED_NNS;
        }

        /**
         * {@inheritDoc}
         */
        @Nonnull
        @Override
        protected SendResultInfo send(@Nonnull final PersistentNotification notification,
                                      @Nonnull final Collection<PersistentNotificationAddress> addresses) {
            return SendResultInfo.empty();
        }

    }

}
