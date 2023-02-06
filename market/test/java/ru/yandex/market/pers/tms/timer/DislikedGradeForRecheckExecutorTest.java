package ru.yandex.market.pers.tms.timer;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.grade.client.model.ModState;
import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.db.DbGradeVoteService;
import ru.yandex.market.pers.grade.core.db.RecheckQueueService;
import ru.yandex.market.pers.grade.core.db.RecheckQueueService.GradeComplaint;
import ru.yandex.market.pers.grade.core.model.core.GradeRecheckReason;
import ru.yandex.market.pers.grade.core.model.core.ModelGrade;
import ru.yandex.market.pers.grade.core.model.vote.GradeVoteKind;
import ru.yandex.market.pers.tms.MockedPersTmsTest;
import ru.yandex.market.pers.tms.moderation.DislikedGradesRecheckService;
import ru.yandex.market.pers.tms.timer.moderation.DislikedGradesRecheckExecutor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author varvara
 * 20.05.2019
 */
public class DislikedGradeForRecheckExecutorTest extends MockedPersTmsTest {

    private static final String GRADE_VOTE_STATE_TEST_TABLE = "V_GRADE_VOTE_STAT";

    @Autowired
    private RecheckQueueService recheckQueueService;

    @Autowired
    private DbGradeVoteService gradeVoteService;

    @Autowired
    private DislikedGradesRecheckExecutor dislikedGradeForRecheckExecutor;
    @Autowired
    private DislikedGradesRecheckService dislikedGradesRecheckService;
    @Autowired
    private GradeCreator gradeCreator;

    @Before
    public void setUp() {
        dislikedGradesRecheckService.setGradeVoteStatTableName(GRADE_VOTE_STATE_TEST_TABLE);
    }

    @Test
    public void testDislikedGradesForRecheck() {
        // grades not for recheck
        long gradeIdNoneCond = createGradeWithVotes(ModState.APPROVED, 4, 30);
        long gradeIdFirstCond = createGradeWithVotes(ModState.APPROVED, 2, 35);
        long gradeIdSecondCond = createGradeWithVotes(ModState.APPROVED, 5, 50);
        long gradeIdWrongModState1 = createGradeWithVotes(ModState.REJECTED, 2, 50);
        long gradeIdWrongModState2 = createGradeWithVotes(ModState.UNMODERATED, 2, 50);
        long gradeIdAlreadyOnRecheck = createGradeWithVotes(ModState.UNMODERATED, 2, 50);
        recheckQueueService.addGradesWithComplaintToRecheckQueue(List.of(new GradeComplaint(gradeIdAlreadyOnRecheck)));

        // grades for recheck
        long gradeIdForRecheck1 = createGradeWithVotes(ModState.APPROVED, 2, 50);
        long gradeIdForRecheck2 = createGradeWithVotes(ModState.APPROVED, 5, 100);

        dislikedGradeForRecheckExecutor.runTmsJob();

        final List<Long> recheckGrades = pgJdbcTemplate.queryForList("select grade_id from recheck_grade_queue", Long.class);
        assertEquals(recheckGrades.size(), 3);
        assertTrue(recheckGrades.containsAll(Arrays.asList(gradeIdAlreadyOnRecheck, gradeIdForRecheck1, gradeIdForRecheck2)));

        checkRecheckReason(gradeIdAlreadyOnRecheck, GradeRecheckReason.COMPLAINT);
        checkRecheckReason(gradeIdForRecheck1, GradeRecheckReason.DISLIKED_GRADE);
        checkRecheckReason(gradeIdForRecheck2, GradeRecheckReason.DISLIKED_GRADE);
    }

    private void checkRecheckReason(long gradeId, GradeRecheckReason recheckReason) {
        final List<Long> reasons = pgJdbcTemplate.queryForList("select RECHECK_REASON from recheck_grade_queue where grade_id = ?",  Long.class, gradeId);
        assertEquals(1, reasons.size());
        assertEquals(recheckReason.getValue(), reasons.get(0).intValue());
    }

    private long createGradeWithVotes(ModState modState, int agree, int reject) {
        ModelGrade grade = constructTestGrade(modState);
        long gradeId = gradeCreator.createGrade(grade);
        createVotes(gradeId, agree, reject);
        return gradeId;
    }

    private ModelGrade constructTestGrade(ModState modState) {
        ModelGrade grade = GradeCreator.constructModelGradeRnd();
        grade.setModState(modState);
        grade.setAverageGrade(2);
        return grade;
    }

    private void createVotes(long gradeId, int agree, int reject) {
        for (int i = 0; i < agree; i++) {
            createVote(gradeId, GradeVoteKind.agree);
        }

        for (int i = 0; i < reject; i++) {
            createVote(gradeId, GradeVoteKind.reject);
        }
    }

    private void createVote(long gradeId, GradeVoteKind voteType) {
        gradeVoteService.createVote(gradeId, GradeCreator.rndUid(), voteType, "5217148121158766543");
    }

}
