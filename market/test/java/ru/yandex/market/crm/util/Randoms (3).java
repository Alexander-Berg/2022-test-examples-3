package ru.yandex.market.crm.util;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.lang.StringUtils;

/**
 * Вспомогательные методы для генерации случайнх значений для тестов.
 */
public class Randoms {

    private static ThreadLocalRandom random() {
        return ThreadLocalRandom.current();
    }

    public static <T extends Enum> T enumValue(Class<T> cls) {
        T[] value = cls.getEnumConstants();
        int p = random().nextInt(value.length - 1);
        return value[p];
    }

    public static byte[] bytea() {
        return CrmStrings.getBytes(UUID.randomUUID().toString());
    }

    public static String string() {
        return UUID.randomUUID().toString();
    }

    public static String stringNumber() {
        return String.valueOf(longValue());
    }

    public static int intValue() {
        return random().nextInt();
    }

    public static int positiveIntValue() {
        return Math.abs(random().nextInt());
    }

    public static long longValue() {
        return random().nextLong();
    }

    public static long positiveLongValue() {
        return Math.abs(random().nextLong());
    }

    public static String email() {
        return string() + "@example.com";
    }

    public static String url() {
        return "http://" + hex(8) + ".com";
    }

    public static String ip() {
        return random().nextInt(255) + "." + random().nextInt(255) + "." +
                random().nextInt(255) + "." + random().nextInt(255);
    }

    public static BigDecimal bigDecimal() {
        return BigDecimal.valueOf(longValue(), 2);
    }

    public static long unsignedLongValue() {
        return random().nextLong(Long.MAX_VALUE);
    }

    public static long unsignedLongValue(long minValue) {
        return random().nextLong(minValue, Long.MAX_VALUE);
    }

    public static LocalDate date() {
        return LocalDate.now()
                .plusDays(random().nextInt(1000) - 500);
    }

    public static Duration duration() {
        return Duration.ofSeconds(random().nextLong(10000) - 5000);
    }

    public static OffsetDateTime dateTime() {
        return OffsetDateTime.now().plusSeconds(random().nextInt(100000) - 50000)
                .truncatedTo(ChronoUnit.MILLIS);
    }

    public static OffsetDateTime dateTime(ZoneId zoneId) {
        return OffsetDateTime.now(zoneId).plusSeconds(random().nextInt(100000) - 50000)
                .truncatedTo(ChronoUnit.MILLIS);
    }

    public static LocalTime time() {
        return LocalTime.ofSecondOfDay(random().nextLong(24 * 60 * 60));
    }

    public static Boolean booleanValue() {
        return random().nextBoolean();
    }

    public static String hex(int length) {
        StringBuilder sb = new StringBuilder(length);
        for(int i = 0; i < length; ++i) {
            sb.append("0123456789abcdef".charAt(random().nextInt(16)));
        }
        return sb.toString();
    }

    /**
     * @return random string in /+7\d{10}/ format, e.g. "+79123456789"
     */
    public static String phoneNumber() {
        long val = random().nextLong(10_000_000_000L);
        return "+7" + StringUtils.leftPad("" + val, 10, '0');
    }

}
