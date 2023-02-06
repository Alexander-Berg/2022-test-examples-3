package ru.yandex.market.sqb.test;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * Утилитный класс для работы с YAML.
 *
 * @author Vladislav Bauer
 */
public final class YamlUtils {

    private YamlUtils() {
        throw new UnsupportedOperationException();
    }


    @Nonnull
    public static List<Map<String, Object>> read(@Nonnull final Supplier<String> reader) {
        final String content = reader.get();
        final Yaml yaml = createYaml();
        final List<Map<String, Object>> rows = yaml.load(content);

        return rows.stream()
                .map(YamlUtils::toUpperCase)
                .collect(Collectors.toList());
    }


    private static Yaml createYaml() {
        final LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setAllowDuplicateKeys(false);

        return new Yaml(loaderOptions);
    }

    private static Map<String, Object> toUpperCase(final Map<String, Object> map) {
        return map.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> StringUtils.upperCase(e.getKey()),
                        e -> Objects.toString(e.getValue(), StringUtils.EMPTY)
                ));
    }

}
