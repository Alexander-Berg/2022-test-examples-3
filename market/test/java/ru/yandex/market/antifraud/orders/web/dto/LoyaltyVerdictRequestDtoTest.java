package ru.yandex.market.antifraud.orders.web.dto;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import ru.yandex.market.antifraud.orders.web.AntifraudJsonUtil;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dzvyagin
 */
public class LoyaltyVerdictRequestDtoTest {

    @Test
    public void serializationTest() throws IOException {
        ObjectMapper mapper = AntifraudJsonUtil.OBJECT_MAPPER;
        LoyaltyVerdictRequestDto request = LoyaltyVerdictRequestDto.builder()
                .coins(Collections.singletonList(new CoinDto(1L, 1L)))
                .orderIds(Arrays.asList(123L, 1234L))
                .reason("reason")
                .uid(444L)
                .build();
        String json = mapper.writeValueAsString(request);
        LoyaltyVerdictRequestDto deserialized = mapper.readValue(json, LoyaltyVerdictRequestDto.class);
        assertThat(deserialized).isEqualTo(request);
    }
}
