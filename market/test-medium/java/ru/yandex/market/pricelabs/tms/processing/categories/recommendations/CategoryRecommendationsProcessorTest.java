package ru.yandex.market.pricelabs.tms.processing.categories.recommendations;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pricelabs.model.program.NewProgramRecommendationSettings;
import ru.yandex.market.pricelabs.model.program.ProgramRecommendationSettings;
import ru.yandex.market.pricelabs.services.database.model.JobType;
import ru.yandex.market.pricelabs.tms.jobs.CategoryRecommendationsProcessorJob;
import ru.yandex.market.pricelabs.tms.processing.AbstractYTImportingProcessor;
import ru.yandex.market.pricelabs.tms.processing.AbstractYTImportingProcessorTest;
import ru.yandex.market.pricelabs.tms.processing.YtSourceTargetScenarioExecutor;
import ru.yandex.market.tms.quartz2.model.Executor;

public class CategoryRecommendationsProcessorTest extends
        AbstractYTImportingProcessorTest<NewProgramRecommendationSettings, ProgramRecommendationSettings> {

    @Autowired
    private CategoryRecommendationsProcessor processor;

    @Autowired
    private CategoryRecommendationsProcessorJob job;


    @Override
    protected Class<ProgramRecommendationSettings> getTargetClass() {
        return ProgramRecommendationSettings.class;
    }

    @Override
    protected Class<NewProgramRecommendationSettings> getSourceClass() {
        return NewProgramRecommendationSettings.class;
    }

    @Override
    protected AbstractYTImportingProcessor<NewProgramRecommendationSettings, ProgramRecommendationSettings>
    getProcessor() {
        return processor;
    }

    @Override
    protected YtSourceTargetScenarioExecutor<NewProgramRecommendationSettings, ProgramRecommendationSettings>
    newExecutor() {
        return executors.categoryRecommendations();
    }

    @Override
    protected Executor getJob() {
        return job;
    }

    @Override
    protected JobType getJobType() {
        return JobType.SYNC_CATEGORY_RECOMMENDATIONS;
    }


    @Override
    protected String getSourceCsv() {
        return "tms/processing/imports/category_recommendations_source.csv";
    }

    @Override
    protected String getSourceCsv2() {
        return "tms/processing/imports/category_recommendations_source2.csv";
    }

    @Override
    protected String getTargetCsv() {
        return "tms/processing/imports/category_recommendations_target.csv";
    }

    @Override
    protected String getTargetCsv2() {
        return "tms/processing/imports/category_recommendations_target2.csv";
    }


}
