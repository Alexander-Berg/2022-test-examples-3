package ru.yandex.market.partner.notification.dao;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.partner.notification.AbstractFunctionalTest;


class NotificationTemplateDaoTest extends AbstractFunctionalTest {

    @Autowired
    NotificationTemplateDao notificationTemplateDao;


    @Test
    @DbUnitDataSet(before = "NotificationTemplateDao/isTemplateExists.before.csv")
    void testIsTemplateExists() {
        Assertions.assertTrue(notificationTemplateDao.isTemplateExists(101L));
        Assertions.assertFalse(notificationTemplateDao.isTemplateExists(404L));
    }
}
