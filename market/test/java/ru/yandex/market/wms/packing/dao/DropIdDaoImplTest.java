package ru.yandex.market.wms.packing.dao;

import java.math.BigDecimal;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.packing.exception.BoxNotFoundException;
import ru.yandex.market.wms.shippingsorter.core.sorting.entity.BoxInfo;

public class DropIdDaoImplTest extends IntegrationTest {

    @Autowired
    private DropIdDaoImpl dropIdDao;

    @Test
    @DatabaseSetup("/db/dao/drop-id/db_setup.xml")
    public void getBoxInfoByParcelId() {
        BoxInfo expected = BoxInfo.builder()
                .boxWeight(30200)
                .boxLength(new BigDecimal("100.00000"))
                .boxWidth(new BigDecimal("60.00000"))
                .boxHeight(new BigDecimal("40.00000"))
                .carrierCode("CARRIER-MP1")
                .operationDayId(18854L)
                .carrierName("DPD")
                .build();

        BoxInfo boxInfo = dropIdDao.getBoxInfoByParcelId("P000000501");

        Assertions.assertEquals(expected, boxInfo);
    }

    @Test
    @DatabaseSetup("/db/dao/drop-id/db_setup.xml")
    public void getBoxInfoByParcelId_BoxNotFoundException() {
        Assertions.assertThrows(BoxNotFoundException.class, () -> dropIdDao.getBoxInfoByParcelId("P000000502"));
    }
}
