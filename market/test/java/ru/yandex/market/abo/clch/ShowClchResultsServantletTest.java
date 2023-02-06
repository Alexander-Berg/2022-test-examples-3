package ru.yandex.market.abo.clch;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.Test;

import ru.yandex.market.abo.clch.model.CheckerType;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author artemmz
 * @date 14/08/2020.
 */
class ShowClchResultsServantletTest {
    private static final String SEP = ",";
    private static final CheckerType CHECKER_TYPE = CheckerType.PHONE;

    private static final String SAME_1 = "(495)1500171(м) (Рег.проверка)";
    private static final String SAME_2 = "(495)1500171(к) (СПАРК)";
    private static final Set<String> OTHER_1 = Set.of("9872290886(м) (Рег.проверка)", "002509860(м) (Собранные_данные)");

    private static final String CSV_1 = StreamEx.of(OTHER_1).append(SAME_1).collect(Collectors.joining(SEP + " ", "[", "]"));
    private static final String CSV_2 = "[(495)1500151(м) (СПАРК), " + SAME_2 + ", 9913005487(м) (СПАРК), 9913073093(м) (СПАРК)]";

    @Test
    void findSame() {
        String same1 = ShowClchResultsServantlet.findDiff(CSV_1, CSV_2, SEP, CHECKER_TYPE, Sets::intersection);
        String same2 = ShowClchResultsServantlet.findDiff(CSV_2, CSV_1, SEP, CHECKER_TYPE, Sets::intersection);
        assertEquals(SAME_1, same1);
        assertEquals(SAME_2, same2);
    }

    @Test
    void findLeftDiff() {
        String leftDiff = ShowClchResultsServantlet.findDiff(CSV_1, CSV_2, SEP, CHECKER_TYPE, Sets::difference);
        assertEquals(OTHER_1, Arrays.stream(leftDiff.split(SEP)).map(String::trim).collect(Collectors.toSet()));
    }
}