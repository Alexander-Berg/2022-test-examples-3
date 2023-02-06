package ru.yandex.market.pricelabs.tms.processing.stats;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pricelabs.model.BlueBidsRecommendation;
import ru.yandex.market.pricelabs.model.NewBlueBidsRecommendation;
import ru.yandex.market.pricelabs.services.database.model.JobType;
import ru.yandex.market.pricelabs.tms.jobs.BlueBidsRecommenderProcessingJob;
import ru.yandex.market.pricelabs.tms.processing.AbstractYTImportingProcessorTest;
import ru.yandex.market.pricelabs.tms.processing.YtSourceTargetScenarioExecutor;

public class BlueBidsRecommenderProcessorTest
        extends AbstractYTImportingProcessorTest<NewBlueBidsRecommendation, BlueBidsRecommendation> {

    @Autowired
    private BlueBidsRecommenderProcessor processor;

    @Autowired
    private BlueBidsRecommenderProcessingJob job;

    //

    @Override
    protected BlueBidsRecommenderProcessor getProcessor() {
        return processor;
    }

    @Override
    protected YtSourceTargetScenarioExecutor<NewBlueBidsRecommendation, BlueBidsRecommendation> newExecutor() {
        return executors.blueBidsRecommender();
    }

    @Override
    protected BlueBidsRecommenderProcessingJob getJob() {
        return job;
    }

    @Override
    protected JobType getJobType() {
        return JobType.SYNC_BLUE_BIDS_RECOMMENDER_PRIORITY;
    }

    @Override
    protected String getSourceCsv() {
        return "tms/processing/stats/blue_bids_recommender_source.csv";
    }

    @Override
    protected String getSourceCsv2() {
        return "tms/processing/stats/blue_bids_recommender_source2.csv";
    }

    @Override
    protected String getTargetCsv() {
        return "tms/processing/stats/blue_bids_recommender_target.csv";
    }

    @Override
    protected String getTargetCsv2() {
        return "tms/processing/stats/blue_bids_recommender_target2.csv";
    }

    @Override
    protected Class<NewBlueBidsRecommendation> getSourceClass() {
        return NewBlueBidsRecommendation.class;
    }

    @Override
    protected Class<BlueBidsRecommendation> getTargetClass() {
        return BlueBidsRecommendation.class;
    }
}
