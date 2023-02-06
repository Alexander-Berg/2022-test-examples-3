package ru.yandex.market.abo.core.rating.partner.report;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.api.entity.rating.operational.RatingMetric;
import ru.yandex.market.abo.core.rating.partner.report.loader.DsbbRatingReportPartsLoader;
import ru.yandex.market.abo.core.rating.partner.report.loader.DsbsRatingReportPartsLoader;
import ru.yandex.market.abo.core.rating.partner.report.loader.FulfillmentYtReportPartsLoader;
import ru.yandex.market.abo.core.rating.partner.report.model.PartnerRatingReportPart;
import ru.yandex.market.abo.core.rating.partner.report.service.PartnerRatingReportPartService;
import ru.yandex.market.abo.cpa.order.model.PartnerModel;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 10.12.2020
 */
class PartnerRatingReportManagerTest {

    private PartnerRatingReportManager partnerRatingReportManager;

    @Mock
    private PartnerRatingReportPartService reportPartService;
    @Mock
    private FulfillmentYtReportPartsLoader fulfillmentYtReportPartsLoader;
    @Mock
    DsbbRatingReportPartsLoader dsbbRatingReportPartsLoader;
    @Mock
    DsbsRatingReportPartsLoader dsbsRatingReportPartsLoader;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);

        when(fulfillmentYtReportPartsLoader.acceptedModel()).thenReturn(PartnerModel.FULFILLMENT);
        when(dsbbRatingReportPartsLoader.acceptedModel()).thenReturn(PartnerModel.DSBB);
        when(dsbsRatingReportPartsLoader.acceptedModel()).thenReturn(PartnerModel.DSBS);
    }

    @Test
    void updateReportParts__alreadyUpdated() {
        partnerRatingReportManager = new PartnerRatingReportManager(
                reportPartService,
                List.of(
                        fulfillmentYtReportPartsLoader,
                        dsbbRatingReportPartsLoader,
                        dsbbRatingReportPartsLoader,
                        dsbsRatingReportPartsLoader,
                        dsbsRatingReportPartsLoader
                )
        );

        when(reportPartService.isReportPartsUpdated(eq(PartnerModel.FULFILLMENT), any())).thenReturn(true);
        when(reportPartService.isReportPartsUpdated(eq(PartnerModel.DSBB), any())).thenReturn(true);
        when(reportPartService.isReportPartsUpdated(eq(PartnerModel.DSBS), any())).thenReturn(true);

        partnerRatingReportManager.updateReportParts(PartnerModel.DSBB);
        partnerRatingReportManager.updateReportParts(PartnerModel.DSBS);
        partnerRatingReportManager.updateReportParts(PartnerModel.FULFILLMENT);

        verify(reportPartService, never()).saveNewParts(any(), any());
        verify(reportPartService, never()).saveReportPartsUpdateFlag(any(), any());
        verify(fulfillmentYtReportPartsLoader, never()).loadReportParts(any());
        verify(dsbbRatingReportPartsLoader, never()).loadReportParts(any());
        verify(dsbbRatingReportPartsLoader, never()).loadReportParts(any());
        verify(dsbsRatingReportPartsLoader, never()).loadReportParts(any());
        verify(dsbsRatingReportPartsLoader, never()).loadReportParts(any());
    }

    @Test
    void updateFFReportParts() {
        var reportPart = initReportPart(PartnerModel.FULFILLMENT, RatingMetric.FF_LATE_SHIP_RATE);
        when(fulfillmentYtReportPartsLoader.loadReportParts(any())).thenReturn(List.of(reportPart));

        partnerRatingReportManager = new PartnerRatingReportManager(
                reportPartService,
                List.of(fulfillmentYtReportPartsLoader)
        );

        when(reportPartService.isReportPartsUpdated(eq(PartnerModel.FULFILLMENT), any())).thenReturn(false);

        partnerRatingReportManager.updateReportParts(PartnerModel.FULFILLMENT);

        verify(fulfillmentYtReportPartsLoader).loadReportParts(LocalDate.now());
        verify(reportPartService).saveNewParts(PartnerModel.FULFILLMENT, List.of(reportPart));
        verify(reportPartService).saveReportPartsUpdateFlag(PartnerModel.FULFILLMENT, LocalDate.now());
    }

    @Test
    void updateDSBBReportParts() {
        var reportPart = initReportPart(PartnerModel.DSBB, RatingMetric.LATE_SHIP_RATE);
        when(dsbbRatingReportPartsLoader.loadReportParts(any())).thenReturn(List.of(reportPart));

        partnerRatingReportManager = new PartnerRatingReportManager(
                reportPartService,
                List.of(
                        dsbbRatingReportPartsLoader,
                        dsbbRatingReportPartsLoader
                )
        );

        partnerRatingReportManager.updateReportParts(PartnerModel.DSBB);

        verify(reportPartService).saveNewParts(PartnerModel.DSBB, List.of(reportPart));
        verify(reportPartService).saveReportPartsUpdateFlag(PartnerModel.DSBB, LocalDate.now());
    }

    @Test
    void updateDSBSReportParts() {
        var reportPartTesting = initReportPart(PartnerModel.DSBS, RatingMetric.DSBS_LATE_DELIVERY_RATE);
        when(dsbsRatingReportPartsLoader.loadReportParts(any())).thenReturn(List.of(reportPartTesting));

        var reportPart = initReportPart(PartnerModel.DSBS, RatingMetric.DSBS_LATE_DELIVERY_RATE);
        when(dsbsRatingReportPartsLoader.loadReportParts(any())).thenReturn(List.of(reportPart));

        partnerRatingReportManager = new PartnerRatingReportManager(
                reportPartService,
                List.of(
                        dsbsRatingReportPartsLoader,
                        dsbsRatingReportPartsLoader
                )
        );

        partnerRatingReportManager.updateReportParts(PartnerModel.DSBS);

        verify(reportPartService).saveNewParts(PartnerModel.DSBS, List.of(reportPart));
        verify(reportPartService).saveReportPartsUpdateFlag(PartnerModel.DSBS, LocalDate.now());
    }

    private PartnerRatingReportPart initReportPart(
            PartnerModel partnerModel, RatingMetric metric
    ) {
        var partnerRatingReportPart = Mockito.mock(PartnerRatingReportPart.class);
        when(partnerRatingReportPart.getType()).thenReturn(metric);
        when(reportPartService.isReportPartsUpdated(eq(partnerModel), any())).thenReturn(false);
        return partnerRatingReportPart;
    }
}
