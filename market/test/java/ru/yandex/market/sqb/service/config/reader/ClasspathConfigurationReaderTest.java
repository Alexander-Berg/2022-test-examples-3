package ru.yandex.market.sqb.service.config.reader;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.sqb.exception.SqbConfigurationException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static ru.yandex.market.sqb.test.ConfigurationReaderUtils.BASE_CLASS;
import static ru.yandex.market.sqb.test.ConfigurationReaderUtils.FILE_NEGATIVE;
import static ru.yandex.market.sqb.test.ConfigurationReaderUtils.FILE_POSITIVE;

/**
 * Unit-тесты для {@link ClasspathConfigurationReader}.
 *
 * @author Vladislav Bauer
 */
class ClasspathConfigurationReaderTest {

    @Test
    void testPositive() {
        final ClasspathConfigurationReader reader = new ClasspathConfigurationReader(BASE_CLASS, FILE_POSITIVE);
        final String content = reader.get();

        assertThat(content, not(emptyOrNullString()));
        assertThat(reader.getBaseClass(), equalTo(BASE_CLASS));
        assertThat(reader.getFileName(), equalTo(FILE_POSITIVE));
    }

    @Test
    void testNegative() {
        checkNegative(BASE_CLASS, FILE_NEGATIVE);
    }


    private void checkNegative(final Class<?> clazz, final String fileName) {
        final ClasspathConfigurationReader reader = new ClasspathConfigurationReader(clazz, fileName);
        Assertions.assertThrows(SqbConfigurationException.class, reader::get);
    }

}
