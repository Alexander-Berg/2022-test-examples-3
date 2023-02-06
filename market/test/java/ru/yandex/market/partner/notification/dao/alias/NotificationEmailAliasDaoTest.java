package ru.yandex.market.partner.notification.dao.alias;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.notification.mail.model.address.EmailAddress;
import ru.yandex.market.partner.notification.AbstractFunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationEmailAliasDaoTest extends AbstractFunctionalTest {

    @Autowired
    NotificationEmailAliasDao notificationEmailAliasDao;

    @Test
    @DbUnitDataSet(before = "NotificationEmailAliasDaoTest/shouldSelectAliasByType.before.csv")
    public void shouldSelectAliasByType() {
        var actual = notificationEmailAliasDao.getByNotificationType(100L);
        assertEquals(
                List.of(
                        new NotificationEmailAlias(100L, "ShopAdmins", EmailAddress.Type.TO),
                        new NotificationEmailAlias(100L, "YaManagerOnly", EmailAddress.Type.CC)
                ),
                actual
        );
    }
}
