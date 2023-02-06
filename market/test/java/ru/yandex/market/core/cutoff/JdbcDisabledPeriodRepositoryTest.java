package ru.yandex.market.core.cutoff;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNull;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author stani on 26.10.17.
 */
@DbUnitDataSet(before = "JdbcDisabledPeriodRepositoryTest.before.csv")
class JdbcDisabledPeriodRepositoryTest extends FunctionalTest {

    @Autowired
    private JdbcDisabledPeriodRepository jdbcDisabledPeriodRepository;

    /**
     * Тест корректности выдачи активных периодов методами
     * {@link JdbcDisabledPeriodRepository#getActiveDisabledPeriodBeginDate(long)} и
     * {@link JdbcDisabledPeriodRepository#getActiveDisabledPeriodBeginDate(Collection)},
     * проверяются крайние случаи для пустых коллекций и не существующих магазинов.
     */
    @Test
    void getActiveDisabledPeriodBeginDate() {
        assertNotNull(jdbcDisabledPeriodRepository.getActiveDisabledPeriodBeginDate(1L));
        assertNull(jdbcDisabledPeriodRepository.getActiveDisabledPeriodBeginDate(-1L));
        assertTrue(jdbcDisabledPeriodRepository.getActiveDisabledPeriodBeginDate(emptyList()).isEmpty());
        assertTrue(jdbcDisabledPeriodRepository.getActiveDisabledPeriodBeginDate(singletonList(-1L)).isEmpty());
        assertTrue(jdbcDisabledPeriodRepository.getActiveDisabledPeriodBeginDate(singletonList(3L)).isEmpty());

        Map<Long, Instant> periods =
                jdbcDisabledPeriodRepository.getActiveDisabledPeriodBeginDate(Arrays.asList(1L, 2L, 3L, -1L));
        assertThat(periods.keySet(), containsInAnyOrder(1L, 2L));
        assertFalse(periods.containsValue(null));

    }

}
