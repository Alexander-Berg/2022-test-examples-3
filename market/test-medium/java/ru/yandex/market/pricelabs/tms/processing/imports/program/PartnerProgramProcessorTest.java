package ru.yandex.market.pricelabs.tms.processing.imports.program;

import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pricelabs.model.program.NewPartnerProgram;
import ru.yandex.market.pricelabs.model.program.PartnerProgram;
import ru.yandex.market.pricelabs.services.database.model.JobType;
import ru.yandex.market.pricelabs.tms.jobs.program.PartnerProgramProcessingJob;
import ru.yandex.market.pricelabs.tms.processing.AbstractYTImportingProcessorTest;
import ru.yandex.market.pricelabs.tms.processing.YtSourceTargetScenarioExecutor;

public class PartnerProgramProcessorTest extends AbstractYTImportingProcessorTest<NewPartnerProgram,
        PartnerProgram> {

    @Autowired
    private PartnerProgramProcessor processor;

    @Autowired
    private PartnerProgramProcessingJob job;

    @Override
    public PartnerProgramProcessor getProcessor() {
        return processor;
    }

    @Override
    public PartnerProgramProcessingJob getJob() {
        return job;
    }

    @Override
    protected JobType getJobType() {
        return JobType.PARTNER_PROGRAM_PRIORITY;
    }

    @Override
    protected YtSourceTargetScenarioExecutor<NewPartnerProgram, PartnerProgram> newExecutor() {
        return executors.partner();
    }

    @Override
    protected Consumer<PartnerProgram> getTargetUpdate() {
        return s -> {
            if (s.getShop_id() > 2) {
                s.setUpdated_at(getInstant());
            }
        };
    }

    @Override
    protected String getSourceCsv() {
        return "tms/processing/imports/program/partner_program_source.csv";
    }

    @Override
    protected String getSourceCsv2() {
        return "tms/processing/imports/program/partner_program_source2.csv";
    }

    @Override
    protected String getTargetCsv() {
        return "tms/processing/imports/program/partner_program_target.csv";
    }

    @Override
    protected String getTargetCsv2() {
        return "tms/processing/imports/program/partner_program_target2.csv";
    }

    @Override
    protected Class<NewPartnerProgram> getSourceClass() {
        return NewPartnerProgram.class;
    }

    @Override
    protected Class<PartnerProgram> getTargetClass() {
        return PartnerProgram.class;
    }
}
