package ru.yandex.market.partner.notification.service.providers;

import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.partner.notification.AbstractFunctionalTest;


public class TemplateDataProviderTest extends AbstractFunctionalTest {

    long id = 1651654260;

    @Autowired
    TemplateDataProvider templateDataProvider;

    @Test
    public void getDataWithDescriptions() {
        var pair = templateDataProvider.getDataWithDescriptions(id).get(0);
        var expectedData = new TreeMap(Map.of(
                "donorShopId","111111",
                "donorWarehouseName", "Исходный DBS",
                "newCampaignId", "222222222",
                "newShopId", "222222",
                "newWarehouseName", "Реплицированный FBS"
        ));

        var expectedDescription = "Тестоыве данные для шаблона";
        Assertions.assertEquals(new TreeMap<>(expectedData), new TreeMap<>(pair.first));
        Assertions.assertEquals(expectedDescription, pair.second);
    }
}
