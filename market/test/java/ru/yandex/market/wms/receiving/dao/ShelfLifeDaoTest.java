package ru.yandex.market.wms.receiving.dao;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.model.dto.SkuShelfLifeInfoHolder;
import ru.yandex.market.wms.common.spring.dao.ShelfLifeDao;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;
import ru.yandex.market.wms.receiving.ReceivingIntegrationTest;

class ShelfLifeDaoTest  extends ReceivingIntegrationTest {

    @Autowired
    ShelfLifeDao shelfLifeDao;

    @Test
    @DatabaseSetup(value = "/dao/sku/before.xml", connection = "wmwhseConnection")
    public void emptyValuesForOSGShouldBeNulls() {
        SkuShelfLifeInfoHolder skuInfo = shelfLifeDao.getShelflifeInfoBySkuId(SkuId.of("12345", "SKU_1"));
        Assertions.assertNull(skuInfo.getShelfLifePercentage());
        Assertions.assertNull(skuInfo.getShelfLifeOnReceivingPercentage());
    }

}
