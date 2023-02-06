package ru.yandex.market.pers.tms.timer;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.moderation.AutoModResultService;
import ru.yandex.market.pers.grade.core.moderation.FilterType;
import ru.yandex.market.pers.tms.MockedPersTmsTest;
import ru.yandex.market.pers.tms.quartz.MonitorableVerboseExecutor;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 23.08.2021
 */
public class JobsConfigTest extends MockedPersTmsTest {

    @Autowired
    private GradeCreator gradeCreator;

    @Autowired
    private AutoModResultService autoModResultService;

    @Autowired
    @Qualifier("dbCleanupExecutor")
    private MonitorableVerboseExecutor cleanupExecutor;

    @Test
    public void testCleanup() throws Exception {
        long gradeId = gradeCreator.createModelGrade(1L, 1L);
        long gradeIdOld = gradeCreator.createModelGrade(2L, 1L);

        autoModResultService.saveFailedFilterDescriptions(gradeId, "name", FilterType.NEGATIVE);
        autoModResultService.saveFailedFilterDescriptions(gradeIdOld, "name", FilterType.NEGATIVE);

        pgJdbcTemplate.update("update grade set cr_time = now() - interval '2' year where id = ?", gradeId);

        pgJdbcTemplate.update("update grade " +
            "set cr_time = now() - interval '3' year - interval '1' day where id = ?", gradeIdOld);
        pgJdbcTemplate.update("update auto_mod_result " +
                "set update_time = now() - interval '3' year - interval '1' day where grade_id = ?", gradeIdOld);

        cleanupExecutor.runTmsJob();

        Assert.assertEquals(1,
            pgJdbcTemplate.queryForObject("select count(*) from auto_mod_result", Integer.class).intValue());

        Assert.assertEquals(1,
            pgJdbcTemplate.queryForObject("select count(*) from security_data", Integer.class).intValue());
    }
}
