package ru.yandex.market.global.checkout.domain.promo.apply;

import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.global.checkout.domain.promo.model.PromoType;
import ru.yandex.market.global.checkout.mapper.JsonMapper;
import ru.yandex.market.global.checkout.util.RandomDataGenerator;
import ru.yandex.mj.generated.server.model.PromoCommunicationArgsDto;

public class PromoArgsJacksonTest {
    private static final EnhancedRandom RANDOM = RandomDataGenerator.dataRandom(PromoArgsJacksonTest.class).build();

    @SneakyThrows
    @ParameterizedTest(name = "type = {0}")
    @EnumSource(PromoType.class)
    public void testSerialize(PromoType promoType) {
        PromoArgs toSerialize = RANDOM.nextObject(promoType.getArgsClass());
        String json = JsonMapper.DB_JSON_MAPPER.writeValueAsString(toSerialize);
        PromoArgs deserialized = JsonMapper.DB_JSON_MAPPER.readValue(json, promoType.getArgsClass());
        Assertions.assertThat(deserialized).isEqualTo(toSerialize);
    }

    @SneakyThrows
    @Test
    public void testEmptyCommunication() {
        PromoCommunicationArgsDto toSerialize = RANDOM.nextObject(PromoCommunicationArgsDto.class)
                .checkout(null)
                .informer(null)
                .push(null);
        String json = JsonMapper.DB_JSON_MAPPER.writeValueAsString(toSerialize);
        PromoCommunicationArgsDto deserialized = JsonMapper.DB_JSON_MAPPER.readValue(json,
                PromoCommunicationArgsDto.class);
        Assertions.assertThat(deserialized).isEqualTo(toSerialize);
    }
}
