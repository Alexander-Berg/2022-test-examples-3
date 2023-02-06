package ru.yandex.direct.core.entity.changes.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static org.junit.Assert.assertThat;
import static ru.yandex.direct.dbschema.ppcdict.tables.StatRollbacks.STAT_ROLLBACKS;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class StatRollbacksRepositoryTest {

    @Autowired
    private StatRollbacksRepository statRollbacksRepository;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Test
    public void testStatRollbacksRepository() {
        long orderId = 1L;
        LocalDateTime rollbacktime = LocalDateTime.of(2000, 1, 2, 3, 4, 5);
        LocalDate borderDate = LocalDate.of(2000, 1, 1);
        String hosts = "test_host";

        dslContextProvider.ppcdict()
                .insertInto(STAT_ROLLBACKS)
                .columns(STAT_ROLLBACKS.ORDER_ID,
                        STAT_ROLLBACKS.ROLLBACK_TIME,
                        STAT_ROLLBACKS.BORDER_DATE,
                        STAT_ROLLBACKS.HOSTS)
                .values(orderId, rollbacktime, borderDate, hosts)
                .execute();

        LocalDateTime fromTime = LocalDateTime.of(1999, 5, 5, 5, 5, 5);

        List<Long> got = statRollbacksRepository.getChangedOrderIds(List.of(1L, 2L, 3L), fromTime);

        assertThat(got.size(), Matchers.is(1));
        assertThat(got.get(0).longValue(), Matchers.is(1L));
    }
}
