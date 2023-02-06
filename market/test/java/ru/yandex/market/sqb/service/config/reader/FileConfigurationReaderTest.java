package ru.yandex.market.sqb.service.config.reader;

import java.io.File;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.sqb.exception.SqbConfigurationException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static ru.yandex.market.sqb.test.ConfigurationReaderUtils.FILE_POSITIVE;
import static ru.yandex.market.sqb.test.ConfigurationReaderUtils.createUri;
import static ru.yandex.market.sqb.test.ObjectGenerationUtils.createName;

/**
 * Unit-тесты для {@link FileConfigurationReader}.
 *
 * @author Vladislav Bauer
 */
class FileConfigurationReaderTest {

    @Test
    void testPositive() throws Exception {
        final File file = new File(createUri(FILE_POSITIVE));
        final FileConfigurationReader reader = new FileConfigurationReader(file);
        final String content = reader.get();

        assertThat(content, not(emptyOrNullString()));
        assertThat(reader.getFile(), equalTo(file));
    }

    @Test
    void testNegative() {
        final File file = new File(createName());
        final FileConfigurationReader reader = new FileConfigurationReader(file);
        Assertions.assertThrows(SqbConfigurationException.class, reader::get);
    }

}
