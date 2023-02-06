package ru.yandex.market.antifraud.orders.web.dto;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import ru.yandex.market.antifraud.orders.web.AntifraudJsonUtil;
import ru.yandex.market.antifraud.orders.web.entity.LoyaltyVerdictType;
import ru.yandex.market.antifraud.orders.web.entity.PromoVerdictType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dzvyagin
 */
public class LoyaltyVerdictDtoTest {

    @Test
    public void serializationTest() throws IOException {
        ObjectMapper mapper = AntifraudJsonUtil.OBJECT_MAPPER;
        LoyaltyVerdictDto response = new LoyaltyVerdictDto(
                LoyaltyVerdictType.OTHER,
                Collections.singletonList(654L),
                Arrays.asList(
                        new PromoVerdictDto(12L, 11L, PromoVerdictType.OK),
                        new PromoVerdictDto(15L, 22L, PromoVerdictType.USED)
                ), true);
        String json = mapper.writeValueAsString(response);
        LoyaltyVerdictDto deserialized = mapper.readValue(json, LoyaltyVerdictDto.class);
        assertThat(deserialized).isEqualTo(response);
    }
}
