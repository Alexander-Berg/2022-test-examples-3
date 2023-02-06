package ru.yandex.market.pers.tms.timer;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.db.DbUtil;
import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.application.monitoring.MonitoringStatus;
import ru.yandex.market.pers.grade.client.model.ModState;
import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.db.DbGradeAdminService;
import ru.yandex.market.pers.grade.core.model.core.ModReason;
import ru.yandex.market.pers.tms.MockedPersTmsTest;
import ru.yandex.market.pers.tms.moderation.ModelGradesAutomoderation;
import ru.yandex.market.pers.tms.timer.moderation.UninformativeGradesRemoderationExecutor;
import ru.yandex.market.util.db.ConfigurationService;

import static org.junit.Assert.assertEquals;
import static ru.yandex.market.pers.tms.moderation.UninformativeGradesRemoderationService.UNINFORMATIVE_GRADES_REMODERATION_MESSAGE;
import static ru.yandex.market.pers.tms.moderation.UninformativeGradesRemoderationService.UNINFORMATIVE_GRADES_REMODERATION_NAME;
import static ru.yandex.market.pers.tms.moderation.UninformativeGradesRemoderationService.UNINFORMATIVE_GRADE_ID_REMODERATION_FROM_KEY;
import static ru.yandex.market.pers.tms.moderation.UninformativeGradesRemoderationService.UNINFORMATIVE_GRADE_ID_REMODERATION_TO_KEY;
import static ru.yandex.market.pers.tms.moderation.UninformativeGradesRemoderationService.UNINFORMATIVE_REMODERATION_BATCH_SIZE_KEY;
import static ru.yandex.market.pers.tms.moderation.UninformativeGradesRemoderationService.UNINFORMATIVE_REMODERATION_IS_ACTIVE_KEY;

/**
 * @author Damir Shagaev / damir-vvlpx@ / 24.03.2020
 */
public class UninformativeGradesRemoderationExecutorTest extends MockedPersTmsTest {

    private static final long MODEL_ID = 1;
    private static final long USER_ID = 10231;

    @Autowired
    UninformativeGradesRemoderationExecutor executor;

    @Autowired
    ModelGradesAutomoderation automoderation;

    @Autowired
    private GradeCreator gradeCreator;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private DbGradeAdminService gradeAdminService;

    @Autowired
    private ComplexMonitoring complexMonitoring;

    @Test
    public void testUpdateGradeIdsBounds() throws Exception {
        int gradesCount = 5;
        configurationService.mergeValue(UNINFORMATIVE_REMODERATION_IS_ACTIVE_KEY, String.valueOf(true));
        configurationService.mergeValue(UNINFORMATIVE_REMODERATION_BATCH_SIZE_KEY, 3L);
        List<Long> gradeIds = createGradesAndReturnIds(gradesCount);
        setGradeIdsBounds(gradeIds.get(0), gradesCount);

        gradeIds.forEach(id ->
                addUninformativeGrades(id, id % 2 == 0 ? ModState.AUTOMATICALLY_REJECTED : ModState.REJECTED));

        executor.runTmsJob();

        assertEquals(gradeIds.get(2), configurationService.getValue(UNINFORMATIVE_GRADE_ID_REMODERATION_TO_KEY,
                Long.class));
    }

    @Test
    public void testUpdateGradeModState() throws Exception {
        configurationService.mergeValue(UNINFORMATIVE_REMODERATION_IS_ACTIVE_KEY, String.valueOf(true));
        int gradesCount = 1;
        List<Long> gradeIds = createGradesAndReturnIds(gradesCount);
        setGradeIdsBounds(gradeIds.get(0), gradesCount);
        gradeIds.forEach(id ->
                addUninformativeGrades(id, id % 2 == 0 ? ModState.AUTOMATICALLY_REJECTED : ModState.REJECTED));

        executor.runTmsJob();

        assertEquals(ModState.UNMODERATED.value(), pgJdbcTemplate
                .queryForObject("SELECT mod_state FROM grade WHERE id = " + gradeIds.get(0), Integer.class).intValue());
    }

    @Test
    public void testUpdateGradeIdsBoundsWithEmptyRemoderation() throws Exception {
        configurationService.mergeValue(UNINFORMATIVE_REMODERATION_IS_ACTIVE_KEY, String.valueOf(true));
        setGradeIdsBounds(-100, 10);

        executor.runTmsJob();

        assertEquals(MonitoringStatus.WARNING, complexMonitoring.getResult().getStatus());
        assertEquals("WARN {" + UNINFORMATIVE_GRADES_REMODERATION_NAME + ": "
                + UNINFORMATIVE_GRADES_REMODERATION_MESSAGE + "}", complexMonitoring.getResult().getMessage());
    }

    @Test
    public void testUpdateGradeModStateWithAutomoderation() throws Exception {
        int gradesCount = 1;
        configurationService.mergeValue(UNINFORMATIVE_REMODERATION_IS_ACTIVE_KEY, String.valueOf(true));
        List<Long> gradeIds = createGradesAndReturnIds(gradesCount);
        changeCreationTimeForAutoModeration(gradeIds);
        setGradeIdsBounds(gradeIds.get(0), gradesCount);
        addUninformativeGrades(gradeIds.get(0), gradeIds.get(0) % 2 == 0 ? ModState.AUTOMATICALLY_REJECTED : ModState.REJECTED);

        executor.runTmsJob();
        automoderation.process();

        assertEquals(ModState.READY.value(), pgJdbcTemplate.queryForObject("SELECT mod_state FROM grade WHERE id = "
                + gradeIds.get(0), Integer.class).intValue());
    }

    private void changeCreationTimeForAutoModeration(List<Long> gradeIds) {
        DbUtil.queryInList(gradeIds, (sqlBindList, list) ->
                pgJdbcTemplate.update("update grade set CR_TIME = now() - make_interval(hours := 4) where id in (" + sqlBindList + ")",
                        list.toArray())
        );
    }

    private void addUninformativeGrades(long gradeId, ModState modState) {
        gradeAdminService.moderateGradeReplies(Collections.singletonMap(gradeId, ModReason.UNINFORMATIVE.forModel()),
                DbGradeAdminService.FAKE_MODERATOR,
                modState
        );
    }

    private List<Long> createGradesAndReturnIds(int number) {
        return LongStream.range(0, number).map(i -> {
                var grade = GradeCreator.constructModelGrade(MODEL_ID, USER_ID + i);
                grade.setCpa(true); // for automoderation
                return gradeCreator.createGrade(grade);
            })
                .boxed().collect(Collectors.toList());
    }

    private void setGradeIdsBounds(long startGradeId, int gradesCount) {
        configurationService.mergeValue(UNINFORMATIVE_GRADE_ID_REMODERATION_FROM_KEY, startGradeId - 1);
        configurationService.mergeValue(UNINFORMATIVE_GRADE_ID_REMODERATION_TO_KEY, startGradeId + gradesCount + 1);
    }
}
