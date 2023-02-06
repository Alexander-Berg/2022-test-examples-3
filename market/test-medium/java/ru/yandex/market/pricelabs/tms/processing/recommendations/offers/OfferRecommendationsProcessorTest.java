package ru.yandex.market.pricelabs.tms.processing.recommendations.offers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pricelabs.model.recommendation.NewOfferRecommendation;
import ru.yandex.market.pricelabs.model.recommendation.OfferRecommendation;
import ru.yandex.market.pricelabs.services.database.model.JobType;
import ru.yandex.market.pricelabs.tms.jobs.imports.recommendations.OffersRecommendationsProcessorJob;
import ru.yandex.market.pricelabs.tms.processing.AbstractYTImportingProcessorTest;
import ru.yandex.market.pricelabs.tms.processing.YtSourceTargetScenarioExecutor;
import ru.yandex.market.pricelabs.tms.processing.recommendations.OffersRecommendationsProcessor;

public class OfferRecommendationsProcessorTest extends AbstractYTImportingProcessorTest<NewOfferRecommendation,
        OfferRecommendation> {


    @Autowired
    private OffersRecommendationsProcessor processor;

    @Autowired
    private OffersRecommendationsProcessorJob job;

    //

    @Override
    protected OffersRecommendationsProcessor getProcessor() {
        return processor;
    }

    @Override
    protected YtSourceTargetScenarioExecutor<NewOfferRecommendation, OfferRecommendation> newExecutor() {
        return executors.offersRecommender();
    }

    @Override
    protected OffersRecommendationsProcessorJob getJob() {
        return job;
    }

    @Override
    protected JobType getJobType() {
        return JobType.SYNC_OFFER_RECOMMENDER_PRIORITY;
    }

    @Override
    public List<NewOfferRecommendation> readSourceList2() {
        return super.readSourceList2();
    }

    @Override
    protected String getSourceCsv() {
        return "tms/processing/offers/recommendations/recommended_bids_source.csv";
    }

    @Override
    protected String getSourceCsv2() {
        return "tms/processing/offers/recommendations/recommended_bids_source.csv";
    }

    @Override
    protected String getTargetCsv() {
        return "tms/processing/offers/recommendations/recommended_bids_target.csv";
    }

    @Override
    protected String getTargetCsv2() {
        return "tms/processing/offers/recommendations/recommended_bids_target.csv";
    }

    @Override
    protected Class<NewOfferRecommendation> getSourceClass() {
        return NewOfferRecommendation.class;
    }

    @Override
    protected Class<OfferRecommendation> getTargetClass() {
        return OfferRecommendation.class;
    }
}
