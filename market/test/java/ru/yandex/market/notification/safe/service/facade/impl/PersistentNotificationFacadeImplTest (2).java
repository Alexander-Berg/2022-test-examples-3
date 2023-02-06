package ru.yandex.market.notification.safe.service.facade.impl;

import org.junit.jupiter.api.Test;

import ru.yandex.market.notification.safe.composer.NotificationComposer;
import ru.yandex.market.notification.safe.composer.PersistentNotificationComposer;
import ru.yandex.market.notification.safe.service.facade.PersistentDaoFacade;
import ru.yandex.market.notification.safe.service.facade.PersistentNotificationFacade;
import ru.yandex.market.notification.safe.service.provider.SpamFilterProvider;
import ru.yandex.market.notification.safe.service.provider.SpamHashProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;

/**
 * Unit-тесты для {@link PersistentNotificationFacadeImpl}.
 *
 * @author Vladislav Bauer
 */
public class PersistentNotificationFacadeImplTest {

    @Test
    public void testConstruction() {
        final PersistentDaoFacade daoFacade = mock(PersistentDaoFacade.class);
        final NotificationComposer notificationComposer = mock(NotificationComposer.class);
        final PersistentNotificationComposer persistentNotificationComposer =
                mock(PersistentNotificationComposer.class);
        final SpamHashProvider spamHashProvider = mock(SpamHashProvider.class);
        final SpamFilterProvider spamFilterProvider = mock(SpamFilterProvider.class);

        final PersistentNotificationFacade facade = new PersistentNotificationFacadeImpl(
                daoFacade, notificationComposer, persistentNotificationComposer,
                spamHashProvider, spamFilterProvider
        );

        assertThat(facade.getPersistentDaoFacade(), equalTo(daoFacade));
        assertThat(facade.getNotificationComposer(), equalTo(notificationComposer));
        assertThat(facade.getPersistentNotificationComposer(), equalTo(persistentNotificationComposer));
        assertThat(facade.getSpamHashProvider(), equalTo(spamHashProvider));
        assertThat(facade.getSpamFilterProvider(), equalTo(spamFilterProvider));
    }

}
