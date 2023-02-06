package ru.yandex.market.abo.core.rating.partner;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import one.util.streamex.EntryStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.api.entity.rating.operational.RatingMetric;
import ru.yandex.market.abo.core.rating.partner.details.ComponentDetails;
import ru.yandex.market.abo.core.rating.partner.details.PartnerRatingDetails;
import ru.yandex.market.abo.cpa.order.model.PartnerModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 15.10.2020
 */
class PartnerRatingRepoTest extends EmptyTest {

    private static final long PARTNER_ID = 123L;
    private static final int ORDERS_COUNT = 11;

    @Autowired
    private PartnerRatingRepo.RatingRepo ratingRepo;

    @Autowired
    private PartnerRatingRepo.RatingActualRepo ratingActualRepo;

    @Autowired
    private PartnerRatingRepo.RatingConfigRepo ratingConfigRepo;

    @Test
    void serializationTest() {
        var ratesMap = Map.of(
                RatingMetric.FF_PLANFACT_RATE, 8.0,
                RatingMetric.FF_LATE_SHIP_RATE, 5.0,
                RatingMetric.FF_RETURN_RATE, 0.5
        );

        var partnerRating = new PartnerRating(
                LocalDateTime.now(),
                PARTNER_ID,
                PartnerModel.FULFILLMENT,
                ratesMap,
                new PartnerRatingDetails(
                        ORDERS_COUNT,
                        EntryStream.of(ratesMap).mapKeyValue(ComponentDetails::new).toList()
                ),
                69.87
        );

        ratingRepo.save(partnerRating);
        ratingActualRepo.save(partnerRating.toActual());
        flushAndClear();

        assertEquals(partnerRating, ratingRepo.findAll().get(0));
    }

    @Test
    void saveAndFetchRatingConfig() {
        var partnerRatingConfig = PartnerRatingConfig.builder()
                .partnerId(PARTNER_ID)
                .partnerModel(PartnerModel.FULFILLMENT)
                .inboundAllowedOnLowRating(true)
                .build();
        PartnerRatingConfig savedConfig = ratingConfigRepo.save(partnerRatingConfig);
        Optional<PartnerRatingConfig> maybeFetchedConfig =
                ratingConfigRepo.findByPartnerIdAndPartnerModel(PARTNER_ID, PartnerModel.FULFILLMENT);
        assertTrue(maybeFetchedConfig.isPresent());
        PartnerRatingConfig fetchedConfig = maybeFetchedConfig.get();
        assertEquals(savedConfig, fetchedConfig);
    }

    @Test
    void testRepo() {
        var ratesMap = Map.of(
                RatingMetric.LATE_SHIP_RATE, 8.0,
                RatingMetric.CANCELLATION_RATE, 5.0,
                RatingMetric.RETURN_RATE, 0.5
        );
        var partnerRating = new PartnerRating(
                LocalDateTime.now(),
                PARTNER_ID,
                PartnerModel.DSBB,
                ratesMap,
                new PartnerRatingDetails(
                        ORDERS_COUNT,
                        EntryStream.of(ratesMap).mapKeyValue(ComponentDetails::new).toList()
                ),
                69.87
        );

        ratingRepo.save(partnerRating);
        ratingActualRepo.save(partnerRating.toActual());
        flushAndClear();

        ratingRepo.setActualFalse(PartnerModel.DSBB);
        var ratings = ratingRepo.findAll();
        var actualRatings = ratingActualRepo.findAll();
        assertFalse(ratings.get(0).getActual());
        assertEquals(1, actualRatings.size());

        ratingActualRepo.deleteAllByPartnerModel(PartnerModel.DSBB);

        actualRatings = ratingActualRepo.findAll();
        assertEquals(0, actualRatings.size());
    }
}
