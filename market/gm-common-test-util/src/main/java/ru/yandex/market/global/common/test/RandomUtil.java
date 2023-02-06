package ru.yandex.market.global.common.test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.api.Randomizer;
import io.github.benas.randombeans.randomizers.number.DoubleRandomizer;
import io.github.benas.randombeans.randomizers.range.ZonedDateTimeRangeRandomizer;
import io.github.benas.randombeans.randomizers.time.OffsetDateTimeRandomizer;

import static io.github.benas.randombeans.randomizers.range.ZonedDateTimeRangeRandomizer.aNewZonedDateTimeRangeRandomizer;
import static io.github.benas.randombeans.randomizers.time.OffsetDateTimeRandomizer.aNewOffsetDateTimeRandomizer;

/**
 * @author moskovkin@yandex-team.ru
 * @since 16.04.2020
 */
public class RandomUtil {
    private RandomUtil() {
    }

    public static EnhancedRandomEnhancedBuilder baseRandom(long seed) {
        return (EnhancedRandomEnhancedBuilder) (new EnhancedRandomEnhancedBuilder())
                .seed(seed)
                .collectionSizeRange(0, 5)
                .randomizationDepth(100)
                .overrideDefaultInitialization(true);
    }

    public static Randomizer<OffsetDateTime> fixedOffsetDateTimeRandomizer(long seed, ZoneOffset offset) {
        return new Randomizer<>() {
            private OffsetDateTimeRandomizer delegate = aNewOffsetDateTimeRandomizer(seed);

            @Override
            public OffsetDateTime getRandomValue() {
                return delegate.getRandomValue().withOffsetSameInstant(offset);
            }
        };
    }

    public static Randomizer<BigDecimal> fixedPrecisionBigDecimalRandomizer(long seed, int scale) {
        return new Randomizer<>() {
            private final DoubleRandomizer doubleRandomizer = new DoubleRandomizer(seed);

            @Override
            public BigDecimal getRandomValue() {
                return BigDecimal.valueOf(doubleRandomizer.getRandomValue()).setScale(scale, RoundingMode.HALF_UP)
                        .stripTrailingZeros();
            }
        };
    }

    public static Randomizer<String> fixedValuesStringRandomizer(long seed, String... values) {
        return new Randomizer<>() {
            private final EnhancedRandom random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
                    .seed(seed)
                    .build();

            @Override
            public String getRandomValue() {
                return values[random.nextInt(values.length)];
            }
        };
    }

    public static Randomizer<Integer> fixedValuesIntegerRandomizer(long seed, Integer... values) {
        return new Randomizer<>() {
            private final EnhancedRandom random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
                    .seed(seed)
                    .build();

            @Override
            public Integer getRandomValue() {
                return values[random.nextInt(values.length)];
            }
        };
    }

    public static Randomizer<Long> fixedValuesLongRandomizer(long seed, Long... values) {
        return new Randomizer<>() {
            private final EnhancedRandom random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
                    .seed(seed)
                    .build();

            @Override
            public Long getRandomValue() {
                return values[random.nextInt(values.length)];
            }
        };
    }

    @SafeVarargs
    public static <T> Randomizer<T> fixedValuesRandomizer(long seed, T... values) {
        return new Randomizer<>() {
            private final EnhancedRandom random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
                    .seed(seed)
                    .build();

            @Override
            public T getRandomValue() {
                return values[random.nextInt(values.length)];
            }
        };
    }

    public static <E extends Enum<E>>  Randomizer<String> enumToStringRandomizer(long seed, Class<E> enumClazz) {
        return new Randomizer<>() {
            private final EnhancedRandom random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
                    .seed(seed)
                    .build();
            private final List<E> values = EnumSet.allOf(enumClazz).stream()
                    .collect(Collectors.toUnmodifiableList());

            @Override
            public String getRandomValue() {
                return values.get(random.nextInt(values.size())).name();
            }
        };
    }

