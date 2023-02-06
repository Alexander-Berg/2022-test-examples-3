package ru.yandex.market.checker.dao;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.checker.EmptyTest;
import ru.yandex.market.checker.core.JobInfo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author imelnikov
 */
public class JobInfoDaoTest extends EmptyTest {

    @Autowired
    private JobInfoDao jobInfoDao;

    @Autowired
    private JdbcTemplate ucJdbcTemplate;

    @Test
    public void loadJob() {
        List<JobInfo> list = jobInfoDao.jobList();
        assertFalse(list.isEmpty());
        for (JobInfo job : list) {
            jobInfoDao.createCheckerDao(job);
        }
    }

    @Test
    public void testLiquibaseForDebug() {
        int count = ucJdbcTemplate.queryForObject("select count(*) from uc_job", Integer.class);
        assertTrue(count != 0);

        count = ucJdbcTemplate.queryForObject("select count(*) from uc_net_check", Integer.class);
        assertTrue(count != 0);
    }
}
