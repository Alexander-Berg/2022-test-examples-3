package ru.yandex.market.antifraud.yql.clean;

import java.time.LocalDate;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexander Novikov <a href="mailto:hronos@yandex-team.ru"></a>
 * @date 23.12.2019
 */
public class YqlCleanerTest {

    @Test
    public void shouldBeDeleted(){
        int keepdays = 5;
        LocalDate today = LocalDate.now();
        LocalDate t4dAgo = today.minusDays(4);
        LocalDate t5dAgo = today.minusDays(5);
        LocalDate t6dAgo = today.minusDays(6);
        assertFalse(YqlCleaner.shouldBeDeleted(today, t4dAgo, keepdays));
        assertFalse(YqlCleaner.shouldBeDeleted(today, t5dAgo, keepdays));
        assertTrue(YqlCleaner.shouldBeDeleted(today, t6dAgo, keepdays));
    }
}
