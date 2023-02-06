package ru.yandex.market.api.util;

import org.junit.Assert;
import org.junit.Test;

import java.security.NoSuchAlgorithmException;

import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.util.codecs.MessageProtectionService;

public class MessageProtectionServiceTest extends UnitTestBase {

    private MessageProtectionService service;

    {
        service = new MessageProtectionService("dF6+0R8tk/43py6hMhoJkA==".getBytes(ApiStrings.UTF8));
    }

    @Test
    public void should() {
        String[] values = new String[]{"", "1", "12", "12345678", "qazxsw", "1234567890"};
        for (String value : values) {
            Assert.assertEquals(value, new String(service.decrypt(service.encode(value)), ApiStrings.UTF8));
        }
    }

}
