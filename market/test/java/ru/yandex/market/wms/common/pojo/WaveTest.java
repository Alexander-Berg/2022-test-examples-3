package ru.yandex.market.wms.common.pojo;


import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.common.spring.dao.entity.Order;
import ru.yandex.market.wms.common.spring.dao.entity.Wave;
import ru.yandex.market.wms.common.spring.enums.WaveType;

import static org.assertj.core.api.Assertions.assertThat;

public class WaveTest {

    @Test
    public void commentNullTest() {
        Wave w = Wave.builder().build();
        w.fillWaveComment();
        assertThat(w.getComment()).isEqualTo("");
    }

    @Test
    public void commentWaveTypeTest() {
        Wave w = Wave.builder().build();
        w.setWaveType(WaveType.SINGLE);
        w.fillWaveComment();
        assertThat(w.getComment()).isEqualTo("Синглы");
    }

    @Test
    public void commentCarrierTest() {
        Wave w = Wave.builder().build();
        w.setRealOrders(List.of(buildOrder("Carr1", "2021-09-08T11:45:00+00:03",
                "2021-09-08T08:45:00+00:03", 1)));
        w.fillWaveComment();
        assertThat(w.getComment()).isEqualTo("Carr1 8.9.2021");
    }

    @Test
    public void commentMultiCarrierTest() {
        Wave w = Wave.builder()
                .realOrders(new ArrayList<>())
                .build();
        w.getRealOrders().add(buildOrder("Carr1", "2021-09-08T11:45:00+00:03",
                "2021-09-08T08:45:00+00:03", 1));
        w.getRealOrders().add(buildOrder("Carr2", "2021-09-08T11:45:00+00:03",
                "2021-09-08T08:45:00+00:03", 1));
        w.fillWaveComment();
        assertThat(w.getComment()).isEqualTo("Мульти 11:45 8.9.2021");
    }

    @Test
    public void commentFullTest() {
        Wave w = Wave.builder()
                .realOrders(new ArrayList<>())
                .waveType(WaveType.SINGLE)
                .build();
        w.getRealOrders().add(buildOrder("Carr1", "2021-09-08T07:45:00+00:03",
                "2021-09-08T04:45:00+00:03", 1));
        w.getRealOrders().add(buildOrder("Carr1", "2021-09-08T11:45:00+00:03",
                "2021-09-08T08:45:00+00:03", 1));
        w.fillWaveComment();
        assertThat(w.getComment()).isEqualTo("Синглы Мульти 07:45 8.9.2021");
    }

    @Test
    public void commentTrimLengthTest() {
        StringBuilder longCarrierName = new StringBuilder("Carr");
        for (int i = 0; i < 60; i++) {
            longCarrierName.append(i).append(" ");
        }
        Wave w = Wave.builder()
                .realOrders(List.of(buildOrder(longCarrierName.toString(), "2021-09-08T07:45:00+00:03",
                        "2021-09-08T04:45:00+00:03", 1)))
                .build();
        w.fillWaveComment();
        assertThat(w.getComment()).isEqualTo("Carr0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 ");
    }

    private Order buildOrder(String carrier, String date1, String date2, int qty) {
        return Order.builder()
                .carrierName(carrier)
                .scheduledShipDate(OffsetDateTime.parse(date1))
                .scheduledShipDateInDB(OffsetDateTime.parse(date2))
                .totalqty(BigDecimal.valueOf(qty))
                .build();
    }
}
