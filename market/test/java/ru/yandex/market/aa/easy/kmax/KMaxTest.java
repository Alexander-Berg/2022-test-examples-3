package ru.yandex.market.aa.easy.kmax;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.aa.util.AATest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * @author antipov93.
 */
abstract class KMaxTest extends AATest {

    private static final Random RANDOM = new Random(0);

    @ParameterizedTest(name = "elements: {0}, k: {1}")
    @MethodSource("simpleTestCases")
    void simpleTests(List<Integer> elements, int k, Collection<Integer> expected) {
        KMax solution = solution();
        var answer = assertTimeoutPreemptively(timeLimit(), () -> solution.kMax(elements, k));
        validate(expected, answer);
    }

    @ParameterizedTest(name = "{index}")
    @MethodSource("randomTestCases")
    void randomTests(List<Integer> elements, int k) {
        var solution = solution();
        var baseSolution = new KMaxSort();
        var baseSolutionAnswer = baseSolution.kMax(elements, k);
        var answer = assertTimeout(timeLimit(), () -> solution.kMax(elements, k));
        validate(baseSolutionAnswer, answer);
    }

    @Test
    @DisplayName("Throw exception when elements is null")
    void elementsIsNull() {
        KMax solution = solution();
        assertThrows(Throwable.class, () -> solution.kMax(null, 0));
    }

    protected abstract KMax solution();

    private static void validate(Collection<Integer> expected, Collection<Integer> answer) {
        assertNotNull(answer);
        assertEquals(expected.size(), answer.size());
        assertEquals(countsMap(expected), countsMap(answer));
    }

    private static Map<Integer, Long> countsMap(Collection<Integer> elements) {
        return StreamEx.of(elements).groupingBy(Function.identity(), Collectors.counting());
    }

    private static StreamEx<Arguments> simpleTestCases() {
        return StreamEx.of(
                Arguments.of(List.of(1, 2, 3, 4), 3, List.of(4, 3, 2)),
                Arguments.of(List.of(1, 2, 3, 4), 4, List.of(4, 3, 2, 1)),
                Arguments.of(List.of(1, 1, 1, 1), 1, List.of(1)),
                Arguments.of(List.of(1), 0, List.of()),
                Arguments.of(List.of(1, 1, 1, 1, 2, 2), 4, List.of(2, 2, 1, 1)),
                Arguments.of(List.of(1, 5, 6, 6, 6, 7, 7, 5), 5, List.of(7, 7, 6, 6, 6)),
                Arguments.of(List.of(1, 5, 6, 6, 6, 7, 7, 5), 6, List.of(7, 7, 6, 6, 6, 5)),
                Arguments.of(List.of(), 10, List.of()),
                Arguments.of(List.of(1, 2, 3), 5, List.of(1, 2, 3)),
                Arguments.of(List.of(), 0, List.of())
        );
    }

    private static StreamEx<Arguments> randomTestCases() {
        return StreamEx.of(
                randomTestCase(5, 3, 1, 1),
                randomTestCaseSmall(5, 5),
                randomTestCaseSmall(10, 5),
                randomTestCaseSmall(10, 10),
                randomTestCaseSmall(10, 9),
                randomTestCaseSmall(10, 1),
                randomTestCase(10, 4, -1, 1),
                randomTestCaseBig(1_000_000, 1_000_000),
                randomTestCaseBig(1_000_000, 1_000),
                randomTestCaseBig(1_000_000, 900_000),
                randomTestCaseBig(1_000_000, 500_000),
                randomTestCaseBig(1_000_000, 1),
                randomTestCase(1_000_000, 500_000, 1, 1),
                randomTestCase(1_000_000, 600_000, 0, 10)
        );
    }

    private static Arguments randomTestCaseSmall(int size, int k) {
        return randomTestCase(size, k, -10, 10);
    }

    private static Arguments randomTestCaseBig(int size, int k) {
        return randomTestCase(size, k, -1_000_000, 1_000_000);
    }

    private static Arguments randomTestCase(int size, int k, int min, int max) {
        var elements = IntStreamEx.range(size).map(i -> nextInt(min, max)).boxed().toList();
        return Arguments.of(elements, k);
    }

    private static int nextInt(int min, int max) {
        return min + RANDOM.nextInt(max - min + 1);
    }
}