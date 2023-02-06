package ru.yandex.market.pers.tms.timer;

import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.application.monitoring.MonitoringStatus;
import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.db.DbGradeModelService;
import ru.yandex.market.pers.tms.MockedPersTmsTest;
import ru.yandex.market.report.ReportService;
import ru.yandex.market.report.model.Model;
import ru.yandex.market.report.model.Vendor;
import ru.yandex.market.util.db.ConfigurationService;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pers.tms.timer.ModelVendorExecutor.BATCH_SIZE_DEFAULT;
import static ru.yandex.market.pers.tms.timer.ModelVendorExecutor.MODEL_WITHOUT_VENDOR_BATCH_SIZE_KEY;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 13.03.2018
 */
public class ModelVendorExecutorTest extends MockedPersTmsTest {
    private static final Long MODEL_ID = 1L;
    private static final Long VENDOR_ID = 1L;
    private static final Long LOCAL_ID = 1234L;

    @Autowired
    ModelVendorExecutor executor;
    @Autowired
    @Qualifier("report")
    private ReportService reportService;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private GradeCreator gradeCreator;
    @Autowired
    private ComplexMonitoring complexMonitoring;
    @Autowired
    private DbGradeModelService gradeModelService;

    @Before
    public void setUp() throws Exception {
        configurationService.mergeValue(MODEL_WITHOUT_VENDOR_BATCH_SIZE_KEY, Long.valueOf(BATCH_SIZE_DEFAULT));
    }

    @Test
    public void testJobWithReportModelWithoutVendorAndNullVendorId() throws Exception {
        addNewModelForTest(MODEL_ID, modelWithoutVendor());
        createModelVendor();
        executor.runTmsJob();
        assertEquals(1, getModelVendorCount(MODEL_ID, ModelVendorExecutor.NOT_EXISTING_VENDOR));
    }

    @Test
    public void testJobWithReportModelWithVendorAndNullVendorId() throws Exception {
        addNewModelForTest(MODEL_ID, modelWithVendor());
        createModelVendor();
        executor.runTmsJob();
        assertEquals(1, getModelVendorCount(MODEL_ID, VENDOR_ID));
    }

    @Test
    public void testJobWithReportModelWithVendorAndCleanModelVendorTable() throws Exception {
        addNewModelForTest(MODEL_ID, modelWithVendor());
        executor.runTmsJob();
        assertEquals(1, getModelVendorCount(MODEL_ID, VENDOR_ID));
    }

    @Test
    public void testJobWithReportModelWithoutVendorAndCleanModelVendorTable() throws Exception {
        addNewModelForTest(MODEL_ID, modelWithoutVendor());
        executor.runTmsJob();
        assertEquals(1, getModelVendorCount(MODEL_ID, ModelVendorExecutor.NOT_EXISTING_VENDOR));
    }

    @Test
    public void testMonitoringWithEqualsBatchSize() throws Exception {
        configurationService.mergeValue(MODEL_WITHOUT_VENDOR_BATCH_SIZE_KEY, 2L);
        addNewModelForTest(MODEL_ID, modelWithVendor());
        addNewModelForTest(MODEL_ID + 1, modelWithVendor());
        assertEquals(0, getModelVendorCount(MODEL_ID, VENDOR_ID));
        assertEquals(0, getModelVendorCount(MODEL_ID + 1, VENDOR_ID));
        executor.runTmsJob();
        assertEquals(MonitoringStatus.CRITICAL, complexMonitoring.getResult().getStatus());
        assertEquals("CRIT {modelWithoutVendorBatchSizeExceeded: Model count equals to batch size}",
                complexMonitoring.getResult().getMessage());

        resetMonitoring();

        addNewModelForTest(MODEL_ID + 2, modelWithVendor());
        assertEquals(0, getModelVendorCount(MODEL_ID + 2, VENDOR_ID));
        executor.runTmsJob();
        assertEquals(MonitoringStatus.OK, complexMonitoring.getResult().getStatus());
    }

    private void addNewModelForTest(long modelId, Model model) {
        gradeModelService.deleteModelVendor(List.of(modelId));
        gradeCreator.createModelGrade(modelId, 2000L);
        when(reportService.getModelById(eq(modelId))).thenReturn(Optional.of(model));
    }

    private int getModelVendorCount(long modelId, long vendorId) {
        return pgJdbcTemplate.queryForObject("select count(*) from model_vendor where model_id=? and vendor_id=?",
                Integer.class, modelId, vendorId).intValue();
    }

    private void createModelVendor() {
        gradeModelService.updateModelVendor(MODEL_ID, null);
    }

    private Model modelWithVendor() {
        Model model = new Model();
        model.setId(MODEL_ID);
        model.setVendor(new Vendor(VENDOR_ID, String.valueOf(VENDOR_ID)));
        return model;
    }

    private Model modelWithoutVendor() {
        Model model = new Model();
        model.setId(MODEL_ID);
        return model;
    }
}
