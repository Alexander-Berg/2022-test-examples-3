package ru.yandex.market.b2b.clients;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class Randoms {

    private static ThreadLocalRandom random() {
        return ThreadLocalRandom.current();
    }

    public static String string() {
        return UUID.randomUUID().toString();
    }

    public static BigDecimal bigDecimal() {
        return new BigDecimal(ThreadLocalRandom.current().nextLong());
    }

    public static BigDecimal bigDecimal(long max, int decimal) {
        return BigDecimal.valueOf(ThreadLocalRandom.current().nextLong(max * (int)Math.pow(10, decimal)), decimal);
    }

    public static OffsetDateTime offsetDateTime() {
        return OffsetDateTime.now().minusSeconds(ThreadLocalRandom.current().nextInt(-10000, 10000));
    }

    public static <T extends Enum> T enumConstant(Class<T> cls) {
        T[] values = cls.getEnumConstants();
        return values[ThreadLocalRandom.current().nextInt(values.length)];
    }

    public static String stringNumber() {
        return String.valueOf(longValue());
    }

    public static long longValue() {
        return random().nextLong();
    }
}
