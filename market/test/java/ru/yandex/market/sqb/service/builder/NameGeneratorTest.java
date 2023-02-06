package ru.yandex.market.sqb.service.builder;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Unit-тесты для {@link NameGenerator}.
 *
 * @author Vladislav Bauer
 */
class NameGeneratorTest {

    @Test
    void testGenerate() {
        final String customPrefix = "prefix_";
        final NameGenerator generator = new NameGenerator(customPrefix);

        for (long counter = 1; counter <= 10; counter++) {
            final String generated = generator.generate();

            assertThat(generator.getPrefix(), equalTo(customPrefix));
            assertThat(generator.getCounter(), equalTo(counter));
            assertThat(generated, equalTo(customPrefix + counter));
        }
    }

}
