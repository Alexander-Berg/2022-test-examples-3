package ru.yandex.market.abo.clch;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.abo.clch.checker.CheckerResult;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author artemmz
 * @date 20.06.18.
 */
public class CheckerResultServiceTest extends ClchTest {
    @Autowired
    private CheckerResultService checkerResultService;

    @Test
    public void daoTest() {
        List<CheckerResult> results = checkerResultService.getResults(0L, new HashSet<>(Arrays.asList(1L, 2L, 3L)));
        assertNotNull(results);
    }
}