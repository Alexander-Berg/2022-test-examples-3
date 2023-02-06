package ru.yandex.market.sqb.test;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;

import ru.yandex.market.sqb.service.config.ConfigurationModelServiceTest;
import ru.yandex.market.sqb.service.config.ConfigurationReaderFactory;

/**
 * Утилитный класс для работы с тестовыми файлами/данными.
 *
 * @author Vladislav Bauer
 */
public final class ConfigurationReaderUtils {

    public static final Class<?> BASE_CLASS = ConfigurationModelServiceTest.class;
    public static final String FILE_POSITIVE = xmlFile(ConfigurationModelServiceTest.CONFIG_CORRECT);
    public static final String FILE_INSUFFICIENT_PARAMETER =
            xmlFile(ConfigurationModelServiceTest.CONFIG_INSUFFICIENT_PARAMETER);
    public static final String FILE_DIFFERENT_NAME = xmlFile(ConfigurationModelServiceTest.CONFIG_DIFFERENT_NAME);
    public static final String FILE_NEGATIVE = xmlFile(ConfigurationModelServiceTest.CONFIG_NOT_EXISTED);
    public static final String FILE_INVALID_SQL = xmlFile(ConfigurationModelServiceTest.CONFIG_INVALID_SQL);
    public static final String CONTENT = "text sample";

    private static final String SCHEME_FILE = "file://";
    private static final String EXTENSION_XML = ".xml";
    private static final String EXTENSION_SQL = ".sql";


    private ConfigurationReaderUtils() {
        throw new UnsupportedOperationException();
    }


    @Nonnull
    public static Supplier<String> createReader(@Nonnull final String fileName) {
        return ConfigurationReaderFactory.createClasspathReader(BASE_CLASS, fileName);
    }

    @Nonnull
    public static URI createUri(final String fileName) throws URISyntaxException {
        final URL url = BASE_CLASS.getResource(fileName);
        return url != null ? url.toURI() : URI.create(SCHEME_FILE + fileName);
    }

    @Nonnull
    public static String xmlFile(@Nonnull final String fileName) {
        return fileWithExtension(fileName, EXTENSION_XML);
    }

    @Nonnull
    public static String sqlFile(@Nonnull final String fileName) {
        return fileWithExtension(fileName, EXTENSION_SQL);
    }


    private static String fileWithExtension(final String fileName, final String extension) {
        return StringUtils.isEmpty(fileName) ? fileName : fileName + extension;
    }

}
