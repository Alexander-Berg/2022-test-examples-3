package ru.yandex.market.wms.datacreator.dao;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.datacreator.config.DataCreatorIntegrationTest;
import ru.yandex.market.wms.datacreator.dto.ItemDto;

public class DataCreatorSkuDaoTest extends DataCreatorIntegrationTest {

    @Autowired
    private DataCreatorSkuDao skuDao;

    @Test
    @DatabaseSetup(value = "/service/items/before.xml", connection = "wmwhse1Connection")
    void getUitByLocAndLot_ShouldFindByAllParamsTest() {
        ItemDto item = skuDao.getItem(ItemDto.builder()
                .storerKey("10264169")
                .manufacturerSku("000116.4251497")
                .lot("0000013572")
                .loc("4-02")
                .serialNumber("995720063486")
                .build()
        );

        Assertions.assertEquals("10264169", item.getStorerKey());
        Assertions.assertEquals("000116.4251497", item.getManufacturerSku());
        Assertions.assertEquals("0000013572", item.getLot());
        Assertions.assertEquals("4-02", item.getLoc());
        Assertions.assertEquals("995720063486", item.getSerialNumber());
    }

    @Test
    @DatabaseSetup(value = "/service/items/before.xml", connection = "wmwhse1Connection")
    void getUitByLocAndLot_ShouldFindBySerialNumberTest() {
        ItemDto item = skuDao.getItem(ItemDto.builder()
                .serialNumber("995720063486")
                .build()
        );

        Assertions.assertEquals("10264169", item.getStorerKey());
        Assertions.assertEquals("000116.4251497", item.getManufacturerSku());
        Assertions.assertEquals("0000013572", item.getLot());
        Assertions.assertEquals("4-02", item.getLoc());
        Assertions.assertEquals("995720063486", item.getSerialNumber());
    }
}
