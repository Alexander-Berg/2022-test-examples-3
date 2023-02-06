package ru.yandex.market.antifraud.orders.web.entity;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import ru.yandex.market.antifraud.orders.entity.loyalty.LoyaltyAntifraudContext;
import ru.yandex.market.antifraud.orders.entity.loyalty.LoyaltyLogEntity;
import ru.yandex.market.antifraud.orders.web.AntifraudJsonUtil;
import ru.yandex.market.antifraud.orders.web.dto.CoinDto;
import ru.yandex.market.antifraud.orders.web.dto.LoyaltyVerdictDto;
import ru.yandex.market.antifraud.orders.web.dto.LoyaltyVerdictRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.PromoVerdictDto;

import static org.junit.Assert.assertEquals;

public class LoyaltyLogEntityTest {
    @Test
    public void serializationTest() {
        LoyaltyVerdictRequestDto requestDto = LoyaltyVerdictRequestDto.builder()
                .orderIds(Collections.singletonList(1994L))
                .uid(1996L)
                .coins(Collections.singletonList(new CoinDto(1999L, 2009L)))
                .reason("CONFLICT")
                .build();

        LoyaltyAntifraudContext context = LoyaltyAntifraudContext.builder()
                .originRequest(requestDto)
                .build();
        LoyaltyVerdictDto verdictDto =
                new LoyaltyVerdictDto(
                        LoyaltyVerdictType.OK,
                        Arrays.asList(9141L, 9142L),
                        Collections.singletonList(
                                new PromoVerdictDto(442223L, 3333L, PromoVerdictType.OK)
                        ), null);
        LoyaltyLogEntity logEntity = new LoyaltyLogEntity(
                context,
                verdictDto,
                "req-id",
                "TESTING"
        );
        String json = AntifraudJsonUtil.toJson(logEntity);
        LoyaltyLogEntity parsedLog = AntifraudJsonUtil.fromJson(json, LoyaltyLogEntity.class);
        assertEquals(logEntity, parsedLog);
    }
}
