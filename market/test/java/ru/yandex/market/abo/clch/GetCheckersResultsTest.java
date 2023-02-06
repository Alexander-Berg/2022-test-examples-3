package ru.yandex.market.abo.clch;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.abo.clch.checker.CheckerResult;

/**
 * @author kukabara
 */
public class GetCheckersResultsTest extends ClchTest {

    @Autowired
    private CheckerStarter checkerStarter;

    @Test
    public void test() {
        Long sessionId = 121632614L;
        List<CheckerResult> results = checkerStarter.getCheckerResults(
                new HashSet<>(Arrays.asList(19057L, 130687L)), sessionId);

        if (results.size() == 0) {
            System.out.println("NO results");
        }
        for (CheckerResult r : results) {
            System.out.println(r.getCheckerId() + " " + r.toString());
        }
    }
}
