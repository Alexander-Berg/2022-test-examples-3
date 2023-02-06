package ru.yandex.market.mboc.common.offers.repository;

import java.time.temporal.ChronoUnit;

import Market.DataCamp.DataCampExplanation;
import Market.DataCamp.DataCampOfferMarketContent.MarketContentProcessing;
import Market.DataCamp.DataCampOfferMarketContent.ShopModelRating;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampOfferStatus;
import Market.DataCamp.DataCampResolution;
import Market.DataCamp.DataCampValidationResult;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import ru.yandex.market.mboc.common.contentprocessing.from.repository.QueueFromContentProcessingRepository;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.offers.model.ContentProcessingResponse;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.mboc.common.contentprocessing.from.repository.QueueFromContentProcessingRepository.CONTENT_PROCESSING_RESPONSE_TABLE;
import static ru.yandex.market.mboc.common.contentprocessing.from.repository.QueueFromContentProcessingRepository.ContentProcessingResponseQueueStats.SLA_HOURS;
import static ru.yandex.market.mboc.common.utils.DateTimeUtils.dateTimeNow;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.simpleOffer;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.simpleSupplier;

public class QueueFromContentProcessingRepositoryTest extends BaseDbTestClass {

    private static final String VALIDATION_TEXT = "Validation error";
    private static final String CODE = "market.ir";

    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private QueueFromContentProcessingRepository repository;

    @Test
    public void collectStats() {
        supplierRepository.insert(simpleSupplier());
        offerRepository.insertOffers(simpleOffer(1), simpleOffer(2), simpleOffer(3));

        repository.insertBatch(
                new ContentProcessingResponse(1L,
                        MarketContentProcessing.newBuilder().build(),
                    ShopModelRating.newBuilder().build(),
                    DataCampResolution.Resolution.newBuilder().build(),
                    0
                ),
                new ContentProcessingResponse(2L,
                        MarketContentProcessing.newBuilder().build(),
                        ShopModelRating.newBuilder().build(),
                        DataCampResolution.Resolution.newBuilder().build(),
                        0
                ),
                new ContentProcessingResponse(3L,
                        MarketContentProcessing.newBuilder().build(),
                        ShopModelRating.newBuilder().build(),
                        DataCampResolution.Resolution.newBuilder().build(),
                        0
                )
        );

        var params = new MapSqlParameterSource()
                .addValue("insertedOverSla1", dateTimeNow().minus(SLA_HOURS, ChronoUnit.HOURS))
                .addValue("insertedOverSla2", dateTimeNow().minus(SLA_HOURS + 1, ChronoUnit.HOURS));
        namedParameterJdbcTemplate.update("update " + CONTENT_PROCESSING_RESPONSE_TABLE
                + " set inserted = :insertedOverSla1 where offer_id = 1", params);
        namedParameterJdbcTemplate.update("update " + CONTENT_PROCESSING_RESPONSE_TABLE
                + " set inserted = :insertedOverSla2 where offer_id = 2", params);

        var stats = repository.collectStats();

        assertThat(stats.getInQueueCount()).isEqualTo(3);
        assertThat(stats.getOldestInQueueSec()).isEqualTo((SLA_HOURS + 1) * 3600);
        assertThat(stats.getOverSlaCount()).isEqualTo(2);
    }

    @Test
    public void testVerdictsInfoIsParsedCorrectly() {
        supplierRepository.insert(simpleSupplier());
        offerRepository.insertOffers(simpleOffer(1));
        DataCampResolution.Resolution resolution = DataCampResolution.Resolution.newBuilder()
            .addBySource(DataCampResolution.Verdicts.newBuilder()
                .addVerdict(DataCampResolution.Verdict.newBuilder()
                    .addResults(DataCampValidationResult.ValidationResult.newBuilder()
                        .addMessages(DataCampExplanation.Explanation.newBuilder()
                            .setText(VALIDATION_TEXT)
                            .setCode(CODE)
                            .setLevel(DataCampExplanation.Explanation.Level.FATAL)
                            .build())
                        .build())
                    .build())
                .build())
            .build();

        DataCampOfferStatus.OfferStatus offerStatus = DataCampOfferStatus.OfferStatus.newBuilder()
            .addDisabled(DataCampOfferMeta.Flag.newBuilder()
                .setFlag(true)
                .setMeta(DataCampOfferMeta.UpdateMeta.newBuilder()
                    .setSource(DataCampOfferMeta.DataSource.MARKET_DATACAMP).build())
                .build())
            .build();

        repository.insert(
            new ContentProcessingResponse(1L,
                MarketContentProcessing.newBuilder().build(),
                ShopModelRating.newBuilder().build(),
                resolution,
                0
            ));

        //Parsing verification
        ContentProcessingResponse processingResponse = repository.findById(1L);
        DataCampExplanation.Explanation message = processingResponse.getResolution()
            .getBySource(0)
            .getVerdict(0)
            .getResults(0)
            .getMessages(0);
        assertThat(message.getText()).isEqualTo(VALIDATION_TEXT);
        assertThat(message.getCode()).isEqualTo(CODE);
        assertThat(message.getLevel()).isEqualTo(DataCampExplanation.Explanation.Level.FATAL);
    }
}
