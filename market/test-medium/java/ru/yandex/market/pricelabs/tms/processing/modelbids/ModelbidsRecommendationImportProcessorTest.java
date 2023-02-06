package ru.yandex.market.pricelabs.tms.processing.modelbids;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pricelabs.misc.Utils;
import ru.yandex.market.pricelabs.model.NewBrandModelId;
import ru.yandex.market.pricelabs.model.NewModelbidsRecommendation;
import ru.yandex.market.pricelabs.services.database.model.JobType;
import ru.yandex.market.pricelabs.tms.jobs.PostProcessingModelbidsRecommendation;
import ru.yandex.market.pricelabs.tms.processing.AbstractYTImportingProcessorTest;
import ru.yandex.market.pricelabs.tms.processing.YtSourceTargetScenarioExecutor;
import ru.yandex.market.tms.quartz2.model.Executor;

public class ModelbidsRecommendationImportProcessorTest extends
        AbstractYTImportingProcessorTest<NewBrandModelId, NewModelbidsRecommendation> {

    @Autowired
    private ModelbidsRecommendationImportProcessor processor;

    @Autowired
    private PostProcessingModelbidsRecommendation job;

    //

    @Override
    protected List<NewModelbidsRecommendation> updateRows(List<NewModelbidsRecommendation> oldRows,
                                                          List<NewModelbidsRecommendation> newRows) {

        var oldMap = toMap(oldRows);
        var newMap = toMap(newRows);
        oldMap.putAll(newMap);

        var list = new ArrayList<>(oldMap.values());
        list.sort(Comparator.comparingInt(NewModelbidsRecommendation::getRegion_id)
                .thenComparingInt(NewModelbidsRecommendation::getModel_id));
        return list;
    }

    private Map<String, NewModelbidsRecommendation> toMap(List<NewModelbidsRecommendation> list) {
        return Utils.toMap(list, r -> r.getRegion_id() + "@" + r.getModel_id());
    }

    @Override
    public ModelbidsRecommendationImportProcessor getProcessor() {
        return processor;
    }

    @Override
    public Executor getJob() {
        return ctx -> job.scheduleImportFromYt(null);
    }

    @Override
    protected JobType getJobType() {
        return JobType.MODELBIDS_IMPORT;
    }

    @Override
    protected YtSourceTargetScenarioExecutor<NewBrandModelId, NewModelbidsRecommendation> newExecutor() {
        return executors.modelBidsImport();
    }

    @Override
    protected String getSourceCsv() {
        return "tms/processing/modelbids/modelbids_recommendation_import_source.csv";
    }

    @Override
    protected String getSourceCsv2() {
        return "tms/processing/modelbids/modelbids_recommendation_import_source2.csv";
    }

    @Override
    protected String getTargetCsv() {
        return "tms/processing/modelbids/modelbids_recommendation_import_target.csv";
    }

    @Override
    protected String getTargetCsv2() {
        return "tms/processing/modelbids/modelbids_recommendation_import_target2.csv";
    }

    @Override
    protected Class<NewBrandModelId> getSourceClass() {
        return NewBrandModelId.class;
    }

    @Override
    protected Class<NewModelbidsRecommendation> getTargetClass() {
        return NewModelbidsRecommendation.class;
    }
}
