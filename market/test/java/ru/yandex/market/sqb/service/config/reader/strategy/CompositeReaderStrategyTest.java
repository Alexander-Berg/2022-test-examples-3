package ru.yandex.market.sqb.service.config.reader.strategy;

import java.util.Collection;
import java.util.function.Supplier;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.sqb.service.config.reader.TextConfigurationReader;

/**
 * Unit-тест для {@link CompositeReaderStrategy}.
 *
 * @author Kirill Lakhtin (klaktin@yandex-team.ru)
 */
class CompositeReaderStrategyTest {

    private CompositeReaderStrategy instance;
    private TextConfigurationReader reader;

    @BeforeEach
    void init() {
        reader = new TextConfigurationReader("test");
        instance = new CompositeReaderStrategy(reader);
    }

    @Test
    void testGetConfigurationReaders() {
        Collection<Supplier<String>> readers = instance.getConfigurationReaders();

        MatcherAssert.assertThat(readers, Matchers.containsInAnyOrder(reader));
    }
}
