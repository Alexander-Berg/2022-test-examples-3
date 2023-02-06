package ru.yandex.market.pers.tms.timer;

import java.sql.Timestamp;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.db.DbGradeModelService;
import ru.yandex.market.pers.grade.core.db.DbGradeService;
import ru.yandex.market.pers.grade.core.model.core.GradeModelInfo;
import ru.yandex.market.pers.grade.core.model.core.ModelGrade;
import ru.yandex.market.pers.tms.MockedPersTmsTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author vvolokh
 * 08.08.2019
 */
public class ModelIdTransferExecutorTest extends MockedPersTmsTest {
    private static final Long SOURCE_MODEL_ID = 268L;
    private final static Long TARGET_MODEL_ID = 269L;
    private static final Long TEST_VENDOR_ID = 123L;
    private static final Long TEST_LOCAL_ID = 1234L;
    private static final Long TEST_CAT_ID = 321L;
    private static final String TEST_CAT_NAME = "Тестовая категория";

    @Autowired
    private DbGradeService gradeService;

    @Autowired
    private ModelIdTransferExecutor transferExecutor;

    @Autowired
    private DbGradeModelService gradeModelService;

    @Autowired
    private GradeCreator gradeCreator;

    @Test
    public void testTransfer() throws Exception {
        //given:
        ModelGrade gradeToCreate = GradeCreator.constructModelGradeRnd();
        gradeToCreate.setResourceId(SOURCE_MODEL_ID);
        gradeToCreate.setText("text");
        long gradeId = gradeCreator.createGrade(gradeToCreate);
        long oldModelId = gradeToCreate.getResourceId();

        gradeModelService.updateModelVendor(oldModelId, TEST_VENDOR_ID);
        gradeModelService.updateModelCategories(List.of(new GradeModelInfo(oldModelId, TEST_CAT_ID, TEST_CAT_NAME, null)));

        pgJdbcTemplate.update("insert into model_id_transfer (old_id, new_id) values (?, ?)", oldModelId, TARGET_MODEL_ID);

        //when:
        transferExecutor.runTmsJob();

        //then:
        //check grade transferred
        assertEquals(TARGET_MODEL_ID, pgJdbcTemplate.queryForObject("SELECT resource_id FROM GRADE WHERE id = ?", Long.class, gradeId));

        //check model_vendor transferred
        assertNull(pgJdbcTemplate.queryForObject("SELECT vendor_id FROM MODEL_VENDOR WHERE model_id = ?", Long.class, TARGET_MODEL_ID));

        //check transfer request marked as completed
        assertNotNull(pgJdbcTemplate.queryForObject("select upd_time from model_id_transfer where old_id = ?", Timestamp.class, SOURCE_MODEL_ID));
    }

}
