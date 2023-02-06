package ru.yandex.market.mbo.utils;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.api.Randomizer;
import io.github.benas.randombeans.randomizers.range.DoubleRangeRandomizer;
import io.github.benas.randombeans.randomizers.range.IntegerRangeRandomizer;
import io.github.benas.randombeans.randomizers.range.LongRangeRandomizer;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.gwt.models.visual.Word;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author anmalysh
 * @since 1/30/2019
 */
@SuppressWarnings("checkstyle:magicnumber")
public class RandomTestUtils {

    private static final long SEED = 12321321312L;

    private static final int MIN_STRING_LENGTH = 2;
    public static final int MAX_STRING_LENGTH = 20;

    private static final int MIN_COLLECTION_SIZE = 1;
    private static final int MAX_COLLECTION_SIZE = 5;

    private static final long MIN_LONG = 1L;
    private static final long MAX_LONG = 1000L;

    private static final int MIN_INTEGER = 1;
    private static final int MAX_INTEGER = 1000;

    private static final double MIN_DOUBLE = 1.0;
    private static final double MAX_DOUBLE = 100.0;

    private static final EnhancedRandom RANDOM = createNewRandom(SEED);

    private RandomTestUtils() { }

    private static EnhancedRandomBuilder createDefaultBuilder(long seed) {
        return EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
            .seed(seed)
            .stringLengthRange(MIN_STRING_LENGTH, MAX_STRING_LENGTH)
            .collectionSizeRange(MIN_COLLECTION_SIZE, MAX_COLLECTION_SIZE)
            .randomize(Long.class, new LongRangeRandomizer(MIN_LONG, MAX_LONG, seed))
            .randomize(Integer.class, new IntegerRangeRandomizer(MIN_INTEGER, MAX_INTEGER, seed))
            .randomize(Double.class, new DoubleRangeRandomizer(MIN_DOUBLE, MAX_DOUBLE, seed));
    }

    public static EnhancedRandom createNewRandom(long seed) {
        return createDefaultBuilder(seed)
            .randomize(Option.class, new ImplRandomizer<>(OptionImpl.class))
            .randomize(TovarCategory.class, new TovarCategoryRandomizer(seed))
            .randomize(Parameter.class, new ParameterRandomizer(seed))
            .randomize(Option.class, new OptionRandomizer(seed))
            .build();
    }

    /**
     * Этот класс использует статический рандом. Это неправильно,
     * так как созданный объект будет отличаться, если запускать один тест или сразу все тесты.
     *
     * Используйте рандом, который создается на каждый тест (createNewRandom) с переданным seed.
     */
    @Deprecated
    public static <T> T randomObject(Class<T> clazz, String... ignore) {
        return RANDOM.nextObject(clazz, ignore);
    }

    static class ImplRandomizer<INT, IMPL extends INT> implements Randomizer<INT> {

        private final Class<IMPL> implClass;

        ImplRandomizer(Class<IMPL> implClass)  {
            this.implClass = implClass;
        }

        @Override
        public INT getRandomValue() {
            return RANDOM.nextObject(implClass);
        }
    }

    static class TovarCategoryRandomizer implements Randomizer<TovarCategory> {

        private final EnhancedRandom random;

        TovarCategoryRandomizer(long seed) {
            random = createDefaultBuilder(seed).build();
        }

        @Override
        public TovarCategory getRandomValue() {
            TovarCategory category = random.nextObject(TovarCategory.class);
            // Should have russian name
            category.setName(random.nextObject(String.class));
            return category;
        }
    }

    static class ParameterRandomizer implements Randomizer<Parameter> {

        private final EnhancedRandom random;

        ParameterRandomizer(long seed) {
            random = createDefaultBuilder(seed).build();
        }

        @Override
        public Parameter getRandomValue() {
            Parameter parameter = random.nextObject(Parameter.class);
            parameter.addName(createRusName());
            parameter.setType(Param.Type.ENUM);
            int optionNum = Math.abs(random.nextInt()) % 10 + 3;
            List<Option> options = Stream.generate(() -> parameter.getId())
                .limit(optionNum)
                .map(this::createOption)
                .collect(Collectors.toList());
            parameter.setOptions(options);
            return parameter;
        }

        private Option createOption(long paramId) {
            Option option = random.nextObject(OptionImpl.class, "parent");
            option.addName(createRusName());
            option.setParamId(paramId);
            return option;
        }

        private Word createRusName() {
            Word rusName = random.nextObject(Word.class);
            rusName.setLangId(Word.DEFAULT_LANG_ID);
            return rusName;
        }
    }

    static class OptionRandomizer implements Randomizer<Option> {

        private final EnhancedRandom random;

        OptionRandomizer(long seed) {
            random = createDefaultBuilder(seed).build();
        }

        @Override
        public Option getRandomValue() {
            Option option = random.nextObject(OptionImpl.class, "parent");
            option.addName(createRusName());
            return option;
        }

        private Word createRusName() {
            Word rusName = random.nextObject(Word.class);
            rusName.setLangId(Word.DEFAULT_LANG_ID);
            return rusName;
        }
    }
}
