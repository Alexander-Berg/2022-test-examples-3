package ru.yandex.market.pers.notify.mail;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.pers.notify.test.MarketUtilsMockedDbTest;

import java.util.List;

public class MailManagerTest extends MarketUtilsMockedDbTest {
    @Autowired
    MailManager mailManager;

    @Test
    public void testGetAvailableEmails() throws Exception {
        long uid = 161658075L;
        List<String> emails = mailManager.getAvailableEmails(uid);
        System.out.println(emails);
    }
}
