package ru.yandex.market.jmf.module.mail.test;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.jmf.module.mail.impl.MailFormatter;

@SpringJUnitConfig(InternalModuleMailTestConfiguration.class)
public class MailFormatterTest {

    @Inject
    MailFormatter mailFormatter;

    @Test
    public void simpleTest() {
        String ownerGid = "1234567890";
        String host = "localhost";
        String messageId = mailFormatter.messageId(ownerGid, host);
        String entityGid = mailFormatter.entityGid(messageId);
        Assertions.assertEquals(ownerGid, entityGid);
    }
}