    public static <E extends Enum<E>> Randomizer<EnumSet<E>> enumSetRandomizer(long seed, Class<E> enumClazz) {
        return new Randomizer<>() {
            private final EnhancedRandom random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
                    .seed(seed)
                    .build();

            @Override
            public EnumSet<E> getRandomValue() {
                EnumSet<E> result1 = EnumSet.allOf(enumClazz);
                result1.removeIf(e -> random.nextBoolean());
                return result1;
            }
        };
    }

    @SafeVarargs
    public static <T> Randomizer<T> randomImplementationRandomizer(long seed, Class<? extends T>... implementations) {
        return new Randomizer<>() {
            private final EnhancedRandom random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
                    .seed(seed)
                    .build();

            @Override
            public T getRandomValue() {
                return random.nextObject(implementations[random.nextInt(implementations.length)]);
            }
        };
    }

    public static Randomizer<Instant> instantRangeRandomizer(Instant from, Instant to, long seed) {
        return new Randomizer<>() {
            final ZonedDateTimeRangeRandomizer randomizer = aNewZonedDateTimeRangeRandomizer(
                    from.atZone(ZoneId.of("Europe/Moscow")),
                    to.atZone(ZoneId.of("Europe/Moscow")),
                    seed
            );

            @Override
            public Instant getRandomValue() {
                return randomizer.getRandomValue().toInstant();
            }
        };
    }

    public static <T> Randomizer<T> someTimeNullRandomizer(long seed, Class<T> clazz) {
        return someTimeNullRandomizer(seed, new Randomizer<>() {
            private final EnhancedRandom random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
                    .seed(seed)
                    .build();

            @Override
            public T getRandomValue() {
                return random.nextObject(clazz);
            }
        });
    }

    public static <T> Randomizer<T> someTimeNullRandomizer(long seed, Randomizer<T> delegate) {
        return new Randomizer<>() {
            private static final int MAX_BOUND = 9;
            private static final int NULL_BOUND = 2;
            private final EnhancedRandom random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
                    .seed(seed)
                    .build();

            @Override
            public T getRandomValue() {
                return random.nextInt(MAX_BOUND) > NULL_BOUND
                        ? delegate.getRandomValue()
                        : null;
            }
        };
    }

    public static Randomizer<String> stringFromUnicodeScriptRandomizer(
            long seed,
            int minLength,
            int maxLength,
            Character.UnicodeScript... scriptsArg
    ) {
        return new Randomizer<>() {
            private final EnhancedRandom random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
                    .seed(seed)
                    .build();

            private final Map<Character.UnicodeScript, List<Integer>> codePointByScript =
                    getUnicodeScriptCharacters(scriptsArg);

            @Override
            public String getRandomValue() {
                StringBuilder sb = new StringBuilder();
                Character.UnicodeScript randomScript = scriptsArg[random.nextInt(scriptsArg.length)];
                List<Integer> scriptCodePoints = codePointByScript.get(randomScript);
                int length = minLength + random.nextInt(maxLength - minLength);
                for (int i = 0; i < length; i++) {
                    sb.appendCodePoint(
                            scriptCodePoints.get(random.nextInt(scriptCodePoints.size()))
                    );
                }
                return sb.toString();
            }

            private Map<Character.UnicodeScript, List<Integer>> getUnicodeScriptCharacters(
                    Character.UnicodeScript... scriptsArg
            ) {
                Set<Character.UnicodeScript> scripts = Set.of(scriptsArg);
                Map<Character.UnicodeScript, List<Integer>> result = new HashMap<>();
                for (int c = Character.MIN_CODE_POINT; c < Character.MAX_CODE_POINT; c++) {
                    Character.UnicodeScript script = Character.UnicodeScript.of(c);
                    if (!scripts.contains(script)) {
                        continue;
                    }
                    if (script == Character.UnicodeScript.LATIN) {
                        //Skip non english letters and other latin trash for now
                        if (Character.UnicodeBlock.of(c) != Character.UnicodeBlock.BASIC_LATIN) {
                            continue;
                        }
                    }
                    result.computeIfAbsent(script, s -> new ArrayList<>()).add(c);
                }
                return result;
            }
        };
    }

