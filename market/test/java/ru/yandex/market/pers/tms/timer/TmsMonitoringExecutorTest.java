package ru.yandex.market.pers.tms.timer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.time.DateUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.grade.client.model.ModState;
import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.model.core.ShopGrade;
import ru.yandex.market.pers.grade.core.moderation.GradeModeratorModificationProxy;
import ru.yandex.market.pers.tms.MockedPersTmsTest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.pers.tms.timer.TmsMonitoringExecutor.LOST_IN_SAAS_GRADE_CREATE_INTERVAL_DAYS_DEFAULT;
import static ru.yandex.market.pers.tms.timer.TmsMonitoringExecutor.LOST_IN_SAAS_GRADE_MOD_INTERVAL_HOURS_DEFAULT;
import static ru.yandex.market.pers.tms.timer.TmsMonitoringExecutor.OLD_GRADE_MODERATION_THRESHOLD_DEFAULT;

public class TmsMonitoringExecutorTest extends MockedPersTmsTest {
    @Autowired
    private GradeCreator gradeCreator;

    @Autowired
    private TmsMonitoringExecutor tmsMonitoringExecutor;

    @Autowired
    private GradeModeratorModificationProxy gradeModeratorService;

    private long authorId = 0;

    @Test
    public void testOldGradeModeration() {
        StringBuilder stringBuilder = new StringBuilder();
        tmsMonitoringExecutor.checkOldGradeOnModeration(stringBuilder);
        assertTrue(stringBuilder.toString().isEmpty());

        // old grades, but newer moderated - should fail
        List<Long> badGrades = createGrades();
        tmsMonitoringExecutor.checkOldGradeOnModeration(stringBuilder);
        assertOldGradesDetected(stringBuilder, badGrades);
        stringBuilder.setLength(0); //dirty cleanup

        // moderate them
        pgJdbcTemplate.batchUpdate("insert into MOD_GRADE_LAST(GRADE_ID, MODERATOR_ID, MOD_TIME, MOD_STATE, MOD_REASON) " +
            "values (?, 1, now(), 1, null)",
            badGrades,
            badGrades.size(),
            (ps, argument) -> ps.setLong(1, argument));

        // now ok
        tmsMonitoringExecutor.checkOldGradeOnModeration(stringBuilder);
        assertTrue(stringBuilder.toString().isEmpty());

        // pretend they were moderated long ago
        pgJdbcTemplate.batchUpdate("update MOD_GRADE_LAST\n " +
                "set mod_time = now() - make_interval(days := ? + 1) \n" +
                "where grade_id = ?",
            badGrades,
            badGrades.size(),
            (ps, argument) -> {
                ps.setInt(1, OLD_GRADE_MODERATION_THRESHOLD_DEFAULT);
                ps.setLong(2, argument);
            });

        // boom, failed again!
        tmsMonitoringExecutor.checkOldGradeOnModeration(stringBuilder);
        assertOldGradesDetected(stringBuilder, badGrades);
    }

    private void assertOldGradesDetected(StringBuilder stringBuilder, List<Long> badGrades) {
        String monitoringMessage = stringBuilder.toString();
        for (Long gradeId : badGrades) {
            assertTrue(monitoringMessage.contains(String.valueOf(gradeId)));
        }
    }

    @Test
    public void testLostInSaasGrades() {

        StringBuilder stringBuilder = new StringBuilder();
        tmsMonitoringExecutor.checkLostInSaasGrades(stringBuilder);
        assertTrue(stringBuilder.toString().isEmpty());

        final long notLostGradeId1 = createGrade(new Date(), ModState.APPROVED);
        pgJdbcTemplate.update("insert into saas_indexing(grade_id, mod_time, index_time) " +
            "values (?, now() - make_interval(hours=>50), now() - make_interval(hours=>48))", notLostGradeId1);
        final long notLostGradeId2 = createGrade(DateUtils.addDays(new Date(), -LOST_IN_SAAS_GRADE_CREATE_INTERVAL_DAYS_DEFAULT - 10), ModState.APPROVED);
        pgJdbcTemplate.update("insert into saas_indexing(grade_id, mod_time, index_time) " +
            "values (?, now() - make_interval(hours=>50), now() - make_interval(hours=>48))", notLostGradeId2);
        final long lostGradeId = createGrade(new Date(), ModState.APPROVED);
        pgJdbcTemplate.update("insert into saas_indexing(grade_id, mod_time, index_time) " +
            "values (?, now() - make_interval(days=>1), null)", lostGradeId);

        tmsMonitoringExecutor.checkLostInSaasGrades(stringBuilder);
        String monitoringMessage = stringBuilder.toString();
        assertTrue(monitoringMessage.contains(String.valueOf(lostGradeId)));
        assertFalse(monitoringMessage.contains(String.valueOf(notLostGradeId1)));
        assertFalse(monitoringMessage.contains(String.valueOf(notLostGradeId2)));

    }

    @Test
    public void testLostInIndexingGrades() {
        StringBuilder stringBuilder = new StringBuilder();
        tmsMonitoringExecutor.checkLostInIndexingGrades(stringBuilder);
        assertTrue(stringBuilder.toString().isEmpty());

        final long notLostGradeId = createGrade(new Date(), ModState.APPROVED);
        pgJdbcTemplate.update("insert into saas_indexing(grade_id, mod_time, index_time) " +
                "values (?, now() - make_interval(hours=>?+1), now() - make_interval(hours=>?) / 2)", notLostGradeId,
            LOST_IN_SAAS_GRADE_MOD_INTERVAL_HOURS_DEFAULT, LOST_IN_SAAS_GRADE_CREATE_INTERVAL_DAYS_DEFAULT);
        final long lostGradeId = createGrade(new Date(), ModState.APPROVED);

        tmsMonitoringExecutor.checkLostInIndexingGrades(stringBuilder);
        String monitoringMessage = stringBuilder.toString();
        assertTrue(monitoringMessage.contains(String.valueOf(lostGradeId)));
        assertFalse(monitoringMessage.contains(String.valueOf(notLostGradeId)));
    }

    private List<Long> createGrades() {
        //good
        createGrade(new Date(), ModState.READY);
        createGrade(DateUtils.addDays(new Date(), -1), ModState.READY);
        //bad
        List<Long> badGrades = new ArrayList<>();
        badGrades.add(createGrade(DateUtils.addDays(new Date(), -OLD_GRADE_MODERATION_THRESHOLD_DEFAULT - 1), ModState.READY));
        badGrades.add(createGrade(DateUtils.addDays(new Date(), -OLD_GRADE_MODERATION_THRESHOLD_DEFAULT - 2), ModState.READY));
        return badGrades;
    }

    private long createGrade(Date date, ModState modState) {
        ShopGrade grade = GradeCreator.constructShopGrade(1L, authorId++);
        grade.setModState(modState);
        grade.setCreated(date);
        grade.setAverageGrade(4);
        return gradeCreator.createGrade(grade);
    }
}
