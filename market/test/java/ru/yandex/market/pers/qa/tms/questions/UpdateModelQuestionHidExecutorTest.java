package ru.yandex.market.pers.qa.tms.questions;

import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.pers.qa.mock.ReportServiceMockUtils;
import ru.yandex.market.report.model.Category;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author varvara
 * 21.08.2018
 */
public class UpdateModelQuestionHidExecutorTest extends UpdateModelQuestionInfoExecutorTest {

    // data for processing questions
    private static final long MODEL_HID = 1324;
    private static final long MODEL_NO_HID = 13245;
    private static final long MODEL_NO_MODEL = 132456;
    private static final long HID = 100500;
    private static final Category CATEGORY = new Category(HID, "Мобильные телефоны");

    // data for skip questions
    private static final long MODEL_NO_HID_2 = 132486585;
    private static final long MODEL_HID_2 = 13123;
    private static final long HID_2 = 100501;

    @Test
    public void testUpdateModelQuestionHidExecutor() {
        ReportServiceMockUtils.mockReportServiceByModelIdCategories(
            reportService,
            MODEL_HID,
            MODEL_NO_HID,
            CATEGORY);

        // create questions for processing
        final Long processQuestionHid = createQuestion(MODEL_HID);
        final Long processQuestionNoHid = createQuestion(MODEL_NO_HID);
        final Long processQuestionNoModel = createQuestion(MODEL_NO_MODEL);

        // create questions for skip
        final Long skipQuestionHid = createQuestion(MODEL_HID_2);
        final Long skipQuestionNoHid = createQuestion(MODEL_NO_HID_2);

        qaJdbcTemplate.update("update qa.question set entity_src = null");
        qaJdbcTemplate.update("delete from qa.model_without_category");
        qaJdbcTemplate.update("update qa.question set entity_src = ? where id = ?", HID_2, skipQuestionHid);
        qaJdbcTemplate.update("insert into qa.model_without_category(model_id, cr_time) values(?, now())", MODEL_NO_HID_2);

        executor.fillHids();

        // check processing questions
        checkQuestionHid(HID, processQuestionHid);
        checkQuestionNullHid(processQuestionNoHid);
        checkQuestionNullHid(processQuestionNoModel);

        // check skip questions, they still the same
        checkQuestionHid(HID_2, skipQuestionHid);
        checkQuestionNullHid(skipQuestionNoHid);

        // check table qa.model_without_category table
        checkModelWithoutHid(MODEL_NO_HID);
        checkModelWithoutHid(MODEL_NO_MODEL);
        checkModelWithoutHid(MODEL_NO_HID_2);
    }

    private void checkModelWithoutHid(Long modelId) {
        final long count = qaJdbcTemplate.queryForObject(
            "select count(*) from qa.model_without_category where model_id = ?",
            Long.class,
            modelId);
        assertEquals(1, count, "model without category in table qa.model_without_category");
    }

    private void checkQuestionNullHid(long questionId) {
        List<Long> hids = qaJdbcTemplate.queryForList("select entity_src from qa.question where id = ?",
            Long.class, questionId);

        assertEquals(1, hids.size());
        assertNull(hids.get(0));
    }

    private void checkQuestionHid(Long categoryId, long questionId) {
        List<Long> hids = qaJdbcTemplate.queryForList("select entity_src from qa.question where id = ?",
            Long.class, questionId);

        assertEquals(1, hids.size());
        assertNotNull(hids.get(0));
        assertEquals(categoryId, hids.get(0));
    }
}
