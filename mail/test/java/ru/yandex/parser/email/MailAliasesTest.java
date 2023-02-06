package ru.yandex.parser.email;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.test.util.TestBase;

public class MailAliasesTest extends TestBase {
    public MailAliasesTest() {
        super(false, 0L);
    }

    @Test
    public void testNormalize() {
        Assert.assertEquals(
            "ını-ını@yandex.ru",
            MailAliases.INSTANCE.normalizeEmail("Inı-Inı@yandex.com.tr"));
        Assert.assertEquals(
            "ını-ını@yandex.ru",
            MailAliases.INSTANCE.normalizeEmail("Inı.Inı@yandex.com.tr"));
        Assert.assertEquals(
            "ını/δ/җ@yandex.ru",
            MailAliases.INSTANCE.normalizeEmail("Inı/Δ/Җ@yandex.com.tr"));
        Assert.assertEquals(
            "ını/ını@yandex.ru",
            MailAliases.INSTANCE.normalizeEmail("Inı/Inı@yandex.com.tr"));
        Assert.assertEquals(
            "ını/@yandex.ru",
            MailAliases.INSTANCE.normalizeEmail("Inı/@yandex.com.tr"));
        Assert.assertEquals(
            "ını///ını@yandex.ru",
            MailAliases.INSTANCE.normalizeEmail("Inı///Inı@yandex.com.tr"));
        Assert.assertEquals(
            "ını@ını.com.tr",
            MailAliases.INSTANCE.normalizeEmail("Inı@Inı.com.tr"));
    }

    @Test
    public void testNormalizeSurrogates() {
        Assert.assertEquals(
            "\ud801\udc37@yandex.ru",
            MailAliases.INSTANCE.normalizeEmail("\ud801\udc0f@yandex.com.tr"));
        Assert.assertEquals(
            "\ud801\udc37---\ud801\udc37@yandex.ru",
            MailAliases.INSTANCE.normalizeEmail(
                "\ud801\udc0f---\ud801\udc0f@yandex.com.tr"));
        Assert.assertEquals(
            "\ud801@yandex.ru",
            MailAliases.INSTANCE.normalizeEmail("\ud801@yandex.com.tr"));
        Assert.assertEquals(
            "\ud801a@yandex.ru",
            MailAliases.INSTANCE.normalizeEmail("\ud801A@yandex.ru"));
        Assert.assertEquals(
            "\udc0f\udc0f@yandex.ru",
            MailAliases.INSTANCE.normalizeEmail("\udc0f\udc0f@yandex.com.tr"));
    }
}

