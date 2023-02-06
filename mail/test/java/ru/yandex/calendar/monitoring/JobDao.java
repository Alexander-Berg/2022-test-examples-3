package ru.yandex.calendar.monitoring;

import lombok.val;

import ru.yandex.calendar.util.db.CalendarJdbcDaoSupport;
import ru.yandex.commune.test.random.RunWithRandomTest;

public class JobDao extends CalendarJdbcDaoSupport {
    @RunWithRandomTest
    public int getFailedJobCount() {
        val q = "select count(*) from job where status in ('ready', 'starting', 'running') and attempt > 10";
        return getJdbcTemplate().queryForInt(q);
    }
}
