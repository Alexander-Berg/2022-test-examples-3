package ru.yandex.market.adv.service.random;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Тесты на сервис RandomServiceImpl")
class RandomServiceImplTest {

    private static final int BOUND = 1000;

    private final RandomService randomService = new RandomServiceImpl();

    @DisplayName("Успешное получение рандомного числа в указанном диапазоне")
    @Test
    void get_correct_int() {
        Assertions.assertThat(randomService.get(BOUND))
                .isGreaterThanOrEqualTo(0)
                .isLessThan(BOUND);
    }

    @DisplayName("Исключительная ситуация - граница задана отрицательным числом")
    @Test
    void get_negativeInt_exception() {
        Assertions.assertThatThrownBy(() -> randomService.get(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
