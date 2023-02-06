package ru.yandex.market.notification.safe.service.facade.impl;

import org.junit.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.notification.safe.dao.PersistentNotificationAddressDao;
import ru.yandex.market.notification.safe.dao.PersistentNotificationDao;
import ru.yandex.market.notification.safe.dao.PersistentNotificationDestinationDao;
import ru.yandex.market.notification.safe.dao.PersistentNotificationGroupDao;
import ru.yandex.market.notification.safe.service.facade.PersistentDaoFacade;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit-тесты для {@link PersistentDaoFacadeImpl}.
 *
 * @author Vladislav Bauer
 */
public class PersistentDaoFacadeImplTest {

    @Test
    public void testConstruction() {
        final TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        final PersistentNotificationGroupDao groupDao = mock(PersistentNotificationGroupDao.class);
        final PersistentNotificationDao notificationDao = mock(PersistentNotificationDao.class);
        final PersistentNotificationDestinationDao destinationDao = mock(PersistentNotificationDestinationDao.class);
        final PersistentNotificationAddressDao addressDao = mock(PersistentNotificationAddressDao.class);

        final PersistentDaoFacade facade = new PersistentDaoFacadeImpl(
            transactionTemplate, groupDao, notificationDao, destinationDao, addressDao
        );

        assertThat(facade.getTransactionTemplate(), equalTo(transactionTemplate));
        assertThat(facade.getNotificationGroupDao(), equalTo(groupDao));
        assertThat(facade.getNotificationDao(), equalTo(notificationDao));
        assertThat(facade.getDestinationDao(), equalTo(destinationDao));
        assertThat(facade.getAddressDao(), equalTo(addressDao));
    }

}
