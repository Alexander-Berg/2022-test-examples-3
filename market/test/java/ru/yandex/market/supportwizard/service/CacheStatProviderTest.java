package ru.yandex.market.supportwizard.service;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import ru.yandex.market.supportwizard.config.BaseFunctionalTest;
public class CacheStatProviderTest extends BaseFunctionalTest {

    @Test
    void parsingTest() {
        String lineToParse = "key=LS_MSG_SRV_getLastMessagesForShop_11152210 exp=1635139571 la=1635135972" +
                " cas=4280074605 fetch=no cls=6 size=273";
        CacheInformationUnit unit = CacheStatProvider.parseString(lineToParse);
        Assert.assertEquals(unit.getKey() + ":" + unit.getSize(),
                "LS_MSG_SRV_getLastMessagesForShop_11152210:273");
    }
}
