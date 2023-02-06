package ru.yandex.market.vendors.analytics.tms.jobs.cutoff;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendors.analytics.core.mds.MdsJsonReader;
import ru.yandex.market.vendors.analytics.tms.FunctionalTest;

import static org.mockito.Mockito.when;

/**
 * Functional tests for {@link ImportVendorCutoffsExecutor}.
 *
 * @author fbokovikov
 */
class ImportVendorCutoffsExecutorTest extends FunctionalTest {
    @Autowired
    private MdsJsonReader mdsJsonReader;

    @Autowired
    private ImportVendorCutoffsExecutor job;

    @Test
    @DbUnitDataSet(
            before = "ImportVendorCutoffsExecutorTest.doJob.before.csv",
            after = "ImportVendorCutoffsExecutorTest.doJob.after.csv"
    )
    void doJob() {
        when(mdsJsonReader.readFile(
                "https://s3.mdst.yandex.net/vendors-public/analytics/analytics-cutoffs.json",
                ImportVendorCutoffsExecutor.VendorCutoffsDTO[].class
        )).thenReturn(
                mockResponse()
        );
        job.doJob(null);
    }

    private static ImportVendorCutoffsExecutor.VendorCutoffsDTO[] mockResponse() {
        var cutoff1 = new ImportVendorCutoffsExecutor.CutoffDTO();
        cutoff1.setId(1000);
        cutoff1.setFromTime(Instant.now());
        cutoff1.setType(1);

        var cutoff2 = new ImportVendorCutoffsExecutor.CutoffDTO();
        cutoff2.setId(1001);
        cutoff2.setFromTime(Instant.now());
        cutoff2.setType(5);

        var cutoff3 = new ImportVendorCutoffsExecutor.CutoffDTO();
        cutoff3.setId(2000);
        cutoff3.setFromTime(Instant.now());
        cutoff3.setType(1);

        var cutoff4 = new ImportVendorCutoffsExecutor.CutoffDTO();
        cutoff4.setId(3000);
        cutoff4.setFromTime(Instant.now());
        cutoff4.setType(1);

        var vendor1 = new ImportVendorCutoffsExecutor.VendorCutoffsDTO();
        vendor1.setVendorId(100L);
        vendor1.setCutoffs(List.of(cutoff1, cutoff2));

        var vendor2 = new ImportVendorCutoffsExecutor.VendorCutoffsDTO();
        vendor2.setVendorId(200L);
        vendor2.setCutoffs(List.of(cutoff3));

        var vendor3 = new ImportVendorCutoffsExecutor.VendorCutoffsDTO();
        vendor3.setVendorId(300L);
        vendor3.setCutoffs(List.of(cutoff4));

        var vendor4 = new ImportVendorCutoffsExecutor.VendorCutoffsDTO();
        vendor4.setVendorId(400L);
        vendor4.setCutoffs(List.of(cutoff4));

        var vendor5 = new ImportVendorCutoffsExecutor.VendorCutoffsDTO();
        vendor5.setVendorId(500L);
        vendor5.setCutoffs(List.of(cutoff4));

        return new ImportVendorCutoffsExecutor.VendorCutoffsDTO[]{vendor1, vendor2, vendor3, vendor4, vendor5};
    }
}
