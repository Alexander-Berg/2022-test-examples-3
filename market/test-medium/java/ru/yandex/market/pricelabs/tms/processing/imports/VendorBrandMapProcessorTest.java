package ru.yandex.market.pricelabs.tms.processing.imports;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pricelabs.model.NewVendorBrandMap;
import ru.yandex.market.pricelabs.model.VendorBrandMap;
import ru.yandex.market.pricelabs.services.database.model.JobType;
import ru.yandex.market.pricelabs.tms.jobs.VendorBlueMapProcessingJob;
import ru.yandex.market.pricelabs.tms.processing.AbstractYTImportingProcessorTest;
import ru.yandex.market.pricelabs.tms.processing.YtSourceTargetScenarioExecutor;

public class VendorBrandMapProcessorTest extends AbstractYTImportingProcessorTest<NewVendorBrandMap, VendorBrandMap> {

    @Autowired
    private VendorBrandMapProcessor processor;

    @Autowired
    private VendorBlueMapProcessingJob job;

    //


    @Override
    public VendorBrandMapProcessor getProcessor() {
        return processor;
    }

    @Override
    public VendorBlueMapProcessingJob getJob() {
        return job;
    }

    @Override
    protected JobType getJobType() {
        return JobType.SYNC_VENDOR_BLUE_MAP_PRIORITY;
    }

    @Override
    protected YtSourceTargetScenarioExecutor<NewVendorBrandMap, VendorBrandMap> newExecutor() {
        return executors.vendorBrandMap();
    }

    @Override
    protected String getSourceCsv() {
        return "tms/processing/imports/vendor_brand_map_source.csv";
    }

    @Override
    protected String getSourceCsv2() {
        return "tms/processing/imports/vendor_brand_map_source2.csv";
    }

    @Override
    protected String getTargetCsv() {
        return "tms/processing/imports/vendor_brand_map_target.csv";
    }

    @Override
    protected String getTargetCsv2() {
        return "tms/processing/imports/vendor_brand_map_target2.csv";
    }

    @Override
    protected Class<NewVendorBrandMap> getSourceClass() {
        return NewVendorBrandMap.class;
    }

    @Override
    protected Class<VendorBrandMap> getTargetClass() {
        return VendorBrandMap.class;
    }
}
