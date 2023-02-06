package ru.yandex.market.pers.qa.tms.questions;

import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.pers.qa.mock.ReportServiceMockUtils;
import ru.yandex.market.report.model.Vendor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author varvara
 * 21.08.2018
 */
public class UpdateModelVendorIdExecutorTest extends UpdateModelQuestionInfoExecutorTest {

    // data for processing questions
    private static final long MODEL_VENDOR = 1324;
    private static final long MODEL_NO_VENDOR = 13245;
    private static final long MODEL_VENDOR_WITH_TRANSITION = 12345678; //transition to (MODEL_VENDOR_WITH_TRANSITION + 1)
    private static final long MODEL_NO_MODEL = 132456;
    private static final long VENDOR_ID = 100500;
    private static final Vendor VENDOR = new Vendor(VENDOR_ID, "Эпл инк.");

    // data for skip questions
    private static final long MODEL_NO_VENDOR_2 = 132486585;
    private static final long MODEL_VENDOR_2 = 13123;
    private static final long VENDOR_ID_2 = 100501;

    @Test
    public void testUpdateModelVendorExecutor() {
        ReportServiceMockUtils.mockReportServiceByModelIdVendor(
            reportService,
            MODEL_VENDOR,
            MODEL_NO_VENDOR,
            MODEL_VENDOR_WITH_TRANSITION,
            VENDOR);

        // create questions with model for processing
        final Long processQuestionVendor = createQuestion(MODEL_VENDOR);
        final Long processQuestionVendorWithTransition = createQuestion(MODEL_VENDOR_WITH_TRANSITION);
        final Long processQuestionNoVendor = createQuestion(MODEL_NO_VENDOR);
        final Long processQuestionNoModel = createQuestion(MODEL_NO_MODEL);

        // create questions with model for skip
        final Long skipQuestionVendor = createQuestion(MODEL_VENDOR_2);
        final Long skipQuestionNoVendor = createQuestion(MODEL_NO_VENDOR_2);

        qaJdbcTemplate.update("insert into qa.model_vendor(model_id, vendor_id) values (?, null)", MODEL_NO_VENDOR_2);
        qaJdbcTemplate.update("insert into qa.model_vendor(model_id, vendor_id) values (?, ?)", MODEL_VENDOR_2, VENDOR_ID_2);
        qaJdbcTemplate.update("insert into qa.model_vendor(model_id, vendor_id) values (?, ?)", MODEL_VENDOR_WITH_TRANSITION + 1, null);

        executor.updateVendorIds();

        // check processing models
        checkModelVendor(MODEL_VENDOR, VENDOR_ID);
        checkModelVendor(MODEL_VENDOR_WITH_TRANSITION + 1, VENDOR_ID);
        checkQuestionNullVendor(MODEL_NO_VENDOR);
        checkQuestionNullVendor(MODEL_VENDOR_WITH_TRANSITION);
        checkQuestionNullVendor(MODEL_NO_MODEL);

        // check skip models, they still the same
        checkModelVendor(MODEL_VENDOR_2, VENDOR_ID_2);
        checkQuestionNullVendor(MODEL_NO_VENDOR_2);
    }

    private void checkQuestionNullVendor(long modelId) {
        List<Long> vendorIds = qaJdbcTemplate.queryForList("select vendor_id from qa.model_vendor where model_id = ?",
            Long.class, modelId);

        assertEquals(1, vendorIds.size());
        assertNull(vendorIds.get(0));
    }

    private void checkModelVendor(long modelId, Long vendorId) {
        List<Long> vendorIds = qaJdbcTemplate.queryForList("select vendor_id from qa.model_vendor where model_id = ?",
            Long.class, modelId);

        assertEquals(1, vendorIds.size());
        assertNotNull(vendorIds.get(0));
        assertEquals(vendorId, vendorIds.get(0));
    }
}
