package ru.yandex.direct.core.entity.logmail.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.testing.configuration.CoreTest;

@CoreTest
@RunWith(SpringRunner.class)
public class LogMailServiceTest {

    @Autowired
    private LogMailService logMailService;

    @Test
    public void addLogMail_OneLogMail_WithoutErrors() {
        logMailService.addDealNotificationLogMail("address@yandex.ru", "New campaign", "New campaign created");
    }
}
