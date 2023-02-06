package ru.yandex.market.pricelabs.tms.processing.imports;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pricelabs.model.program.NewPartnerBusiness;
import ru.yandex.market.pricelabs.model.program.PartnerBusiness;
import ru.yandex.market.pricelabs.services.database.model.JobType;
import ru.yandex.market.pricelabs.tms.jobs.PartnerBusinessProcessingJob;
import ru.yandex.market.pricelabs.tms.processing.AbstractYTImportingProcessorTest;
import ru.yandex.market.pricelabs.tms.processing.YtSourceTargetScenarioExecutor;

public class PartnerBusinessProcessorTest extends AbstractYTImportingProcessorTest<NewPartnerBusiness,
        PartnerBusiness> {

    @Autowired
    private PartnerBusinessProcessor processor;

    @Autowired
    private PartnerBusinessProcessingJob job;

    @Override
    public PartnerBusinessProcessor getProcessor() {
        return processor;
    }

    @Override
    public PartnerBusinessProcessingJob getJob() {
        return job;
    }

    @Override
    protected JobType getJobType() {
        return JobType.PARTNER_BUSINESS_PRIORITY;
    }

    @Override
    protected YtSourceTargetScenarioExecutor<NewPartnerBusiness, PartnerBusiness> newExecutor() {
        return executors.partnerBusiness();
    }

    @Override
    protected String getSourceCsv() {
        return "tms/processing/imports/partner_business_source.csv";
    }

    @Override
    protected String getSourceCsv2() {
        return "tms/processing/imports/partner_business_source2.csv";
    }

    @Override
    protected String getTargetCsv() {
        return "tms/processing/imports/partner_business_target.csv";
    }

    @Override
    protected String getTargetCsv2() {
        return "tms/processing/imports/partner_business_target2.csv";
    }

    @Override
    protected Class<NewPartnerBusiness> getSourceClass() {
        return NewPartnerBusiness.class;
    }

    @Override
    protected Class<PartnerBusiness> getTargetClass() {
        return PartnerBusiness.class;
    }
}
