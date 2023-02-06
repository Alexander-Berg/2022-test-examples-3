package ru.yandex.market.common.test.json;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;

import ru.yandex.market.common.test.transformer.CompositeStringTransformer;
import ru.yandex.market.common.test.transformer.PatternFunctionalStringTransformer;
import ru.yandex.market.common.test.transformer.StringTransformer;

/**
 * Трансформер json-файлов, поддерживающий json-специфичные макросы.
 */
public class JsonTransformer extends CompositeStringTransformer {

    /**
     * Макрос {@code timestamp}, позволяющий указывать timestamp строкой.
     * Пример: {@code "start_date": "timestamp(2018-12-15T23:01)"} будет преобразован
     * в {@code "start_date": 1544904060}
     */
    private static final String PATTERN_TIMESTAMP = "\"timestamp\\((\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2})\\)\"";

    private static final Map<String, Function<Matcher, String>> REPLACEMENTS =
            ImmutableMap.<String, Function<Matcher, String>>builder()
                    .put(PATTERN_TIMESTAMP, match -> {
                        final String timeStr = match.group(1);
                        final LocalDateTime localTime = LocalDateTime.parse(timeStr);
                        final long seconds = localTime.atZone(ZoneId.of("Europe/Moscow"))
                                .toInstant()
                                .getEpochSecond();
                        return String.valueOf(seconds);
                    })
                    .build();

    @Override
    protected void customizeTransformers(final List<StringTransformer> transformers) {
        final List<PatternFunctionalStringTransformer> customTransformers = REPLACEMENTS.entrySet()
                .stream()
                .map(e -> new PatternFunctionalStringTransformer(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
        transformers.addAll(customTransformers);
    }
}