    public static Randomizer<String> allDifferentStringRandomizer(long seed, String prefix) {
        return new Randomizer<>() {
            private final EnhancedRandom random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
                    .seed(seed)
                    .build();

            @Override
            public String getRandomValue() {
                return prefix + random.nextInt(Integer.MAX_VALUE);
            }
        };
    }

    public static Randomizer<String> allDifferentStringRandomizer(long seed) {
        return new Randomizer<>() {
            private final EnhancedRandom random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
                    .seed(seed)
                    .build();

            @Override
            public String getRandomValue() {
                return random.nextObject(String.class) + "_" + random.nextLong();
            }
        };
    }

    @SafeVarargs
    public static <T> Randomizer<List<T>> randomValuesRandomizer(long seed, int min, int max, T... values) {
        return new Randomizer<>() {
            private final EnhancedRandom random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
                    .seed(seed)
                    .build();

            @Override
            public List<T> getRandomValue() {
                List<T> seed = List.of(values);
                List<T> result = new ArrayList<>();
                int randomSizePart = max > min ? random.nextInt(max - min) : 0;
                int size = min + randomSizePart;
                while (result.size() < size) {
                    result.add(randomItem(random, seed));
                }
                return List.copyOf(result);
            }
        };
    }

    public static <T> Randomizer<List<T>> randomSizeRandomizer(long seed,
                                                               int min,
                                                               int max,
                                                               Randomizer<T> elementRandomizer
    ) {
        return new Randomizer<>() {
            private final EnhancedRandom random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
                    .seed(seed)
                    .build();

            @Override
            public List<T> getRandomValue() {
                return IntStream.range(0, min + random.nextInt(max - min))
                        .mapToObj(i -> elementRandomizer.getRandomValue())
                        .collect(Collectors.toList());
            }
        };
    }

    public static <T> Randomizer<List<T>> allDifferentElementsRandomizer(
            long seed,
            Class<T> clazz,
            int minSize,
            int maxSize,
            Comparator<T> comparator
    ) {
        return new Randomizer<>() {
            private final EnhancedRandom random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
                    .seed(seed)
                    .build();

            @Override
            public List<T> getRandomValue() {
                TreeSet<T> result = new TreeSet<>(comparator);
                int randomSizePart = maxSize > minSize ? random.nextInt(maxSize - minSize) : 0;
                int size = minSize + randomSizePart;
                while (result.size() < size) {
                    result.add(random.nextObject(clazz));
                }
                return List.copyOf(result);
            }
        };
    }

    public static Randomizer<String> lowercaseStringRandomizer(long seed) {
        return new Randomizer<>() {
            private final EnhancedRandom random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
                    .seed(seed)
                    .build();

            @Override
            public String getRandomValue() {
                return random.nextObject(String.class).toLowerCase() + "_" + random.nextLong();
            }
        };
    }

    public static Field getDeclaredField(Class<?> clazz, String name) {
        try {
            return clazz.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static class EnhancedRandomEnhancedBuilder extends EnhancedRandomBuilder {
        public EnhancedRandomEnhancedBuilder apply(
                long seed,
                BiConsumer<Long, EnhancedRandomEnhancedBuilder> enhancer
        ) {
            enhancer.accept(seed, this);
            return this;
        }
    }

    public static <T> T randomItem(Random random, List<T> items) {
        return items.get(random.nextInt(items.size()));
    }
    public static <T> T someNotExistingValue(EnhancedRandom random, Class<T> clazz, Set<T> existingValues) {
        T result = random.nextObject(clazz);
        while (existingValues.contains(result)) {
            result = random.nextObject(clazz);
        }
        return result;
    }
    public static long seed(Class<?> clazz) {
        return clazz.getName().hashCode();
    }
}
