package ru.yandex.market.abo.core.cutoff.feature;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.cpa.MbiApiService;
import ru.yandex.market.core.feature.model.FeatureCutoffReason;
import ru.yandex.market.core.feature.model.FeatureCutoffType;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.param.model.ParamCheckStatus;
import ru.yandex.market.mbi.api.client.entity.CutoffActionStatus;
import ru.yandex.market.mbi.api.client.entity.GenericStatusResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbi.api.client.entity.GenericStatusResponse.OK_RESPONSE;

/**
 * @author artemmz
 * @date 05/03/2020.
 */
class FeatureCutoffManagerTest {
    private static final long SHOP_ID = 3123L;
    private static final FeatureType FEATURE_TYPE = FeatureType.MARKETPLACE;
    private static final FeatureCutoffType CUTOFF_TYPE = FeatureCutoffType.PINGER;
    private static final FeatureCutoffReason CUTOFF_REASON = FeatureCutoffReason.PINGER_API;
    @InjectMocks
    FeatureCutoffManager featureCutoffManager;
    @Mock
    MbiApiService mbiApiService;
    @Mock
    FeatureCutoffRepository cutoffRepository;
    @Mock
    FeatureCutoff cutoff;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mbiApiService.featureCutoff(eq(SHOP_ID), eq(FEATURE_TYPE), eq(CUTOFF_TYPE), any(), anyBoolean())).thenReturn(OK_RESPONSE);

        when(cutoffRepository.findTopByShopIdAndFeatureTypeAndFeatureCutoffTypeIdOrderByCreationTimeDesc(
                SHOP_ID, FEATURE_TYPE, CUTOFF_TYPE.getId())).thenReturn(Optional.of(cutoff));

        when(cutoff.getShopId()).thenReturn(SHOP_ID);
        when(cutoff.getFeatureType()).thenReturn(FEATURE_TYPE);
        when(cutoff.getFeatureCutoffType()).thenReturn(CUTOFF_TYPE);
    }

    @Test
    void testFeatureCutoff() {
        var cutoffBuilder = NoStatusFeatureCutoffBuilder.create(SHOP_ID, FEATURE_TYPE, CUTOFF_TYPE).withReason(CUTOFF_REASON);

        GenericStatusResponse mbiResp = featureCutoffManager.featureCutoff(cutoffBuilder, true);
        assertEquals(OK_RESPONSE, mbiResp);

        verify(mbiApiService, times(1)).featureCutoff(SHOP_ID, FEATURE_TYPE, CUTOFF_TYPE, null, true);

        verify(cutoffRepository, times(1)).save(expected(cutoffBuilder, true));
    }

    private static FeatureCutoff expected(NoStatusFeatureCutoffBuilder fromBuilder, boolean open) {
        FeatureCutoff cutoff = fromBuilder.build();
        cutoff.setOpened(CutoffActionStatus.OK);
        cutoff.setStatus(open ? ParamCheckStatus.FAIL : ParamCheckStatus.SUCCESS);
        return cutoff;
    }

    @ParameterizedTest
    @EnumSource(value = ParamCheckStatus.class)
    void removeManually(ParamCheckStatus dbCutoffStatus) {
        when(cutoff.getStatus()).thenReturn(dbCutoffStatus);

        if (dbCutoffStatus == ParamCheckStatus.SUCCESS) {
            assertThrows(IllegalArgumentException.class, () -> featureCutoffManager.removeCutoffManually(cutoff));
            verify(cutoffRepository, never()).save(any(FeatureCutoff.class));
        } else {
            featureCutoffManager.removeCutoffManually(cutoff);
            verify(cutoffRepository).save(cutoff);
            verify(cutoff).setOpened(CutoffActionStatus.OK);
            verify(cutoff).setStatus(ParamCheckStatus.SUCCESS);
        }
    }
}
