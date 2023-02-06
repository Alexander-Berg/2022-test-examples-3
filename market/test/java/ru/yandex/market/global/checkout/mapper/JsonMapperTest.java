package ru.yandex.market.global.checkout.mapper;

import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.global.checkout.domain.promo.apply.free_delivery_area.FreeDeliveryAreaArgs;
import ru.yandex.market.global.checkout.util.RandomDataGenerator;

import static ru.yandex.market.global.checkout.mapper.JsonMapper.DB_JSON_MAPPER;

@Slf4j
public class JsonMapperTest {
    private static final EnhancedRandom RANDOM = RandomDataGenerator.dataRandom(JsonMapperTest.class).build();

    @SneakyThrows
    @Test
    void testSerializeFreeDeliveryArea() {
        FreeDeliveryAreaArgs original = RANDOM.nextObject(FreeDeliveryAreaArgs.class);
        String serialized = DB_JSON_MAPPER.writeValueAsString(original);
        FreeDeliveryAreaArgs deserialized = DB_JSON_MAPPER.readValue(serialized, FreeDeliveryAreaArgs.class);
        log.debug(serialized);
        Assertions.assertThat(deserialized)
                .isEqualTo(original);
    }
}
