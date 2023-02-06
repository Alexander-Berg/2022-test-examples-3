package ru.yandex.direct.common.log.container;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Test;

import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.utils.JsonUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;


public class LogPriceDataToJsonTest {
    private static final TypeReference<HashMap<String, String>> MAP_TYPE_REFERENCE =
            new TypeReference<HashMap<String, String>>() {
            };

    @Test
    public void logPriceData_convertedToJsonCorrectly() {
        // просто проверяем, что в JSON'е всё корректно после конвертации
        LogPriceData logPriceData =
                new LogPriceData(1L, 2L, 3L, 1. / 3, 2. / 3, CurrencyCode.YND_FIXED, LogPriceData.OperationType.UPDATE);

        Map<String, String> actual = JsonUtils.fromJson(JsonUtils.toJson(logPriceData), MAP_TYPE_REFERENCE);

        // Сравниваем строками, чтобы было нагляднее, что за значения ожидаем в числовых значениях
        assertThat(actual).containsOnly(
                entry("cid", "1"),
                entry("pid", "2"),
                entry("bid", "0"),
                entry("id", "3"),
                entry("price_ctx", "0.3333333333333333"),
                entry("price", "0.6666666666666666"),
                entry("type", "update"),
                entry("currency", "YND_FIXED")
        );
    }
}
