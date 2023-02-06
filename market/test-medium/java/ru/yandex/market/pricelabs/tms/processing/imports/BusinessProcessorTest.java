package ru.yandex.market.pricelabs.tms.processing.imports;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pricelabs.model.Business;
import ru.yandex.market.pricelabs.model.NewBusiness;
import ru.yandex.market.pricelabs.model.types.Status;
import ru.yandex.market.pricelabs.services.database.model.JobType;
import ru.yandex.market.pricelabs.tms.jobs.BusinessProcessingJob;
import ru.yandex.market.pricelabs.tms.processing.AbstractYTImportingProcessorTest;
import ru.yandex.market.pricelabs.tms.processing.YtSourceTargetScenarioExecutor;

public class BusinessProcessorTest extends AbstractYTImportingProcessorTest<NewBusiness, Business> {

    @Autowired
    private BusinessProcessor processor;

    @Autowired
    private BusinessProcessingJob job;

    @Override
    public BusinessProcessor getProcessor() {
        return processor;
    }

    @Override
    public BusinessProcessingJob getJob() {
        return job;
    }

    @Override
    protected JobType getJobType() {
        return JobType.SYNC_BUSINESS_PRIORITY;
    }

    @Override
    protected YtSourceTargetScenarioExecutor<NewBusiness, Business> newExecutor() {
        return executors.business();
    }

    @Override
    protected List<Business> updateRows(List<Business> oldRows, List<Business> newRows) {
        oldRows.forEach(b -> b.setStatus(Status.DELETED));
        var updatedRowsMap = oldRows.stream()
                .collect(Collectors.toMap(Business::getId, Function.identity()));
        newRows.forEach(b -> updatedRowsMap.put(b.getId(), b));
        return new ArrayList<>(updatedRowsMap.values());
    }

    @Override
    protected String getSourceCsv() {
        return "tms/processing/imports/business_source.csv";
    }

    @Override
    protected String getSourceCsv2() {
        return "tms/processing/imports/business_source2.csv";
    }

    @Override
    protected String getTargetCsv() {
        return "tms/processing/imports/business_target.csv";
    }

    @Override
    protected String getTargetCsv2() {
        return "tms/processing/imports/business_target2.csv";
    }

    @Override
    protected Class<NewBusiness> getSourceClass() {
        return NewBusiness.class;
    }

    @Override
    protected Class<Business> getTargetClass() {
        return Business.class;
    }
}
