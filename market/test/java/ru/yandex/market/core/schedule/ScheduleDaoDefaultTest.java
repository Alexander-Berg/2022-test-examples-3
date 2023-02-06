package ru.yandex.market.core.schedule;

import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Тест расписаний по умолчанию.
 *
 * @author Vadim Lyalin
 */
public class ScheduleDaoDefaultTest {

    private JdbcTemplate jdbcTemplate;

    private ScheduleDao scheduleDao;

    @BeforeEach
    public void setUp() throws Exception {
        jdbcTemplate = mock(JdbcTemplate.class);

        scheduleDao = new ScheduleDao(jdbcTemplate, null, null);
    }

    @Test
    public void getScheduleOrDefault_Default() {
        long scheduleId = 1L;
        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(scheduleId)))
                .thenReturn(Collections.emptyList());

        Schedule expectedSchedule = scheduleDao.getDefaultSchedule(scheduleId);
        Schedule actualSchedule = scheduleDao.getScheduleOrDefault(scheduleId);
        Assertions.assertEquals(expectedSchedule, actualSchedule);
    }

    @Test
    public void getScheduleOrDefault_Schedule() {
        long scheduleId = 1L;
        Schedule expectedSchedule = new Schedule(scheduleId, Collections.emptyList());
        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(scheduleId)))
                .thenReturn(Collections.singletonList(expectedSchedule));

        Schedule actualSchedule = scheduleDao.getScheduleOrDefault(scheduleId);
        Assertions.assertEquals(expectedSchedule, actualSchedule);
    }
}
