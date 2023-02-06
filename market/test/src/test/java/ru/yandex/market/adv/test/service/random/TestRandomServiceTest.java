package ru.yandex.market.adv.test.service.random;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.AbstractAdvShopTest;
import ru.yandex.market.adv.service.random.RandomService;

@DisplayName("Тесты на сервис TestRandomService")
class TestRandomServiceTest extends AbstractAdvShopTest {

    private static final int BOUND = 50;

    @Autowired
    private RandomService randomService;

    @DisplayName("Успешное получение заранее заданного числа вместо рандомного")
    @Test
    void get_correct_int() {
        Assertions.assertThat(randomService.get(BOUND)).isEqualTo(BOUND - 1);
    }

    @DisplayName("Исключительная ситуация - граница задана отрицательным числом")
    @Test
    void get_negativeInt_exception() {
        Assertions.assertThatThrownBy(() -> randomService.get(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
