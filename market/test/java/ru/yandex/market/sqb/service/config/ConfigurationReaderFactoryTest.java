package ru.yandex.market.sqb.service.config;

import java.io.File;
import java.net.URI;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import ru.yandex.market.sqb.service.config.reader.ClasspathConfigurationReader;
import ru.yandex.market.sqb.service.config.reader.FileConfigurationReader;
import ru.yandex.market.sqb.service.config.reader.TextConfigurationReader;
import ru.yandex.market.sqb.service.config.reader.UriConfigurationReader;
import ru.yandex.market.sqb.test.TestUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static ru.yandex.market.sqb.test.ConfigurationReaderUtils.BASE_CLASS;
import static ru.yandex.market.sqb.test.ConfigurationReaderUtils.CONTENT;
import static ru.yandex.market.sqb.test.ConfigurationReaderUtils.FILE_POSITIVE;
import static ru.yandex.market.sqb.test.ConfigurationReaderUtils.createUri;

/**
 * Unit-тесты для {@link ConfigurationReaderFactory}.
 *
 * @author Vladislav Bauer
 */
class ConfigurationReaderFactoryTest {

    @Test
    void testConstructorContract() {
        TestUtils.checkConstructor(ConfigurationReaderFactory.class);
    }

    @Test
    void testCreateTextReader() throws Exception {
        final URI uri = createUri(FILE_POSITIVE);

        final Supplier<String> uriReader = ConfigurationReaderFactory.createUriReader(uri);
        final Supplier<String> fileReader = ConfigurationReaderFactory.createFileReader(new File(uri));
        final Supplier<String> textReader = ConfigurationReaderFactory.createTextReader(CONTENT);

        final Supplier<String> classpathReader =
                ConfigurationReaderFactory.createClasspathReader(BASE_CLASS, FILE_POSITIVE);


        assertThat(uriReader, instanceOf(UriConfigurationReader.class));
        assertThat(fileReader, instanceOf(FileConfigurationReader.class));
        assertThat(textReader, instanceOf(TextConfigurationReader.class));
        assertThat(classpathReader, instanceOf(ClasspathConfigurationReader.class));
    }

}
