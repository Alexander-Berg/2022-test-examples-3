package ru.yandex.autotests.market.stat.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;

/**
 * Created by entarrion on 11.07.16.
 */
public class RandomUtils {
    private static final Random RND = new Random();

    public static <T> T choice(List<T> seq) {
        return seq.get(RND.nextInt(seq.size()));
    }

    public static <T> T choice(Collection<T> seq) {
        return choice(new ArrayList<>(seq));
    }

    public static <T> T choice(T... seq) {
        return choice(Arrays.asList(seq));
    }

    public static Random getRnd() {
        return RND;
    }
}
