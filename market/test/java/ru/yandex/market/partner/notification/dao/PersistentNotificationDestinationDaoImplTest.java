package ru.yandex.market.partner.notification.dao;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.notification.safe.model.PersistentNotificationDestination;
import ru.yandex.market.notification.safe.model.PersistentNotificationGroup;
import ru.yandex.market.notification.safe.model.data.PersistentBinaryData;
import ru.yandex.market.partner.notification.AbstractFunctionalTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class PersistentNotificationDestinationDaoImplTest extends AbstractFunctionalTest {
    @Autowired
    PersistentNotificationGroupDaoImpl persistentNotificationGroupDao;
    @Autowired
    PersistentNotificationDestinationDaoImpl persistentNotificationDestinationDao;

    @Test
    void create() {
        var group = persistentNotificationGroupDao.create(new PersistentNotificationGroup(1L, null));
        assertThat(persistentNotificationDestinationDao.create(List.of(
                new PersistentNotificationDestination(null, group.getId(), createBinaryData()),
                new PersistentNotificationDestination(null, group.getId(), createBinaryData())
        )), equalTo(2));
    }

    private static PersistentBinaryData createBinaryData() {
        return new PersistentBinaryData("<data/>", "TEST_TYPE");
    }
}
