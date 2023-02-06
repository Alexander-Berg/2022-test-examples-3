package ru.yandex.market.abo.core.cutoff.feature.listener;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.cutoff.feature.FeatureCutoff;
import ru.yandex.market.abo.cpa.order.limit.CpaOrderLimitPartner;
import ru.yandex.market.abo.cpa.order.limit.count.CpaOrderCountService;
import ru.yandex.market.abo.cpa.order.model.PartnerModel;
import ru.yandex.market.abo.cpa.order.limit.CpaOrderLimitService;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.param.model.ParamCheckStatus;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.abo.core.rating.operational.OperationalRatingCalculator.RATING_DATE_FROM;
import static ru.yandex.market.abo.core.rating.operational.OperationalRatingService.NEW_SUPPLIER_ORDERS;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 06.11.2020
 */
class CrossdockNewbieLimitOrdersListenerTest {

    private static final long SHOP_ID = 123L;

    @InjectMocks
    private CrossdockNewbieLimitOrdersListener crossdockNewbieLimitOrdersListener;

    @Mock
    private CpaOrderLimitService cpaOrderLimitService;
    @Mock
    private CpaOrderCountService cpaOrderCountService;

    @Mock
    private FeatureCutoff featureCutoff;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);

        when(featureCutoff.getShopId()).thenReturn(SHOP_ID);
        when(featureCutoff.getFeatureType()).thenReturn(FeatureType.CROSSDOCK);
        when(featureCutoff.getStatus()).thenReturn(ParamCheckStatus.SUCCESS);
        when(featureCutoff.isExperiment()).thenReturn(false);
    }

    @Test
    void featureStatusChanged__notAcceptedFeature() {
        when(featureCutoff.getFeatureType()).thenReturn(FeatureType.DROPSHIP);

        crossdockNewbieLimitOrdersListener.featureStatusChanged(featureCutoff);

        verifyNoMoreInteractions(cpaOrderLimitService, cpaOrderCountService);
    }

    @Test
    void featureStatusChanged__notAcceptedStatus() {
        when(featureCutoff.getStatus()).thenReturn(ParamCheckStatus.FAIL);

        crossdockNewbieLimitOrdersListener.featureStatusChanged(featureCutoff);

        verifyNoMoreInteractions(cpaOrderLimitService, cpaOrderCountService);
    }

    @Test
    void featureStatusChanged__ordersCountMoreThenThreshold() {
        when(cpaOrderCountService.loadTotalCount(
                new CpaOrderLimitPartner(SHOP_ID, PartnerModel.CROSSDOCK)
        )).thenReturn(NEW_SUPPLIER_ORDERS + 1L);

        crossdockNewbieLimitOrdersListener.featureStatusChanged(featureCutoff);

        verifyNoMoreInteractions(cpaOrderLimitService);
    }

    @Test
    void featureStatusChanged_experiment() {
        when(featureCutoff.isExperiment()).thenReturn(true);

        crossdockNewbieLimitOrdersListener.featureStatusChanged(featureCutoff);

        verifyNoMoreInteractions(cpaOrderLimitService);
    }

    @Test
    void featureStatusChanged_limitCreated() {
        crossdockNewbieLimitOrdersListener.featureStatusChanged(featureCutoff);
        verify(cpaOrderLimitService).addTemporaryLimitForNewbieIfNotExceptional(SHOP_ID, PartnerModel.CROSSDOCK);
    }
}
