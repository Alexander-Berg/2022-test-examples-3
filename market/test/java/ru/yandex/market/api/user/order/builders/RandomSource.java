package ru.yandex.market.api.user.order.builders;

import ru.yandex.market.api.common.RoomAddress;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Random;

/**
 * Источник случайных значений для билдеров. В конструктор необзодимо передать сидирующее значение для
 * повторяемости результатов.
 *
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class RandomSource {

    private static final String CHARS = "abcdefghijklmopqrstuvwxyz";
    private static final String NUMBERS = "0123456789";

    private final Random random;

    /**
     * Конструктор
     * @param seed сидирующее значение. (Обязательно для работы класса)
     */
    public RandomSource(long seed) {
        random = new Random(seed);
    }

    public int getInt() {
        return random.nextInt();
    }

    public int getInt(int to) {
        return random.nextInt(to);
    }

    public int getInt(int from, int to) {
        return from + random.nextInt(to - from);
    }

    public long getLong() {
        return random.nextLong();
    }

    public String getString() {
        return getString(CHARS, 12);
    }

    public String getString(String chars, int length) {
        StringBuilder builder = new StringBuilder();
        while (length-- != 0) {
            builder.append(chars.charAt(random.nextInt(chars.length())));
        }
        return builder.toString();
    }

    public BigDecimal getPrice(int to, int toCents) {
        BigDecimal result = BigDecimal.valueOf(random.nextInt(to + 1));
        return result.add(BigDecimal.valueOf(random.nextInt(toCents + 1) / 100.0));
    }

    public <T> T from(Collection<T> collection) {
        int size = collection.size();
        int index = random.nextInt(size);

        Iterator<T> iterator = collection.iterator();
        while (index-- != 0) {
            iterator.next();
        }
        return iterator.next();
    }

    public <T extends Enum> T from(Class<T> clazz) {
        T[] enums = clazz.getEnumConstants();
        return enums[random.nextInt(enums.length)];
    }

    public String getEmail() {
        return getString(CHARS, 15) + "@test.com";
    }

    public String getNumber() {
        return "+7" + getString(NUMBERS, 10);
    }

    public LocalDate getLocalDate() {
        return LocalDate.of(2016, from(Month.class), getInt(27) + 1);
    }

    public Date getDate() {
        return new Date(2016, random.nextInt(12), random.nextInt(27) + 1);
    }

    public RoomAddress getRoomAddress() {
        RoomAddress address = new RoomAddress();

        address.setCountry(getString());
        address.setCity(getString());
        address.setStreet(getString());
        address.setBlock(getString());
        address.setHouse(getString());
        address.setEntrance(getString());
        address.setFloor(getString());
        address.setRoom(getString());

        address.setComment(getString());
        address.setIntercom(getString());
        address.setPostCode(getString());
        address.setGeoLocation(getString());

        return address;
    }
}
