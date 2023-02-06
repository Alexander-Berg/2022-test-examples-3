package ru.yandex.market.abo.core.cutoff.feature;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import ru.yandex.market.abo.core.message.Messages;
import ru.yandex.market.core.feature.model.FeatureCutoffReason;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.param.model.ParamCheckStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.abo.core.cutoff.feature.FeatureStatusManager.resolveTid;
import static ru.yandex.market.core.feature.model.FeatureType.PROMO_CPC;
import static ru.yandex.market.core.feature.model.FeatureType.RED_MARKET;
import static ru.yandex.market.core.param.model.ParamCheckStatus.FAIL;
import static ru.yandex.market.core.param.model.ParamCheckStatus.REVOKE;
import static ru.yandex.market.core.param.model.ParamCheckStatus.SUCCESS;

/**
 * tests for {@link FeatureStatusManager#resolveTid(ru.yandex.market.abo.core.cutoff.feature.FeatureCutoff)}
 *
 * @author antipov93.
 * @date 30.01.19.
 */
public class FeatureCutoffResolveTidTest {

    @Test
    void testTidIsNotNull() {
        FeatureCutoff cutoff = mockCutoff(FAIL, RED_MARKET, FeatureCutoffReason.PINGER_API, 1);
        assertEquals(1, (int) resolveTid(cutoff).orElse(0));
    }

    @Test
    void testStatusIsNotFailed() {
        FeatureCutoff cutoff = mockCutoff(SUCCESS, RED_MARKET, FeatureCutoffReason.PINGER_API, null);
        assertFalse(resolveTid(cutoff).isPresent());
    }

    /**
     * some old {@link ParamCheckStatus}es didn't have corresponding {@link FeatureType}s.
     */
    @Test
    void testFeatureTypeIsNull() {
        FeatureCutoff cutoff = mockCutoff(FAIL, null, FeatureCutoffReason.MANUAL, null);
        assertFalse(resolveTid(cutoff).isPresent());
    }

    /**
     * there's no (PROMO_CPC, PINGER_API) -> tid in {@link FeatureCutoffTemplate}
     */
    @Test
    void testCannotResolveTid() {
        FeatureCutoff cutoff = mockCutoff(FAIL, PROMO_CPC, FeatureCutoffReason.PINGER_API, null);
        assertFalse(resolveTid(cutoff).isPresent());
    }

    @Test
    void testRevokeRedMarketByPinger() {
        FeatureCutoff cutoff = mockCutoff(REVOKE, RED_MARKET, FeatureCutoffReason.PINGER_API, null);
        Optional<Integer> optTid = resolveTid(cutoff);
        assertTrue(optTid.isPresent());
        assertEquals(Messages.MBI.RED_API, optTid.get().intValue());
    }

    @Test
    void testPromoManualFail() {
        FeatureCutoff cutoff = mockCutoff(FAIL, PROMO_CPC, FeatureCutoffReason.MANUAL, null);
        Optional<Integer> optTid = resolveTid(cutoff);
        assertTrue(optTid.isPresent());
        assertEquals(Messages.MBI.PROMO_MANUAL_FAIL, optTid.get().intValue());
    }

    @Test
    void testPromoRecheckFail() {
        FeatureCutoff cutoff = mockCutoff(FAIL, PROMO_CPC, FeatureCutoffReason.RECHECK, null);
        Optional<Integer> optTid = resolveTid(cutoff);
        assertTrue(optTid.isPresent());
        assertEquals(Messages.MBI.PROMO_RECHECK_FAIL, optTid.get().intValue());
    }

    private static FeatureCutoff mockCutoff(ParamCheckStatus status,
                                            FeatureType featureType,
                                            FeatureCutoffReason reason,
                                            Integer tid) {
        FeatureCutoff mock = mock(FeatureCutoff.class);
        when(mock.getStatus()).thenReturn(status);
        when(mock.getFeatureType()).thenReturn(featureType);
        when(mock.getReason()).thenReturn(reason);
        when(mock.getTid()).thenReturn(tid);
        return mock;
    }
}
