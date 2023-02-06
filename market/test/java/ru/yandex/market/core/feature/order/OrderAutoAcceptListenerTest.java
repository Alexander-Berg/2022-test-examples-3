package ru.yandex.market.core.feature.order;

import java.time.Clock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.api.cpa.CPADataPusher;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.feature.FeatureService;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.feature.model.ShopFeature;
import ru.yandex.market.core.param.model.ParamCheckStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link OrderAutoAcceptListener}
 */
@DbUnitDataSet(before = "OrderAutoAcceptListenerTest.before.csv")
class OrderAutoAcceptListenerTest extends FunctionalTest {

    @Autowired
    FeatureService featureService;

    @Autowired
    CPADataPusher cpaDataPusher;

    @Autowired
    private Clock clock;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(Clock.systemDefaultZone().instant());
    }

    @Test
    void testShopDataPushed() {
        featureService.changeStatus(0, ShopFeature.of(1, FeatureType.ORDER_AUTO_ACCEPT, ParamCheckStatus.NEW));
        assertThat(featureService.getFeature(1, FeatureType.ORDER_AUTO_ACCEPT).getStatus())
                .isEqualTo(ParamCheckStatus.SUCCESS);
        verify(cpaDataPusher).pushShopInfoToCheckout(1L);
    }

    @Test
    void testTransitionToFail() {
        featureService.changeStatus(0, ShopFeature.of(1, FeatureType.ORDER_AUTO_ACCEPT, ParamCheckStatus.FAIL));
        assertThat(featureService.getFeature(1, FeatureType.ORDER_AUTO_ACCEPT).getStatus())
                .isEqualTo(ParamCheckStatus.FAIL);

        featureService.changeStatus(0, ShopFeature.of(1, FeatureType.ORDER_AUTO_ACCEPT, ParamCheckStatus.DONT_WANT));
        assertThat(featureService.getFeature(1, FeatureType.ORDER_AUTO_ACCEPT).getStatus())
                .isEqualTo(ParamCheckStatus.DONT_WANT);
    }

    @Test
    void testTransitionToFailFromSuccess() {
        featureService.changeStatus(0, ShopFeature.of(1, FeatureType.ORDER_AUTO_ACCEPT, ParamCheckStatus.SUCCESS));
        featureService.changeStatus(0, ShopFeature.of(1, FeatureType.ORDER_AUTO_ACCEPT, ParamCheckStatus.FAIL));
        assertThat(featureService.getFeature(1, FeatureType.ORDER_AUTO_ACCEPT).getStatus())
                .isEqualTo(ParamCheckStatus.FAIL);
    }
}
