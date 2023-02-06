package ru.yandex.market.pers.tms.timer;


import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.pers.grade.core.db.DbGradeModelService;
import ru.yandex.market.pers.grade.core.model.core.GradeModelInfo;
import ru.yandex.market.pers.tms.MockedPersTmsTest;
import ru.yandex.market.util.db.ConfigurationService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pers.tms.timer.UpdateModelVendorAndCategoryByYtExecutor.IS_ACTIVE_KEY;

/**
 * @author @bahus
 * 28.07.2020
 */
public class UpdateModelVendorAndCategoryByYtExecutorTest extends MockedPersTmsTest {
    public static final long TEST_MODEL = 2128506L;
    public static final long TEST_CATEGORY = 777L;
    public static final long TEST_VENDOR = 585L;

    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private UpdateModelVendorAndCategoryByYtExecutor executor;
    @Autowired
    @Qualifier("ytJdbcTemplate")
    private JdbcTemplate yqlJdbcTemplate;
    @Autowired
    private DbGradeModelService gradeModelService;

    @Captor
    private ArgumentCaptor<ResultSetExtractor<Void>> resultSetExtractorCaptor;

    @Test
    public void test() throws Exception {
        configurationService.mergeValue(IS_ACTIVE_KEY, String.valueOf(true));

        prepareData(TEST_MODEL, TEST_CATEGORY, TEST_VENDOR);
        prepareData(TEST_MODEL + 2, TEST_CATEGORY + 2, TEST_VENDOR + 2);
        prepareData(TEST_MODEL + 3, TEST_CATEGORY + 3, TEST_VENDOR + 3);
        prepareData(TEST_MODEL + 4, null, null);
        prepareData(TEST_MODEL + 5, TEST_CATEGORY + 5, null);
        prepareData(TEST_MODEL + 6, null, TEST_VENDOR + 6);
        prepareData(TEST_MODEL + 7, TEST_CATEGORY + 7, TEST_VENDOR + 7);
        prepareData(TEST_MODEL + 8, null, null);

        executor.runTmsJob();
        verify(yqlJdbcTemplate, times(1)).query(anyString(), resultSetExtractorCaptor.capture());
        resultSetExtractorCaptor.getValue().extractData(prepareResultSet());

        // question have changed data
        assertQuestion(TEST_MODEL, TEST_CATEGORY + 1, TEST_VENDOR + 1, null);
        // question don't have changed data if no data received
        assertQuestion(TEST_MODEL + 2, TEST_CATEGORY + 2, TEST_VENDOR + 2, TEST_CATEGORY + 2 + "name");
        // question have lost data
        assertQuestion(TEST_MODEL + 3, null, null, null);
        // question have obtain data
        assertQuestion(TEST_MODEL + 4, TEST_CATEGORY + 4, TEST_VENDOR + 4, null);
        // question have changed data
        assertQuestion(TEST_MODEL + 5, null, TEST_VENDOR + 5, null);
        assertQuestion(TEST_MODEL + 6, TEST_CATEGORY + 6, null, null);
        // question don't have changed data if same data received
        assertQuestion(TEST_MODEL + 7, TEST_CATEGORY + 7, TEST_VENDOR + 7, null);
        assertQuestion(TEST_MODEL + 8, null, null, null);
    }

    private void assertQuestion(long testModel, Long testCategory, Long testVendor, String testName) {
        Pair<Long, String> category = pgJdbcTemplate.query(
            "SELECT cat_id, cat_name FROM model_category WHERE model_id = " + testModel,
            (ResultSet rs) -> rs.next() ? new Pair<>(rs.getLong("cat_id"), rs.getString("cat_name")) : null);
        Integer withoutCategory = pgJdbcTemplate.queryForObject(
            "SELECT count(*) FROM MODEL_WITHOUT_CATEGORY WHERE MODEL_ID = " + testModel, Integer.class);
        if (testCategory != null) {
            assertNotNull(category);
            assertEquals(testCategory, category.first);
            assertEquals(testName, category.second);
            assertEquals(Integer.valueOf(0), withoutCategory);
        } else {
            assertNull(category);
            assertEquals(Integer.valueOf(1), withoutCategory);
        }

        Long vendor = pgJdbcTemplate.queryForObject(
            "SELECT vendor_id FROM MODEL_VENDOR WHERE model_id = " + testModel, Long.class);
        assertEquals(testVendor, vendor);
    }

    private void prepareData(long model, Long category, Long vendor) {
        setModelVendor(model, vendor);
        if (category == null) {
            setModelWithoutCategory(model);
        } else {
            setModelCategory(model, category, category + "name");
        }
    }

    private void setModelVendor(long modelId, Long vendorId) {
        gradeModelService.updateModelVendor(modelId, vendorId);
    }

    private void setModelCategory(long modelId, Long categoryId, String categoryName) {
        gradeModelService.updateModelCategories(List.of(new GradeModelInfo(modelId, categoryId, categoryName, null)));
    }

    private void setModelWithoutCategory(long modelId) {
        gradeModelService.addModelsWithoutCategories(List.of(modelId));
    }

    private ResultSet prepareResultSet() throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.next()).thenReturn(true, true, true, true, true, true, true, false);
        when(resultSet.getLong(eq("model_id"))).thenReturn(
            TEST_MODEL,
            TEST_MODEL + 3,
            TEST_MODEL + 4,
            TEST_MODEL + 5,
            TEST_MODEL + 6,
            TEST_MODEL + 7,
            TEST_MODEL + 8);
        when(resultSet.getObject(eq("category_id"))).thenReturn(
            TEST_CATEGORY + 1,
            null,
            TEST_CATEGORY + 4,
            null,
            TEST_CATEGORY + 6,
            TEST_CATEGORY + 7,
            null);
        when(resultSet.getObject(eq("vendor_id"))).thenReturn(
            TEST_VENDOR + 1,
            null,
            TEST_VENDOR + 4,
            TEST_VENDOR + 5,
            null,
            TEST_VENDOR + 7,
            null);
        return resultSet;
    }
}
