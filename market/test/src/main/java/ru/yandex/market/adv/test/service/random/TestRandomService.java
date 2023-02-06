package ru.yandex.market.adv.test.service.random;

import org.jetbrains.annotations.NotNull;

import ru.yandex.market.adv.service.random.RandomService;

/**
 * Класс для получения тестового заранее заданного числа вместо рандомного.
 * Date: 06.04.2022
 * Project: adv-shop-integration
 *
 * @author eogoreltseva
 */
public class TestRandomService implements RandomService {

    @NotNull
    @Override
    public int get(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException(String.format("Bound %s is not positive", bound));
        }
        return bound - 1;
    }
}
