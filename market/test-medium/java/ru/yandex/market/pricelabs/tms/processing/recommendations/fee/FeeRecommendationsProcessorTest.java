package ru.yandex.market.pricelabs.tms.processing.recommendations.fee;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pricelabs.misc.TimingUtils;
import ru.yandex.market.pricelabs.model.recommendation.FeeRecommendation;
import ru.yandex.market.pricelabs.model.recommendation.FeeRecommendationSource;
import ru.yandex.market.pricelabs.services.database.model.JobType;
import ru.yandex.market.pricelabs.tms.jobs.imports.recommendations.FeeRecommendationsProcessorJob;
import ru.yandex.market.pricelabs.tms.processing.AbstractYTImportingProcessorTest;
import ru.yandex.market.pricelabs.tms.processing.YtSourceTargetScenarioExecutor;

/**
 * Date: 30.06.2022
 * Project: arcadia-market_pricelabs
 *
 * @author alexminakov
 */
@ParametersAreNonnullByDefault
public class FeeRecommendationsProcessorTest
        extends AbstractYTImportingProcessorTest<FeeRecommendationSource, FeeRecommendation> {

    private static final Instant DEFAULT_TIME = LocalDateTime
            .of(2022, 7, 4, 0, 0, 0)
            .toInstant(ZoneOffset.UTC);

    @Autowired
    private FeeRecommendationsProcessor processor;
    @Autowired
    private FeeRecommendationsProcessorJob job;

    @BeforeEach
    @Override
    protected void init() {
        super.init();
        TimingUtils.addTime(11 * 60 * 1000);
    }

    @Nonnull
    @Override
    protected Consumer<FeeRecommendation> getTargetUpdate() {
        return (priceRecommendation) -> {
            priceRecommendation.setComputation_datetime(DEFAULT_TIME);
            priceRecommendation.setUpdated_at(DEFAULT_TIME);
        };
    }

    @Nonnull
    @Override
    protected List<FeeRecommendation> asUpdated(List<FeeRecommendation> list) {
        list.forEach(p -> getTargetUpdate());
        return list;
    }

    @Nonnull
    @Override
    protected FeeRecommendationsProcessor getProcessor() {
        return processor;
    }

    @Nonnull
    @Override
    protected YtSourceTargetScenarioExecutor<FeeRecommendationSource, FeeRecommendation> newExecutor() {
        return executors.feeRecommendations();
    }

    @Nonnull
    @Override
    protected FeeRecommendationsProcessorJob getJob() {
        return job;
    }

    @Nonnull
    @Override
    protected JobType getJobType() {
        return JobType.SYNC_FEE_RECOMMENDATIONS_PRIORITY;
    }

    @Nonnull
    @Override
    public List<FeeRecommendationSource> readSourceList2() {
        return super.readSourceList2();
    }

    @Nonnull
    @Override
    protected String getSourceCsv() {
        return "tms/processing/offers/recommendations/fee_recommendation_source.csv";
    }

    @Nonnull
    @Override
    protected String getSourceCsv2() {
        return "tms/processing/offers/recommendations/fee_recommendation_source.csv";
    }

    @Nonnull
    @Override
    protected String getTargetCsv() {
        return "tms/processing/offers/recommendations/fee_recommendation_target.csv";
    }

    @Nonnull
    @Override
    protected String getTargetCsv2() {
        return "tms/processing/offers/recommendations/fee_recommendation_target.csv";
    }

    @Nonnull
    @Override
    protected Class<FeeRecommendationSource> getSourceClass() {
        return FeeRecommendationSource.class;
    }

    @Nonnull
    @Override
    protected Class<FeeRecommendation> getTargetClass() {
        return FeeRecommendation.class;
    }
}
