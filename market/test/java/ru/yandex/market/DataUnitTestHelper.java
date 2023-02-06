package ru.yandex.market;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import ru.yandex.market.sqb.service.config.ConfigurationReaderFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author fbokovikov
 */
class DataUnitTestHelper {

    static final String EXPECTED_RESULTS = "/%s/expected.yaml";
    static final String EXPECTED_FIELDS = "/%s/fields.lst";

    static final String CONFIG_FOLDER = "/etc/yandex/mbi-billing";
    static final String CONFIG_EXTENSION = ".xml";
    static final String CONFIG_XML = CONFIG_FOLDER + File.separator + "%s" + CONFIG_EXTENSION;

    private static final Set<String> ALL_CONFIGURATIONS = getAllConfigurations();

    @Nonnull
    static Supplier<String> resource(@Nonnull String configName, @Nonnull String fileFormat) {
        String config = String.format(fileFormat, configName);
        return ConfigurationReaderFactory.createClasspathReader(ShopsDataTest.class, config);
    }

    static void verifyConfig(@Nonnull String configName) {
        assertThat(ALL_CONFIGURATIONS)
                .as("Одна из конфигураций не указана в параметрах теста: %s", configName)
                .contains(configName);
    }

    private static Set<String> getAllConfigurations() {
        //получаем все xml-конфиги в директории с конфигурациями (в т.ч. в поддиректориях)
        URL resource = ShopsDataTest.class.getResource(CONFIG_FOLDER);
        String extensionWithoutLeadingDot = CONFIG_EXTENSION.substring(1);
        Collection<File> files = FileUtils.listFiles(FileUtils.toFile(resource),
                new String[]{extensionWithoutLeadingDot}, true);
        // получаем имена конфигураций относительно CONFIG_FOLDER и отрезаем у них расширение
        // итог - имена вида "shops_data", "banner/first_config" и тд
        return files.stream()
                .map(File::getPath)
                .map(n -> StringUtils.substringAfterLast(n, CONFIG_FOLDER))
                .map(n -> StringUtils.substringBetween(n, File.separator, CONFIG_EXTENSION))
                .collect(Collectors.toUnmodifiableSet());
    }

}
