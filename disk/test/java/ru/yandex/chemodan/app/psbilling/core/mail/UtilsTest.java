package ru.yandex.chemodan.app.psbilling.core.mail;

import org.junit.Assert;
import org.junit.Test;


public class UtilsTest {

    @Test
    public void testOneEmailParsing() {
        String email = "test@yandex.ru";
        Assert.assertEquals(email, Utils.parseFirstEmailFromBalance(email));
    }

    @Test
    public void testManyEmailsParsing() {
        String email = "test@yandex.ru";
        Assert.assertEquals(email, Utils.parseFirstEmailFromBalance(email + ";test1@yandex.ru;test2@yandex.ru"));
    }
}
