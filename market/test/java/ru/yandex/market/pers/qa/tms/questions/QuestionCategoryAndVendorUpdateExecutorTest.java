package ru.yandex.market.pers.qa.tms.questions;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import ru.yandex.market.pers.qa.PersQaTmsTest;
import ru.yandex.market.pers.qa.model.Question;
import ru.yandex.market.pers.qa.model.SecurityData;
import ru.yandex.market.pers.qa.service.QuestionService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author @bahus
 * 28.07.2020
 */
class QuestionCategoryAndVendorUpdateExecutorTest extends PersQaTmsTest {

    public static final long UID = 404L;
    public static final long TEST_MODEL = 2128506L;
    public static final long TEST_CATEGORY = 777L;
    public static final long TEST_VENDOR = 585L;
    public static final String TEXT = "some test text";
    public static final String SELECT_VENDOR_SQL = "SELECT vendor_id FROM qa.model_vendor WHERE model_id = %d";

    @Autowired
    private QuestionService questionService;
    @Autowired
    private QuestionCategoryAndVendorUpdateExecutor executor;
    @Autowired
    @Qualifier("pgJdbcTemplate")
    private JdbcTemplate jdbcTemplate;
    @Autowired
    @Qualifier("yqlJdbcTemplate")
    private JdbcTemplate yqlJdbcTemplate;

    @Captor
    private ArgumentCaptor<ResultSetExtractor<Void>> resultSetExtractorCaptor;

    @Test
    public void test() throws SQLException {
        long questionId1 = prepareQuestion(TEST_MODEL, TEST_CATEGORY, TEST_VENDOR);
        long questionId2 = prepareQuestion(TEST_MODEL + 2, TEST_CATEGORY + 2, TEST_VENDOR + 2);
        long questionId3 = prepareQuestion(TEST_MODEL + 3, TEST_CATEGORY + 3, TEST_VENDOR + 3);
        long questionId4 = prepareQuestion(TEST_MODEL + 4, null, null);
        long questionId5 = prepareQuestion(TEST_MODEL + 5, TEST_CATEGORY + 5, null);
        long questionId6 = prepareQuestion(TEST_MODEL + 6, null, TEST_VENDOR + 6);
        long questionId7 = prepareQuestion(TEST_MODEL + 7, TEST_CATEGORY + 7, TEST_VENDOR + 7);
        long questionId8 = prepareQuestion(TEST_MODEL + 8, null, null);

        executor.processQuestionCategoryAndVendorUpdate();
        verify(yqlJdbcTemplate).query(anyString(), resultSetExtractorCaptor.capture());
        resultSetExtractorCaptor.getValue().extractData(prepareResultSet());

        // question have changed data
        assertQuestion(questionId1, TEST_MODEL, TEST_CATEGORY + 1, TEST_VENDOR + 1);
        // question don't have changed data if no data received
        assertQuestion(questionId2, TEST_MODEL + 2, TEST_CATEGORY + 2, TEST_VENDOR + 2);
        // question have lost data
        assertQuestion(questionId3, TEST_MODEL + 3, null, null);
        // question have obtain data
        assertQuestion(questionId4, TEST_MODEL + 4, TEST_CATEGORY + 4, TEST_VENDOR + 4);
        // question have changed data
        assertQuestion(questionId5, TEST_MODEL + 5, null, TEST_VENDOR + 5);
        assertQuestion(questionId6, TEST_MODEL + 6, TEST_CATEGORY + 6, null);
        // question don't have changed data if same data received
        assertQuestion(questionId7, TEST_MODEL + 7, TEST_CATEGORY + 7, TEST_VENDOR + 7);
        assertQuestion(questionId8, TEST_MODEL + 8, null, null);
    }

    private void assertQuestion(long questionId1, long testModel, Long category, Long vendor) {
        Question questionById1 = questionService.getQuestionById(questionId1);
        Long vendorId1 = jdbcTemplate.queryForObject(String.format(SELECT_VENDOR_SQL, testModel), Long.class);
        assertEquals(questionById1.getEntitySource(), category == null ? null : String.valueOf(category));
        assertEquals(vendorId1, vendor);
    }

    private long prepareQuestion(long model, Long category, Long vendor) {
        setModelVendor(model, vendor);
        Question question = Question.buildModelQuestion(UID, TEXT, model, category);
        return questionService.createQuestionGetId(question, new SecurityData());
    }

    private ResultSet prepareResultSet() throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.next()).thenReturn(true, true, true, true, true, true, true, false);
        when(resultSet.getString(eq("model_id"))).thenReturn(
            String.valueOf(TEST_MODEL),
            String.valueOf(TEST_MODEL + 3),
            String.valueOf(TEST_MODEL + 4),
            String.valueOf(TEST_MODEL + 5),
            String.valueOf(TEST_MODEL + 6),
            String.valueOf(TEST_MODEL + 7),
            String.valueOf(TEST_MODEL + 8));
        when(resultSet.getString(eq("category_id"))).thenReturn(
            String.valueOf(TEST_CATEGORY + 1),
            null,
            String.valueOf(TEST_CATEGORY + 4),
            null,
            String.valueOf(TEST_CATEGORY + 6),
            String.valueOf(TEST_CATEGORY + 7),
            null);
        when(resultSet.getString(eq("vendor_id"))).thenReturn(
            String.valueOf(TEST_VENDOR + 1),
            null,
            String.valueOf(TEST_VENDOR + 4),
            String.valueOf(TEST_VENDOR + 5),
            null,
            String.valueOf(TEST_VENDOR + 7),
            null);
        return resultSet;
    }

    private void setModelVendor(long modelId, Long vendorId) {
        String sql = String.format("INSERT INTO qa.model_vendor VALUES(%d, %d)", modelId, vendorId);
        jdbcTemplate.execute(sql);
    }
}
