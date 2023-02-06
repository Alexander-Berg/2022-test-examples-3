package ru.yandex.market.abo.clch.checker;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.abo.clch.ClchTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Olga Bolshakova (obolshakova@yandex-team.ru)
 * @date 03.04.2008
 */
public class FeedFileCheckerTest extends ClchTest {
    private static final long SHOP_1 = 1;
    private static final long SHOP_2 = 2;

    @Autowired
    @Qualifier("feedFileChecker")
    private Checker feedFileChecker;
    @Autowired
    private JdbcTemplate pgJdbcTemplate;

    @Test
    public void testCompare() {
        createFeed(SHOP_1, 10, "/a");
        createFeed(SHOP_1, 11, "/b");
        createFeed(SHOP_2, 20, "/a");

        feedFileChecker.configure(new CheckerDescriptor(0, "testChecker"));
        CheckerResult result = feedFileChecker.checkShops(SHOP_1, SHOP_2);
        assertEquals(1., result.getResult());
        assertEquals("[/a, /b]", result.getValue1());
        assertEquals("[/a]", result.getValue2());
    }

    private void createFeed(long shopId, long feedId, String tail) {
        pgJdbcTemplate.update("" +
                        "INSERT INTO ext_shop_feeds VALUES " +
                        "(?, ?, NULL, NULL, NULL,?, NULL)",
                shopId, feedId, tail);
    }
}
