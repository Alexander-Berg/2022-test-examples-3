package ru.yandex.market.abo.clch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;

import com.google.common.math.IntMath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.clch.checker.Checker;
import ru.yandex.market.abo.clch.checker.CheckerResult;
import ru.yandex.market.abo.test.TestHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * @date 27.11.17.
 */
public class CheckerStarterTest {
    private static final Random RND = new Random();
    private static final long SESSION_ID = 0;
    private static final Set<Long> SHOPS = new HashSet<>(Arrays.asList(1L, 2L, 3L));

    @InjectMocks
    private CheckerStarter checkerStarter;
    @Mock
    private CheckerManager checkerManager;
    @Mock
    private Checker checker;
    @Mock
    private ExecutorService pool;

    private final List<Checker> checkers = new ArrayList<>();

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        TestHelper.mockExecutorService(pool);
        checkers.clear();
        checkers.addAll(Arrays.asList(checker, checker));

        when(checkerManager.loadCheckers(SESSION_ID)).thenReturn(checkers);
        when(checker.checkShops(anyLong(), anyLong()))
                .then(inv -> {
                    long shop1 = (long) inv.getArguments()[0];
                    long shop2 = (long) inv.getArguments()[1];
                    return new CheckerResult(
                            shop1,
                            shop2,
                            "shop value for " + shop1,
                            "shop value for " + shop2,
                            RND.nextDouble(),
                            RND.nextInt()
                    );
                });
    }

    /**
     * https://ru.wikipedia.org/wiki/Сочетание in case u bother about number of possibleComparisons
     */
    @Test
    public void getResults() throws Exception {
        List<CheckerResult> results = checkerStarter.getCheckerResults(SHOPS, SESSION_ID);
        int possibleComparisons = IntMath.factorial(SHOPS.size()) / (2 * IntMath.factorial(SHOPS.size() - 2));

        assertEquals(results.size(), checkers.size() * possibleComparisons);
        verify(checker, times(possibleComparisons * checkers.size())).checkShops(anyLong(), anyLong());

        Set<Long> checkedShops = new TreeSet<>();
        results.forEach(r -> {
            checkedShops.add(r.getShopId1());
            checkedShops.add(r.getShopId2());
        });
        assertEquals(SHOPS, checkedShops);
    }

    @Test
    public void propagateException() throws Exception {
        assertThrows(RuntimeException.class, () -> {
            when(checker.checkShops(anyLong(), anyLong())).thenReturn(null).thenThrow(new TimeoutException("ups"));
            checkerStarter.getCheckerResults(SHOPS, SESSION_ID);
        });
    }
}
