package ru.yandex.market.wms.api.repository.sku.itemmaster;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.ManufacturerSku;
import ru.yandex.market.wms.common.spring.repository.sku.itemmaster.ItemMasterRepository;
import ru.yandex.market.wms.common.spring.repository.sku.itemmaster.entity.ItemMasterManufacturerSku;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ItemMasterRepositoryTest extends IntegrationTest {
    @Autowired
    private ItemMasterRepository itemMasterRepository;

    @Test
    @DatabaseSetup("/item-master/repository/sku/1/before.xml")
    void checkZeroSku() {

        Iterable<ManufacturerSku> items = Arrays.asList(
                new ManufacturerSku("649164", "test")
        );

        List<ItemMasterManufacturerSku> expectedResult = Collections.emptyList();

        List<ItemMasterManufacturerSku> receivedResult = itemMasterRepository.getItemMasterManufacturerSkuData(items);

        assertEquals(expectedResult, receivedResult);
    }

    @Test
    @DatabaseSetup("/item-master/repository/sku/1/before.xml")
    void checkOneSku() {

        Iterable<ManufacturerSku> items = Arrays.asList(
            new ManufacturerSku("649164", "e75019")
        );

        List<ItemMasterManufacturerSku> expectedResult = Arrays.asList(
                ItemMasterManufacturerSku.builder()
                        .sku("ROV0000000000001206580")
                        .storerKey("649164")
                        .manufacturerSku("e75019")
                        .masterBomSku(null)
                        .build()
        );

        List<ItemMasterManufacturerSku> receivedResult = itemMasterRepository.getItemMasterManufacturerSkuData(items);

        assertEquals(expectedResult, receivedResult);
    }

    @Test
    @DatabaseSetup("/item-master/repository/sku/2/before.xml")
    void checkBomSku() {

        Iterable<ManufacturerSku> items = Arrays.asList(
                new ManufacturerSku("649164", "e75019")
        );

        List<ItemMasterManufacturerSku> expectedResult = Arrays.asList(
                ItemMasterManufacturerSku.builder()
                        .sku("ROV0000000000001206580")
                        .storerKey("649164")
                        .manufacturerSku("e75019")
                        .masterBomSku(null)
                        .build(),
                ItemMasterManufacturerSku.builder()
                        .sku("ROV0000000000001206580BOM1")
                        .storerKey("649164")
                        .manufacturerSku("e75019")
                        .masterBomSku("ROV0000000000001206580")
                        .build(),
                ItemMasterManufacturerSku.builder()
                        .sku("ROV0000000000001206580BOM2")
                        .storerKey("649164")
                        .manufacturerSku("e75019")
                        .masterBomSku("ROV0000000000001206580")
                        .build()
        );

        List<ItemMasterManufacturerSku> receivedResult = itemMasterRepository.getItemMasterManufacturerSkuData(items);

        assertEquals(expectedResult, receivedResult);
    }
}
