package ru.yandex.market.core.feature.direct;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.feature.FeatureService;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.feature.model.ShopFeature;
import ru.yandex.market.core.param.model.ParamCheckStatus;

/**
 * Тесты для {@link DirectFeatureListener}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class DirectFeatureListenerTest extends FunctionalTest {

    @Autowired
    private FeatureService featureService;

    @DisplayName("У чисто-ТВ магазина появляется фича потребителя фидов. Ставится FORCE_ALIVE")
    @DbUnitDataSet(
            before = "DirectFeatureListenerTest.testEnableVertical.before.csv",
            after = "DirectFeatureListenerTest.testEnableVertical.after.csv"
    )
    void testEnableVerticalFeature() {
        ShopFeature feature = ShopFeature.of(1001L, FeatureType.VERTICAL_SHARE, ParamCheckStatus.SUCCESS);
        featureService.changeStatus(1L, feature);
    }

    @DisplayName("У директового магазина появляется фича потребителя фидов. Ставится FORCE_ALIVE")
    @DbUnitDataSet(
            before = "DirectFeatureListenerTest.testEnableDirect.before.csv",
            after = "DirectFeatureListenerTest.testEnableDirect.after.csv"
    )
    void testEnableDirectFeature() {
        ShopFeature firstFeature = ShopFeature.of(1001L, FeatureType.DIRECT_STANDBY, ParamCheckStatus.SUCCESS);
        ShopFeature secondFeature = ShopFeature.of(1001L, FeatureType.DIRECT_STATUS, ParamCheckStatus.SUCCESS);
        featureService.changeStatus(1L, firstFeature);
        featureService.changeStatus(1L, secondFeature);
    }

    @DisplayName("У чисто-ТВ магазина есть признак FORCE_ALIVE. Убираем фичу потребителя фидов, признак снимается")
    @DbUnitDataSet(
            before = "DirectFeatureListenerTest.testDisableVertical.before.csv",
            after = "DirectFeatureListenerTest.testDisableVertical.after.csv"
    )
    void testDisableVerticalFeature() {
        ShopFeature feature = ShopFeature.of(1001L, FeatureType.VERTICAL_SHARE, ParamCheckStatus.DONT_WANT);
        featureService.changeStatus(1L, feature);
    }

    @DisplayName("У директового магазина есть признак FORCE_ALIVE. Убираем фичу потребителя фидов, признак снимается")
    @DbUnitDataSet(
            before = "DirectFeatureListenerTest.testDisableDirect.before.csv",
            after = "DirectFeatureListenerTest.testDisableDirect.after.csv"
    )
    void testDisableDirectFeature() {
        ShopFeature firstFeature = ShopFeature.of(1001L, FeatureType.DIRECT_STANDBY, ParamCheckStatus.SUCCESS);
        ShopFeature secondFeature = ShopFeature.of(1001L, FeatureType.DIRECT_STATUS, ParamCheckStatus.DONT_WANT);
        featureService.changeStatus(1L, firstFeature);
        featureService.changeStatus(1L, secondFeature);
    }
}
