package ru.yandex.market.pers.grade.admin.action;

import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.pers.grade.admin.MockedPersGradeAdminTest;
import ru.yandex.market.pers.grade.client.model.Delivery;
import ru.yandex.market.pers.grade.client.model.GradeType;
import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.model.core.ModelGrade;
import ru.yandex.market.pers.grade.core.model.core.ShopGrade;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CommonActionsControllerTest extends MockedPersGradeAdminTest {

    private static final long FAKE_USER = 1L;

    @Autowired
    private GradeCreator gradeCreator;

    @Test
    public void testMoveGrade() throws Exception {
        //given
        long fromModelId = 1;
        long toModelId = 2;
        ModelGrade grade = createTestModelGrade(fromModelId, FAKE_USER);
        //when
        moveGrade(grade.getId(), toModelId);
        //then
        assertEquals(toModelId, getGradeResourceId(grade.getId()).longValue());
        assertEquals(Integer.valueOf(1), getLogEntryCount(grade.getId(), fromModelId, toModelId));
    }

    @Test
    public void testMoveGradeToSameModel() throws Exception {
        //given
        long fromModelId = 1;
        long toModelId = fromModelId;
        ModelGrade grade = createTestModelGrade(fromModelId, FAKE_USER);
        //when
        moveGrade(grade.getId(), toModelId);
        //then
        assertEquals(toModelId, getGradeResourceId(grade.getId()).longValue());
        assertEquals(Integer.valueOf(0), getLogEntryCount(grade.getId(), fromModelId, toModelId));
    }

    @Test
    public void testMoveGradeWithNonExistentGrade() throws Exception {
        //given non-existent grade
        long nonExistentGradeId = 1000000;
        long fromModelId = 1;
        long toModelId = fromModelId;
        //when
        moveGrade(nonExistentGradeId, toModelId);
        //then
        assertNull(getGradeResourceId(nonExistentGradeId));
        assertEquals(Integer.valueOf(0), getLogEntryCount(nonExistentGradeId, fromModelId, toModelId));
    }

    @Test
    public void testMoveGradeWithSameDestinationModel() throws Exception {
        //given
        long fromModelId = 1;
        long toModelId = 4327823;
        // two grades form one user
        ModelGrade firstGrade = createTestModelGrade(fromModelId, FAKE_USER);
        ModelGrade secondGrade = createTestModelGrade(fromModelId + 1, FAKE_USER);
        //when
        moveGrade(firstGrade.getId(), toModelId);
        moveGrade(secondGrade.getId(), toModelId);
        //then
        assertEquals(toModelId, getGradeResourceId(firstGrade.getId()).longValue());
        assertEquals(Integer.valueOf(1), getLogEntryCount(firstGrade.getId(), fromModelId, toModelId));
        //not moved
        assertEquals(fromModelId + 1, getGradeResourceId(secondGrade.getId()).longValue());
        assertEquals(Integer.valueOf(0), getLogEntryCount(secondGrade.getId(), fromModelId + 1, toModelId));
    }

    @Test
    public void testMoveGradeWithModeratorId() throws Exception {
        //given
        long fromModelId = 1;
        long toModelId = 4737845;
        ModelGrade grade = createTestModelGrade(fromModelId, FAKE_USER);
        //when
        moveGrade(grade.getId(), toModelId);
        //then
        checkModerator(FAKE_MODERATOR_ID, grade.getId());
    }

    @Test
    public void testMoveAllModelGrades() throws Exception {
        //given
        long fromModelId = 1;
        long toModelId = 2;
        List<ModelGrade> grades = List.of(
            createTestModelGrade(fromModelId, FAKE_USER),
            createTestModelGrade(fromModelId, FAKE_USER + 1),
            createTestModelGrade(fromModelId + 1, FAKE_USER + 2),
            createTestModelGrade(fromModelId + 100, FAKE_USER)
        );
        //when
        moveGrades(fromModelId, toModelId, GradeType.MODEL_GRADE.value());
        //then
        assertEquals(toModelId, getGradeResourceId(grades.get(0).getId()).longValue());
        assertEquals(toModelId, getGradeResourceId(grades.get(1).getId()).longValue());
        assertEquals(fromModelId + 1, getGradeResourceId(grades.get(2).getId()).longValue());
        assertEquals(fromModelId + 100, getGradeResourceId(grades.get(3).getId()).longValue());
        assertEquals(Integer.valueOf(1), getLogEntryCount(grades.get(0).getId(), fromModelId, toModelId));
        assertEquals(Integer.valueOf(1), getLogEntryCount(grades.get(1).getId(), fromModelId, toModelId));
    }


    @Test
    public void testMoveAllShopGrades() throws Exception {
        //given
        long fromShopId = 1;
        long toShopId = 2;
        List<ShopGrade> grades = List.of(
            createTestShopGrade(fromShopId, "423423L", FAKE_USER),
            createTestShopGrade(fromShopId, "434213432", FAKE_USER + 1),
            createTestShopGrade(fromShopId + 1, "42324",FAKE_USER + 2)
        );
        ModelGrade modelGrade = createTestModelGrade(fromShopId, FAKE_USER);
        //when
        moveGrades(fromShopId, toShopId, GradeType.SHOP_GRADE.value());
        //then
        assertEquals(toShopId, getGradeResourceId(grades.get(0).getId()).longValue());
        assertEquals(toShopId, getGradeResourceId(grades.get(1).getId()).longValue());
        assertEquals(fromShopId + 1, getGradeResourceId(grades.get(2).getId()).longValue());
        assertEquals(fromShopId, getGradeResourceId(modelGrade.getId()).longValue());
        assertEquals(Integer.valueOf(0), getLogEntryCount(grades.get(0).getId(), fromShopId, toShopId));
        assertEquals(Integer.valueOf(0), getLogEntryCount(grades.get(1).getId(), fromShopId, toShopId));
    }

    private void checkModerator(long moderatorId, long gradeId) {
        Long actionId = pgJdbcTemplate.queryForObject(
            "select mth.transition " +
                "FROM grade_transition_history mth " +
                "where mth.grade_id = ?", Long.class, gradeId);

        Long resultModeratorId =pgJdbcTemplate.queryForObject(
            "select moderator_id from action_data where id = ?",
            Long.class,
            actionId
        );

        assertEquals(moderatorId, resultModeratorId.longValue());
    }

    private Integer getLogEntryCount(long gradeId, long from, long to) {
        String sql = "SELECT count(*) FROM grade_transition_history " +
            "WHERE old_resource = ? AND new_resource = ? AND grade_id = ?";
        return pgJdbcTemplate.queryForObject(sql, Integer.class, from, to, gradeId);
    }

    private ModelGrade createTestModelGrade(long modelId, final long authorId) {
        ModelGrade grade = GradeCreator.constructModelGrade(modelId, authorId);

        grade.setText("Best you can afford");
        grade.setPro("Fast, supports deep class III warp diving");
        grade.setContra("Price is huge. Required experienced engineers to operate/repair");

        grade.setAverageGrade(1);
        long id = gradeCreator.createGrade(grade);
        grade.setId(id);
        return grade;
    }

    private ShopGrade createTestShopGrade(long shopId, String orderId, long authorId) {
        ShopGrade shopGrade = GradeCreator.constructShopGrade(shopId, authorId)
            .fillShopGradeCreationFields(orderId, Delivery.PICKUP);

        shopGrade.setText("Best you can afford");
        shopGrade.setPro("Fast, supports deep class III warp diving");
        shopGrade.setContra("Price is huge. Required experienced engineers to operate/repair");

        shopGrade.setAverageGrade(1);
        long id = gradeCreator.createGrade(shopGrade);
        shopGrade.setId(id);
        return shopGrade;
    }

    private void moveGrade(long gradeId, long toModelId) throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/api/actions/move-grade")
            .param("grade-id", String.valueOf(gradeId))
            .param("to-resource", String.valueOf(toModelId)))
            .andDo(print())
            .andExpect(status().is2xxSuccessful())
            .andReturn();
    }

    private void moveGrades(long fromModelId, long toModelId, int type) throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/api/actions/move-grades")
            .param("from-model", String.valueOf(fromModelId))
            .param("to-model", String.valueOf(toModelId))
            .param("type", String.valueOf(type)))
            .andDo(print())
            .andExpect(status().is2xxSuccessful())
            .andReturn();
    }

    private Long getGradeResourceId(long gradeId) {
        List<Long> resourceIds = pgJdbcTemplate.queryForList("select resource_id from grade where id = ?", Long.class, gradeId);
        return resourceIds.isEmpty() ? null : resourceIds.get(0);
    }
}
