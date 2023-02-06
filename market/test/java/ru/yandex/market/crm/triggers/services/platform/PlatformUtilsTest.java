package ru.yandex.market.crm.triggers.services.platform;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.crm.platform.FactHolder;
import ru.yandex.market.crm.platform.config.PlatformConfiguration;
import ru.yandex.market.crm.platform.models.JournalView;
import ru.yandex.market.crm.util.CrmStrings;

public class PlatformUtilsTest {

    private PlatformUtils utils;

    @Before
    public void setUp() {
        var configRepository = new PlatformConfiguration().configRepository();
        utils = new PlatformUtils(configRepository);
    }

    @Test
    public void checkConvert() {
        String message = "tskv\ttype=JournalView\tvalue" +
                "=ChUSEzgyMTE2MTg0MDE1MzU1Mjk0NzAQmJjBpdgsGjBrbm93bGVkZ2Uva2FrLXZ5YnJhdC1ha2t1bXVsamF0b3JueWotc2h1cnVwb3ZlcnQgAQ==";

        List<FactHolder> result = utils.convert(CrmStrings.getBytes(message));

        Assert.assertEquals("Должны получить один факт т.к. исходное сообщение содержит одну строку", 1, result.size());

        JournalView fact = result.get(0).getFact();
        Assert.assertEquals("knowledge/kak-vybrat-akkumuljatornyj-shurupovert", fact.getId());
    }

}
