package ru.yandex.market.abo.core.export.rating;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.abo.api.entity.rating.operational.RatingMetric;
import ru.yandex.market.abo.core.rating.partner.PartnerRatingActual;
import ru.yandex.market.abo.core.rating.partner.PartnerRatingService;
import ru.yandex.market.abo.core.rating.partner.details.ComponentDetails;
import ru.yandex.market.abo.core.rating.partner.details.PartnerRatingDetails;
import ru.yandex.market.abo.cpa.order.model.PartnerModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 05.02.2021
 */
class OperationalRatingProcessorTest {

    private static final long FF_PARTNER_ID = 123L;

    private static final double FF_LATE_SHIP_RATE = 5.0;
    private static final double FF_PLAN_FACT_RATE = 3.4;
    private static final double FF_RETURN_RATE = 0.3;
    private static final double FF_TOTAL = 96.43;

    private static final long DROPSHIP_ID = 124L;

    private static final double LATE_SHIP_RATE = 12.0;
    private static final double CANCELLATION_RATE = 3.0;
    private static final double RETURN_RATE = 1.5;
    private static final double DROPSHIP_TOTAL = 83.45;

    private static final LocalDateTime CALC_TIME = LocalDateTime.now().minusDays(2);

    private static final String EXPECTED_CSV_EXPORT = String.join("\n", List.of(
            PartnerRatingForExport.HEADER,
            DateUtil.asDate(CALC_TIME).getTime() + ";" + DROPSHIP_ID + ";" + LATE_SHIP_RATE + ";"
                    + CANCELLATION_RATE + ";" + RETURN_RATE + ";" + DROPSHIP_TOTAL + ";0;0;0;0;0;0;0;0;0",
            DateUtil.asDate(CALC_TIME).getTime() + ";" + FF_PARTNER_ID + ";0;0;0;" + FF_TOTAL + ";"
                    + "0;0;0;" + FF_PLAN_FACT_RATE + ";" + FF_RETURN_RATE + ";" + FF_LATE_SHIP_RATE + ";0;0;0"
    ));


    @InjectMocks
    private OperationalRatingProcessor operationalRatingProcessor;

    @Mock
    private PartnerRatingService partnerRatingService;

    @Mock
    private PartnerRatingActual dsbbRating;
    @Mock
    private PartnerRatingActual ffRating;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);

        when(dsbbRating.getPartnerId()).thenReturn(DROPSHIP_ID);
        when(dsbbRating.getPartnerModel()).thenReturn(PartnerModel.DSBB);
        when(dsbbRating.getCalcTime()).thenReturn(CALC_TIME);
        when(dsbbRating.getDetails()).thenReturn(new PartnerRatingDetails(
                10,
                List.of(new ComponentDetails(RatingMetric.LATE_SHIP_RATE, LATE_SHIP_RATE),
                        new ComponentDetails(RatingMetric.CANCELLATION_RATE, CANCELLATION_RATE),
                        new ComponentDetails(RatingMetric.RETURN_RATE, RETURN_RATE))
        ));
        when(dsbbRating.getTotal()).thenReturn(DROPSHIP_TOTAL);

        when(ffRating.getPartnerId()).thenReturn(FF_PARTNER_ID);
        when(ffRating.getPartnerModel()).thenReturn(PartnerModel.FULFILLMENT);
        when(ffRating.getCalcTime()).thenReturn(CALC_TIME);
        when(ffRating.getOrdersCount()).thenReturn(10);
        when(ffRating.getDetails()).thenReturn(new PartnerRatingDetails(
                10,
                List.of(new ComponentDetails(RatingMetric.FF_PLANFACT_RATE, FF_PLAN_FACT_RATE),
                        new ComponentDetails(RatingMetric.FF_LATE_SHIP_RATE, FF_LATE_SHIP_RATE),
                        new ComponentDetails(RatingMetric.FF_RETURN_RATE, FF_RETURN_RATE))
        ));
        when(ffRating.getTotal()).thenReturn(FF_TOTAL);

        when(partnerRatingService.getActualForMarket()).thenReturn(List.of(dsbbRating, ffRating));
    }

    @Test
    void csvGenerationTest() {
        String actualCsvReport = String.join("\n", operationalRatingProcessor.getReportRows());
        assertEquals(EXPECTED_CSV_EXPORT, actualCsvReport);
    }
}
