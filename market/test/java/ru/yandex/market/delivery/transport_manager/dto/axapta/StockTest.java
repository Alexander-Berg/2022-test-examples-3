package ru.yandex.market.delivery.transport_manager.dto.axapta;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.domain.enums.CountType;
import ru.yandex.market.delivery.transport_manager.dto.Stock;


class StockTest {
    @DisplayName("Сток в верхнем регистре")
    @Test
    void parseValidUpper() {
        Assertions.assertEquals(new Stock(123, CountType.FIT), new Stock("123F"));
    }

    @DisplayName("Сток в нижнем регистре")
    @Test
    void parseValidLower() {
        Assertions.assertEquals(new Stock(123, CountType.FIT), new Stock("123f"));
    }

    @DisplayName("Неверный формат")
    @Test
    void parseInvalid() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Stock("ROSTOV_FIT"));
    }

    @DisplayName("Неизвестный тип стока")
    @Test
    void parseUnknownStock() {
        Assertions.assertEquals(new Stock(123, CountType.UNDEFINED), new Stock("123B"));
    }

    @DisplayName("Тест json парсинга")
    @Test
    void parseJson() throws IOException {
        Assertions.assertEquals(
            new Stock(123, CountType.FIT),
            new ObjectMapper().readValue("\"123F\"", Stock.class)
        );
    }

    @DisplayName("Тест записи json")
    @Test
    void writeJson() throws IOException {
        Assertions.assertEquals(
            "\"123F\"",
            new ObjectMapper().writeValueAsString(new Stock(123, CountType.FIT))
        );
    }
}
