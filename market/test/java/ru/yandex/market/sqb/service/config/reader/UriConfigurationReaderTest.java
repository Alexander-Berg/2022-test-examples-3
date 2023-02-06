package ru.yandex.market.sqb.service.config.reader;

import java.net.URI;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.sqb.exception.SqbConfigurationException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static ru.yandex.market.sqb.test.ConfigurationReaderUtils.FILE_NEGATIVE;
import static ru.yandex.market.sqb.test.ConfigurationReaderUtils.FILE_POSITIVE;
import static ru.yandex.market.sqb.test.ConfigurationReaderUtils.createUri;

/**
 * Unit-тесты для {@link UriConfigurationReader}.
 *
 * @author Vladislav Bauer
 */
class UriConfigurationReaderTest {

    @Test
    void testPositive() throws Exception {
        final URI uri = createUri(FILE_POSITIVE);
        final UriConfigurationReader reader = new UriConfigurationReader(uri);
        final String content = reader.get();

        assertThat(content, not(emptyOrNullString()));
        assertThat(reader.getUri(), equalTo(uri));
    }

    @Test
    void testNegative() throws Exception {
        final URI uri = createUri(FILE_NEGATIVE);
        final UriConfigurationReader reader = new UriConfigurationReader(uri);
        Assertions.assertThrows(SqbConfigurationException.class, reader::get);
    }

}
