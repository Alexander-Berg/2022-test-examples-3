package ru.yandex.market.checkout.wiremock;

import java.util.Random;
import java.util.function.Function;

import static java.lang.Math.abs;

/**
 * @author mkasumov
 */
public class RandomPositiveIntSupplier implements Function<Random, Integer> {

    @Override
    public Integer apply(Random random) {
        return abs(random.nextInt());
    }
}
