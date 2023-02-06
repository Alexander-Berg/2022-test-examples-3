package ru.yandex.market.pers.grade.core.db;

import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.grade.client.model.GradeType;
import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.MockedTest;
import ru.yandex.market.pers.grade.core.model.core.AbstractGrade;

import static org.junit.Assert.assertEquals;

/**
 * @author Damir Shagaev / damir-vvlpx@ / 21.06.2021
 */
public class MoveGradeServiceTest extends MockedTest {

    public static final long MODEL_ID = 3413414;
    public static final long SHOP_ID = 5817843;
    public static final long UID = 9245245;

    @Autowired
    private GradeCreator gradeCreator;

    @Autowired
    private MoveGradeService moveGradeService;

    @Autowired
    private DbGradeService gradeService;

    @Test
    public void testMoveGradeWithDifferentModerator() {
        long firstGradeId = gradeCreator.createModelGrade(MODEL_ID, UID);
        long secondGradeId = gradeCreator.createModelGrade(MODEL_ID + 1, UID + 1);
        long toModelId = 432687;

        moveGradeService.moveGrade(firstGradeId, toModelId, 750001L);
        moveGradeService.moveGrade(secondGradeId, toModelId, 750002L);

        checkModerator(750001L, firstGradeId);
        checkModerator(750002L, secondGradeId);
    }

    @Test
    public void testMoveAllGrades() {
        List<Long> modelGrades = List.of(
            gradeCreator.createModelGrade(MODEL_ID, UID),
            gradeCreator.createModelGrade(MODEL_ID, UID + 1),
            gradeCreator.createModelGrade(MODEL_ID, UID + 1),
            gradeCreator.createModelGrade(MODEL_ID + 1, UID + 2),
            gradeCreator.createModelGrade(MODEL_ID + 1, UID + 3)
        );
        long toModelId = MODEL_ID + 2;

        pgJdbcTemplate.update("delete from saas_index_queue");

        moveGradeService.moveGrades(GradeType.MODEL_GRADE, MODEL_ID, toModelId, 632932L);
        moveGradeService.moveGrades(GradeType.MODEL_GRADE, MODEL_ID + 1, toModelId, 342312L);

        checkModerator(632932L, modelGrades.get(0));
        checkModerator(632932L, modelGrades.get(1));
        checkModerator(632932L, modelGrades.get(2));
        checkModerator(342312L, modelGrades.get(3));
        checkModerator(342312L, modelGrades.get(4));

        assertEquals(
            modelGrades,
            pgJdbcTemplate.queryForList(
                "select grade_id from saas_index_queue order by grade_id",
                Long.class
            ));
    }

    @Test
    public void testMoveShopGradeWithHistory() {
        long firstGradeId = gradeCreator.createShopGrade(UID, SHOP_ID);
        long secondGradeId = gradeCreator.createShopGrade(UID, SHOP_ID);
        long toShopId = 432687;

        moveGradeService.moveGrade(secondGradeId, toShopId, 750001L);

        checkModerator(750001L, firstGradeId);
        checkModerator(750001L, secondGradeId);
        checkLogGradeTransitionHistory(secondGradeId, 2L);
        assertEquals(toShopId, gradeService.getGrade(firstGradeId).getResourceId().longValue());
        AbstractGrade secondGrade = gradeService.getGrade(secondGradeId);
        assertEquals(toShopId, secondGrade.getResourceId().longValue());
        assertEquals(firstGradeId, secondGrade.getFixId().longValue());
    }

    @Test
    public void testMoveTwoShopGradeWithOneAuthor() {
        List<Long> shopGrades = List.of(
            gradeCreator.createShopGrade(UID, SHOP_ID),
            gradeCreator.createFeedbackGrade(SHOP_ID, UID, "order1")
        );
        long toShopId = SHOP_ID + 10;

        moveGradeService.moveGrade(shopGrades.get(0), toShopId, 750001L);

        checkLogGradeTransitionHistory(shopGrades.get(0).longValue(), 1L);
        assertEquals(toShopId, gradeService.getGrade(shopGrades.get(0)).getResourceId().longValue());
        assertEquals(SHOP_ID, gradeService.getGrade(shopGrades.get(1)).getResourceId().longValue());
    }

