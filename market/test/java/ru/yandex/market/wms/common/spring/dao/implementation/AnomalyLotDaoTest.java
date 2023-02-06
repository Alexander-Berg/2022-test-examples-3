package ru.yandex.market.wms.common.spring.dao.implementation;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.model.enums.AnomalyContainerType;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.AnomalyContainer;
import ru.yandex.market.wms.common.spring.dao.entity.AnomalyLot;
import ru.yandex.market.wms.common.spring.enums.AnomalyCategory;
import ru.yandex.market.wms.common.spring.enums.ReceivingItemType;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

class AnomalyLotDaoTest extends IntegrationTest {

    @Autowired
    private AnomalyLotDao anomalyLotDao;

    @Test
    @DatabaseSetup("/db/dao/anomalylot/before.xml")
    void getAnomalyLotsByReceiptKey() {
        List<AnomalyLot> lots = anomalyLotDao.findAnomalyByReceiptKey("0000000016");
        assertions.assertThat(lots).hasOnlyOneElementSatisfying(anomalyLot -> {
            assertions.assertThat(anomalyLot.getId()).isEqualTo("1");
            assertions.assertThat(anomalyLot.getAnomalyContainer().getTransportUnitId()).isEqualTo("PLT00001");
            assertions.assertThat(anomalyLot.getAnomalyContainer().getReceiptKey()).isEqualTo("0000000016");
            assertions.assertThat(anomalyLot.getStorerKey()).isEqualTo("12");
            assertions.assertThat(anomalyLot.getDescription()).isEqualTo("Некоторый товар 1");
            assertions.assertThat(anomalyLot.getTypes()).containsOnly(ReceivingItemType.DAMAGED);
            assertions.assertThat(anomalyLot.getAmount()).isEqualTo(1);
            assertions.assertThat(anomalyLot.getAnomalyContainer().getLoc()).isEqualTo("DAMAGE01");
            assertions.assertThat(anomalyLot.getMfgDate())
                    .isEqualTo(LocalDateTime.of(2021, 9, 23, 11, 00, 00).toInstant(
                            ZoneOffset.UTC));
            assertions.assertThat(anomalyLot.getExpDate())
                    .isEqualTo(LocalDateTime.of(2021, 9, 23, 11, 00, 00).toInstant(
                            ZoneOffset.UTC));
            assertions.assertThat(anomalyLot.getAnomalyContainer().getSubreceipt()).isEqualTo("subreceipt");
            assertions.assertThat(anomalyLot.getAnomalyContainer().getInitReceivingUnit()).isEqualTo("RETURNBOX");
            assertions.assertThat(anomalyLot.getAnomalyContainer().getReturnId()).isEqualTo("RETID");
        });
    }

    @Test
    @ExpectedDatabase(value = "/db/dao/anomalylot/after.xml", assertionMode = NON_STRICT_UNORDERED)
    void insertTooLongAltSku() {
        anomalyLotDao.addAll(List.of(
                AnomalyLot.builder(
                                AnomalyContainer.builder(AnomalyContainerType.SECONDARY)
                                        .receiptKey("0000000016")
                                        .transportUnitId("PLT00001")
                                        .loc("DAMAGE01")
                                        .build()
                        )
                        .id("1")
                        .types(Set.of(ReceivingItemType.NO_RUSSIAN_DESCRIPTION))
                        .storerKey("12")
                        .amount(1)
                        .altSku("010467003301005321gJk6o54AQBJfX240640191ffd092LGYcm3FRQrRdNOO" +
                                "+8t0pz78QTyxxBmYKhLXaAS03jKV7oy+DWGy1SeU+BZ8o7B8+hs9LvPdNA7B6NPGjrCm34A==")
                        .category(AnomalyCategory.FOOD.getCategory())
                        .build()
        ), "TEST");
    }
}
