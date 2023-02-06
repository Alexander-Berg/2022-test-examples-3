package ru.yandex.market.wms.shippingsorter.sorting.service;

import java.math.BigDecimal;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import ru.yandex.market.wms.shippingsorter.configuration.ShippingSorterSecurityTestConfiguration;
import ru.yandex.market.wms.shippingsorter.core.sorting.entity.BoxId;
import ru.yandex.market.wms.shippingsorter.core.sorting.entity.BoxInfo;
import ru.yandex.market.wms.shippingsorter.sorting.IntegrationTest;

@Import(ShippingSorterSecurityTestConfiguration.class)
public class BoxInfoServiceTest extends IntegrationTest {

    @Autowired
    private BoxInfoService boxInfoService;

    @Test
    @DatabaseSetup("/sorting/service/box-info/before-insert.xml")
    @ExpectedDatabase("/sorting/service/box-info/after-insert.xml")
    public void getOrUpsertBoxInfoTest_insert() {
        BoxId boxId = BoxId.builder()
                .id("P123456789")
                .build();

        BoxInfo boxInfo = BoxInfo.builder()
                .boxWeight(300)
                .boxLength(new BigDecimal("3.0"))
                .boxWidth(new BigDecimal("1.0"))
                .boxHeight(new BigDecimal("2.0"))
                .carrierCode("some code")
                .operationDayId(1221L)
                .carrierName("DPD")
                .build();

        BoxInfo actualBoxInfo = boxInfoService.getOrUpsertBoxInfo(boxId, boxInfo);

        Assertions.assertEquals(boxInfo, actualBoxInfo);
    }

    @Test
    @DatabaseSetup("/sorting/service/box-info/before-update.xml")
    @ExpectedDatabase("/sorting/service/box-info/after-update.xml")
    public void getOrUpsertBoxInfoTest_update() {
        BoxId boxId = BoxId.builder()
                .id("P123456789")
                .build();

        BoxInfo boxInfo = BoxInfo.builder()
                .boxWeight(1000)
                .boxLength(new BigDecimal("30.0"))
                .boxWidth(new BigDecimal("10.0"))
                .boxHeight(new BigDecimal("20.0"))
                .carrierCode("some new code")
                .operationDayId(2112L)
                .carrierName("Boxberry")
                .build();

        BoxInfo actualBoxInfo = boxInfoService.getOrUpsertBoxInfo(boxId, boxInfo);

        Assertions.assertEquals(boxInfo, actualBoxInfo);
    }

    @Test
    @DatabaseSetup("/sorting/service/box-info/before-update.xml")
    @ExpectedDatabase("/sorting/service/box-info/before-update.xml")
    public void getOrUpsertBoxInfoTest_get() {
        BoxId boxId = BoxId.builder()
                .id("P123456789")
                .build();

        BoxInfo expectedBoxInfo = BoxInfo.builder()
                .boxWeight(300)
                .boxLength(new BigDecimal("3.000"))
                .boxWidth(new BigDecimal("1.000"))
                .boxHeight(new BigDecimal("2.000"))
                .carrierCode("some code")
                .operationDayId(1221L)
                .carrierName("DPD")
                .build();

        BoxInfo actualBoxInfo = boxInfoService.getOrUpsertBoxInfo(boxId, null);

        Assertions.assertEquals(expectedBoxInfo, actualBoxInfo);
    }
}
