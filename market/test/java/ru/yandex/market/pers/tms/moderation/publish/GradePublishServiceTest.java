package ru.yandex.market.pers.tms.moderation.publish;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.grade.client.model.ModState;
import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.db.GradeMasterJdbc;
import ru.yandex.market.pers.service.common.util.ExpFlagService;
import ru.yandex.market.pers.tms.MockedPersTmsTest;
import ru.yandex.market.pers.tms.moderation.AbstractAutomoderation;

import static org.junit.Assert.assertEquals;

public class GradePublishServiceTest extends MockedPersTmsTest {

    static volatile long SHOP_ID = 1L;
    static volatile long USER_ID = 1L;
    static volatile long MODEL_ID = 1L;

    @Autowired
    private GradePublishService service;

    @Autowired
    private GradeCreator gradeCreator;

    @Autowired
    private GradeMasterJdbc gradeMasterJdbc;

    @Autowired
    private ExpFlagService expFlagService;

    @Test
    public void testPublish() {
        long shopGrade = gradeCreator.createShopGrade(USER_ID, SHOP_ID);
        long modelGrade = gradeCreator.createModelGrade(MODEL_ID, USER_ID);
        long clusterGrade = gradeCreator.createClusterGrade(MODEL_ID + 10, USER_ID);

        expFlagService.setFlag(AbstractAutomoderation.EXP_DISABLE_AUTO_PUBLISH_MODEL_GRADE_KEY, true);

        setModStateForGrade(ModState.READY_TO_PUBLISH, shopGrade, modelGrade, clusterGrade);
        service.publishGrades();
        assertModStateForGrade(ModState.READY_TO_PUBLISH, shopGrade);
        assertModStateForGrade(ModState.APPROVED, modelGrade);
        assertModStateForGrade(ModState.APPROVED, clusterGrade);
    }

    private void setModStateForGrade(ModState modState, long... ids) {
        gradeMasterJdbc.getPgJdbcTemplate()
            .update("update grade.grade set mod_state = ? where id = any(?)", modState.value(), ids);
    }

    private void assertModStateForGrade(ModState modState, long gradeId) {
        assertEquals((Integer) modState.value(), gradeMasterJdbc.getPgJdbcTemplate().queryForObject("select mod_state from grade.grade where id = ?", Integer.class, gradeId));
    }
}
