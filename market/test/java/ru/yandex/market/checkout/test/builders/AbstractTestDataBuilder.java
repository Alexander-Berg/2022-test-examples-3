package ru.yandex.market.checkout.test.builders;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;

import static org.apache.commons.lang3.RandomUtils.nextInt;
import static org.apache.commons.lang3.RandomUtils.nextLong;

/**
 * @author Nicolai Iusiumbeli <armor@yandex-team.ru>
 * date: 27/01/2017
 */
public abstract class AbstractTestDataBuilder<T> {

    private static final int STRING_LENGTH = 50;

    public abstract T build();

    public List<T> build(long count) {
        List<T> list = new ArrayList<T>();
        for (int i = 0; i < count; i++) {
            list.add(build());
        }
        return list;
    }

    protected String randomIfNull(String val) {
        if (val == null) {
            return
                    RandomStringUtils.randomAlphanumeric(STRING_LENGTH / 2) + "\n\t;" +
                            RandomStringUtils.randomAlphanumeric(STRING_LENGTH / 2) + "\n";
        }
        return val;
    }

    protected Long randomIfNull(Long val) {
        if (val == null) {
            return nextLong(0, 10000);
        }
        return val;
    }

    protected Date randomIfNull(Date val) {
        if (val == null) {
            return new Date(Math.abs(
                    System.currentTimeMillis() +
                            nextLong(0, 100000000) * (Integer.signum(nextInt(0, 21) - 10))
            ));
        }
        return val;
    }


}
