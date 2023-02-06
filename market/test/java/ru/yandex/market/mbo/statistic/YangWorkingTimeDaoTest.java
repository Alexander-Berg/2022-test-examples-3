package ru.yandex.market.mbo.statistic;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.yandex.market.mbo.statistic.model.YangWorkingTime;
import ru.yandex.market.mbo.utils.BaseDbTest;

import java.sql.Timestamp;

import static org.assertj.core.api.Assertions.assertThat;

public class YangWorkingTimeDaoTest extends BaseDbTest {
    private static final String ASSIGNMENT_ID = "assignment_id";
    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate;
    private YangWorkingTimeDao yangWorkingTimeDao;

    @Before
    public void setup() {
        yangWorkingTimeDao = new YangWorkingTimeDao(jdbcTemplate);
    }

    @Test
    public void initTest() {
        yangWorkingTimeDao.initYangWorkingTime(ASSIGNMENT_ID, new Timestamp(100L));
        assertRow(ASSIGNMENT_ID, new YangWorkingTime()
            .setAssignmentId(ASSIGNMENT_ID)
            .setLastPing(new Timestamp(100L))
            .setTotalTime(0L));
    }

    @Test
    public void testNullWorkingTime() {
        YangWorkingTime yangWorkingTime = yangWorkingTimeDao.getYangWorkingTime(ASSIGNMENT_ID);
        assertThat(yangWorkingTime).isNull();
    }

    @Test
    public void testUpdate() {
        yangWorkingTimeDao.initYangWorkingTime(ASSIGNMENT_ID, new Timestamp(0L));
        YangWorkingTime lastPing = new YangWorkingTime()
                .setAssignmentId(ASSIGNMENT_ID)
                .setTotalTime(100L)
                .setLastPing(new Timestamp(100L));
        yangWorkingTimeDao.updateYangWorkingTime(lastPing);
        assertRow(ASSIGNMENT_ID, lastPing);
    }

    private void assertRow(String assignmentId, YangWorkingTime expected) {
        YangWorkingTime real = yangWorkingTimeDao.getYangWorkingTime(assignmentId);
        assertThat(real.getLastPing()).isEqualTo(expected.getLastPing());
        assertThat(real.getAssignmentId()).isEqualTo(expected.getAssignmentId());
        assertThat(real.getTotalTime()).isEqualTo(expected.getTotalTime());
    }

}
