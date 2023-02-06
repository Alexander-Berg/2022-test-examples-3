package ru.yandex.market.antifraud.orders.storage.entity.rules;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.antifraud.orders.web.AntifraudJsonUtil;

/**
 * @author dzvyagin
 */
public class ItemAutoLimitDetectorConfigurationTest {

    @Test
    public void serializationTest() {
        ItemAutoLimitDetectorConfiguration conf = new ItemAutoLimitDetectorConfiguration(
            true,
            BigDecimal.ONE,
            BigDecimal.ONE,
            List.of(new ItemAutoLimitDetectorConfiguration.CategoryMultiplierPair(1234, BigDecimal.valueOf(4))),
            Set.of(123L),
            ItemAutoLimitDetectorConfiguration.RestrictionType.PREPAY,
            Set.of("reseller"));
        String json = AntifraudJsonUtil.toJson(conf);
        ItemAutoLimitDetectorConfiguration deserialized = AntifraudJsonUtil.fromJson(json,
                ItemAutoLimitDetectorConfiguration.class);
        Assertions.assertThat(deserialized).isEqualTo(conf);
    }

}
