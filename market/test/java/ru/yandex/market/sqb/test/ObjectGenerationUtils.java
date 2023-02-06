package ru.yandex.market.sqb.test;

import java.util.List;
import java.util.UUID;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;

import ru.yandex.market.sqb.model.conf.AliasModel;
import ru.yandex.market.sqb.model.conf.ArgumentModel;
import ru.yandex.market.sqb.model.conf.ParameterModel;
import ru.yandex.market.sqb.model.conf.TemplateModel;

/**
 * Утилитный класс для генерации тестовых данных.
 *
 * @author Vladislav Bauer
 */
public final class ObjectGenerationUtils {

    public static final String PREFIX_PARAMETER = "PARAMETER_";
    public static final String PREFIX_TEMPLATE = "TEMPLATE_";
    public static final String PREFIX_ALIAS = "ALIAS_";
    public static final String PREFIX_ARGUMENT = "ARG_";
    public static final int GENERATED_COUNT = 10;

    public static final IntFunction<AliasModel> CREATOR_ALIAS =
            index -> {
                final String name = PREFIX_ALIAS + index;
                return new AliasModel(name, String.valueOf(index));
            };

    public static final IntFunction<ArgumentModel> CREATOR_ARGUMENT =
            index -> {
                final String name = PREFIX_ARGUMENT + index;
                return new ArgumentModel(name, String.valueOf(index));
            };

    public static final IntFunction<TemplateModel> CREATOR_TEMPLATE =
            index -> {
                final String name = PREFIX_TEMPLATE + index;
                return new TemplateModel(name, empty(), empty(), empty());
            };

    public static final IntFunction<ParameterModel> CREATOR_PARAMETER =
            index -> {
                final String name = PREFIX_PARAMETER + index;
                return createNamedParameter(name);
            };


    private ObjectGenerationUtils() {
        throw new UnsupportedOperationException();
    }


    @Nonnull
    public static String[] namesLegal() {
        return new String[]{
                "test_name",
                "TEST",
                "_tTest09",
                "T34_test"
        };
    }

    @Nonnull
    public static String[] namesIllegal() {
        return new String[]{
                null,
                "",
                "0",
                "+0^&234234",
                "TEST+"
        };
    }

    @Nonnull
    public static String createName() {
        return UUID.randomUUID().toString();
    }

    @Nonnull
    public static <T> List<T> createObjects(final int count, @Nonnull final IntFunction<T> creator) {
        return IntStream.range(0, count)
                .mapToObj(creator)
                .collect(Collectors.toList());
    }

    public static ParameterModel createNamedParameter(final String name) {
        final List<ArgumentModel> arguments = createObjects(GENERATED_COUNT, CREATOR_ARGUMENT);

        return new ParameterModel(name, empty(), empty(), empty(), empty(), empty(), empty(), arguments);
    }

    private static String empty() {
        return StringUtils.EMPTY;
    }

}
