package ru.yandex.market.antifraud.orders.entity.loyalty;

import java.time.Instant;
import java.util.List;

import org.junit.Test;

import ru.yandex.market.antifraud.orders.web.AntifraudJsonUtil;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author dzvyagin
 */
public class LoyaltyCoinTest {

    @Test
    public void serializationTest() {
        Instant now = Instant.now();
        LoyaltyCoin coin = LoyaltyCoin.builder()
                .id(1L)
                .coinId(12L)
                .promoId(13L)
                .uid(14L)
                .status("status")
                .startDate(now)
                .endDate(now.plusSeconds(3600))
                .actionTime(now.plusSeconds(1800))
                .orderIds(List.of(1L,2L,3L))
                .build();
        String json = AntifraudJsonUtil.toJson(coin);
        assertThat(AntifraudJsonUtil.fromJson(json, LoyaltyCoin.class)).isEqualTo(coin);
    }

}
