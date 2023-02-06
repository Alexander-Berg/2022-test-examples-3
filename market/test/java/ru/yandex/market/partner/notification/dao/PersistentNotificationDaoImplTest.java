package ru.yandex.market.partner.notification.dao;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dbunit.database.DatabaseConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.test.db.DbUnitDataBaseConfig;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.notification.safe.model.PersistentNotification;
import ru.yandex.market.notification.safe.model.data.PersistentBinaryData;
import ru.yandex.market.notification.safe.model.data.PersistentDeliveryData;
import ru.yandex.market.notification.safe.model.type.NotificationStatus;
import ru.yandex.market.notification.simple.model.type.NotificationPriority;
import ru.yandex.market.notification.simple.model.type.NotificationTransport;
import ru.yandex.market.partner.notification.AbstractFunctionalTest;
import ru.yandex.market.partner.notification.dao.type.PersistentNotificationType;


@DbUnitDataBaseConfig(
        @DbUnitDataBaseConfig.Entry(
                name = DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS,
                value = "true"
        ))
class PersistentNotificationDaoImplTest extends AbstractFunctionalTest {

    private static final PersistentNotificationType NOTIFICATION_TYPE = new PersistentNotificationType(
            2, "test_type", NotificationPriority.NORMAL, 1L, Set.of());

    private static final PersistentBinaryData BINARY_CONTENT =
            new PersistentBinaryData(
                    "<content><text>Тестовое сообщение для магазина The Shop" +
                    ".</text><parse-mode>Markdown</parse-mode></content>",
                    "type"
            );

    private static final PersistentDeliveryData PERSISTENT_DELIVERY_DATA = new PersistentDeliveryData(
            NotificationTransport.EMAIL,
            NotificationPriority.HIGH,
            Instant.now(),
            null,
            0
    );

    @Autowired
    PersistentNotificationDaoImpl persistentNotificationDao;

    @Autowired
    TransactionTemplate transactionTemplate;

    @Test
    @DbUnitDataSet(
            before = "PersistentNotificationDaoImpl/shouldCreateNewMessage.before.csv",
            after = "PersistentNotificationDaoImpl/shouldCreateNewMessage.after.csv"
    )
    void shouldCreateNewMessage() {
        persistentNotificationDao.create(
                List.of(
                        new PersistentNotification(
                                null, 1L, NOTIFICATION_TYPE, NotificationStatus.NEW,
                                Instant.now(), BINARY_CONTENT, PERSISTENT_DELIVERY_DATA, "", false
                        )
                )
        );
    }

    @Test
    @DbUnitDataSet(
            before = "PersistentNotificationDaoImpl/shouldMarkAsError.before.csv",
            after = "PersistentNotificationDaoImpl/shouldMarkAsError.after.csv"
    )
    void shouldMarkAsError() {
        persistentNotificationDao.markAsError(Map.of(100L, Instant.now()), NotificationStatus.SENDING_ERROR);
    }

    @Test
    @DbUnitDataSet(
            before = "PersistentNotificationDaoImpl/shouldMarkAsSent.before.csv",
            after = "PersistentNotificationDaoImpl/shouldMarkAsSent.after.csv"
    )
    void shouldMarkAsSent() {
        persistentNotificationDao.markAsSent(List.of(100L));
    }

    @Test
    void findForPreparing() {
        transactionTemplate.execute(status -> persistentNotificationDao.findForPreparing(3, 6));
    }

    @Test
    void findPrepared() {
        transactionTemplate.execute(status -> persistentNotificationDao.findPrepared(3, 6));
    }
}
