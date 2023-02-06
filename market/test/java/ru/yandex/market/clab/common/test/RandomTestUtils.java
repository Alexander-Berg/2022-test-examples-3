package ru.yandex.market.clab.common.test;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.api.Randomizer;
import io.github.benas.randombeans.randomizers.range.DoubleRangeRandomizer;
import io.github.benas.randombeans.randomizers.range.IntegerRangeRandomizer;
import io.github.benas.randombeans.randomizers.range.LongRangeRandomizer;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Random;

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

    private static final Random RANDOM_SEED = new Random(SEED);

    private static final EnhancedRandom RANDOM = createDefaultBuilder()
        .randomize(LocalDateTime.class, new LocalDateTimeRandomizer())
        .build();

    private RandomTestUtils() { }

    private static EnhancedRandomBuilder createDefaultBuilder() {
        return EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
            .seed(RANDOM_SEED.nextLong())
            .stringLengthRange(MIN_STRING_LENGTH, MAX_STRING_LENGTH)
            .collectionSizeRange(MIN_COLLECTION_SIZE, MAX_COLLECTION_SIZE)
            .randomize(Long.class, new LongRangeRandomizer(MIN_LONG, MAX_LONG, RANDOM_SEED.nextLong()))
            .randomize(Integer.class, new IntegerRangeRandomizer(MIN_INTEGER, MAX_INTEGER, RANDOM_SEED.nextLong()))
            .randomize(Double.class, new DoubleRangeRandomizer(MIN_DOUBLE, MAX_DOUBLE, RANDOM_SEED.nextLong()));
    }

    public static byte[] randomBytes() {
        return randomBytes(20);
    }

    public static byte[] randomBytes(int length) {
        byte[] result = new byte[length];
        RANDOM.nextBytes(result);
        return result;
    }

    public static String randomString() {
        return RANDOM.nextObject(String.class);
    }

    public static Long randomLong() {
        return RANDOM.nextObject(Long.class);
    }

    public static <T> T randomObject(Class<T> clazz, String... ignore) {
        return RANDOM.nextObject(clazz, ignore);
    }

    static class LocalDateTimeRandomizer implements Randomizer<LocalDateTime> {

        private final EnhancedRandom random = createDefaultBuilder().build();

        @Override
        public LocalDateTime getRandomValue() {
            LocalDateTime dateTime = random.nextObject(LocalDateTime.class);
            dateTime.truncatedTo(ChronoUnit.MILLIS);
            return dateTime;
        }
    }
}
