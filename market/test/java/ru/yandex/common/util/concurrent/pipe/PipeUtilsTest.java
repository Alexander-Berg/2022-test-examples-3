package ru.yandex.common.util.concurrent.pipe;

import junit.framework.TestCase;
import ru.yandex.common.util.concurrent.Executors;
import ru.yandex.common.util.concurrent.StoppableExecutorService;
import ru.yandex.common.util.functional.Filter;
import ru.yandex.common.util.functional.Function;

import java.util.ArrayList;
import java.util.List;

/**
 * Date: Sep 27, 2010
 * Time: 1:42:39 PM
 *
 * @author Timur Abishev (ttim@yandex-team.ru)
 * @author Yuri Zemlyanskiy (urikz@yandex-team.ru)
 */
public class PipeUtilsTest extends TestCase {
    private static final Function<Integer, List<Integer>> VALUE_TO_RANGE_FUNCTION = new Function<Integer, List<Integer>>() {
        @Override
        public List<Integer> apply(Integer arg) {
            ArrayList<Integer> result = new ArrayList<Integer>();

            for (int value = arg * 100; value < arg * 100 + 100; value++) {
                result.add(value);
            }

            return result;
        }
    };

    private static final Filter<Integer> IS_PRIME_FILTER = new Filter<Integer>() {
        @Override
        public boolean fits(Integer arg) {
            if (arg <= 1) {
                return false;
            }

            for (int i = 2; i < arg; i++) {
                if (arg % i == 0) {
                    return false;
                }
            }

            return true;
        }
    };

    private static int countPrimes(int from, int to) {
        if (to < from || to < 2) {
            return 0;
        }

        int result = 0, primeAmount = 0;
        int[] smallestDivisor = new int[to + 1];
        int[] primes = new int[to + 1];

        for (int i = 0; i <= to; i++) {
            smallestDivisor[i] = -1;
        }
        for (int i = 2; i <= to; i++) {
            if (smallestDivisor[i] == -1) {
                smallestDivisor[i] = i;
                primes[primeAmount++] = i;

                result += (i >= from) ? 1 : 0;
            }
            for (int j = 0; (j <= smallestDivisor[i]) && ((long) i * primes[j]) <= to; j++) {
                smallestDivisor[i * primes[j]] = j;
            }
        }

        return result;
    }

    public void testAlternativeMapExecutor() throws InterruptedException {
        StoppableExecutorService executor = Executors.newFixedThreadPool(100);

        List<Integer> result =
                MapExecutors.ALTERNATIVE
                        .from(1, 2, 3, 4, 5, 6, 7, 8, 9)
                        .mapMultiply(VALUE_TO_RANGE_FUNCTION)
                        .filter(IS_PRIME_FILTER)
                        .filter(IS_PRIME_FILTER)
                        .calculate(executor);

        executor.awaitCompletionAndStop();
        assertEquals(countPrimes(100, 999), result.size());
    }

}
    