package ru.yandex.market.antifraud.orders.storage.entity.antifraud;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import ru.yandex.market.antifraud.orders.web.AntifraudJsonUtil;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Alexander Novikov <a href="mailto:hronos@yandex-team.ru"></a>
 * @date 25.02.2020
 */
public class PassportFeaturesTest {

    @Test
    public void serializationTest() throws IOException {
        ObjectMapper mapper = AntifraudJsonUtil.OBJECT_MAPPER;

        PassportFeatures accountState = PassportFeatures.builder()
                .uid(123456L)
                .hasRules(true)
                .karma(5000L)
                .accountCreated(true)
                .build();

        String json = "{\"uid\":123456,\"karma\":5000,\"hasRules\":true,\"accountCreated\":true}";

        PassportFeatures deserialized = mapper.readValue(json, PassportFeatures.class);
        assertThat(deserialized).isEqualTo(accountState);
    }
}
