package ru.yandex.market.pers.tms.timer;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.grade.client.model.ModState;
import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.db.DbGradeService;
import ru.yandex.market.pers.grade.core.db.DbGradeVoteService;
import ru.yandex.market.pers.grade.core.model.core.ModelGrade;
import ru.yandex.market.pers.grade.core.model.vote.GradeVoteKind;
import ru.yandex.market.pers.grade.core.model.vote.GradeVotes;
import ru.yandex.market.pers.tms.MockedPersTmsTest;

import static org.junit.Assert.assertEquals;

/**
 * @author vvolokh
 * 12.07.2019
 */
public class GradeVotesTransferExecutorTest extends MockedPersTmsTest {
    private static final String GRADE_VOTE_STATE_TEST_TABLE = "V_GRADE_VOTE_STAT_MVSRC";
    private static final String GRADE_VOTE_STATE_TEST_TABLE_PG = "V_GRADE_VOTE_STAT";
    private static final long MODEL_ID = 12345L;

    @Autowired
    private DbGradeService gradeService;

    @Autowired
    private DbGradeVoteService gradeVoteService;

    @Autowired
    private GradeCreator gradeCreator;

    @Before
    public void initView() {
        gradeVoteService.changeVoteStatViewPgForTests(GRADE_VOTE_STATE_TEST_TABLE_PG);
    }

    @Test
    public void testVoteTransfer() throws Exception {
        long authorId = 123L;
        long oldGradeId = createModelGrade("text 1", authorId);

        // check there are no votes initially
        Map<Long, GradeVotes> votesMap = gradeVoteService.getVotesByGradeIds(List.of(oldGradeId));
        assertEquals(new GradeVotes(0,0), votesMap.get(oldGradeId));

        // create votes
        gradeVoteService.createVote(oldGradeId, authorId, GradeVoteKind.agree, null);
        gradeVoteService.createVote(oldGradeId, authorId + 1, GradeVoteKind.reject, null);

        votesMap = gradeVoteService.getVotesByGradeIds(List.of(oldGradeId));
        assertEquals(new GradeVotes(1,1), votesMap.get(oldGradeId));

        // update grade, check votes are transferred
        long newGradeId = createModelGrade("text 2", authorId);

        votesMap = gradeVoteService.getVotesByGradeIds(List.of(oldGradeId, newGradeId));
        assertEquals(new GradeVotes(1,1), votesMap.get(oldGradeId));
        assertEquals(new GradeVotes(1,1), votesMap.get(newGradeId));

        // vote again, check vote added to both grades
        gradeVoteService.createVote(oldGradeId, authorId + 2, GradeVoteKind.agree, null);

        votesMap = gradeVoteService.getVotesByGradeIds(List.of(oldGradeId, newGradeId));
        assertEquals(new GradeVotes(2,1), votesMap.get(oldGradeId));
        assertEquals(new GradeVotes(2,1), votesMap.get(newGradeId));
    }

    private long createModelGrade(String text, long authorId) {
        ModelGrade grade = GradeCreator.constructModelGrade(MODEL_ID, authorId);
        grade.setText(text);
        grade.setModState(ModState.APPROVED);
        grade.setAverageGrade(4);

        return gradeCreator.createGrade(grade);
    }
}
