package ru.yandex.market.wms.shippingsorter.sorting.repository;

import java.math.BigDecimal;
import java.util.Optional;

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
import ru.yandex.market.wms.shippingsorter.sorting.entity.BoxInfoEntity;

@Import(ShippingSorterSecurityTestConfiguration.class)
public class BoxInfoRepositoryTest extends IntegrationTest {

    @Autowired
    private BoxInfoRepository boxInfoRepository;

    @Test
    @DatabaseSetup("/sorting/service/box-info/before-update.xml")
    @ExpectedDatabase("/sorting/service/box-info/before-update.xml")
    public void findByBoxIdTest_exist() {
        Optional<BoxInfoEntity> expectedBoxInfoEntity = Optional.of(
                BoxInfoEntity.builder()
                        .boxId("P123456789")
                        .boxWeight(300)
                        .boxWidth(new BigDecimal("1.000"))
                        .boxHeight(new BigDecimal("2.000"))
                        .boxLength(new BigDecimal("3.000"))
                        .carrierCode("some code")
                        .carrierName("DPD")
                        .operationDayId(1221L)
                        .serviceFrom("PACKING")
                        .build()
        );

        Optional<BoxInfoEntity> boxInfoEntity = boxInfoRepository.findByBoxId("P123456789");

        Assertions.assertEquals(expectedBoxInfoEntity, boxInfoEntity);
    }

    @Test
    @DatabaseSetup("/sorting/service/box-info/before-update.xml")
    @ExpectedDatabase("/sorting/service/box-info/before-update.xml")
    public void findByBoxIdTest_notExist() {
        Optional<BoxInfoEntity> boxInfoEntity = boxInfoRepository.findByBoxId("P123456780");

        Assertions.assertEquals(Optional.empty(), boxInfoEntity);
    }

    @Test
    @DatabaseSetup("/sorting/service/box-info/before-insert.xml")
    @ExpectedDatabase("/sorting/service/box-info/after-insert.xml")
    public void insertTest() {
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

        boxInfoRepository.insert(boxId, boxInfo, "PACKING");
    }

    @Test
    @DatabaseSetup("/sorting/service/box-info/before-update.xml")
    @ExpectedDatabase("/sorting/service/box-info/after-update.xml")
    public void updateTest() {
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

        boxInfoRepository.update(boxId, boxInfo, "PACKING");
    }
}
