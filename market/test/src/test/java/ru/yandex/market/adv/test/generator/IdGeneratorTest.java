package ru.yandex.market.adv.test.generator;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.AbstractAdvShopTest;
import ru.yandex.market.adv.generator.IdGenerator;

/**
 * Date: 23.11.2021
 * Project: arcadia-market_adv_adv-shop
 *
 * @author alexminakov
 */
class IdGeneratorTest extends AbstractAdvShopTest {

    @Autowired
    private IdGenerator idGenerator;

    @DisplayName("Получение из генератора заранее заданной 1")
    @Test
    void generate_correct_one() {
        Assertions.assertThat(idGenerator.generate())
                .isEqualTo("1");
    }
}
