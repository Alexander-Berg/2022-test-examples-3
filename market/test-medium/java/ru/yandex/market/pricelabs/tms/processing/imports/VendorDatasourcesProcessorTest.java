package ru.yandex.market.pricelabs.tms.processing.imports;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pricelabs.CoreTestUtils;
import ru.yandex.market.pricelabs.model.NewVendorDatasource;
import ru.yandex.market.pricelabs.model.VendorDatasource;
import ru.yandex.market.pricelabs.model.types.AutostrategyTarget;
import ru.yandex.market.pricelabs.services.database.model.JobType;
import ru.yandex.market.pricelabs.tms.jobs.VendorBlueDatasourcesProcessingJob;
import ru.yandex.market.pricelabs.tms.processing.AbstractYTImportingProcessorTest;
import ru.yandex.market.pricelabs.tms.processing.YtSourceTargetScenarioExecutor;

public class VendorDatasourcesProcessorTest
        extends AbstractYTImportingProcessorTest<NewVendorDatasource, VendorDatasource> {

    @Autowired
    private VendorDatasourcesProcessor processor;

    @Autowired
    private VendorBlueDatasourcesProcessingJob job;

    //


    @Override
    public VendorDatasourcesProcessor getProcessor() {
        return processor;
    }

    @Override
    public VendorBlueDatasourcesProcessingJob getJob() {
        return job;
    }

    @Override
    protected JobType getJobType() {
        return JobType.SYNC_VENDOR_BLUE_DATASOURCES_PRIORITY;
    }

    @Override
    protected YtSourceTargetScenarioExecutor<NewVendorDatasource, VendorDatasource> newExecutor() {
        return executors.vendorDatasources();
    }

    @Override
    protected String getSourceCsv() {
        return "tms/processing/imports/vendor_datasources_source.csv";
    }

    @Override
    protected String getSourceCsv2() {
        return "tms/processing/imports/vendor_datasources_source2.csv";
    }

    @Override
    protected String getTargetCsv() {
        return "tms/processing/imports/vendor_datasources_target.csv";
    }

    @Override
    protected String getTargetCsv2() {
        return "tms/processing/imports/vendor_datasources_target2.csv";
    }

    @Override
    protected Class<NewVendorDatasource> getSourceClass() {
        return NewVendorDatasource.class;
    }

    @Override
    protected Class<VendorDatasource> getTargetClass() {
        return VendorDatasource.class;
    }

    @Test
    public void testSelectTarget() {
        getExecutor().insertSource(readSourceList());
        processor.sync();
        var targetList = processor.selectTarget(AutostrategyTarget.vendorBlue);
        CoreTestUtils.compare(asUpdated(readTargetList()), targetList);
    }
}