    @Test
    public void testMoveToShopWithExistingGrade() {
        long firstGradeId = gradeCreator.createShopGrade(UID, SHOP_ID);
        long toShopId = SHOP_ID + 10;

        // existing grade
        gradeCreator.createShopGrade(UID, toShopId);

        // move to shop with grade from same author
        moveGradeService.moveGrade(firstGradeId, toShopId, 750001L);

        checkLogGradeTransitionHistory(firstGradeId, 0L);
        assertEquals(SHOP_ID, gradeService.getGrade(firstGradeId).getResourceId().longValue());
    }

    @Test
    public void testMoveToYandexMarketIdWithExistingGrade() {
        long moveableShopGradeId = gradeCreator.createShopGrade(UID, SHOP_ID);
        long anotherShopGradeId = gradeCreator.createShopGrade(UID + 1, SHOP_ID); // shouldn't be moved
        long toShopId = MoveGradeService.YANDEX_MARKET_SHOP_ID;

        //existed shop grade
        gradeCreator.createShopGrade(UID, toShopId);
        moveGradeService.moveGrade(moveableShopGradeId, toShopId, 750001L);

        assertEquals(toShopId, gradeService.getGrade(moveableShopGradeId).getResourceId().longValue());
        assertEquals(SHOP_ID, gradeService.getGrade(anotherShopGradeId).getResourceId().longValue());
    }

    @Test
    public void testMoveToModelWithExistingGrade() {
        long firstGradeId = gradeCreator.createModelGrade(MODEL_ID, UID);
        long toModelId = MODEL_ID + 10;

        // existing grade
        gradeCreator.createModelGrade(toModelId, UID);

        // move to model with grade from same author
        moveGradeService.moveGrade(firstGradeId, toModelId, 750001L);

        checkLogGradeTransitionHistory(firstGradeId, 0L);
        assertEquals(MODEL_ID, gradeService.getGrade(firstGradeId).getResourceId().longValue());
    }

    @Test
    public void testMoveModelGradeWithHistory() {
        long firstGradeId = gradeCreator.createModelGrade(MODEL_ID, UID);
        long secondGradeId = gradeCreator.createModelGrade(MODEL_ID, UID);
        long toModelId = MODEL_ID + 15;

        // move last version in grade history
        moveGradeService.moveGrade(secondGradeId, toModelId, 750001L);

        checkModerator(750001L, firstGradeId);
        assertEquals(toModelId, gradeService.getGrade(firstGradeId).getResourceId().longValue());

        checkModerator(750001L, secondGradeId);
        checkLogGradeTransitionHistory(secondGradeId, 2L);
        AbstractGrade secondGrade = gradeService.getGrade(secondGradeId);
        assertEquals(toModelId, secondGrade.getResourceId().longValue());
        assertEquals(firstGradeId, secondGrade.getFixId().longValue());
    }

    private void checkModerator(long moderatorId, long gradeId) {
        Long actionId = getTransitionId(gradeId);

        Long resultModeratorId = pgJdbcTemplate.queryForObject(
            "select moderator_id from action_data where id = ?",
            Long.class,
            actionId
        );
        assertEquals(moderatorId, resultModeratorId.longValue());
    }

    private void checkLogGradeTransitionHistory(long gradeId, Long expectedCount) {
        Long transitionId = getTransitionId(gradeId);
        Long resultCount = pgJdbcTemplate.queryForObject(
            "select count(*) " +
                "FROM GRADE_TRANSITION_HISTORY mth " +
                "where mth.transition = ?", Long.class, transitionId);

        assertEquals(expectedCount, resultCount);
    }

    private Long getTransitionId(long gradeId) {
        List<Long> transitions = pgJdbcTemplate.queryForList(
            "select mth.transition " +
                "FROM GRADE_TRANSITION_HISTORY mth " +
                "where mth.grade_id = ?", Long.class, gradeId);
        return transitions.isEmpty() ? -1 : transitions.get(0);
    }

}
