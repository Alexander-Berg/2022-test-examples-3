package ru.yandex.market.sqb.service.config.reader;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Supplier;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.sqb.service.config.reader.strategy.PackageConfigurationStrategy;

/**
 * Unit-тест для {@link PackageConfigurationReader}.
 *
 * @author Kirill Lakhtin (klaktin@yandex-team.ru)
 */
class PackageConfigurationReaderTest {

    @Test
    void testGet() {
        final PackageConfigurationStrategy strategy = Mockito.mock(PackageConfigurationStrategy.class);
        final PackageConfigurationReader instance = new PackageConfigurationReader(strategy);

        Collection<Supplier<String>> data = Arrays.asList(
                new TextConfigurationReader("1"),
                new TextConfigurationReader("2")
        );
        Mockito.when(strategy.getConfigurationReaders()).thenReturn(data);

        Collection<String> result = instance.get();

        MatcherAssert.assertThat(result, Matchers.containsInAnyOrder("1", "2"));
    }
}
