package ru.yandex.direct.utils;

import java.util.Collection;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class PassportUtilsGenerateRandomPasswordTest {
    private static final Set<Integer> ALPHANUMS = Stream.of(
            IntStream.rangeClosed('0', '9').boxed(),
            IntStream.rangeClosed('a', 'z').boxed(),
            IntStream.rangeClosed('A', 'Z').boxed())
            .flatMap(Function.identity())
            .collect(toSet());

    private final int size;

    public PassportUtilsGenerateRandomPasswordTest(int size) {
        this.size = size;
    }

    @Parameterized.Parameters
    public static Collection<?> parameters() {
        return IntStream.rangeClosed(1, 16).boxed().collect(toList());
    }

    @Test
    public void testGenerateRandom() {
        String password = PassportUtils.generateRandomPassword(size);

        assertEquals(password.length(), size);
        assertTrue(
                ALPHANUMS.containsAll(
                        password.chars()
                                .boxed()
                                .collect(toSet())));
    }
}
