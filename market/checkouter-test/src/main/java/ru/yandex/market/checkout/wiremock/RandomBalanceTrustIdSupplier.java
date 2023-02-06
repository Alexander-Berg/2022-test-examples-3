package ru.yandex.market.checkout.wiremock;

import java.util.Random;
import java.util.function.Function;

import static java.lang.Integer.toHexString;

/**
 * @author mkasumov
 */
public class RandomBalanceTrustIdSupplier implements Function<Random, String> {

    @Override
    public String apply(Random random) {
        return toHexString(random.nextInt()) +
                toHexString(random.nextInt()) +
                toHexString(random.nextInt());
    }
}
