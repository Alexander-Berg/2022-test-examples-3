package ru.yandex.market.abo.cpa.pinger;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.checkorder.CheckOrderDbService;
import ru.yandex.market.abo.core.cutoff.feature.FeatureCutoff;
import ru.yandex.market.abo.core.cutoff.feature.FeatureCutoffManager;
import ru.yandex.market.abo.core.feature.ShopFeatureListItemShort;
import ru.yandex.market.abo.cpa.MbiApiService;
import ru.yandex.market.abo.cpa.pinger.accept.PingerAcceptOrder;
import ru.yandex.market.abo.cpa.pinger.accept.PingerAcceptOrderRepo;
import ru.yandex.market.abo.test.TestHelper;
import ru.yandex.market.abo.util.db.toggle.ToggleService;
import ru.yandex.market.core.feature.model.FeatureCutoffType;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.param.model.ParamCheckStatus;
import ru.yandex.market.core.partner.placement.PartnerPlacementProgramType;
import ru.yandex.market.mbi.api.client.entity.abo.AboCutoffInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.abo.AboCutoff.SELFCHECK_REQUIRED;

/**
 * @author artemmz
 * @date 05/11/2020.
 */
class PingerShopsProviderTest {
    private static final Long SHOP_ID = 43253453L;
    @InjectMocks
    PingerShopsProvider pingerShopsProvider;

    @Mock
    MbiApiService mbiApiService;
    @Mock
    FeatureCutoffManager featureCutoffManager;
    @Mock
    CheckOrderDbService checkOrderDbService;
    @Mock
    PingerAcceptOrderRepo pingerAcceptOrderRepo;
    @Mock
    ToggleService dbToggleService;

    @Mock
    ExecutorService outerPool;
    @Mock
    ExecutorService innerPool;

    @Mock
    ShopFeatureListItemShort marketplaceFeature;
    @Mock
    ShopFeatureListItemShort featureToPing;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(mbiApiService.getShopsWithEnabledFeature(FeatureType.MARKETPLACE)).thenReturn(List.of(marketplaceFeature));
        when(mbiApiService.getShopsWithEnabledFeature(not(eq(FeatureType.MARKETPLACE)))).thenReturn(List.of(featureToPing));

        when(featureToPing.getShopId()).thenReturn(SHOP_ID);
        when(marketplaceFeature.isSuccess()).thenReturn(true);
        when(marketplaceFeature.getShopId()).thenReturn(SHOP_ID);

        when(checkOrderDbService.findChecksInProgress()).thenReturn(Collections.emptyList());

        TestHelper.mockExecutorService(outerPool, innerPool);
    }

    @ParameterizedTest
    @CsvSource({"SUCCESS, SUCCESS", "SUCCESS, FAIL", "FAIL, FAIL"})
    void testGetShopsToPing(ParamCheckStatus featureStatus, ParamCheckStatus marketplaceStatus) {
        when(marketplaceFeature.isSuccess()).thenReturn(marketplaceStatus.isSuccess());
        when(featureToPing.isSuccess()).thenReturn(featureStatus.isSuccess());

        Set<Long> shopsToPing = pingerShopsProvider.getShopsToPing();
        if (featureStatus.isSuccess() && marketplaceStatus.isSuccess()) {
            assertEquals(Set.of(SHOP_ID), shopsToPing);
        } else {
            assertTrue(shopsToPing.isEmpty());
        }
    }

    @Test
    void noPiPing() {
        when(featureToPing.getCpaPartnerInterface()).thenReturn(true);
        when(featureToPing.isSuccess()).thenReturn(true);
        when(marketplaceFeature.isSuccess()).thenReturn(true);
        assertTrue(pingerShopsProvider.getShopsToPing().isEmpty());
    }

    @Test
    void noAcceptPing() {
        when(featureToPing.isSuccess()).thenReturn(true);
        when(marketplaceFeature.isSuccess()).thenReturn(true);
        when(dbToggleService.configEnabled(any())).thenReturn(true);
        when(pingerAcceptOrderRepo.findAllByFinishTimeIsNull()).thenReturn(Collections.emptyList());
        assertFalse(pingerShopsProvider.getShopsToPing().isEmpty());

        var order = mock(PingerAcceptOrder.class);
        when(order.getPartnerId()).thenReturn(SHOP_ID);
        when(pingerAcceptOrderRepo.findAllByFinishTimeIsNull()).thenReturn(List.of(order));
        assertTrue(pingerShopsProvider.getShopsToPing().isEmpty());
    }

    @Test
    void noPingWhenSelfcheckRequired() {
        when(mbiApiService.getShopsWithAboCutoff(eq(SELFCHECK_REQUIRED))).thenReturn(Set.of(SHOP_ID));
        when(featureToPing.isSuccess()).thenReturn(true);
        when(marketplaceFeature.isSuccess()).thenReturn(true);

        assertTrue(pingerShopsProvider.getShopsToPing().isEmpty());
    }

    @Test
    void shopHasCutoffsPreventingPinger() {
        when(mbiApiService.getAboCutoffs(eq(SHOP_ID)))
                .thenReturn(List.of(
                        new AboCutoffInfo(SELFCHECK_REQUIRED, true, PartnerPlacementProgramType.DROPSHIP_BY_SELLER))
                );

        assertFalse(pingerShopsProvider.shopHasNoCutoffsPreventingPinger(SHOP_ID));
    }

    @Test
    void shopHasNoCutoffsPreventingPinger() {
        when(mbiApiService.getAboCutoffs(eq(SHOP_ID)))
                .thenReturn(List.of(
                        new AboCutoffInfo(SELFCHECK_REQUIRED, false, PartnerPlacementProgramType.DROPSHIP_BY_SELLER))
                );

        assertTrue(pingerShopsProvider.shopHasNoCutoffsPreventingPinger(SHOP_ID));
    }

    @Test
    void shopHasManualCutoff() {
        when(mbiApiService.getAboCutoffs(eq(SHOP_ID)))
                .thenReturn(List.of(
                        new AboCutoffInfo(SELFCHECK_REQUIRED, false, PartnerPlacementProgramType.DROPSHIP_BY_SELLER))
                );

        FeatureCutoff manualCutoff = new FeatureCutoff() {{
            setFeatureCutoffTypeId(1009);
            setStatus(ParamCheckStatus.FAIL);
        }};

        when(featureCutoffManager
                .lastFeatureState(eq(SHOP_ID), eq(FeatureCutoffType.MANUAL)))
                .thenReturn(Optional.of(manualCutoff));

        assertFalse(pingerShopsProvider.shopHasNoCutoffsPreventingPinger(SHOP_ID));
    }
}
