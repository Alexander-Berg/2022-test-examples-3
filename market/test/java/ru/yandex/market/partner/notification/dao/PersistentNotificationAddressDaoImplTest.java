package ru.yandex.market.partner.notification.dao;

import java.util.List;

import org.dbunit.database.DatabaseConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataBaseConfig;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.notification.safe.model.PersistentNotificationAddress;
import ru.yandex.market.notification.safe.model.data.PersistentBinaryData;
import ru.yandex.market.notification.safe.model.type.NotificationAddressStatus;
import ru.yandex.market.partner.notification.AbstractFunctionalTest;


@DbUnitDataBaseConfig(
        @DbUnitDataBaseConfig.Entry(
                name = DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS,
                value = "true"
        ))
class PersistentNotificationAddressDaoImplTest extends AbstractFunctionalTest {

    private static final PersistentBinaryData ADDRESS_DATA =
            new PersistentBinaryData(
                    "<address botId=\"MarketPartnerBot\" telegramId=\"1234567\"/>",
                    "TELEGRAM_BOT"
            );
    @Autowired
    PersistentNotificationAddressDaoImpl persistentNotificationAddressDao;

    @Test
    @DbUnitDataSet(
            before = "PersistentNotificationAddressDaoImpl/shouldCreateAddress.before.csv",
            after = "PersistentNotificationAddressDaoImpl/shouldCreateAddress.after.csv"
    )
    public void shouldCreateAddress() {
        persistentNotificationAddressDao.create(
                List.of(
                        new PersistentNotificationAddress(
                                null,
                                100L,
                                1L,
                                ADDRESS_DATA,
                                NotificationAddressStatus.NEW
                        )
                )
        );
    }

    @Test
    @DbUnitDataSet(
            before = "PersistentNotificationAddressDaoImpl/shouldUpdateStatus.before.csv",
            after = "PersistentNotificationAddressDaoImpl/shouldUpdateStatus.after.csv"
    )
    public void shouldUpdateStatus() {
        persistentNotificationAddressDao.updateStatus(100L, NotificationAddressStatus.SENT);
    }

    @Test
    @DbUnitDataSet(
            before = "PersistentNotificationAddressDaoImpl/shouldFindUnprocessed.before.csv"
    )
    public void shouldFindUnprocessed() {
        var result = persistentNotificationAddressDao.findUnprocessed(List.of(100L, 101L, 102L));
        Assertions.assertEquals(2, result.size());
    }

}
